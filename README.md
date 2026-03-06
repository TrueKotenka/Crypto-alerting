# Crypto Alerting System

An event-driven, microservices-based application built with **Scala** and **Functional Programming** principles. This system monitors real-time cryptocurrency prices, detects significant price fluctuations, and generates alerts using a distributed messaging pipeline.



## 📖 Overview

This project was developed to demonstrate advanced Scala concepts, including functional concurrency, streaming, and event-driven architecture. It consists of three independent microservices communicating asynchronously via **Apache Kafka**.

### Core Features:
* **Real-time Data Ingestion:** Fetches live cryptocurrency tickers from the Binance Public API.
* **Stream Processing:** Analyzes price streams on the fly to detect percentage-based fluctuations.
* **Thread-Safe State Management:** Uses Cats Effect `Ref` for lock-free, concurrent state management of historical prices.
* **Persistent Storage:** Stores triggered alerts in a PostgreSQL database using Doobie.
* **REST API:** Exposes endpoints to query historical alerts.

## 🛠 Tech Stack

* **Language:** Scala 2.13.18
* **Concurrency & Effects:** Cats Effect 3
* **Streaming:** FS2, fs2-kafka
* **HTTP Client & Server:** sttp, Tapir, Http4s
* **Database & SQL:** PostgreSQL, Doobie
* **JSON Serialization:** Circe
* **Configuration:** PureConfig
* **Infrastructure:** Docker, Docker Compose, Apache Kafka, Zookeeper

## Project Structure

The project is structured as an `sbt` multi-module build to enforce a strict separation of concerns:

* `core/`: Shared domain models (`CryptoPrice`, `PriceAlert`), JSON codecs, and configuration definitions.
* `ingestor/`: A producer service that polls the Binance API and publishes raw prices to the `crypto-prices-raw` Kafka topic.
* `processor/`: The analytical engine. It consumes raw prices, calculates deltas using an in-memory state (`Ref`), and publishes alerts to the `crypto-alerts` topic if the threshold is breached.
* `api/`: A dual-purpose service that runs two concurrent background fibers:
  1. A Kafka Consumer saving new alerts to PostgreSQL.
  2. An HTTP Server (Tapir/Http4s) exposing the REST API.

## How to Run

### 1. Start the Infrastructure
Make sure you have Docker and Docker Compose installed. Start Kafka, Zookeeper, Kafdrop, and PostgreSQL:
```bash
docker-compose up -d

```

*Note: Kafdrop (Kafka Web UI) will be available at `http://localhost:9000`.*

### 2. Run the Microservices

Open three separate terminal windows and start the services via `sbt`:

**Terminal 1 (Ingestor):**

```bash
sbt "project ingestor" run

```

**Terminal 2 (Processor):**

```bash
sbt "project processor" run

```

**Terminal 3 (API & Database Consumer):**

```bash
sbt "project api" run

```

## API Usage

Once the API service is running, you can fetch the generated alerts. The server runs on port `8080`.

**Get all alerts:**

```bash
curl -X GET "http://localhost:8080/api/v1/alerts"

```

**Get alerts filtered by symbol (e.g., BTCUSDT):**

```bash
curl -X GET "http://localhost:8080/api/v1/alerts?symbol=BTCUSDT"

```
