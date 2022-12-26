package shop.domain.brand

import shop.domain.brand.BrandPayload._

trait BrandAlgebra[F[_]] {
  def findAll: F[List[Brand]]
  def create(name: BrandName): F[BrandId]
}
