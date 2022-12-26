package shop.interpreters

import cats.effect.IO
import eu.timepit.refined.auto._
import shop.auth.Crypto
import shop.config.types.PasswordSalt
import shop.domain.auth.AuthPayload._
import weaver.SimpleIOSuite

object CryptoSuite extends SimpleIOSuite {
  private val salt: PasswordSalt = PasswordSalt("53kr3t")

  test("password encoding and decoding roundtrip") {
    Crypto.make[IO](salt).map { crypto =>
      val ini: Password          = Password("simple123")
      val enc: EncryptedPassword = crypto.encrypt(ini)
      val dec: Password          = crypto.decrypt(enc)
      expect.same(dec, ini)
    }
  }
}
