package shop.http.routes.checkout

import cats._
import cats.implicits._
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{ AuthMiddleware, Router }
import org.http4s.{ AuthedRoutes, HttpRoutes }
import shop.domain.auth.UserAuthPayload.CommonUser
import shop.domain.cart.CartPayload.CartNotFound
import shop.domain.checkout.CheckoutPayload.Card
import shop.domain.order.OrderPayload.{ EmptyCartError, OrderOrPaymentError }
import shop.ext.http4s.refined.RefinedRequestDecoder
import shop.http.utils.json._
import shop.programs.Checkout

case class CheckoutRoutes[F[_]: JsonDecoder: MonadThrow](checkout: Checkout[F]) extends Http4sDsl[F] {
  private[routes] val prefixPath = "/checkout"

  private val httpRoutes: AuthedRoutes[CommonUser, F] =
    AuthedRoutes.of { case ar @ POST -> Root as user =>
      ar.req.decodeR[Card] { card =>
        checkout
          .process(user.value.userId, card)
          .flatMap(Created(_))
          .recoverWith {
            case CartNotFound(userId)   => NotFound(s"Cart not found for user: ${userId.value}")
            case EmptyCartError         => BadRequest("Shopping cart is empty!")
            case e: OrderOrPaymentError => BadRequest(e.show)
          }
      }
    }

  def routes(authMiddleware: AuthMiddleware[F, CommonUser]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )

}
