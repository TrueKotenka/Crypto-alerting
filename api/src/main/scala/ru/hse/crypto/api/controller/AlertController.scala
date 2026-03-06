package ru.hse.crypto.api.controller

import cats.effect.IO
import ru.hse.crypto.api.repository.AlertRepository
import ru.hse.crypto.core.Codecs._
import sttp.tapir._
import sttp.tapir.generic.auto.schemaForCaseClass
import sttp.tapir.json.circe._
import sttp.tapir.server.ServerEndpoint

class AlertController(repository: AlertRepository[IO]) {

  // Описание эндпоинта
  val getAlertsEndpoint = endpoint.get
    .in("api" / "v1" / "alerts")
    .in(query[Option[String]]("symbol").description("Filter by crypto symbol, e.g., BTCUSDT"))
    .out(jsonBody[List[ru.hse.crypto.core.domain.PriceAlert]])
    .errorOut(stringBody)
    .serverLogic { symbolOpt =>
      // Логика: идем в БД и возвращаем Right (успех)
      repository.getAlerts(symbolOpt)
        .map(alerts => Right(alerts))
        .handleErrorWith(e => IO.pure(Left(s"Database error: ${e.getMessage}")))
    }

  // Список всех эндпоинтов для регистрации в сервере
  val all: List[ServerEndpoint[Any, IO]] = List(getAlertsEndpoint)
}
