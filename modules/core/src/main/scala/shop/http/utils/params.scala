package shop.http.utils

import cats.implicits._
import eu.timepit.refined._
import eu.timepit.refined.api.{ Refined, Validate }
import io.estatico.newtype.Coercible
import io.estatico.newtype.ops._
import org.http4s._

object params {
  implicit def coercibleQueryParamDecoder[A: Coercible[B, *], B: QueryParamDecoder]: QueryParamDecoder[A] =
    QueryParamDecoder[B].map(_.coerce[A])

  implicit def refinedParamDecoder[T: QueryParamDecoder, P](implicit
      env: Validate[T, P]
  ): QueryParamDecoder[T Refined P] =
    QueryParamDecoder[T].emap(
      refineV[P](_).leftMap(m => ParseFailure(m, m))
    )
}
