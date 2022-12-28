package shop.domain.health

import derevo.circe.magnolia.encoder
import derevo.derive
import io.circe.Encoder
import io.estatico.newtype.macros._
import monocle.Iso
import shop.domain.health.HealthPayloads.Status.Status
import shop.http.utils.json._ // DON'T REMOVE IT

object HealthPayloads {
  @derive(encoder)
  @newtype case class RedisStatus(value: Status)
  object RedisStatus {
    implicit val redisStatusEncoder: Encoder[Status.Value] =
      Encoder.encodeEnumeration(Status)
  }

  @derive(encoder)
  @newtype case class PostgresStatus(value: Status)
  object PostgresStatus {
    implicit val postgresStatusEncoder: Encoder[Status.Value] =
      Encoder.encodeEnumeration(Status)
  }

  @derive(encoder)
  case class AppStatus(
      redis: RedisStatus,
      postgres: PostgresStatus
  )
  object Status extends Enumeration {
    type Status = Value
    val Okay, Unreachable = Value

    val _Bool: Iso[Status, Boolean] =
      Iso[Status, Boolean] {
        case Okay => true
        case _    => false
      }(if (_) Okay else Unreachable)
  }

}
