package shop.domain.user

import shop.domain.auth.AuthPayload._
import shop.domain.auth.UserAuthPayload.UserWithPassword

trait UserAlgebra[F[_]] {
  def find(username: UserName): F[Option[UserWithPassword]]
  def create(username: UserName, password: EncryptedPassword): F[UserId]
}
