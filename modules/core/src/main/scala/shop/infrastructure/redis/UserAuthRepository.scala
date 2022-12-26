package shop.infrastructure.redis

import cats.Functor
import cats.implicits._
import dev.profunktor.auth.jwt.JwtToken
import dev.profunktor.redis4cats.RedisCommands
import io.circe.parser.decode
import pdi.jwt.JwtClaim
import shop.domain.auth.UserAuthAlgebra
import shop.domain.auth.UserAuthPayload.CommonUser
import shop.domain.user.UserPayload.User

class UserAuthRepository[F[_]: Functor](
    redis: RedisCommands[F, String, String]
) extends UserAuthAlgebra[F, CommonUser] {
  override def findUser(jwtToken: JwtToken)(claim: JwtClaim): F[Option[CommonUser]] =
    redis
      .get(jwtToken.value)
      .map {
        _.flatMap { u =>
          decode[User](u).toOption.map(CommonUser.apply)
        }
      }
}
