package shop.domain.category

import derevo.cats._
import derevo.circe.magnolia.{ decoder, encoder }
import derevo.derive
import eu.timepit.refined.auto._
import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype
import shop.optics.uuid

import java.util.UUID

object CategoryPayload {
  @derive(decoder, encoder, eqv, show, uuid)
  @newtype case class CategoryId(value: UUID)

  @derive(decoder, encoder, eqv, show)
  @newtype case class CategoryName(value: String)

  @newtype
  case class CategoryParam(value: NonEmptyString) {
    def toDomain: CategoryName = CategoryName(value.toLowerCase)
  }

  @derive(decoder, encoder, eqv, show)
  case class Category(categoryId: CategoryId, name: CategoryName)
}
