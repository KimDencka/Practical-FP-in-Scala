package shop.retries

import cats.effect.Temporal
import cats.syntax.show._
import org.typelevel.log4cats.Logger
import retry.RetryDetails.{ GivingUp, WillDelayAndRetry }
import retry.{ RetryDetails, RetryPolicy, retryingOnAllErrors }

trait Retry[F[_]] {
  def retry[A](policy: RetryPolicy[F], retriable: Retriable)(fa: F[A]): F[A]
}

object Retry {
  def apply[F[_]: Retry]: Retry[F] = implicitly[Retry[F]]

  implicit def forLoggerTemporal[F[_]: Logger: Temporal]: Retry[F] =
    new Retry[F] {
      override def retry[A](policy: RetryPolicy[F], retriable: Retriable)(
          fa: F[A]
      ): F[A] = {
        def onError(e: Throwable, details: RetryDetails): F[Unit] =
          details match {
            case WillDelayAndRetry(_, retriesSoFar, _) =>
              Logger[F].error(
                s"Failed to process ${retriable.show} with ${e.getMessage}. So far we have retried $retriesSoFar times."
              )
            case GivingUp(totalRetries, _) =>
              Logger[F].error(
                s"Giving up on ${retriable.show} after $totalRetries retries."
              )
          }
        retryingOnAllErrors[A](policy, onError)(fa)
      }
    }
}
