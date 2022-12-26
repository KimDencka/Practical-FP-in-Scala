package shop.domain.category

import shop.domain.category.CategoryPayload._

class CategoryService[F[_]](categoryRepo: CategoryAlgebra[F]) {
  def findAll(): F[List[Category]] = categoryRepo.findAll
  def create(categoryName: CategoryName): F[CategoryId] =
    categoryRepo.create(categoryName)
}
