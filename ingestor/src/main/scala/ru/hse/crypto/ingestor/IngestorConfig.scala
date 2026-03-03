package ru.hse.crypto.ingestor

import pureconfig.ConfigReader
import pureconfig.generic.semiauto.deriveReader
import ru.hse.crypto.core.KafkaConfig

import scala.concurrent.duration.FiniteDuration

case class IngestorConfig(
                         kafka: KafkaConfig,
                         symbols: List[String],
                         pollInterval: FiniteDuration
                         )
object IngestorConfig {
  implicit val reader: ConfigReader[IngestorConfig] = deriveReader[IngestorConfig]
}