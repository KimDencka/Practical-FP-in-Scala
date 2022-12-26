package shop.domain.auth

import derevo.cats.show
import derevo.circe.magnolia.{ decoder, encoder }
import derevo.derive
import dev.profunktor.auth.jwt.JwtSymmetricAuth
import io.estatico.newtype.macros.newtype
import shop.domain.auth.AuthPayload.{ EncryptedPassword, UserId, UserName }
import shop.domain.user.UserPayload.User

object UserAuthPayload {
  @newtype case class AdminJwtAuth(value: JwtSymmetricAuth)
  @newtype case class UserJwtAuth(value: JwtSymmetricAuth)

  @derive(decoder, encoder)
  case class UserWithPassword(id: UserId, name: UserName, password: EncryptedPassword)

  @derive(show)
  @newtype
  case class CommonUser(value: User)

  @derive(show)
  @newtype
  case class AdminUser(value: User)
}
