package shop.http.routes.admin

import cats.MonadThrow
import cats.implicits._
import io.circe.JsonObject
import io.circe.syntax.EncoderOps
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{ AuthMiddleware, Router }
import org.http4s.{ AuthedRoutes, HttpRoutes }
import shop.domain.auth.UserAuthPayload.AdminUser
import shop.domain.item.ItemPayload.CreateItemParam
import shop.domain.item.ItemService
import shop.ext.http4s.refined._
import shop.http.utils.json._

final case class AdminItemRoutes[F[_]: JsonDecoder: MonadThrow](item: ItemService[F]) extends Http4sDsl[F] {
  private[admin] val prefixPath = "/items"

  private val httpRoutes: AuthedRoutes[AdminUser, F] =
    AuthedRoutes.of { case ar @ POST -> Root as _ =>
      ar.req.decodeR[CreateItemParam] { i =>
        item.create(i.toDomain).flatMap { id =>
          Created(JsonObject.singleton("item_id", id.asJson))
        }
      }
    }

  def routes(authMiddleware: AuthMiddleware[F, AdminUser]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )
}
