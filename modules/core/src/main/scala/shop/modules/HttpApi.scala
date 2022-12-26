package shop.modules

import cats.effect._
import cats.implicits._
import dev.profunktor.auth.JwtAuthMiddleware
import org.http4s._
import org.http4s.implicits._
import org.http4s.server.Router
import org.http4s.server.middleware._
import shop.domain.auth.UserAuthPayload.{ AdminUser, CommonUser }
import shop.domain.brand.BrandService
import shop.domain.cart.CartService
import shop.domain.category.CategoryService
import shop.domain.health.HealthService
import shop.domain.item.ItemService
import shop.domain.order.OrderService
import shop.http.routes.admin.{ AdminBrandRoutes, AdminCategoryRoutes, AdminItemRoutes }
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
    cartService: CartService[F],
    brandService: BrandService[F],
    categoryService: CategoryService[F],
    itemService: ItemService[F],
    orderService: OrderService[F],
    healthService: HealthService[F],
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
  private val healthRoutes   = HealthRoutes[F](healthService).routes
  private val brandRoutes    = BrandRoutes[F](brandService).routes
  private val categoryRoutes = CategoryRoutes[F](categoryService).routes
  private val itemRoutes     = ItemRoutes[F](itemService).routes

  // Secured routes
  private val cartRoutes     = CartRoutes[F](cartService).routes(userMiddleware)
  private val checkoutRoutes = CheckoutRoutes[F](checkout).routes(userMiddleware)
  private val orderRoutes    = OrderRoutes[F](orderService).routes(userMiddleware)

  // Admin routes
  private val adminBrandRoutes    = AdminBrandRoutes[F](brandService).routes(adminMiddleware)
  private val adminCategoryRoutes = AdminCategoryRoutes[F](categoryService).routes(adminMiddleware)
  private val adminItemRoutes     = AdminItemRoutes[F](itemService).routes(adminMiddleware)

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
