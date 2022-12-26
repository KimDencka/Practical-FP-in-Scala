package shop.storage

import cats.effect._
import cats.implicits._
import dev.profunktor.auth.jwt._
import dev.profunktor.redis4cats.log4cats._
import dev.profunktor.redis4cats.{Redis, RedisCommands}
import eu.timepit.refined.auto._
import eu.timepit.refined.cats._
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats.noop.NoOpLogger
import pdi.jwt._
import suite.ResourceSuite

import scala.concurrent.duration._
import shop.config.types._
import shop.domain.auth.UserAuthPayload._
import shop.Generators._
import shop.auth.{Crypto, JwtExpire, Tokens}
import shop.domain.auth.AuthPayload._
import shop.domain.brand.BrandPayload._
import shop.domain.cart.CartPayload.Cart
import shop.domain.category.CategoryPayload._
import shop.domain.item.ItemAlgebra
import shop.domain.item.ItemPayload._
import shop.domain.user.UserAlgebra
import shop.effects.GenUUID
import shop.infrastructure.redis.{ AuthRepository, CartRepository, UserAuthRepository }

import java.util.UUID

object RedisSuite extends ResourceSuite {
  implicit val logger: SelfAwareStructuredLogger[IO] = NoOpLogger[IO]

  type Res = RedisCommands[IO, String, String]

  override def sharedResource: Resource[IO, Res] =
    Redis[IO]
      .utf8("redis://localhost")
      .beforeAll(_.flushAll)

  val Exp: ShoppingCartExpiration          = ShoppingCartExpiration(30.seconds)
  val tokenConfig: JwtAccessTokenKeyConfig = JwtAccessTokenKeyConfig("bar")
  val tokenExp: TokenExpiration            = TokenExpiration(30.seconds)
  val jwtClaim: JwtClaim                   = JwtClaim("test")
  val userJwtAuth: UserJwtAuth             = UserJwtAuth(JwtAuth.hmac("bar", JwtAlgorithm.HS256))

  test("Shopping Cart") { redis =>
    val gen = for {
      uid <- userIdGen
      it1 <- itemGen
      it2 <- itemGen
      q1  <- quantityGen
      q2  <- quantityGen
    } yield (uid, it1, it2, q1, q2)

    forall(gen) { case (uid, it1, it2, q1, q2) =>
      Ref.of[IO, Map[ItemId, Item]](Map(it1.itemId -> it1, it2.itemId -> it2)).flatMap { ref =>
        val items = new TestItemAlgebra(ref)
        val c     = new CartRepository[IO](items, redis, Exp)
        for {
          x <- c.get(uid)
          _ <- c.add(uid, it1.itemId, q1)
          _ <- c.add(uid, it2.itemId, q1)
          y <- c.get(uid)
          _ <- c.removeItem(uid, it1.itemId)
          z <- c.get(uid)
          _ <- c.update(uid, Cart(Map(it2.itemId -> q2)))
          w <- c.get(uid)
          _ <- c.delete(uid)
          v <- c.get(uid)
        } yield expect.all(
          x.items.isEmpty,
          y.items.size === 2,
          z.items.size === 1,
          v.items.isEmpty,
          w.items.headOption.fold(false)(_.quantity === q2)
        )
      }
    }
  }

  test("Authentication") { redis =>
    val gen = for {
      un1 <- userNameGen
      un2 <- userNameGen
      pw  <- passwordGen
    } yield (un1, un2, pw)

    forall(gen) { case (un1, un2, pw) =>
      for {
        t <- JwtExpire.make[IO].map(Tokens.make[IO](_, tokenConfig, tokenExp))
        c <- Crypto.make[IO](PasswordSalt("test"))
        a = new AuthRepository[IO](tokenExp, t, new TestUserAlgebra(un2), redis, c)
        u = new UserAuthRepository[IO](redis)
        x <- u.findUser(JwtToken("invalid"))(jwtClaim)
        y <- a.login(un1, pw).attempt // UserNotFound
        j <- a.newUser(un1, pw)
        e <- jwtDecode[IO](j, userJwtAuth.value).attempt
        k <- a.login(un2, pw).attempt // InvalidPassword
        w <- u.findUser(j)(jwtClaim)
        s <- redis.get(j.value)
        _ <- a.logout(j, un1)
        z <- redis.get(j.value)
      } yield expect.all(
        x.isEmpty,
        y == Left(UserNotFound(un1)),
        e.isRight,
        k == Left(InvalidPassword(un2)),
        w.fold(false)(_.value.username === un1),
        s.nonEmpty,
        z.isEmpty
      )
    }
  }

}

protected class TestUserAlgebra(un: UserName) extends UserAlgebra[IO] {
  override def find(username: UserName): IO[Option[UserWithPassword]] = IO.pure {
    (username === un)
      .guard[Option]
      .as(UserWithPassword(UserId(UUID.randomUUID), un, EncryptedPassword("foo")))
  }

  override def create(
      username: UserName,
      password: EncryptedPassword
  ): IO[UserId] = GenUUID[IO].make[UserId]
}

protected class TestItemAlgebra(ref: Ref[IO, Map[ItemId, Item]]) extends ItemAlgebra[IO] {
  override def findAll: IO[List[Item]] =
    ref.get.map(_.values.toList)

  override def findByBrand(brandName: BrandName): IO[List[Item]] =
    ref.get.map(_.values.filter(_.brand.name === brandName).toList)

  override def findById(itemId: ItemId): IO[Option[Item]] =
    ref.get.map(_.get(itemId))

  override def create(item: CreateItem): IO[ItemId] =
    GenUUID[IO].make[ItemId].flatTap { id =>
      val brand    = Brand(item.brandId, BrandName("foo"))
      val category = Category(item.categoryId, CategoryName("foo"))
      val newItem  = Item(id, item.name, item.description, item.price, brand, category)
      ref.update(_.updated(id, newItem))
    }

  override def update(item: UpdateItem): IO[Unit] =
    ref.update(x => x.get(item.id).fold(x)(i => x.updated(item.id, i.copy(price = item.price))))
}
