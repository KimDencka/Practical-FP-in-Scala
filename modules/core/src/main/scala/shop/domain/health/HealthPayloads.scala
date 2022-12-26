package shop.domain.health

import derevo.cats.eqv
import derevo.circe.magnolia.encoder
import derevo.derive
import io.estatico.newtype.macros._
import monocle.Iso
import shop.http.utils.json._ // DON'T REMOVE IT

object HealthPayloads {
  @derive(encoder)
  @newtype case class RedisStatus(value: Status)

  @derive(encoder)
  @newtype case class PostgresStatus(value: Status)

  @derive(encoder)
  case class AppStatus(
      redis: RedisStatus,
      postgres: PostgresStatus
  )

  @derive(eqv)
  sealed trait Status
  object Status {
    case object Okay        extends Status
    case object Unreachable extends Status

    val _Bool: Iso[Status, Boolean] =
      Iso[Status, Boolean] {
        case Okay        => true
        case Unreachable => false
      }(if (_) Okay else Unreachable)
  }

}
