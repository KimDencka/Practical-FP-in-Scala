package shop.effects

import cats.ApplicativeError
import cats.effect._
import cats.implicits._
import io.estatico.newtype.Coercible
import io.estatico.newtype.ops._

import java.util.UUID

trait GenUUID[F[_]] {
  def make: F[UUID]
  def make[A: Coercible[UUID, *]]: F[A]
  def read[A: Coercible[UUID, *]](str: String): F[A]
}

object GenUUID {
  def apply[F[_]](implicit ev: GenUUID[F]): GenUUID[F] = ev

  implicit def syncGenUUID[F[_]: Sync]: GenUUID[F] =
    new GenUUID[F] {
      def make: F[UUID] =
        Sync[F].delay(UUID.randomUUID())

      def make[A: Coercible[UUID, *]]: F[A] =
        make.map(_.coerce[A])

      def read[A: Coercible[UUID, *]](str: String): F[A] =
        ApplicativeError[F, Throwable].catchNonFatal(UUID.fromString(str).coerce[A])
    }
}
