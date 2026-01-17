# V2 Upgrade Guide - Frontend Integration & Monitoring

## Overview
Your Kafka application has been upgraded with comprehensive frontend integration, enhanced monitoring, and logging capabilities.

## Key Improvements

### 1. **Backend Enhancements**

#### Dependencies Added (pom.xml)
- **Spring Boot Actuator** - Health checks and monitoring endpoints
- **Micrometer** - Application metrics collection
- **Jackson** - JSON serialization/deserialization

#### New Features
- **MessageStorageService** - In-memory message storage with thread-safe operations
- **Message Metrics** - Counters for messages sent and consumed
- **Enhanced Logging** - DEBUG level for app packages with formatted console output
- **CORS Configuration** - Secure cross-origin requests from React frontend
- **DTOs** - Structured API requests and responses

#### New Endpoints
```
POST   /api/messages/send           - Send message via query parameter
POST   /api/messages/send-json      - Send message via JSON body
GET    /api/messages                - Retrieve all messages
GET    /api/messages/{id}           - Get specific message by ID
GET    /api/messages/count          - Get total message count
DELETE /api/messages                - Clear all messages
GET    /api/messages/health         - API health check
```

#### Monitoring Endpoints (Actuator)
```
GET    /actuator/health             - Application health
GET    /actuator/metrics            - Available metrics
GET    /actuator/metrics/{metric}   - Specific metric details
```

### 2. **Frontend - React UI**

#### New Features
- **Message Form** - Send messages with real-time validation
- **Message List** - Auto-refreshing list of consumed messages (2-second intervals)
- **Statistics Dashboard** - Display message count and connection status
- **Error Handling** - User-friendly error messages
- **Clear Messages** - Bulk delete with confirmation
- **Modern Dark Theme** - Professional gradient-based UI design
- **Responsive Design** - Mobile-friendly layout

#### Components Structure
- Main App component with hooks (useState, useEffect)
- Integrated API client with fetch API
- Real-time message polling

### 3. **Configuration Updates**

#### application.properties
```properties
# Consumer Configuration
spring.kafka.consumer.group-id=kafka-group
spring.kafka.consumer.auto-offset-reset=earliest

# Logging Configuration
logging.level.root=INFO
logging.level.com.kafka.app=DEBUG
logging.pattern.console=%d{yyyy-MM-dd HH:mm:ss} - %logger{36} - %msg%n

# Actuator Configuration
management.endpoints.web.exposure.include=health,metrics,prometheus
management.endpoint.health.show-details=always
```

## Getting Started

### 1. Build Backend
```bash
mvn clean install
mvn spring-boot:run
```
The application will be available at `http://localhost:8080`

### 2. Start Frontend
```bash
cd frontend
npm install
npm run dev
```
The frontend will be available at `http://localhost:5173`

### 3. Verify Setup
- Health Check: `http://localhost:8080/api/messages/health`
- Metrics: `http://localhost:8080/actuator/metrics`
- Frontend: `http://localhost:5173`

## API Usage Examples

### Send a Message
```bash
curl -X POST "http://localhost:8080/api/messages/send?message=Hello%20Kafka"
```

### Get All Messages
```bash
curl http://localhost:8080/api/messages
```

### Get Message Count
```bash
curl http://localhost:8080/api/messages/count
```

### View Metrics
```bash
curl http://localhost:8080/actuator/metrics
curl http://localhost:8080/actuator/metrics/kafka.messages.sent
curl http://localhost:8080/actuator/metrics/kafka.messages.consumed
```

## Project Structure

```
src/main/java/com/kafka/app/
├── KafkaApplication.java           # Main Spring Boot app
├── config/
│   └── CorsConfig.java            # CORS configuration
├── controller/
│   └── MessageController.java      # REST API endpoints
├── producer/
│   └── KafkaProducer.java         # Kafka producer with metrics
├── consumer/
│   └── KafkaConsumer.java         # Kafka consumer with storage
├── service/
│   └── MessageStorageService.java # Message storage logic
├── dto/
│   ├── MessageRequest.java        # Request DTO
│   └── ApiResponse.java           # Response wrapper
└── model/
    └── Message.java               # Message domain model

frontend/
├── src/
│   ├── App.jsx                    # Main React component
│   ├── App.css                    # Styled with dark theme
│   └── index.css                  # Global styles
├── package.json
└── vite.config.js
```

## Monitoring & Logging

### Debug Logs
All application logs are set to DEBUG level in `com.kafka.app` package:
- Producer sends
- Consumer receives
- Controller requests
- Message storage operations

### Metrics
Available custom metrics:
- `kafka.messages.sent` - Counter for successfully sent messages
- `kafka.messages.consumed` - Counter for consumed messages
- `kafka.messages.failed` - Counter for failed sends

### Health Check
Access `/actuator/health` for application status:
- UP: All services running
- Shows component details (Kafka connectivity, etc.)

## Features Breakdown

### Message Form
- Real-time input validation
- Loading state with spinner animation
- Error message display
- Disabled state during submission

### Message Display
- Message ID (truncated for readability)
- Message content with word wrapping
- Status badge (PROCESSED)
- Formatted timestamp
- Hover effects for better UX

### Statistics
- Total message count
- Connection status
- Real-time updates via polling

### Responsive Design
- Desktop: Multi-column layout
- Tablet: Adaptive grid
- Mobile: Single column stack

## Next Steps for Further Enhancement

1. **Database Integration**
   - Replace in-memory storage with PostgreSQL
   - Add persistent message history

2. **Authentication**
   - Add Spring Security
   - Implement JWT tokens

3. **Message Filtering**
   - Filter by date range
   - Search functionality
   - Pagination

4. **Advanced Analytics**
   - Message throughput charts
   - Performance metrics dashboard
   - Real-time monitoring graphs

5. **WebSocket Support**
   - Replace polling with WebSocket
   - Real-time message updates

6. **Message Topics**
   - Support multiple Kafka topics
   - Topic management UI

7. **Testing**
   - Integration tests
   - E2E tests with Cypress
   - Load testing

## Troubleshooting

### Frontend can't connect to backend
- Verify backend is running on port 8080
- Check CORS configuration in CorsConfig.java
- Ensure frontend URL is whitelisted

### Messages not appearing
- Check Kafka is running: `docker-compose ps`
- Verify consumer group: `docker exec -it <kafka> kafka-consumer-groups --bootstrap-server localhost:9092 --list`
- Check logs: `curl http://localhost:8080/actuator/health`

### Performance Issues
- Messages list max-height is 500px (scrollable)
- Polling interval set to 2 seconds (adjustable in App.jsx)
- Consider WebSocket for real-time updates

## Deployment Notes

For production deployment:
1. Update CORS allowed origins in CorsConfig.java
2. Set proper environment variables in application.properties
3. Enable metrics export (Prometheus format available)
4. Add authentication/authorization
5. Use database instead of in-memory storage
6. Configure logging to file system
7. Set up monitoring alerts

---

**Version:** 2.0.0  
**Last Updated:** January 17, 2026
