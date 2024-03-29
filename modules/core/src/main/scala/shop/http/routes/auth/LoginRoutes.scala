package shop.http.routes.auth

import cats._
import cats.implicits._
import org.http4s._
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import shop.domain._
import shop.domain.auth.AuthPayload._
import shop.domain.auth._
import shop.ext.http4s.refined._

final case class LoginRoutes[F[_]: JsonDecoder: MonadThrow](auth: AuthService[F]) extends Http4sDsl[F] {
  private[routes] val prefixPath = "/auth"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] { case req @ POST -> Root / "login" =>
    req.decodeR[LoginUser] { user =>
      auth
        .login(user.username.toDomain, user.password.toDomain)
        .flatMap(Ok(_))
        .recoverWith {
          case UserNotFound(u)    => Forbidden(s"User '${u.value.toLowerCase().capitalize}' not found.")
          case InvalidPassword(u) => Forbidden(s"Invalid password for user '${u.value.toLowerCase().capitalize}'")
        }
    }
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )
}
