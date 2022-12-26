package shop.http.routes.order

import cats._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{ AuthMiddleware, Router }
import org.http4s.{ AuthedRoutes, HttpRoutes }
import shop.domain.auth.UserAuthPayload.CommonUser
import shop.domain.order.OrderPayload.OrderId
import shop.domain.order.OrderService
import shop.http.utils.json._

final case class OrderRoutes[F[_]: Monad](order: OrderService[F]) extends Http4sDsl[F] {
  private[routes] val prefixPath = "/orders"

  private val httpRoutes: AuthedRoutes[CommonUser, F] =
    AuthedRoutes.of {
      case GET -> Root as user =>
        Ok(order.findById(user.value.userId))

      case GET -> Root / UUIDVar(orderId) as user =>
        Ok(order.get(user.value.userId, OrderId(orderId)))
    }

  def routes(authMiddleware: AuthMiddleware[F, CommonUser]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )
}
