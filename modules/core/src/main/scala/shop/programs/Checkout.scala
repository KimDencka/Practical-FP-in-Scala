package shop.programs

import cats._
import cats.data.NonEmptyList
import cats.implicits._
import org.typelevel.log4cats.Logger
import retry.RetryPolicy
import shop.domain.auth.AuthPayload.UserId
import shop.domain.cart.CartAlgebra
import shop.domain.cart.CartPayload.{ CartItem, CartTotal }
import shop.domain.checkout.CheckoutPayload.Card
import shop.domain.order.OrderAlgebra
import shop.domain.order.OrderPayload._
import shop.domain.payment.PaymentPayload.Payment
import shop.domain.payment.PaymentService
import shop.effects.Background
import shop.retries.{ Retriable, Retry }
import squants.market.Money

import scala.concurrent.duration._

final case class Checkout[F[_]: Background: Logger: MonadThrow: Retry](
    payments: PaymentService[F],
    cart: CartAlgebra[F],
    order: OrderAlgebra[F],
    policy: RetryPolicy[F]
) {
  private def processPayment(in: Payment): F[PaymentId] =
    Retry[F]
      .retry(policy, Retriable.Payments)(payments.process(in))
      .adaptError { case e =>
        PaymentError(Option(e.getMessage).getOrElse("Unknown"))
      }

  private def createOrder(
      userId: UserId,
      paymentId: PaymentId,
      items: NonEmptyList[CartItem],
      total: Money
  ): F[OrderId] = {
    val action = Retry[F]
      .retry(policy, Retriable.Orders)(
        order.create(userId, paymentId, items, total)
      )
      .adaptError(e => OrderError(e.getMessage))

    def bgAction(fa: F[OrderId]): F[OrderId] =
      fa.onError { case _ =>
        Logger[F].error(
          s"Failed to create order for Payment: ${paymentId.show}. Rescheduling as a background action"
        ) *> Background[F].schedule(bgAction(fa), 1.hour)
      }

    bgAction(action)
  }

  private def ensureNonEmpty[A](xs: List[A]): F[NonEmptyList[A]] =
    MonadThrow[F].fromOption(
      NonEmptyList.fromList(xs),
      EmptyCartError
    )

  def process(userId: UserId, card: Card): F[OrderId] = {
    cart.get(userId).flatMap { case CartTotal(items, total) =>
      for {
        its <- ensureNonEmpty(items)
        pid <- processPayment(Payment(userId, total, card))
        oid <- createOrder(userId, pid, its, total)
        _   <- cart.delete(userId).attempt.void
      } yield oid
    }
  }
}
