package shop.domain.user

import derevo.cats.show
import derevo.circe.magnolia.{ decoder, encoder }
import derevo.derive
import shop.domain.auth.AuthPayload.{ UserId, UserName }

object UserPayload {
  @derive(decoder, encoder, show)
  case class User(userId: UserId, username: UserName)
}
