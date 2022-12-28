package shop.infrastructure.redis

import cats._
import cats.syntax.all._
import dev.profunktor.auth.jwt.JwtToken
import dev.profunktor.redis4cats.RedisCommands
import io.circe.syntax._
import shop.auth.{Crypto, Tokens}
import shop.config.types.TokenExpiration
import shop.domain.auth.AuthAlgebra
import shop.domain.auth.AuthPayload._
import shop.domain.auth.AuthPayload.UserName._
import shop.domain.auth.UserAuthPayload.UserWithPassword.userWPEncoder
import shop.domain.user.UserPayload.User
import shop.domain.user.UserAlgebra
import shop.domain.user.UserPayload.User.userEncoder

import scala.concurrent.duration.FiniteDuration

class AuthRepository[F[_]: MonadThrow](
    tokenExpiration: TokenExpiration,
    tokens: Tokens[F],
    userRepo: UserAlgebra[F],
    redis: RedisCommands[F, String, String],
    crypto: Crypto
) extends AuthAlgebra[F] {
  private val TokenExpiration: FiniteDuration = tokenExpiration.value

  override def newUser(username: UserName, password: Password): F[JwtToken] =
    userRepo.find(username).flatMap {
      case Some(_) => UserNameInUse(username).raiseError[F, JwtToken]
      case None =>
        for {
          i <- userRepo.create(username, crypto.encrypt(password))
          t <- tokens.create
          u = User(i, username).asJson.noSpaces
          _ <- redis.setEx(t.value, u, TokenExpiration)
          _ <- redis.setEx(username.show, t.value, TokenExpiration)
        } yield t

    }

  override def login(username: UserName, password: Password): F[JwtToken] =
    userRepo.find(username).flatMap {
      case None => UserNotFound(username).raiseError[F, JwtToken]
      case Some(user) if user.password =!= crypto.encrypt(password) =>
        InvalidPassword(user.name).raiseError[F, JwtToken]
      case Some(user) =>
        redis.get(username.show).flatMap {
          case Some(t) => JwtToken(t).pure[F]
          case None =>
            tokens.create.flatTap { t =>
              redis.setEx(t.value, user.asJson.noSpaces, TokenExpiration) *>
                redis.setEx(username.show, t.value, TokenExpiration)
            }
        }
    }

  def logout(token: JwtToken, username: UserName): F[Unit] =
    redis.del(token.value) *> redis.del(username.show).void
}
