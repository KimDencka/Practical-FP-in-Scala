package shop.domain

import monocle.law.discipline.IsoTests
import org.scalacheck.{ Arbitrary, Cogen, Gen }
import shop.Generators._
import shop.domain.brand.BrandPayload.BrandId
import shop.domain.category.CategoryPayload.CategoryId
import shop.domain.health.HealthPayloads.Status
import shop.optics.IsUUID
import weaver.FunSuite
import weaver.discipline.Discipline

import java.util.UUID

object OpticsSuite extends FunSuite with Discipline {
  implicit val arbStatus: Arbitrary[Status] =
    Arbitrary(Gen.oneOf(Status.Okay, Status.Unreachable))

  implicit val uuidCogen: Cogen[UUID] =
    Cogen[(Long, Long)].contramap { uuid =>
      uuid.getLeastSignificantBits -> uuid.getMostSignificantBits
    }

  implicit val brandIdArb: Arbitrary[BrandId] =
    Arbitrary(brandIdGen)

  implicit val brandIdCogen: Cogen[BrandId] =
    Cogen[UUID].contramap[BrandId](_.value)

  implicit val catIdArb: Arbitrary[CategoryId] =
    Arbitrary(categoryIdGen)

  implicit val catIdCogen: Cogen[CategoryId] =
    Cogen[UUID].contramap[CategoryId](_.value)

  checkAll("Iso[Status._Bool]", IsoTests(Status._Bool))

  // we don't really need to test these as they are derived, just showing we can
  checkAll("IsUUID[UUID]", IsoTests(IsUUID[UUID]._UUID))
  checkAll("IsUUID[BrandId]", IsoTests(IsUUID[BrandId]._UUID))
  checkAll("IsUUID[CategoryId]", IsoTests(IsUUID[CategoryId]._UUID))

}
