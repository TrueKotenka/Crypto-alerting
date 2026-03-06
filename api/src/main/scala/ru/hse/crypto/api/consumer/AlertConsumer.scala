package ru.hse.crypto.api.consumer

import cats.effect.IO
import fs2.Stream
import fs2.kafka._
import ru.hse.crypto.api.repository.AlertRepository
import ru.hse.crypto.core.domain.PriceAlert

import scala.concurrent.duration._

object AlertConsumer {
  def stream(
              settings: ConsumerSettings[IO, String, PriceAlert],
              topic: String,
              repository: AlertRepository[IO]
            ): Stream[IO, Unit] = {
    KafkaConsumer.stream(settings)
      .subscribeTo(topic)
      .records
      .evalMap { committable =>
        val alert = committable.record.value
        // Сохраняем в БД и возвращаем offset для коммита
        repository.save(alert) *>
          IO.println(s"Saved alert to DB: ${alert.symbol} (change: ${alert.changePercent}%)")
            .as(committable.offset)
      }
      .through(commitBatchWithin(500, 5.seconds))
  }
}
