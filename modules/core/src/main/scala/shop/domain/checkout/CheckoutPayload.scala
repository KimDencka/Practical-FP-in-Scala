package shop.domain.checkout

import derevo.cats._
import derevo.circe.magnolia.{ decoder, encoder }
import derevo.derive
import eu.timepit.refined.api._
import eu.timepit.refined.cats._ // DON'T REMOVE IT
import eu.timepit.refined.boolean._
import eu.timepit.refined.collection._
import eu.timepit.refined.string.{ MatchesRegex, ValidInt }
import io.estatico.newtype.macros.newtype
import shop.http.utils.json._ // DON'T REMOVE IT

object CheckoutPayload {
  type Rgx                = "^[a-zA-Z]+(([',. -][a-zA-Z ])?[a-zA-Z]*)*$"
  type CardNamePred       = String Refined MatchesRegex[Rgx]
  type CardNumberPred     = Long Refined Size[16]
  type CardExpirationPred = String Refined (Size[4] And ValidInt)
  type CardCVVPred        = Int Refined Size[3]

  @derive(decoder, encoder, show)
  @newtype case class CardName(value: CardNamePred)

  @derive(decoder, encoder, show)
  @newtype case class CardNumber(value: CardNumberPred)

  @derive(decoder, encoder, show)
  @newtype case class CardExpiration(value: CardExpirationPred)

  @derive(decoder, encoder, show)
  @newtype case class CardCVV(value: CardCVVPred)

  @derive(decoder, encoder, show)
  case class Card(
      name: CardName,
      number: CardNumber,
      expiration: CardExpiration,
      cvv: CardCVV
  )
}
