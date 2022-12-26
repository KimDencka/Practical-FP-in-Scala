package shop.domain.auth

import dev.profunktor.auth.jwt.JwtToken
import shop.domain.auth.AuthPayload.{ Password, UserName }

trait AuthAlgebra[F[_]] {
  def newUser(username: UserName, password: Password): F[JwtToken]
  def login(username: UserName, password: Password): F[JwtToken]
  def logout(token: JwtToken, username: UserName): F[Unit]
}
