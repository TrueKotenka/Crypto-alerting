package ru.hse.crypto.ingestor

import cats.effect.{ExitCode, IO, IOApp, Resource}
import fs2.Stream
import fs2.kafka._
import io.circe.syntax._
import pureconfig.ConfigSource
import ru.hse.crypto.core.AppTopics
import ru.hse.crypto.core.Domain.CryptoPrice
import ru.hse.crypto.ingestor.client.{BinanceClient, CryptoClient}
import sttp.client4.httpclient.cats.HttpClientCatsBackend

object Main extends IOApp {

  // 1. Сериализатор: Учим Kafka понимать наш CryptoPrice
  // Мы берем стандартный сериализатор строк (String) и говорим:
  // "Перед тем как сериализовать строку, преврати CryptoPrice в JSON-строку".
  import ru.hse.crypto.core.Codecs.cryptoPriceCodec // Импортируем implicit codec из Core модуля

  implicit val priceSerializer: ValueSerializer[IO, CryptoPrice] =
    Serializer[IO, String].contramap[CryptoPrice](price => price.asJson.noSpaces)

  // Сериализатор для ключа (у нас ключ - это String, например "BTCUSDT")
  implicit val keySerializer: KeySerializer[IO, String] = Serializer[IO, String]

  override def run(args: List[String]): IO[ExitCode] = {
    val resource = for {
      // Загружаем конфиг
      config <- Resource.eval(IO.delay(
        ConfigSource.default.at("crypto-ingestor").loadOrThrow[IngestorConfig]
      ))

      // Инициализируем HTTP клиент
      backend <- HttpClientCatsBackend.resource[IO]()
      binanceClient = new BinanceClient[IO](backend)

      // Настраиваем Kafka Producer
      producerSettings = ProducerSettings[IO, String, CryptoPrice]
        .withBootstrapServers(config.kafka.bootstrapServers)

      // Инициализируем сам Producer как ресурс (чтобы он корректно закрылся при остановке)
      producer <- KafkaProducer.resource(producerSettings)
    } yield (config, binanceClient, producer)

    resource.use { case (config, client, producer) =>
      IO.println(s"Starting Ingestor. Tracking symbols: ${config.symbols.mkString(", ")}") *>
        runStream(config, client, producer).compile.drain
    }.as(ExitCode.Success)
  }

  // Основной пайплайн (Stream)
  private def runStream(
                         config: IngestorConfig,
                         client: CryptoClient[IO],
                         producer: KafkaProducer[IO, String, CryptoPrice]
                       ): Stream[IO, Unit] = {
    // 1. Просыпаемся каждые pollInterval (например, 5 секунд)
    Stream.awakeEvery[IO](config.pollInterval)
      // 2. Идем в API за ценами (evalMap выполняет IO эффект внутри потока)
      .evalMap(_ => client.getPrices(config.symbols))
      // 3. API возвращает List[CryptoPrice], а нам нужно развернуть его в поток отдельных элементов
      .flatMap(prices => Stream.emits(prices))
      // 4. Оборачиваем каждый CryptoPrice в формат ProducerRecord для Kafka
      // Record принимает: (Топик, Ключ, Значение)
      .map { price =>
        val record = ProducerRecord(AppTopics.RawPrices, price.symbol, price)
        ProducerRecords.one(record)
      }
      // 5. Логируем для наглядности
      .evalTap(records => records.head match {
        case Some(v) => IO.println(s"Sending to Kafka: ${v.value.symbol} -> ${v.value.price}")
        case None => IO.println("Failed to parse first element from records")
      })
      // 6. Отправляем в Kafka (through - пропускает поток через какую-то трубу)
      .through(KafkaProducer.pipe(producerSettings(config.kafka.bootstrapServers))).drain
  }

  // Вспомогательный метод для ленивой передачи настроек в pipe
  private def producerSettings(servers: String): ProducerSettings[IO, String, CryptoPrice] =
    ProducerSettings[IO, String, CryptoPrice].withBootstrapServers(servers)
}