package shop.http.routes.health

import cats.Monad
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import shop.domain.health.HealthService
import shop.http.utils.json._

final case class HealthRoutes[F[_]: Monad](health: HealthService[F]) extends Http4sDsl[F] {
  private[routes] val prefixPath = "/healthcheck"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] { case GET -> Root =>
    Ok(health.status)
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )
}
