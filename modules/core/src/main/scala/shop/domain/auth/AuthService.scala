package shop.domain.auth

import dev.profunktor.auth.jwt.JwtToken
import shop.domain.auth.AuthPayload.{ Password, UserName }

class AuthService[F[_]](authRepo: AuthAlgebra[F]) {
  def newUser(username: UserName, password: Password): F[JwtToken] =
    authRepo.newUser(username, password)

  def login(username: UserName, password: Password): F[JwtToken] =
    authRepo.login(username, password)

  def logout(jwt: JwtToken, username: UserName): F[Unit] =
    authRepo.logout(jwt, username)

}
