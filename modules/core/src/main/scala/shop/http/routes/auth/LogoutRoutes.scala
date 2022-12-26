package shop.http.routes.auth

import cats.Monad
import cats.implicits._
import dev.profunktor.auth.AuthHeaders
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.server._
import shop.domain.auth.AuthService
import shop.domain.auth.UserAuthPayload.CommonUser

final case class LogoutRoutes[F[_]: Monad](auth: AuthService[F]) extends Http4sDsl[F] {
  private[routes] val prefixPath = "/auth"

  private val httpRoutes: AuthedRoutes[CommonUser, F] = AuthedRoutes.of { case ar @ POST -> Root / "logout" as user =>
    AuthHeaders
      .getBearerToken(ar.req)
      .traverse_(auth.logout(_, user.value.username)) *> NoContent()
  }

  def routes(authMiddleware: AuthMiddleware[F, CommonUser]): HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )
}
