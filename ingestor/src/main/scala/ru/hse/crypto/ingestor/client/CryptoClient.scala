package ru.hse.crypto.ingestor.client

import ru.hse.crypto.core.domain.CryptoPrice

trait CryptoClient[F[_]] {
  def getPrices(symbols: List[String]): F[List[CryptoPrice]]
}
