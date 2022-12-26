package shop.programs

import cats.data.NonEmptyList
import cats.effect._
import cats.implicits._
import org.scalacheck.Gen
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.noop.NoOpLogger
import retry.RetryDetails.{ GivingUp, WillDelayAndRetry }
import retry.RetryPolicies._
import retry.RetryPolicy
import shop.Generators._
import shop.domain.auth.AuthPayload.UserId
import shop.domain.cart.CartPayload.{ CartItem, CartTotal }
import shop.domain.cart.{ CartAlgebra, CartPayload }
import shop.domain.checkout.CheckoutPayload.Card
import shop.domain.item.ItemPayload
import shop.domain.order.OrderAlgebra
import shop.domain.order.OrderPayload._
import shop.domain.payment.PaymentPayload.Payment
import shop.domain.payment.{ PaymentAlgebra => PaymentClient, PaymentService }
import shop.effects.{ Background, TestBackground }
import shop.retries.{ Retry, TestRetry }
import squants.market.{ Money, USD }
import weaver.SimpleIOSuite
import weaver.scalacheck.Checkers

import scala.concurrent.duration._
import scala.util.control.NoStackTrace

object CheckoutSuite extends SimpleIOSuite with Checkers {
  val MaxRetries                   = 3
  val retryPolicy: RetryPolicy[IO] = limitRetries[IO](MaxRetries)

  def successfulClient(paymentId: PaymentId): PaymentService[IO] = new PaymentService[IO](new PaymentClient[IO] {
    override def process(payment: Payment): IO[PaymentId] =
      IO.pure(paymentId)
  })

  val unreachableClient: PaymentService[IO] = new PaymentService[IO](new PaymentClient[IO] {
    override def process(payment: Payment): IO[PaymentId] =
      IO.raiseError(PaymentError(""))
  })

  def recoveringClient(attemptsSoFar: Ref[IO, Int], paymentId: PaymentId): PaymentService[IO] =
    new PaymentService[IO](new PaymentClient[IO] {
      override def process(payment: Payment): IO[PaymentId] =
        attemptsSoFar.get.flatMap {
          case n if n === 1 => IO.pure(paymentId)
          case _            => attemptsSoFar.update(_ + 1) *> IO.raiseError(PaymentError(""))
        }
    })

  val failingOrders: OrderAlgebra[IO] = new TestOrderAlgebra {
    override def create(
        userId: UserId,
        paymentId: PaymentId,
        items: NonEmptyList[CartItem],
        total: Money
    ): IO[OrderId] = IO.raiseError(OrderError(""))
  }

  val emptyCart: CartAlgebra[IO] = new TestCartAlgebra {
    override def get(userId: UserId): IO[CartPayload.CartTotal] =
      IO.pure(CartTotal(List.empty, USD(0)))
  }

  def failingCart(cartTotal: CartTotal): CartAlgebra[IO] = new TestCartAlgebra {
    override def get(userId: UserId): IO[CartTotal] =
      IO.pure(cartTotal)

    override def delete(userId: UserId): IO[Unit] =
      IO.raiseError(new NoStackTrace {})
  }

  def successfulCart(cartTotal: CartTotal): CartAlgebra[IO] = new TestCartAlgebra {
    override def get(userId: UserId): IO[CartTotal] =
      IO.pure(cartTotal)

    override def delete(userId: UserId): IO[Unit] =
      IO.unit
  }

  def successfulOrders(orderId: OrderId): OrderAlgebra[IO] = new TestOrderAlgebra {
    override def create(
        userId: UserId,
        paymentId: PaymentId,
        items: NonEmptyList[CartItem],
        total: Money
    ): IO[OrderId] = IO.pure(orderId)
  }

  val gen: Gen[(UserId, PaymentId, OrderId, CartTotal, Card)] = for {
    uid <- userIdGen
    pid <- paymentIdGen
    oid <- orderIdGen
    crt <- cartTotalGen
    crd <- cardGen
  } yield (uid, pid, oid, crt, crd)

  implicit val bg: Background[IO]                = TestBackground.NoOp
  implicit val lg: SelfAwareStructuredLogger[IO] = NoOpLogger[IO]

  test("empty cart") {
    forall(gen) { case (uid, pid, oid, _, card) =>
      Checkout[IO](
        successfulClient(pid),
        emptyCart,
        successfulOrders(oid),
        retryPolicy
      ).process(uid, card)
        .attempt
        .map {
          case Left(EmptyCartError) => success
          case _                    => failure("Cart was not empty as expected")
        }
    }
  }

  test("unreachable payment client") {
    forall(gen) { case (uid, _, oid, ct, card) =>
      Ref.of[IO, Option[GivingUp]](None).flatMap { retries =>
        implicit val rh: Retry[IO] = TestRetry.givingUp(retries)

        Checkout[IO](
          unreachableClient,
          successfulCart(ct),
          successfulOrders(oid),
          retryPolicy
        ).process(uid, card)
          .attempt
          .flatMap {
            case Left(PaymentError(_)) =>
              retries.get.map {
                case Some(g) => expect.same(g.totalRetries, MaxRetries)
                case None    => failure("expected GivingUp")
              }
            case _ => IO.pure(failure("Expected payment error"))
          }
      }
    }
  }

  test("failing payment client succeeds after one retry") {
    forall(gen) { case (uid, pid, oid, ct, card) =>
      (
        Ref.of[IO, Option[WillDelayAndRetry]](None),
        Ref.of[IO, Int](0)
      ).tupled.flatMap { case (retries, cliRef) =>
        implicit val rh: Retry[IO] = TestRetry.recovering(retries)

        Checkout[IO](
          recoveringClient(cliRef, pid),
          successfulCart(ct),
          successfulOrders(oid),
          retryPolicy
        ).process(uid, card)
          .attempt
          .flatMap {
            case Right(id) =>
              retries.get.map {
                case Some(w) =>
                  expect.same(id, oid) |+| expect.same(0, w.retriesSoFar)
                case None => failure("Expected one retry")
              }
            case Left(_) => IO.pure(failure("Expected Payment Id"))
          }
      }
    }
  }

  test("cannot create order, run in the background") {
    forall(gen) { case (uid, pid, _, ct, card) =>
      (Ref.of[IO, (Int, FiniteDuration)](0 -> 0.seconds), Ref.of[IO, Option[GivingUp]](None)).tupled.flatMap {
        case (bgActions, retries) =>
          implicit val bg: Background[IO] = TestBackground.counter(bgActions)
          implicit val rh: Retry[IO]      = TestRetry.givingUp(retries)

          Checkout[IO](
            successfulClient(pid),
            successfulCart(ct),
            failingOrders,
            retryPolicy
          ).process(uid, card)
            .attempt
            .flatMap {
              case Left(OrderError(_)) =>
                (bgActions.get, retries.get).mapN {
                  case (c, Some(g)) =>
                    expect.same(c, 1 -> 1.hour) |+|
                      expect.same(g.totalRetries, MaxRetries)
                  case _ => failure(s"Expected $MaxRetries retries and reschedule")
                }
              case _ =>
                IO.pure(failure("Expected order error"))
            }
      }
    }
  }

  test("failing to delete cart does not affect checkout") {
    forall(gen) { case (uid, pid, oid, ct, card) =>
      Checkout[IO](
        successfulClient(pid),
        failingCart(ct),
        successfulOrders(oid),
        retryPolicy
      ).process(uid, card)
        .map(expect.same(oid, _))
    }
  }

  test("successful checkout") {
    forall(gen) { case (uid, pid, oid, ct, card) =>
      Checkout[IO](
        successfulClient(pid),
        successfulCart(ct),
        successfulOrders(oid),
        retryPolicy
      ).process(uid, card)
        .map(expect.same(oid, _))
    }
  }

}

protected class TestOrderAlgebra() extends OrderAlgebra[IO] {
  override def get(userId: UserId, orderId: OrderId): IO[Option[Order]] = ???

  override def findById(userId: UserId): IO[List[Order]] = ???

  override def create(userId: UserId, paymentId: PaymentId, items: NonEmptyList[CartItem], total: Money): IO[OrderId] =
    ???
}

protected class TestCartAlgebra() extends CartAlgebra[IO] {
  override def add(userId: UserId, itemId: ItemPayload.ItemId, quantity: CartPayload.Quantity): IO[Unit] = ???

  override def get(userId: UserId): IO[CartPayload.CartTotal] = ???

  override def delete(userId: UserId): IO[Unit] = ???

  override def removeItem(userId: UserId, itemId: ItemPayload.ItemId): IO[Unit] = ???

  override def update(userId: UserId, cart: CartPayload.Cart): IO[Unit] = ???
}
