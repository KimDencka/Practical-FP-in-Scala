package shop.http.routes

import cats.effect._
import org.http4s._
import org.http4s.dsl.io.GET
import org.http4s.client.dsl.io._
import org.http4s.implicits._
import org.scalacheck.Gen
import suite.HttpSuite
import shop.Generators.brandGen
import shop.domain.brand.{ BrandAlgebra, BrandService }
import shop.domain.brand.BrandPayload.{ Brand, BrandId, BrandName }
import shop.effects.GenUUID
import shop.http.routes.brand.BrandRoutes

object BrandRoutesSuite extends HttpSuite {

  def dataBrands(brands: List[Brand]): BrandService[IO] =
    new BrandService[IO](new TestBrandAlgebra {
      override def findAll: IO[List[Brand]] = IO.pure(brands)
    })

  def failingBrands(brands: List[Brand]): BrandService[IO] =
    new BrandService[IO](new TestBrandAlgebra {
      override def findAll: IO[List[Brand]] =
        IO.raiseError(DummyError) *> IO.pure(brands)
    })

  test("GET brands succeeds") {
    forall(Gen.listOf(brandGen)) { b =>
      val req    = GET(uri"/brands")
      val routes = BrandRoutes[IO](dataBrands(b)).routes
      expectHttpBodyAndStatus(routes, req)(b, Status.Ok)
    }
  }

  test("GET brands fails") {
    forall(Gen.listOf(brandGen)) { b =>
      val req    = GET(uri"/brands")
      val routes = BrandRoutes[IO](failingBrands(b)).routes
      expectHttpFailure(routes, req)
    }
  }

}

protected class TestBrandAlgebra extends BrandAlgebra[IO] {
  override def findAll: IO[List[Brand]] = IO.pure(List.empty)

  override def create(name: BrandName): IO[BrandId] = GenUUID[IO].make[BrandId]
}
