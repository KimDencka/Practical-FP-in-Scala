package shop.domain.auth

import derevo.cats.show
import derevo.circe.magnolia.{ decoder, encoder }
import derevo.derive
import dev.profunktor.auth.jwt.JwtSymmetricAuth
import io.circe._
import io.estatico.newtype.macros.newtype
import shop.domain.auth.AuthPayload._
import shop.domain.user.UserPayload.User
import shop.http.utils.json._

object UserAuthPayload {
  @newtype case class AdminJwtAuth(value: JwtSymmetricAuth)
  @newtype case class UserJwtAuth(value: JwtSymmetricAuth)

  @derive(decoder, encoder)
  case class UserWithPassword(id: UserId, name: UserName, password: EncryptedPassword)
  object UserWithPassword {
    implicit val userWPEncoder: Encoder[UserWithPassword] =
      Encoder.forProduct3("id", "name", "password")(u => (u.id, u.name, u.password))

    implicit val userWPDecoder: Decoder[UserWithPassword] =
      Decoder.forProduct3("id", "name", "password")(UserWithPassword.apply)
  }

  @derive(show)
  @newtype case class CommonUser(value: User)

  @derive(show)
  @newtype case class AdminUser(value: User)
}
