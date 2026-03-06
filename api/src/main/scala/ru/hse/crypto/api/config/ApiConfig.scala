package ru.hse.crypto.api.config

import pureconfig.ConfigReader
import pureconfig.generic.semiauto.deriveReader
import ru.hse.crypto.core.config.KafkaConfig

case class ApiConfig(
                      kafka: KafkaConfig,
                      database: DatabaseConfig,
                      server: ServerConfig
                    )

object ApiConfig {
  implicit val dbReader: ConfigReader[DatabaseConfig] = deriveReader[DatabaseConfig]
  implicit val serverReader: ConfigReader[ServerConfig] = deriveReader[ServerConfig]
  implicit val apiReader: ConfigReader[ApiConfig] = deriveReader[ApiConfig]
}
