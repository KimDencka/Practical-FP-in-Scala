package shop.domain.health

import HealthPayloads.AppStatus

trait HealthAlgebra[F[_]] {
  def status: F[AppStatus]
}
