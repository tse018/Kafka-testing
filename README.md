# Spring Boot Kafka Application

A simple Spring Boot application demonstrating Kafka producer and consumer functionality.

## Prerequisites

- Java 17 or higher
- Maven 3.6 or higher
- Docker and Docker Compose

## Quick Start

### 1. Start Kafka with Docker Compose

```bash
docker-compose up -d
```

This will start:
- Zookeeper on port 2181
- Kafka broker on port 9092

### 2. Build and Run the Application

```bash
mvn clean install
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

### 3. Send a Message

Open your browser or use curl to send a message:

```bash
curl -X POST "http://localhost:8080/api/messages/send?message=Hello%20Kafka"
```

You should see in the console logs:
- Producer log: "Producing message: Hello Kafka"
- Consumer log: "Consumed message: Hello Kafka"

## Project Structure

```
src/
├── main/
│   ├── java/com/kafka/app/
│   │   ├── KafkaApplication.java       # Main Spring Boot app
│   │   ├── controller/
│   │   │   └── MessageController.java  # REST API endpoint
│   │   ├── producer/
│   │   │   └── KafkaProducer.java     # Kafka producer service
│   │   ├── consumer/
│   │   │   └── KafkaConsumer.java     # Kafka consumer service
│   │   └── model/
│   │       └── Message.java            # Message model
│   └── resources/
│       └── application.properties       # Spring Boot config
└── test/                                # Unit tests
```

## Configuration

Edit `src/main/resources/application.properties` to customize:
- Kafka bootstrap servers
- Consumer group ID
- Serialization/deserialization formats

## Stopping Services

```bash
# Stop Docker containers
docker-compose down

# Stop the Spring Boot application
# Press Ctrl+C in the terminal
```

## Troubleshooting

### Kafka connection refused
- Ensure Docker containers are running: `docker-compose ps`
- Check ports: `lsof -i :9092`

### Consumer not receiving messages
- Check consumer group: `docker exec -it <kafka-container> kafka-consumer-groups --bootstrap-server localhost:9092 --list`
- Verify topic exists: `docker exec -it <kafka-container> kafka-topics --bootstrap-server localhost:9092 --list`

## Next Steps

- Add JSON serialization with Jackson
- Implement error handling and retries
- Add integration tests
- Scale to multiple consumers
- Implement complex stream processing with Kafka Streams
