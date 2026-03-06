package ru.hse.crypto.api

import cats.effect.{ExitCode, IO, IOApp, Resource}
import com.comcast.ip4s._
import doobie.hikari.HikariTransactor
import fs2.Stream
import fs2.kafka.{AutoOffsetReset, ConsumerSettings}
import org.http4s.ember.server.EmberServerBuilder
import pureconfig.ConfigSource
import ru.hse.crypto.api.config.ApiConfig
import ru.hse.crypto.api.consumer.AlertConsumer
import ru.hse.crypto.api.controller.AlertController
import ru.hse.crypto.api.repository.DoobieAlertRepository
import ru.hse.crypto.core.domain.PriceAlert
import sttp.tapir.server.http4s.Http4sServerInterpreter

import scala.concurrent.ExecutionContext

object Main extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    val resources = for {
      // 1. Читаем конфиг
      config <- Resource.eval(IO.delay(
        ConfigSource.default.at("crypto-api").loadOrThrow[ApiConfig]
      ))

      // 2. Настраиваем пул соединений к БД (HikariCP)
      xa <- HikariTransactor.newHikariTransactor[IO](
        config.database.driver,
        config.database.url,
        config.database.user,
        config.database.password,
        ExecutionContext.global
      )

      // 3. Создаем репозиторий и контроллер
      repo = new DoobieAlertRepository[IO](xa)
      controller = new AlertController(repo)

      // Инициализируем таблицу при старте (вместо Liquibase)
      _ <- Resource.eval(repo.initTable)

      // 4. Настраиваем HTTP Сервер (Http4s)
      httpApp = Http4sServerInterpreter[IO]().toRoutes(controller.all).orNotFound
      server <- EmberServerBuilder
        .default[IO]
        .withHost(Ipv4Address.fromString(config.server.host).getOrElse(ipv4"0.0.0.0"))
        .withPort(Port.fromInt(config.server.port).getOrElse(port"8080"))
        .withHttpApp(httpApp)
        .build

      // 5. Настраиваем Kafka Consumer
      consumerSettings = ConsumerSettings[IO, String, PriceAlert]
        .withAutoOffsetReset(AutoOffsetReset.Earliest)
        .withBootstrapServers(config.kafka.bootstrapServers)
        .withGroupId(config.kafka.groupId)

    } yield (config, repo, consumerSettings, server)

    resources.use { case (config, repo, consumerSettings, server) =>
      IO.println(s"HTTP Server started on http://${config.server.host}:${config.server.port}") *>
        IO.println("Starting Background Kafka Consumer...")

      // МАГИЯ FS2: Запускаем два независимых процесса параллельно!
      Stream(
        // Процесс 1: Консьюмер Kafka (бесконечный поток)
        AlertConsumer.stream(consumerSettings, config.kafka.topic, repo),

        // Процесс 2: HTTP сервер (мы его уже запустили в Resource,
        // но чтобы Stream не завершился, мы ставим бесконечное ожидание)
        Stream.eval(IO.never)
      ).parJoinUnbounded.compile.drain
    }.as(ExitCode.Success)
  }
}
