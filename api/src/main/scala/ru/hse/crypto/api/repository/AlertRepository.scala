package ru.hse.crypto.api.repository

import ru.hse.crypto.core.domain.PriceAlert

trait AlertRepository[F[_]] {
  def initTable: F[Unit]
  def save(alert: PriceAlert): F[Unit]
  def getAlerts(symbolOpt: Option[String]): F[List[PriceAlert]]
}
