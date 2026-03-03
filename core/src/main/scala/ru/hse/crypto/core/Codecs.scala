package ru.hse.crypto.core

import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec
import ru.hse.crypto.core.Domain.{CryptoPrice, PriceAlert}

object Codecs {
  implicit val cryptoPriceCodec: Codec[CryptoPrice] = deriveCodec[CryptoPrice]

  implicit val priceAlertCodec: Codec[PriceAlert] = deriveCodec[PriceAlert]
}
