package shop.http.routes.admin

import cats.data.Kleisli
import cats.effect._
import io.circe._
import io.circe.refined._
import io.circe.syntax._
import org.http4s.Method._
import org.http4s._
import org.http4s.client.dsl.io._
import org.http4s.server.AuthMiddleware
import org.http4s.syntax.literals._
import shop.Generators._
import shop.domain.auth.UserAuthPayload._
import shop.domain.brand.BrandPayload._
import shop.domain.brand.{ BrandAlgebra, BrandService }
import shop.domain.item.{ ItemAlgebra, ItemService }
import shop.domain.item.ItemPayload._
import shop.http.utils.json._
import suite.HttpSuite

object AdminRoutesSuite extends HttpSuite {
  def authMiddleware(authUser: AdminUser): AuthMiddleware[IO, AdminUser] =
    AuthMiddleware(Kleisli.pure(authUser))

  test("POST create brand") {
    val gen = for {
      i <- brandIdGen
      u <- adminUserGen
      b <- brandParamGen
    } yield (i, u, b)

    forall(gen) { case (id, user, brand) =>
      val req          = POST(brand, uri"/brands")
      val brandService = new BrandService[IO](new TestBrandAlgebra(id))
      val routes       = AdminBrandRoutes[IO](brandService).routes(authMiddleware(user))
      val expected     = JsonObject.singleton("brand_id", id.asJson).asJson
      expectHttpBodyAndStatus(routes, req)(expected, Status.Created)
    }
  }

  test("POST create item") {
    val gen = for {
      i <- itemIdGen
      u <- adminUserGen
      c <- createItemParamGen
    } yield (i, u, c)

    forall(gen) { case (id, user, item) =>
      val req          = POST(item, uri"/items")
      val brandService = new ItemService[IO](new TestItemAlgebra(id))
      val routes       = AdminItemRoutes[IO](brandService).routes(authMiddleware(user))
      val expected     = JsonObject.singleton("item_id", id.asJson).asJson
      expectHttpBodyAndStatus(routes, req)(expected, Status.Created)
    }
  }
}

protected class TestBrandAlgebra(brandId: BrandId) extends BrandAlgebra[IO] {
  override def findAll: IO[List[Brand]]             = ???
  override def create(name: BrandName): IO[BrandId] = IO.pure(brandId)
}

protected class TestItemAlgebra(itemId: ItemId) extends ItemAlgebra[IO] {
  override def findAll: IO[List[Item]]                           = ???
  override def findByBrand(brandName: BrandName): IO[List[Item]] = ???
  override def findById(itemId: ItemId): IO[Option[Item]]        = ???
  override def create(item: CreateItem): IO[ItemId]              = IO.pure(itemId)
  override def update(item: UpdateItem): IO[Unit]                = IO.unit
}
