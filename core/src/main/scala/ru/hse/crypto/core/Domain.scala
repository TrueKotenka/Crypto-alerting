package ru.hse.crypto.core

import java.time.Instant

object Domain {
  final case class CryptoPrice(
                                symbol: String,
                                price: BigDecimal,
                                timestamp: Instant
                              )
  final case class PriceAlert(
                               symbol: String,
                               oldPrice: BigDecimal,
                               newPrice: BigDecimal,
                               changePercent: Double,
                               timestamp: Instant,
                               message: String
                             )
}
