package shop.ext.ciris

import ciris.ConfigDecoder
import shop.ext.derevo.Derive

object configDecoder extends Derive[Decoder.Id]

object Decoder {
  type Id[A] = ConfigDecoder[String, A]
}
