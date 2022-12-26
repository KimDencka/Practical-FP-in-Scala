package shop.domain.category

import shop.domain.category.CategoryPayload._

trait CategoryAlgebra[F[_]] {
  def findAll: F[List[Category]]
  def create(name: CategoryName): F[CategoryId]

}
