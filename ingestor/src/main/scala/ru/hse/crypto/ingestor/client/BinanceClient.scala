package ru.hse.crypto.ingestor.client

import cats.effect.Async
import cats.syntax.all._
import io.circe.generic.auto._
import ru.hse.crypto.core.Domain.CryptoPrice
import ru.hse.crypto.ingestor.dto.BinanceTickerResponse
import sttp.client4._
import sttp.client4.circe._

import java.time.Instant

class BinanceClient[F[_]: Async](backend: Backend[F]) extends CryptoClient[F] {

  // API Binance принимает массив символов в формате JSON-строки
  // Пример: https://api.binance.com/api/v3/ticker/price?symbols=["BTCUSDT","ETHUSDT"]
  override def getPrices(symbols: List[String]): F[List[CryptoPrice]] = {
    val symbolsQuery = symbols.map(s => s""""$s"""").mkString("[", ",", "]")
    val uri = uri"https://api.binance.com/api/v3/ticker/price?symbols=$symbolsQuery"

    val request = basicRequest
      .get(uri)
      .response(asJson[List[BinanceTickerResponse]])

    request.send(backend).flatMap { response =>
      response.body match {
        case Right(tickers) =>
          val timestamp = Instant.now()
          val domainPrices = tickers.map { t =>
            // Превращаем DTO в доменную модель, парсим строку в BigDecimal
            CryptoPrice(t.symbol, BigDecimal(t.price), timestamp)
          }
          Async[F].pure(domainPrices)

        case Left(error) =>
          // В случае ошибки (например, отпал интернет) логируем и возвращаем пустой список,
          // чтобы Stream не упал целиком.
          Async[F].delay {
            println(s"Error fetching prices from Binance: $error")
            List.empty[CryptoPrice]
          }
      }
    }
  }
}
