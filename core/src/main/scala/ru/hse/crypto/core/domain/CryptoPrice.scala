package ru.hse.crypto.core.domain

import java.time.Instant

final case class CryptoPrice(
                              symbol: String,
                              price: BigDecimal,
                              timestamp: Instant
                            )
