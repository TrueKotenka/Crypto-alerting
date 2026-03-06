package ru.hse.crypto.core.config

import pureconfig.ConfigReader
import pureconfig.generic.semiauto.deriveReader

final case class KafkaConfig(
                              bootstrapServers: String, // ex: "localhost:9092"
                              groupId: String, // consumer group id
                              topic: String // topic name
                            )
object KafkaConfig {
  implicit val reader: ConfigReader[KafkaConfig] = deriveReader[KafkaConfig]
}
