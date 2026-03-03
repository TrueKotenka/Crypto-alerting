package ru.hse.crypto.core

import pureconfig.ConfigReader
import pureconfig.generic.semiauto.deriveReader

object AppTopics {
  val RawPrices = "crypto-prices-raw"
  val Alerts = "crypto-alerts"
}

final case class KafkaConfig(
                            bootstrapServers: String, // ex: "localhost:9092"
                            groupId: String, // consumer group id
                            topic: String // topic name
                            )
object KafkaConfig {
  implicit val reader: ConfigReader[KafkaConfig] = deriveReader[KafkaConfig]
}

final case class AppConfig (
                        kafka: KafkaConfig
                        )

object AppConfig {
  implicit val reader: ConfigReader[AppConfig] = deriveReader[AppConfig]
}
