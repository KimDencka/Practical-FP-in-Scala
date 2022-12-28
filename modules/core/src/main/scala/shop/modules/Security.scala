package shop.modules

import cats.ApplicativeThrow
import cats.effect._
import cats.implicits._
import dev.profunktor.auth.jwt._
import dev.profunktor.redis4cats.RedisCommands
import eu.timepit.refined.auto._
import io.circe.parser.{ decode => jsonDecode }
import pdi.jwt._
import shop.auth.{ Crypto, JwtExpire, Tokens }
import skunk.Session
import shop.config.types.AppConfig
import shop.domain.auth.AuthPayload._
import shop.domain.user.UserPayload._
import shop.domain.auth.UserAuthPayload._
import shop.domain.auth._
import shop.http.utils.json._
import shop.infrastructure.postgres.UserRepository
import shop.infrastructure.redis._

object Security {
  def make[F[_]: Sync](
      cfg: AppConfig,
      postgres: Resource[F, Session[F]],
      redis: RedisCommands[F, String, String]
  ): F[Security[F]] = {
    val adminJwtAuth: AdminJwtAuth =
      AdminJwtAuth(
        JwtAuth.hmac(
          cfg.adminJwtConfig.secretKey.value.secret,
          JwtAlgorithm.HS256
        )
      )

    val userJwtAuth: UserJwtAuth =
      UserJwtAuth(
        JwtAuth.hmac(
          cfg.tokenConfig.value.secret,
          JwtAlgorithm.HS256
        )
      )

    val adminToken = JwtToken(cfg.adminJwtConfig.adminToken.value.secret)

    for {
      adminClaim <- jwtDecode[F](adminToken, adminJwtAuth.value)
      content    <- ApplicativeThrow[F].fromEither(jsonDecode[ClaimContent](adminClaim.content))
      adminUser = AdminUser(User(UserId(content.uuid), UserName("admin")))
      tokens <- JwtExpire.make[F].map(Tokens.make[F](_, cfg.tokenConfig.value, cfg.tokenExpiration))
      crypto <- Crypto.make[F](cfg.passwordSalt.value)
      userRepo     = new UserRepository[F](postgres)
      authRepo     = new AuthRepository[F](cfg.tokenExpiration, tokens, userRepo, redis, crypto)
      authService  = new AuthService[F](authRepo)
      adminAuth    = new AdminAuthRepository[F](adminToken, adminUser)
      adminAuthSrv = new UserAuthService[F, AdminUser](adminAuth)
      userAuth     = new UserAuthRepository[F](redis)
      userAuthSrv  = new UserAuthService[F, CommonUser](userAuth)
    } yield new Security[F](authService, adminAuthSrv, userAuthSrv, adminJwtAuth, userJwtAuth) {}
  }
}

sealed class Security[F[_]] private (
    val authRepo: AuthService[F],
    val adminAuthRepo: UserAuthService[F, AdminUser],
    val userAuthRepo: UserAuthService[F, CommonUser],
    val adminJwtAuth: AdminJwtAuth,
    val userJwtAuth: UserJwtAuth
)
