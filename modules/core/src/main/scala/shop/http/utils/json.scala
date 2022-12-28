package shop.http.utils

import io.circe._
import io.circe.refined._
import io.estatico.newtype.Coercible
import org.http4s._
import org.http4s.circe._
import shop.domain.auth.AuthPayload.ClaimContent
import shop.domain.brand.BrandPayload._
import shop.domain.cart.CartPayload.Cart
import shop.domain.category.CategoryPayload.CategoryParam
import shop.domain.item.ItemPayload._

object json extends JsonCodecs {
  implicit def deriveEntityEncoder[F[_], A: Encoder]: EntityEncoder[F, A] = jsonEncoderOf[F, A]
}

trait JsonCodecs extends CoercibleCodecs {
  // --------------- ItemPayload ---------------
  implicit val itemNameParamDecoder: Decoder[ItemNameParam] =
    Decoder.forProduct1("name")(ItemNameParam.apply)

  implicit val itemDescriptionParamDecoder: Decoder[ItemDescriptionParam] =
    Decoder.forProduct1("desc")(ItemDescriptionParam.apply)

  implicit val priceParamDecoder: Decoder[PriceParam] =
    Decoder.forProduct1("price")(PriceParam.apply)

  implicit val itemIdParamDecoder: Decoder[ItemIdParam] =
    Decoder.forProduct1("item_id")(ItemIdParam.apply)

  // --------------- BrandPayload ---------------
  implicit val brandNameParamEncoder: Encoder[BrandNameParam] =
    Encoder.forProduct1("name")(_.value)

  implicit val brandNameParamDecoder: Decoder[BrandNameParam] =
    Decoder.forProduct1("name")(BrandNameParam.apply)

  // --------------- CartPayload ---------------
  implicit val cartEncoder: Encoder[Cart] =
    Encoder.forProduct1("items")(_.items)

  implicit val cartDecoder: Decoder[Cart] =
    Decoder.forProduct1("items")(Cart.apply)

  // --------------- AuthPayload ---------------
  implicit val claimContentDecoder: Decoder[ClaimContent] =
    Decoder.forProduct1("uuid")(ClaimContent.apply)

  // --------------- CategoryPayload ---------------
  implicit val categoryParamDecoder: Decoder[CategoryParam] =
    Decoder.forProduct1("name")(CategoryParam.apply)

}

trait CoercibleCodecs {
  implicit def coercibleEncoder[R, N](implicit
      ev: Coercible[Encoder[R], Encoder[N]],
      R: Encoder[R]
  ): Encoder[N] = ev(R)

  implicit def coercibleDecoder[R, N](implicit
      ev: Coercible[Decoder[R], Decoder[N]],
      R: Decoder[R]
  ): Decoder[N] = ev(R)

  implicit def coercibleKeyEncoder[R, N](implicit
      ev: Coercible[KeyEncoder[R], KeyEncoder[N]],
      R: KeyEncoder[R]
  ): KeyEncoder[N] = ev(R)

  implicit def coercibleKeyDecoder[R, N](implicit
      ev: Coercible[KeyDecoder[R], KeyDecoder[N]],
      R: KeyDecoder[R]
  ): KeyDecoder[N] = ev(R)
}
