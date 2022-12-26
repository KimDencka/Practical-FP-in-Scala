package shop.http.routes.category

import cats.Monad
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import shop.domain.category.CategoryService
import shop.http.utils.json._

final case class CategoryRoutes[F[_]: Monad](category: CategoryService[F]) extends Http4sDsl[F] {
  private[routes] val prefixPath = "/categories"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root =>
    Ok(category.findAll())
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )
}
