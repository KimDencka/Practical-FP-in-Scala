package shop.infrastructure.redis

import cats.Applicative
import cats.implicits._
import dev.profunktor.auth.jwt.JwtToken
import pdi.jwt.JwtClaim
import shop.domain.auth.UserAuthAlgebra
import shop.domain.auth.UserAuthPayload.AdminUser

import scala.tools.nsc.tasty.SafeEq

class AdminAuthRepository[F[_]: Applicative](
    adminToken: JwtToken,
    adminUser: AdminUser
) extends UserAuthAlgebra[F, AdminUser] {
  override def findUser(jwtToken: JwtToken)(claim: JwtClaim): F[Option[AdminUser]] =
    (jwtToken === adminToken)
      .guard[Option]
      .as(adminUser)
      .pure[F]
}
