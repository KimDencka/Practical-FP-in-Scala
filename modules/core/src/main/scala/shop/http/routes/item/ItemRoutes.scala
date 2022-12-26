package shop.http.routes.item

import cats.Monad
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import shop.domain.brand.BrandPayload._
import shop.domain.item.ItemService
import shop.http.utils.json._
import shop.http.utils.params._

final case class ItemRoutes[F[_]: Monad](item: ItemService[F]) extends Http4sDsl[F] {
  private[routes] val prefixPath = "/items"

  object BrandNameQueryParam extends OptionalQueryParamDecoderMatcher[BrandNameParam]("brand")

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root :? BrandNameQueryParam(brand) =>
    Ok(brand.fold(item.findAll)(b => item.findByBrand(b.toDomain)))
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )
}
