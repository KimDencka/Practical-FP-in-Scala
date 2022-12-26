package shop.http.routes.secured

import cats.data.Kleisli
import cats.effect._
import org.http4s.Method._
import org.http4s._
import org.http4s.client.dsl.io._
import org.http4s.server.AuthMiddleware
import org.http4s.syntax.literals._
import squants.market.USD
import suite._
import shop.Generators._
import shop.domain.auth.AuthPayload.UserId
import shop.domain.auth.UserAuthPayload.CommonUser
import shop.domain.cart.CartPayload._
import shop.domain.cart._
import shop.domain.item.ItemPayload.ItemId
import shop.http.routes.cart.CartRoutes
import shop.http.utils.json._

object CartRoutesSuite extends HttpSuite {
  def authMiddleware(authUser: CommonUser): AuthMiddleware[IO, CommonUser] =
    AuthMiddleware(Kleisli.pure(authUser))

  def dataCart(cartTotal: CartTotal): CartService[IO] =
    new CartService[IO](new TestCartAlgebra {
      override def get(userId: UserId): IO[CartTotal] = IO.pure(cartTotal)
    })

  test("GET shopping cart succeeds") {
    val gen = for {
      u <- commonUserGen
      c <- cartTotalGen
    } yield u -> c

    forall(gen) { case (user, ct) =>
      val req    = GET(uri"/cart")
      val routes = CartRoutes[IO](dataCart(ct)).routes(authMiddleware(user))
      expectHttpBodyAndStatus(routes, req)(ct, Status.Ok)
    }
  }

  test("POST add item to shopping cart succeeds") {
    val gen = for {
      u <- commonUserGen
      c <- cartGen
    } yield u -> c

    forall(gen) { case (user, c) =>
      val req         = POST(c, uri"/cart")
      val cartService = new CartService[IO](new TestCartAlgebra)
      val routes      = CartRoutes[IO](cartService).routes(authMiddleware(user))
      expectHttpStatus(routes, req)(Status.Created)
    }
  }
}

protected class TestCartAlgebra extends CartAlgebra[IO] {
  override def add(userId: UserId, itemId: ItemId, quantity: Quantity): IO[Unit] = IO.unit
  override def get(userId: UserId): IO[CartTotal] =
    IO.pure(CartTotal(List.empty, USD(0)))

  override def delete(userId: UserId): IO[Unit]                     = IO.unit
  override def removeItem(userId: UserId, itemId: ItemId): IO[Unit] = IO.unit
  override def update(userId: UserId, cart: Cart): IO[Unit]         = IO.unit
}
