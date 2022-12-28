package shop.modules

import cats.effect._
import cats.implicits._
import dev.profunktor.auth.JwtAuthMiddleware
import org.http4s._
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.middleware._
import shop.domain.auth.UserAuthPayload._
import shop.http.routes.admin._
import shop.http.routes.auth._
import shop.http.routes.brand.BrandRoutes
import shop.http.routes.cart.CartRoutes
import shop.http.routes.category.CategoryRoutes
import shop.http.routes.checkout.CheckoutRoutes
import shop.http.routes.health.HealthRoutes
import shop.http.routes.item.ItemRoutes
import shop.http.routes.order.OrderRoutes
import shop.programs.Checkout

import scala.concurrent.duration._

final class HttpApi[F[_]: Async](
    services: Services[F],
    security: Security[F],
    checkout: Checkout[F]
) {

  private val adminMiddleware =
    JwtAuthMiddleware[F, AdminUser](security.adminJwtAuth.value, security.adminAuthRepo.findUser)
  private val userMiddleware =
    JwtAuthMiddleware[F, CommonUser](security.userJwtAuth.value, security.userAuthRepo.findUser)

  // Auth routes
  private val loginRoutes  = LoginRoutes[F](security.authRepo).routes
  private val logoutRoutes = LogoutRoutes[F](security.authRepo).routes(userMiddleware)
  private val userRoutes   = UserRoutes[F](security.authRepo).routes

  // Open routes
  private val healthRoutes   = HealthRoutes[F](services.health).routes
  private val brandRoutes    = BrandRoutes[F](services.brand).routes
  private val categoryRoutes = CategoryRoutes[F](services.category).routes
  private val itemRoutes     = ItemRoutes[F](services.item).routes

  // Secured routes
  private val cartRoutes     = CartRoutes[F](services.cart).routes(userMiddleware)
  private val checkoutRoutes = CheckoutRoutes[F](checkout).routes(userMiddleware)
  private val orderRoutes    = OrderRoutes[F](services.order).routes(userMiddleware)

  // Admin routes
  private val adminBrandRoutes    = AdminBrandRoutes[F](services.brand).routes(adminMiddleware)
  private val adminCategoryRoutes = AdminCategoryRoutes[F](services.category).routes(adminMiddleware)
  private val adminItemRoutes     = AdminItemRoutes[F](services.item).routes(adminMiddleware)

  // Combining all the http routes
  private val openRoutes: HttpRoutes[F] =
    healthRoutes <+> itemRoutes <+> brandRoutes <+>
      categoryRoutes <+> loginRoutes <+> userRoutes <+>
      logoutRoutes <+> cartRoutes <+> orderRoutes <+>
      checkoutRoutes

  private val adminRoutes: HttpRoutes[F] =
    adminItemRoutes <+> adminBrandRoutes <+> adminCategoryRoutes

  private val routes: HttpRoutes[F] = Router(
    versions.v1            -> openRoutes,
    versions.v1 + "/admin" -> adminRoutes
  )

  private val middleware: HttpRoutes[F] => HttpRoutes[F] = {
    { http: HttpRoutes[F] =>
      AutoSlash(http)
    } andThen { http: HttpRoutes[F] =>
      CORS(http)
    } andThen { http: HttpRoutes[F] =>
      Timeout(60.seconds)(http)
    }
  }

  private val loggers: HttpApp[F] => HttpApp[F] = {
    { http: HttpApp[F] =>
      RequestLogger.httpApp(logHeaders = true, logBody = true)(http)
    } andThen { http: HttpApp[F] =>
      ResponseLogger.httpApp(logHeaders = true, logBody = true)(http)
    }
  }

  val httpApp: HttpApp[F] = loggers(middleware(routes).orNotFound)
}
