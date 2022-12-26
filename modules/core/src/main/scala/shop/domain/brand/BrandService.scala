package shop.domain.brand

import shop.domain.brand.BrandPayload._

class BrandService[F[_]](brandRepo: BrandAlgebra[F]) {
  def findAll: F[List[Brand]]                  = brandRepo.findAll
  def create(brandName: BrandName): F[BrandId] = brandRepo.create(brandName)
}
