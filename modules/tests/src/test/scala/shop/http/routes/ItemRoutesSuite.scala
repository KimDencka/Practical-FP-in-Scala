package shop.http.routes

import cats.effect._
import cats.implicits._
import org.http4s.Method._
import org.http4s._
import org.http4s.client.dsl.io._
import org.http4s.syntax.literals._
import org.scalacheck.Gen
import shop.Generators._
import suite.HttpSuite
import shop.domain.brand.BrandPayload.BrandName
import shop.domain.item.{ ItemAlgebra, ItemService }
import shop.domain.item.ItemPayload.{ CreateItem, Item, ItemId, UpdateItem }
import shop.effects.GenUUID
import shop.http.routes.item.ItemRoutes

object ItemRoutesSuite extends HttpSuite {
  def dataItems(items: List[Item]): ItemService[IO] =
    new ItemService[IO](new TestItemAlgebra {
      override def findAll: IO[List[Item]] =
        IO.pure(items)

      override def findByBrand(brandName: BrandName): IO[List[Item]] =
        IO.pure(items.find(_.brand.name === brandName).toList)
    })

  def failingItems(items: List[Item]): ItemService[IO] =
    new ItemService[IO](new TestItemAlgebra {
      override def findAll: IO[List[Item]] =
        IO.raiseError(DummyError) *> IO.pure(items)

      override def findByBrand(brandName: BrandName): IO[List[Item]] =
        findAll
    })

  test("GET items succeeds") {
    forall(Gen.listOf(itemGen)) { it =>
      val req    = GET(uri"/items")
      val routes = ItemRoutes[IO](dataItems(it)).routes
      expectHttpBodyAndStatus(routes, req)(it, Status.Ok)
    }
  }

  test("GET items by brand succeeds") {
    val gen = for {
      i <- Gen.listOf(itemGen)
      b <- brandGen
    } yield i -> b

    forall(gen) { case (it, b) =>
      val req      = GET(uri"/items".withQueryParam("brand", b.name.value))
      val routes   = ItemRoutes[IO](dataItems(it)).routes
      val expected = it.find(_.brand.name === b.name).toList
      expectHttpBodyAndStatus(routes, req)(expected, Status.Ok)
    }
  }

  test("GET items fails") {
    forall(Gen.listOf(itemGen)) { it =>
      val req    = GET(uri"/items")
      val routes = ItemRoutes[IO](failingItems(it)).routes
      expectHttpFailure(routes, req)
    }
  }
}

protected class TestItemAlgebra extends ItemAlgebra[IO] {
  override def findAll: IO[List[Item]]                           = IO.pure(List.empty)
  override def findByBrand(brandName: BrandName): IO[List[Item]] = IO.pure(List.empty)
  override def findById(itemId: ItemId): IO[Option[Item]]        = IO.pure(none[Item])
  override def create(item: CreateItem): IO[ItemId]              = GenUUID[IO].make[ItemId]
  override def update(item: UpdateItem): IO[Unit]                = IO.unit
}
