package shop.domain.auth

import dev.profunktor.auth.jwt.JwtToken
import pdi.jwt.JwtClaim

class UserAuthService[F[_], A](userAuthRepo: UserAuthAlgebra[F, A]) {
  def findUser(jwtToken: JwtToken)(claim: JwtClaim): F[Option[A]] =
    userAuthRepo.findUser(jwtToken)(claim)
}
