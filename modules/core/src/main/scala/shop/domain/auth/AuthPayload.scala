package shop.domain.auth

import derevo.cats._
import derevo.circe.magnolia.{ decoder, encoder }
import derevo.derive
import eu.timepit.refined.auto._
import eu.timepit.refined.types.string.NonEmptyString
import io.circe._
import io.circe.refined._
import io.estatico.newtype.macros.newtype
import shop.optics.uuid
import shop.http.utils.json._

import java.util.UUID
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
      Encoder.forProduct1("username")(_.value)

    implicit val userNameDecoder: Decoder[UserName] =
      Decoder.forProduct1("username")(UserName.apply)
  }

  @derive(decoder, encoder, eqv, show)
  @newtype case class Password(value: String)
  object Password {
    implicit val passwordEncoder: Encoder[Password] =
      Encoder.forProduct1("password")(_.value)

    implicit val passwordDecoder: Decoder[Password] =
      Decoder.forProduct1("password")(Password.apply)
  }

  @derive(decoder, encoder, eqv, show)
  @newtype case class EncryptedPassword(value: String)
  object EncryptedPassword {
    implicit val encryptedPasswordEncoder: Encoder[EncryptedPassword] =
      Encoder.forProduct1("password")(_.value)

    implicit val encryptedPasswordDecoder: Decoder[EncryptedPassword] =
      Decoder.forProduct1("password")(EncryptedPassword.apply)
  }

  // --------- user registration -----------

  @derive(decoder, encoder)
  @newtype case class UserNameParam(value: NonEmptyString) {
    def toDomain: UserName = UserName(value.toLowerCase)
  }
  object UserNameParam {
    implicit val userNameParamEncoder: Encoder[UserNameParam] =
      Encoder.forProduct1("username")(_.value)

    implicit val userNameParamDecoder: Decoder[UserNameParam] =
      Decoder.forProduct1("username")(UserNameParam.apply)
  }

  @derive(decoder, encoder)
  @newtype case class PasswordParam(value: NonEmptyString) {
    def toDomain: Password = Password(value)
  }
  object PasswordParam {
    implicit val passwordParamEncoder: Encoder[PasswordParam] =
      Encoder.forProduct1("password")(_.value)

    implicit val passwordParamDecoder: Decoder[PasswordParam] =
      Decoder.forProduct1("password")(PasswordParam.apply)
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
