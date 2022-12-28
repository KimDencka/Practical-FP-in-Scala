package shop.domain.user

import derevo.cats.show
import derevo.circe.magnolia.{ decoder, encoder }
import derevo.derive
import io.circe._
import shop.domain.auth.AuthPayload.{ UserId, UserName }
import shop.http.utils.json._

object UserPayload {
  @derive(decoder, encoder, show)
  case class User(userId: UserId, username: UserName)
  object User {
    implicit val userEncoder: Encoder[User] =
      Encoder.forProduct2("user_id", "username")(u => (u.userId, u.username))

    implicit val userDecoder: Decoder[User] =
      Decoder.forProduct2("user_id", "username")(User.apply)
  }
}
