package shop.domain.brand

import derevo.cats._
import derevo.circe.magnolia.{ decoder, encoder }
import derevo.derive
import eu.timepit.refined.auto._
import eu.timepit.refined.cats._ // DON'T REMOVE IT
import eu.timepit.refined.types.string.NonEmptyString
import io.estatico.newtype.macros.newtype
import shop.ext.http4s.queryParam
import shop.ext.http4s.refined._ // DON'T REMOVE IT
import shop.http.utils.json._    // DON'T REMOVE IT
import shop.optics.uuid

import java.util.UUID
import scala.util.control.NoStackTrace

object BrandPayload {
  @derive(decoder, encoder, eqv, show, uuid)
  @newtype case class BrandId(value: UUID)

  @derive(decoder, encoder, eqv, show)
  @newtype case class BrandName(value: String) {
    def toBrand(brandId: BrandId): Brand =
      Brand(brandId, this)
  }

  @derive(queryParam, show)
  @newtype case class BrandNameParam(value: NonEmptyString) {
    def toDomain: BrandName = BrandName(value.toLowerCase.capitalize)
  }

  @derive(decoder, encoder, eqv, show)
  case class Brand(brandId: BrandId, name: BrandName)

  @derive(decoder, encoder)
  case class InvalidBrand(value: String) extends NoStackTrace
}
