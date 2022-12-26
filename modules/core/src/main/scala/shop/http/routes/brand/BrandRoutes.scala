package shop.http.routes.brand

import cats.Monad
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import shop.domain.brand.BrandService
import shop.http.utils.json._

final case class BrandRoutes[F[_]: Monad](brand: BrandService[F]) extends Http4sDsl[F] {
  private[routes] val prefixPath = "/brands"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root =>
    Ok(brand.findAll)
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )
}
