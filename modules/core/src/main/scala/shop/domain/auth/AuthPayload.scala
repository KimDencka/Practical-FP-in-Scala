package shop.domain.auth

import derevo.cats._
import derevo.circe.magnolia.{ decoder, encoder }
import derevo.derive
import eu.timepit.refined.auto._
import eu.timepit.refined.types.string.NonEmptyString
import io.circe._
import io.estatico.newtype.macros.newtype
import shop.optics.uuid
import shop.http.utils.json._

import java.util.UUID
import javax.crypto.Cipher
import scala.util.control.NoStackTrace

object AuthPayload {
  @derive(decoder, encoder, eqv, show, uuid)
  @newtype case class UserId(value: UUID)
  object UserId {
    implicit val userIdEncoder: Encoder[UserId] =
      Encoder.forProduct1("user_id")(_.value)

    implicit val userIdDecoder: Decoder[UserId] =
      Decoder.forProduct1("user_id")(UserId.apply)
  }

  @derive(decoder, encoder, eqv, show)
  @newtype case class UserName(value: String)
  object UserName {
    implicit val userNameEncoder: Encoder[UserName] =
      Encoder.forProduct1("name")(_.value)

    implicit val userNameDecoder: Decoder[UserName] =
      Decoder.forProduct1("name")(UserName.apply)
  }

  @derive(decoder, encoder, eqv, show)
  @newtype case class Password(value: String)

  @derive(decoder, encoder, eqv, show)
  @newtype case class EncryptedPassword(value: String)

  @newtype case class EncryptCipher(value: Cipher)
  @newtype case class DecryptCipher(value: Cipher)

  // --------- user registration -----------

  @derive(decoder, encoder)
  @newtype case class UserNameParam(value: NonEmptyString) {
    def toDomain: UserName = UserName(value.toLowerCase)
  }

  @derive(decoder, encoder)
  @newtype case class PasswordParam(value: NonEmptyString) {
    def toDomain: Password = Password(value)
  }

  @derive(decoder, encoder)
  case class CreateUser(
      username: UserNameParam,
      password: PasswordParam
  )

  case class UserNotFound(user: UserName)        extends NoStackTrace
  case class UserNameInUse(user: UserName)       extends NoStackTrace
  case class InvalidPassword(username: UserName) extends NoStackTrace
  case object UnsupportedOperation               extends NoStackTrace
  case object TokenNotFound                      extends NoStackTrace

  // --------- user login -----------

  @derive(decoder, encoder)
  case class LoginUser(
      username: UserNameParam,
      password: PasswordParam
  )

  // --------- admin auth -----------
  @newtype case class ClaimContent(uuid: UUID)
}
