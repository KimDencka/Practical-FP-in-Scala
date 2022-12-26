package shop.domain.user

import shop.domain.auth.AuthPayload.{ EncryptedPassword, Password, UserId, UserName }
import shop.domain.auth.UserAuthPayload.UserWithPassword

class UserService[F[_]](userRepo: UserAlgebra[F]) {
  def find(username: UserName): F[Option[UserWithPassword]] =
    userRepo.find(username)

  def create(username: UserName, password: Password): F[UserId] =
    userRepo.create(username, EncryptedPassword(password.value))
}
