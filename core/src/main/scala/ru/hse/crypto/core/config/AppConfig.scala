package ru.hse.crypto.core.config

import pureconfig.ConfigReader
import pureconfig.generic.semiauto.deriveReader

final case class AppConfig (
                             kafka: KafkaConfig
                           )

object AppConfig {
  implicit val reader: ConfigReader[AppConfig] = deriveReader[AppConfig]
}
