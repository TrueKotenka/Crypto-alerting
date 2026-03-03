package ru.hse.crypto.processor

import pureconfig.ConfigReader
import pureconfig.generic.semiauto.deriveReader
import ru.hse.crypto.core.KafkaConfig

final case class ProcessorConfig(
                                  kafkaConsumer: KafkaConfig,
                                  kafkaProducer: KafkaConfig,
                                  changeThreshold: Double
                                )

object ProcessorConfig {
  implicit val reader: ConfigReader[ProcessorConfig] = deriveReader[ProcessorConfig]
}