ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.18"

// --- Версии библиотек ---
val catsEffectVersion = "3.6.3"
val fs2Version        = "3.12.2"
val fs2KafkaVersion   = "3.9.1"
val sttpVersion       = "4.0.18"
val tapirVersion      = "1.13.8"
val circeVersion      = "0.14.15"
val doobieVersion     = "1.0.0-RC11"
val pureConfigVersion = "0.17.10"
val logbackVersion    = "1.5.32"

// --- Общие настройки для всех модулей ---
lazy val commonSettings = Seq(
  scalacOptions ++= Seq(
    "-deprecation",
    "-encoding", "UTF-8",
    "-feature",
    "-unchecked",
    "-Xfatal-warnings"
  ),
  libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-effect" % catsEffectVersion,
    "co.fs2"        %% "fs2-core"    % fs2Version,
    "ch.qos.logback" % "logback-classic" % logbackVersion // Обязательно для логов Kafka
  )
)

// --- Определение модулей ---

// 1. Root (агрегатор)
lazy val root = (project in file("."))
  .settings(name := "crypto-alerting")
  .aggregate(core, ingestor, processor, api)

// 2. Core: Общие модели (case classes), Codecs
lazy val core = (project in file("core"))
  .settings(
    commonSettings,
    name := "crypto-core",
    libraryDependencies ++= Seq(
      // JSON (Circe - стандарт де-факто для fs2-kafka и tapir)
      "io.circe" %% "circe-core"    % circeVersion,
      "io.circe" %% "circe-generic" % circeVersion,
      "io.circe" %% "circe-parser"  % circeVersion,
      // Config
      "com.github.pureconfig" %% "pureconfig" % pureConfigVersion
    )
  )

// 3. Ingestor: Читает API -> Пишет в Kafka
lazy val ingestor = (project in file("ingestor"))
  .dependsOn(core)
  .settings(
    commonSettings,
    name := "crypto-ingestor",
    libraryDependencies ++= Seq(
      // Kafka Client
      "com.github.fd4s" %% "fs2-kafka" % fs2KafkaVersion,
      // HTTP Client (sttp)
      "com.softwaremill.sttp.client4" %% "core" % sttpVersion,
      "com.softwaremill.sttp.client4" %% "circe" % sttpVersion,
      "com.softwaremill.sttp.client4" %% "fs2" % sttpVersion // Backend для fs2
    )
  )

// 4. Processor: Читает Kafka -> Считает -> Пишет в Kafka
lazy val processor = (project in file("processor"))
  .dependsOn(core)
  .settings(
    commonSettings,
    name := "crypto-processor",
    libraryDependencies ++= Seq(
      "com.github.fd4s" %% "fs2-kafka" % fs2KafkaVersion
    )
  )

// 5. API: Читает Kafka -> Пишет в DB -> Раздает HTTP
lazy val api = (project in file("api"))
  .dependsOn(core)
  .settings(
    commonSettings,
    name := "crypto-api",
    libraryDependencies ++= Seq(
      "com.github.fd4s" %% "fs2-kafka" % fs2KafkaVersion,
      // Database (Doobie + Postgres)
      "org.tpolecat" %% "doobie-core"     % doobieVersion,
      "org.tpolecat" %% "doobie-hikari"   % doobieVersion,
      "org.tpolecat" %% "doobie-postgres" % doobieVersion,
      // HTTP Server (Tapir + Netty/Vertx/Http4s)
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % tapirVersion,
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe"    % tapirVersion,
      "org.http4s"                  %% "http4s-ember-server" % "0.23.33" // Сервер
    )
  )