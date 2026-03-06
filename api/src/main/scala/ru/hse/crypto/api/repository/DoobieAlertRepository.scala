package ru.hse.crypto.api.repository

import cats.effect.Async
import cats.syntax.all._
import doobie._
import doobie.implicits._
import doobie.postgres.implicits._
import ru.hse.crypto.core.domain.PriceAlert

class DoobieAlertRepository[F[_]: Async](xa: Transactor[F]) extends AlertRepository[F] {

  // Создаем таблицу, если ее еще нет
  override def initTable: F[Unit] = {
    sql"""
      CREATE TABLE IF NOT EXISTS alerts (
        id SERIAL PRIMARY KEY,
        symbol VARCHAR(50) NOT NULL,
        old_price NUMERIC NOT NULL,
        new_price NUMERIC NOT NULL,
        change_percent DOUBLE PRECISION NOT NULL,
        timestamp TIMESTAMP NOT NULL,
        message TEXT NOT NULL
      )
    """.update.run.transact(xa).void
  }

  override def save(alert: PriceAlert): F[Unit] = {
    sql"""
      INSERT INTO alerts (symbol, old_price, new_price, change_percent, timestamp, message)
      VALUES (${alert.symbol}, ${alert.oldPrice}, ${alert.newPrice}, ${alert.changePercent}, ${alert.timestamp}, ${alert.message})
    """.update.run.transact(xa).void
  }

  override def getAlerts(symbolOpt: Option[String]): F[List[PriceAlert]] = {
    // Динамическое построение SQL-запроса (Doobie Fragments)
    val baseQuery = fr"SELECT symbol, old_price, new_price, change_percent, timestamp, message FROM alerts"

    val filter = symbolOpt match {
      case Some(sym) => fr"WHERE symbol = $sym"
      case None      => fr"" // Пустой фрагмент, если фильтра нет
    }

    val order = fr"ORDER BY timestamp DESC LIMIT 100"

    (baseQuery ++ filter ++ order)
      .query[PriceAlert]
      .to[List]
      .transact(xa)
  }
}
