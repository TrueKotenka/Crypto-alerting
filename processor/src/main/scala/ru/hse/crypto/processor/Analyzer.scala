package ru.hse.crypto.processor

import cats.effect.{IO, Ref}
import ru.hse.crypto.core.Domain.{CryptoPrice, PriceAlert}
import java.time.Instant

trait Analyzer {
  def process(price: CryptoPrice): IO[Option[PriceAlert]]
}

class PriceAnalyzer(
                     state: Ref[IO, Map[String, CryptoPrice]], // Наше in-memory хранилище
                     threshold: Double
                   ) extends Analyzer {

  override def process(newPrice: CryptoPrice): IO[Option[PriceAlert]] = {
    // modify атомарно обновляет состояние и возвращает результат
    state.modify { currentState =>
      currentState.get(newPrice.symbol) match {
        case Some(oldPrice) =>
          // Считаем изменение цены
          val diff = (newPrice.price - oldPrice.price).abs
          val percentChange = (diff / oldPrice.price).toDouble

          // Если порог превышен, генерируем алерт
          val alertOpt = if (percentChange >= threshold) {
            val direction = if (newPrice.price > oldPrice.price) "UP" else "DOWN"

            Some(PriceAlert(
              symbol = newPrice.symbol,
              oldPrice = oldPrice.price,
              newPrice = newPrice.price,
              changePercent = percentChange * 100, // В процентах
              timestamp = Instant.now(),
              message = f"Price moved $direction! Change: ${percentChange * 100}%.2f%%"
            ))
          } else {
            None
          }
          // Возвращаем обновленное состояние и результат (алерт)
          (currentState.updated(newPrice.symbol, newPrice), alertOpt)

        case None =>
          // Если цены раньше не было, просто сохраняем её (алерта нет)
          (currentState.updated(newPrice.symbol, newPrice), None)
      }
    }
  }
}

object PriceAnalyzer {
  // Конструктор: создаем пустую Map при старте приложения
  def make(threshold: Double): IO[PriceAnalyzer] =
    Ref.of[IO, Map[String, CryptoPrice]](Map.empty).map(ref => new PriceAnalyzer(ref, threshold))
}