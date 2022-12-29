package shop.auth

import cats.effect.Sync
import com.github.t3hnar.bcrypt._
import eu.timepit.refined.auto._

import shop.config.types.PasswordSalt
import shop.domain.auth.AuthPayload._

trait Crypto {
  def encrypt(value: Password): EncryptedPassword
  def decrypt(password: Password, ePassword: EncryptedPassword): Boolean
}

object Crypto {
  def make[F[_]: Sync](passwordSalt: PasswordSalt): F[Crypto] =
    Sync[F].delay {
      new Crypto {
        def encrypt(password: Password): EncryptedPassword = {
          val encrypted = password.value.bcryptBounded(passwordSalt.secret.value)
          EncryptedPassword(encrypted)
        }
        def decrypt(password: Password, ePassword: EncryptedPassword): Boolean = {
          password.value.isBcryptedBounded(ePassword.value)
        }
      }
    }
}
