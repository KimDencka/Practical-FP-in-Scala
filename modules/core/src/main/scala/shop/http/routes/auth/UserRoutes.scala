package shop.http.routes.auth

import cats.MonadThrow
import cats.implicits._
import org.http4s.HttpRoutes
import org.http4s.circe.CirceEntityEncoder.circeEntityEncoder
import org.http4s.circe.JsonDecoder
import org.http4s.dsl.Http4sDsl
import org.http4s.server.Router
import shop.domain._
import shop.domain.auth.AuthPayload.{ CreateUser, UserNameInUse }
import shop.domain.auth.AuthService
import shop.ext.http4s.refined._

final case class UserRoutes[F[_]: JsonDecoder: MonadThrow](auth: AuthService[F]) extends Http4sDsl[F] {
  private[routes] val prefixPath = "/auth"

  private val httpRoutes: HttpRoutes[F] = HttpRoutes.of[F] { case req @ POST -> Root / "users" =>
    req.decodeR[CreateUser] { user =>
      auth
        .newUser(user.username.toDomain, user.password.toDomain)
        .flatMap(Created(_))
        .recoverWith { case UserNameInUse(u) =>
          Conflict(u.show)
        }
    }
  }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> httpRoutes
  )
}
