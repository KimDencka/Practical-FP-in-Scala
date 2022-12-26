package shop.infrastructure

import cats.effect._
import cats.effect.implicits._
import cats.implicits._
import dev.profunktor.redis4cats.RedisCommands
import shop.domain.health.HealthAlgebra
import shop.domain.health.HealthPayloads.{ AppStatus, PostgresStatus, RedisStatus, Status }
import skunk._
import skunk.codec.all._
import skunk.implicits._

import scala.concurrent.duration.DurationInt

class HealthRepository[F[_]: Async](
    postgres: Resource[F, Session[F]],
    redis: RedisCommands[F, String, String]
) extends HealthAlgebra[F] {
  override def status: F[AppStatus] = {
    val q: Query[Void, Int] = sql"SELECT pid FROM pg_stat_activity".query(int4)

    val redisHealth: F[RedisStatus] =
      redis.ping
        .map(_.nonEmpty)
        .timeout(1.second)
        .map(Status._Bool.reverseGet)
        .orElse(Status.Unreachable.pure[F].widen)
        .map(RedisStatus.apply)

    val postgresHealth: F[PostgresStatus] =
      postgres
        .use(_.execute(q))
        .map(_.nonEmpty)
        .timeout(1.second)
        .map(Status._Bool.reverseGet)
        .orElse(Status.Unreachable.pure[F].widen)
        .map(PostgresStatus.apply)

    (redisHealth, postgresHealth)
      .parMapN(AppStatus.apply)
  }
}
