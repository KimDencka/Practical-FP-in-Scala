package shop.infrastructure.postgres

import cats.effect._
import cats.implicits._
import shop.domain.auth.AuthPayload._
import shop.domain.auth.UserAuthPayload.UserWithPassword
import shop.domain.user.UserAlgebra
import shop.domain.user.UserPayload.User
import shop.effects.GenUUID
import shop.infrastructure.postgres.UserRepository._
import shop.sql.codecs._
import skunk._
import skunk.implicits._

class UserRepository[F[_]: Sync](
    postgres: Resource[F, Session[F]]
) extends UserAlgebra[F] {
  override def find(username: UserName): F[Option[UserWithPassword]] =
    postgres.use { session =>
      session
        .prepare(selectUser)
        .use { q =>
          q.option(username).map {
            case Some(u ~ p) => UserWithPassword(u.userId, u.username, p).some
            case _           => none[UserWithPassword]
          }
        }
    }

  override def create(username: UserName, password: EncryptedPassword): F[UserId] =
    postgres.use { session =>
      session.prepare(insertUser).use { cmd =>
        GenUUID[F].make[UserId].flatMap { id =>
          cmd
            .execute(User(id, username) ~ password)
            .as(id)
            .recoverWith { case SqlState.UniqueViolation(_) =>
              UserNameInUse(username).raiseError[F, UserId]
            }
        }
      }
    }
}

object UserRepository {
  val codec: Codec[User ~ EncryptedPassword] =
    (userId ~ userName ~ encPassword).imap { case i ~ n ~ p =>
      User(i, n) ~ p
    } { case u ~ p =>
      u.userId ~ u.username ~ p
    }

  val selectUser: Query[UserName, User ~ EncryptedPassword] =
    sql"""
         SELECT * FROM users
         WHERE name = $userName
       """.query(codec)

  val insertUser: Command[User ~ EncryptedPassword] =
    sql"""
         INSERT INTO users
         VALUES ($codec)
       """.command
}
