package shop.http.routes.cart

import cats.Monad
import cats.implicits._
import org.http4s._
import org.http4s.circe.{ JsonDecoder, toMessageSyntax }
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{ AuthMiddleware, Router }
import shop.domain.auth.UserAuthPayload.CommonUser
import shop.domain.cart.CartPayload.Cart
import shop.domain.cart.CartService
import shop.domain.item.ItemPayload.ItemId
import shop.http.utils.json._

case class CartRoutes[F[_]: JsonDecoder: Monad](cart: CartService[F]) extends Http4sDsl[F] {
  private[routes] val prefixPath = "/cart"

  private val httpRoutes: AuthedRoutes[CommonUser, F] =
    AuthedRoutes.of {
      case GET -> Root as user => Ok(cart.get(user.value.userId))

      case ar @ POST -> Root as user =>
        ar.req.asJsonDecode[Cart].flatMap { c =>
          c.items
            .map { case (id, quantity) =>
              cart.add(user.value.userId, id, quantity)
            }
            .toList
            .sequence *> Created()
        }

      case ar @ PUT -> Root as user =>
        ar.req.asJsonDecode[Cart].flatMap { c =>
          cart.update(user.value.userId, c) *> Ok()
        }

      case DELETE -> Root / UUIDVar(itemId) as user =>
        cart.removeItem(user.value.userId, ItemId(itemId)) *> NoContent()
    }

  def routes(authMiddleware: AuthMiddleware[F, CommonUser]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )

}
