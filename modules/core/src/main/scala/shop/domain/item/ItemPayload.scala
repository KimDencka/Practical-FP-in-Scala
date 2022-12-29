package shop.domain
package item

import derevo.cats._
import derevo.circe.magnolia._
import derevo.derive
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import eu.timepit.refined.cats._ // DON'T REMOVE IT
import eu.timepit.refined.string.{ Uuid, ValidBigDecimal }
import eu.timepit.refined.types.string.NonEmptyString
import io.circe._
import io.circe.refined._ // DON'T REMOVE IT
import io.estatico.newtype.macros.newtype
import shop.domain.brand.BrandPayload.{ Brand, BrandId }
import shop.domain.cart.CartPayload.{ CartItem, Quantity }
import shop.domain.category.CategoryPayload.{ Category, CategoryId }
import shop.http.utils.json._
import shop.optics.uuid
import squants.market.{ Money, USD }

import java.util.UUID

object ItemPayload {
  @derive(decoder, encoder, keyDecoder, keyEncoder, eqv, show, uuid)
  @newtype case class ItemId(value: UUID)
  object ItemId {
    implicit val itemIdKeyEncoder: KeyEncoder[ItemId] =
      (key: ItemId) => key.value.toString

    implicit val itemIdKeyDecoder: KeyDecoder[ItemId] =
      (key: String) => Some(ItemId(UUID.fromString(key)))

    implicit val itemIdEncoder: Encoder[ItemId] =
      Encoder.forProduct1("itemId")(_.value)

    implicit val itemIdDecoder: Decoder[ItemId] =
      Decoder.forProduct1("itemId")(ItemId.apply)
  }

  @derive(decoder, encoder, eqv, show)
  @newtype case class ItemName(value: String)
  object ItemName {
    implicit val itemNameEncoder: Encoder[ItemName] =
      Encoder.forProduct1("name")(_.value)

    implicit val itemNameDecoder: Decoder[ItemName] =
      Decoder.forProduct1("name")(ItemName.apply)
  }

  @derive(decoder, encoder, eqv, show)
  @newtype case class ItemDescription(value: String)
  object ItemDescription {
    implicit val itemDescEncoder: Encoder[ItemDescription] =
      Encoder.forProduct1("description")(_.value)

    implicit val itemDescDecoder: Decoder[ItemDescription] =
      Decoder.forProduct1("description")(ItemDescription.apply)
  }

  @derive(decoder, encoder, eqv, show)
  case class Item(
      itemId: ItemId,
      name: ItemName,
      description: ItemDescription,
      price: Money,
      brand: Brand,
      category: Category
  ) {
    def cart(q: Quantity): CartItem =
      CartItem(this, q)
  }
  // ----- Create item ------

  @derive(decoder, encoder, show)
  @newtype case class ItemNameParam(value: NonEmptyString)

  @derive(decoder, encoder, show)
  @newtype case class ItemDescriptionParam(value: NonEmptyString)

  @derive(decoder, encoder, show)
  @newtype case class PriceParam(value: String Refined ValidBigDecimal)

  @derive(decoder, encoder, show)
  case class CreateItemParam(
      name: ItemNameParam,
      description: ItemDescriptionParam,
      price: PriceParam,
      brandId: BrandId,
      categoryId: CategoryId
  ) {
    def toDomain: CreateItem =
      CreateItem(
        ItemName(name.value),
        ItemDescription(description.value),
        USD(BigDecimal(price.value)),
        brandId,
        categoryId
      )
  }

  case class CreateItem(
      name: ItemName,
      description: ItemDescription,
      price: Money,
      brandId: BrandId,
      categoryId: CategoryId
  )

  // ----- Update item ------

  @derive(decoder, encoder)
  @newtype case class ItemIdParam(value: String Refined Uuid)

  @derive(decoder, encoder)
  case class UpdateItemParam(
      id: ItemIdParam,
      price: PriceParam
  ) {
    def toDomain: UpdateItem =
      UpdateItem(
        ItemId(UUID.fromString(id.value)),
        USD(BigDecimal(price.value))
      )
  }

  @derive(decoder, encoder)
  case class UpdateItem(
      id: ItemId,
      price: Money
  )
}
