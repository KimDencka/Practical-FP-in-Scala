package shop

import cats.effect._
import cats.effect.std.Supervisor
import cats.implicits._
import dev.profunktor.redis4cats.log4cats._
import eu.timepit.refined.auto._
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats._
import retry.RetryPolicies._
import retry.RetryPolicy
import shop.config.Config
import shop.modules._
import shop.programs.Checkout
import shop.resources._

object Main extends IOApp {

  implicit val logger: SelfAwareStructuredLogger[IO] = Slf4jLogger.getLogger[IO]

  override def run(args: List[String]): IO[ExitCode] = {
    Config.load[IO].flatMap { cfg =>
      Logger[IO].info(s"Loaded config $cfg") >>
        Supervisor[IO].use { implicit sp =>
          AppResources
            .make[IO](cfg)
            .evalMap { res =>
              Security.make[IO](cfg, res.postgres, res.redis).map { security =>
                val repositories = Repositories.make[IO](cfg, res)
                val services     = Services.make[IO](repositories)

                val retryPolicy: RetryPolicy[IO] =
                  limitRetries[IO](cfg.checkoutConfig.retriesLimit) |+|
                    exponentialBackoff[IO](cfg.checkoutConfig.retriesBackoff)

                val checkout: Checkout[IO] =
                  Checkout[IO](services.payment, repositories.cart, repositories.order, retryPolicy)

                val api = new HttpApi[IO](services, security, checkout)
                cfg.httpServerConfig -> api.httpApp
              }
            }
            .flatMap { case (cfg, httpApp) =>
              MkHttpServer[IO].newEmber(cfg, httpApp)
            }
            .useForever
        }
    }
  }
}
