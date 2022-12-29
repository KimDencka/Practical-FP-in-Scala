package shop.interpreters

import cats.effect.IO
import com.github.t3hnar.bcrypt._
import eu.timepit.refined.auto._
import shop.auth.Crypto
import shop.config.types.PasswordSalt
import shop.domain.auth.AuthPayload._
import weaver.SimpleIOSuite

object CryptoSuite extends SimpleIOSuite {
  private val salt: PasswordSalt = PasswordSalt("$2a$10$8DU8P4l/N2e9EQedx9APC.")

  test("password encoding and decoding roundtrip") {
    Crypto.make[IO](salt).map { crypto =>
      val ini: Password          = Password("simple123")
      val enc: EncryptedPassword = crypto.encrypt(ini)
      val dec: Boolean           = crypto.decrypt(ini, enc)
      expect.same(dec, ini.value.isBcryptedBounded(enc.value))
    }
  }
}
