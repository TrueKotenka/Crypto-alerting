package ru.hse.crypto.core.domain

import cats.effect.IO

import java.time.Instant
import fs2.kafka.{Deserializer, ValueDeserializer}
import io.circe.parser.decode
import ru.hse.crypto.core.Codecs.priceAlertCodec

final case class PriceAlert(
                             symbol: String,
                             oldPrice: BigDecimal,
                             newPrice: BigDecimal,
                             changePercent: Double,
                             timestamp: Instant,
                             message: String
                           )
object PriceAlert {
  implicit val alertDeserializer: ValueDeserializer[IO, PriceAlert] =
    Deserializer[IO, String].map { str =>
      decode[PriceAlert](str).getOrElse(throw new RuntimeException(s"Failed to parse alert: $str"))
    }
}
