package shop.domain.health

import shop.domain.health.HealthPayloads.AppStatus

class HealthService[F[_]](healthRepo: HealthAlgebra[F]) {
  def status: F[AppStatus] = healthRepo.status
}
