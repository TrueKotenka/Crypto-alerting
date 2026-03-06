package ru.hse.crypto.processor

import cats.effect.{ExitCode, IO, IOApp, Resource}
import fs2.Stream
import fs2.kafka._
import io.circe.parser.decode
import io.circe.syntax._
import pureconfig.ConfigSource
import ru.hse.crypto.core.Codecs._
import ru.hse.crypto.core.domain.{CryptoPrice, PriceAlert}

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

object Main extends IOApp {

  // 1. Настройка сериализации/десериализации для Kafka
  implicit val priceDeserializer: ValueDeserializer[IO, CryptoPrice] =
    Deserializer[IO, String].map { str =>
      decode[CryptoPrice](str).getOrElse(throw new RuntimeException(s"Failed to parse: $str"))
    }

  implicit val alertSerializer: ValueSerializer[IO, PriceAlert] =
    Serializer[IO, String].contramap[PriceAlert](alert => alert.asJson.noSpaces)

  override def run(args: List[String]): IO[ExitCode] = {
    val resources = for {
      config <- Resource.eval(IO.delay(
        ConfigSource.default.at("crypto-processor").loadOrThrow[ProcessorConfig]
      ))

      // Настройки Consumer'а (читателя)
      consumerSettings = ConsumerSettings[IO, String, CryptoPrice]
        .withAutoOffsetReset(AutoOffsetReset.Earliest)
        .withBootstrapServers(config.kafkaConsumer.bootstrapServers)
        .withGroupId(config.kafkaConsumer.groupId)

      // Настройки Producer'а (писателя)
      producerSettings = ProducerSettings[IO, String, PriceAlert]
        .withBootstrapServers(config.kafkaProducer.bootstrapServers)

      producer <- KafkaProducer.resource(producerSettings)

      // Инициализируем наш анализатор состояний
      analyzer <- Resource.eval(PriceAnalyzer.make(config.changeThreshold))
    } yield (config, consumerSettings, producerSettings, producer, analyzer)

    resources.use { case (config, consSettings, prodSettings, producer, analyzer) =>
      IO.println("Starting Processor...") *>
        processStream(config, consSettings, prodSettings, producer, analyzer).compile.drain
    }.as(ExitCode.Success)
  }

  private def processStream(
                             config: ProcessorConfig,
                             consumerSettings: ConsumerSettings[IO, String, CryptoPrice],
                             producerSettings: ProducerSettings[IO, String, PriceAlert],
                             producer: KafkaProducer[IO, String, PriceAlert],
                             analyzer: PriceAnalyzer
                           ): Stream[IO, Unit] = {
    KafkaConsumer.stream(consumerSettings)
      .subscribeTo(config.kafkaConsumer.topic)
      .records // Получаем поток сообщений из Kafka
      .evalMap { committable =>
        val price = committable.record.value

        // Передаем цену в анализатор
        analyzer.process(price).flatMap {
          case Some(alert) =>
            IO.println(s"ALERT TRIGGERED: ${alert.message}") *>
              // Создаем запись для отправки в топик алертов
              producer.produce(ProducerRecords.one(
                ProducerRecord(config.kafkaProducer.topic, alert.symbol, alert)
              )).flatten.as(committable.offset) // Возвращаем offset для коммита

          case None =>
            // Ничего не изменилось, просто возвращаем offset
            IO.pure(committable.offset)
        }
      }
      // Коммитим прочитанные смещения батчами (At-Least-Once delivery semantics)
      .through(commitBatchWithin(500, FiniteDuration(5, TimeUnit.SECONDS)))
  }
}