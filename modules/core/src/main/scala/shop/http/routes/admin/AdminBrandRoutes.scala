package shop.http.routes.admin

import cats._
import cats.implicits._
import io.circe.JsonObject
import io.circe.syntax._
import io.circe.refined._
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{ AuthMiddleware, Router }
import org.http4s.{ AuthedRoutes, HttpRoutes }
import shop.domain.auth.UserAuthPayload.AdminUser
import shop.domain.brand.BrandPayload.BrandNameParam
import shop.domain.brand.BrandService
import shop.ext.http4s.refined._
import shop.http.utils.json._

final case class AdminBrandRoutes[F[_]: JsonDecoder: MonadThrow](brand: BrandService[F]) extends Http4sDsl[F] {
  private[admin] val prefixPath = "/brands"

  private val httpRoutes: AuthedRoutes[AdminUser, F] =
    AuthedRoutes.of { case ar @ POST -> Root as _ =>
      ar.req.decodeR[BrandNameParam] { bp =>
        brand.create(bp.toDomain).flatMap { id =>
          Created(JsonObject.singleton("brand_id", id.asJson))
        }
      }
    }

  def routes(authMiddleware: AuthMiddleware[F, AdminUser]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )
}
