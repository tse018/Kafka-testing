# V2 Architecture & Flow Diagrams

## System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        KAFKA ECOSYSTEM                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────────────────────┐         ┌──────────────────────┐     │
│  │   React Frontend     │         │   Spring Boot App    │     │
│  │  (Port: 5173)        │         │   (Port: 8080)       │     │
│  │                      │         │                      │     │
│  │  ├─ Message Form     │         │  ├─ REST Controller  │     │
│  │  ├─ Message List     │◄───────►├─ KafkaProducer      │     │
│  │  ├─ Stats Dashboard  │  HTTP   ├─ KafkaConsumer      │     │
│  │  └─ Dark UI          │  CORS   ├─ Message Storage    │     │
│  │                      │         └─ Metrics Actuator   │     │
│  └──────────────────────┘         └──────────────────────┘     │
│                                            │                    │
│                                            │                    │
│                            ┌───────────────┼───────────────┐    │
│                            │               │               │    │
│                            ▼               ▼               ▼    │
│                      ┌──────────┐   ┌──────────┐   ┌──────────┐│
│                      │ Zookeeper│   │  Kafka   │   │ Metrics  ││
│                      │          │   │ Broker   │   │ Registry ││
│                      └──────────┘   └──────────┘   └──────────┘│
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

## Message Flow Sequence

```
CLIENT SIDE (Frontend)                SERVER SIDE (Backend)
═══════════════════════════════════════════════════════════════════

User Input
    │
    ▼
┌─────────────────┐
│  Send Message   │
│  (Form Submit)  │
└────────┬────────┘
         │
         │ POST /api/messages/send
         │ (User input text)
         ├────────────────────────┐
         │                        │
         │                    MessageController
         │                        │
         │                        ▼
         │                   KafkaProducer
         │                        │
         │                        ├─► metrics.sent++
         │                        │
         │                        ▼
         │                   Kafka Topic
         │                    ("messages")
         │                        │
         │                    (In Kafka)
         │                        │
         │                        ▼
         │                   KafkaConsumer
         │                        │
         │                        ├─► metrics.consumed++
         │                        │
         │                        ▼
         │                MessageStorageService
         │                        │
         │                        ├─► Store Message
         │                        │
         ◄────────────────────────┘
         │
    (Every 2 sec)
    GET /api/messages
         │
         ├─► Fetch from Storage
         │
    Response with
    All Messages
         │
         ▼
    ┌─────────────────┐
    │ Update UI List  │
    │ Display Message │
    └─────────────────┘
```

## Component Architecture

```
┌────────────────────────────────────────────────────────────────┐
│                    SPRING BOOT APPLICATION                     │
├────────────────────────────────────────────────────────────────┤
│                                                                 │
│  Configuration Layer                                           │
│  ┌──────────────────────────────────────────────────────────┐ │
│  │ CorsConfig          - Frontend access control             │ │
│  │ application.properties - Kafka & logging setup            │ │
│  └──────────────────────────────────────────────────────────┘ │
│                              │                                 │
│  ┌──────────────────────────┴─────────────────────────────────┐│
│  │                                                            ││
│  │  Presentation Layer                                       ││
│  │  ┌─────────────────────────────────────────────────────┐ ││
│  │  │ MessageController                                   │ ││
│  │  │                                                     │ ││
│  │  │ ├─ POST   /api/messages/send        (sendMessage)  │ ││
│  │  │ ├─ POST   /api/messages/send-json   (sendJsonMsg)  │ ││
│  │  │ ├─ GET    /api/messages             (getAllMsg)    │ ││
│  │  │ ├─ GET    /api/messages/{id}        (getById)      │ ││
│  │  │ ├─ GET    /api/messages/count       (getCount)     │ ││
│  │  │ ├─ DELETE /api/messages             (clearAll)     │ ││
│  │  │ ├─ GET    /api/messages/health      (health)       │ ││
│  │  │ └─ GET    /actuator/...             (monitoring)   │ ││
│  │  └─────────────────────────────────────────────────────┘ ││
│  │                              │                             ││
│  │  Business Logic Layer        │                             ││
│  │  ┌──────────────────────────┴──────────────────────────┐ ││
│  │  │                                                     │ ││
│  │  │  ┌──────────────────┐    ┌───────────────────────┐ │ ││
│  │  │  │ KafkaProducer    │    │ KafkaConsumer         │ │ ││
│  │  │  │                  │    │                       │ ││ ││
│  │  │  │ • sendMessage()  │    │ • consume()           │ ││ ││
│  │  │  │ • Metrics track  │    │ • Store messages      │ ││ ││
│  │  │  │ • Error handling │    │ • Metrics track       │ ││ ││
│  │  │  └──────────────────┘    └─────────┬─────────────┘ ││ ││
│  │  │                                     │                │ ││
│  │  │  ┌──────────────────────────────────┴─────────────┐ │ ││
│  │  │  │ MessageStorageService                         │ │ ││
│  │  │  │                                               │ ││ ││
│  │  │  │ • addMessage()                                │ ││ ││
│  │  │  │ • getAllMessages()                            │ ││ ││
│  │  │  │ • getMessageById()                            │ ││ ││
│  │  │  │ • getMessageCount()                           │ ││ ││
│  │  │  │ • clearMessages()                             │ ││ ││
│  │  │  │                                               │ ││ ││
│  │  │  │ Data: CopyOnWriteArrayList<Message>          │ ││ ││
│  │  │  └───────────────────────────────────────────────┘ │ ││
│  │  └──────────────────────────────────────────────────── ┘ ││
│  │                                                           ││
│  │  Data Layer                                              ││
│  │  ┌──────────────────────────────────────────────────────┐││
│  │  │ Domain Models & DTOs                                │││
│  │  │                                                      │││
│  │  │ • Message                 - Domain model             │││
│  │  │ • MessageRequest           - Request DTO             │││
│  │  │ • ApiResponse<T>           - Response wrapper        │││
│  │  └──────────────────────────────────────────────────────┘││
│  └────────────────────────────────────────────────────────────┘│
│                                                                 │
└────────────────────────────────────────────────────────────────┘
                              │
              ┌───────────────┼───────────────┐
              │               │               │
              ▼               ▼               ▼
        ┌─────────────┐ ┌──────────┐  ┌────────────┐
        │ Kafka Topic │ │Micrometer│  │   Logs    │
        │ "messages"  │ │ Metrics  │  │  Console  │
        └─────────────┘ └──────────┘  └────────────┘
```

## React Frontend Component Tree

```
┌──────────────────────────────────────────────────────────────┐
│                       App Component                          │
│                    (Main React App)                          │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  State Management:                                          │
│  • messages: Message[]                                     │
│  • inputValue: string                                      │
│  • loading: boolean                                        │
│  • error: string                                           │
│  • stats: { total, sent, consumed }                        │
│                                                              │
│  Hooks:                                                     │
│  • useEffect() - Auto-fetch every 2 seconds               │
│  • useState() - State management                           │
│                                                              │
│  Functions:                                                 │
│  • fetchMessages() - GET /api/messages                     │
│  • sendMessage() - POST /api/messages/send                 │
│  • clearMessages() - DELETE /api/messages                  │
│                                                              │
├──────────────────────────────────────────────────────────────┤
│                      JSX Structure                          │
│                                                              │
│  ┌────────────────────────────────────────────────────┐   │
│  │  Header Section                                    │   │
│  │  ┌──────────────────────────────────────────────┐ │   │
│  │  │ Title: "Kafka Message Hub"                  │ │   │
│  │  │ Subtitle: "Real-time messaging..."         │ │   │
│  │  └──────────────────────────────────────────────┘ │   │
│  └────────────────────────────────────────────────────┘   │
│                         │                                  │
│  ┌────────────────────────────────────────────────────┐   │
│  │  Form Section                                      │   │
│  │  ┌──────────────────────────────────────────────┐ │   │
│  │  │ Label: "Send a Message"                     │ │   │
│  │  │ Input: textbox (message)                    │ │   │
│  │  │ Button: "Send" / "Sending..."               │ │   │
│  │  │ Error: error message (if exists)            │ │   │
│  │  └──────────────────────────────────────────────┘ │   │
│  └────────────────────────────────────────────────────┘   │
│                         │                                  │
│  ┌────────────────────────────────────────────────────┐   │
│  │  Stats Section (Grid Layout)                       │   │
│  │  ┌──────────────────┐  ┌──────────────────┐      │   │
│  │  │ Total Messages   │  │ Connection       │      │   │
│  │  │ {count}          │  │ Status           │      │   │
│  │  └──────────────────┘  └──────────────────┘      │   │
│  └────────────────────────────────────────────────────┘   │
│                         │                                  │
│  ┌────────────────────────────────────────────────────┐   │
│  │  Messages Section (Scrollable)                     │   │
│  │  ┌──────────────────────────────────────────────┐ │   │
│  │  │ Header: "Messages (X)" + Clear Button        │ │   │
│  │  └──────────────────────────────────────────────┘ │   │
│  │  ┌──────────────────────────────────────────────┐ │   │
│  │  │ Messages List (max-height: 500px)           │ │   │
│  │  │                                              │ │   │
│  │  │  Each Message Item:                          │ │   │
│  │  │  ┌──────────────────────────────────────┐  │ │   │
│  │  │  │ ID: xxx...    Status: PROCESSED     │  │ │   │
│  │  │  │ Content: "Message text here..."     │  │ │   │
│  │  │  │ Time: 14:35:22                      │  │ │   │
│  │  │  └──────────────────────────────────────┘  │ │   │
│  │  │  ┌──────────────────────────────────────┐  │ │   │
│  │  │  │ ID: yyy...    Status: PROCESSED     │  │ │   │
│  │  │  │ Content: "Another message..."       │  │ │   │
│  │  │  │ Time: 14:34:19                      │  │ │   │
│  │  │  └──────────────────────────────────────┘  │ │   │
│  │  │  ...                                        │ │   │
│  │  │                                              │ │   │
│  │  │  OR (No messages):                           │ │   │
│  │  │  "No messages yet. Send one to get started!"│ │   │
│  │  └──────────────────────────────────────────────┘ │   │
│  └────────────────────────────────────────────────────┘   │
│                                                              │
└──────────────────────────────────────────────────────────────┘
```

## API Response Format

```
Success Response (200 OK):
{
  "success": true,
  "message": "Messages retrieved successfully",
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "content": "Hello Kafka",
      "timestamp": 1705516200000,
      "status": "PROCESSED"
    },
    {
      "id": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
      "content": "Test message",
      "timestamp": 1705516201000,
      "status": "PROCESSED"
    }
  ],
  "timestamp": 1705516512345
}

Error Response (500 Internal Server Error):
{
  "success": false,
  "message": "Failed to send message: Connection refused",
  "data": null,
  "timestamp": 1705516512345
}
```

## Data Flow Overview

```
┌────────────────────────────────────────────────────────────────┐
│                                                                │
│  FRONTEND                    KAFKA BROKER                       │
│  ┌──────────────┐                                              │
│  │ React App    │                                              │
│  │              │                                              │
│  │ Send Message ├──► POST /api/messages/send ──┐               │
│  │              │                               │               │
│  │              │                               ▼               │
│  │              │                    ┌──────────────────┐      │
│  │              │                    │ KafkaProducer    │      │
│  │              │                    │                  │      │
│  │              │                    │ • Sends to topic │      │
│  │              │                    │ • Updates metric │      │
│  │              │                    └─────────┬────────┘      │
│  │              │                              │               │
│  │              │                              ▼               │
│  │              │                    ┌──────────────────┐      │
│  │              │◄─────────── GET    │ Topic: messages  │      │
│  │              │  /api/messages     │                  │      │
│  │              │                    │ Queue of msgs    │      │
│  │              │                    └────────┬─────────┘      │
│  │              │                             │                │
│  │ Display List │◄────────────────────────────┤                │
│  │ of Messages  │                             │                │
│  │              │                             ▼                │
│  │              │                   ┌──────────────────┐       │
│  │              │                   │ KafkaConsumer    │       │
│  │              │                   │                  │       │
│  │              │                   │ • Reads messages │       │
│  │              │                   │ • Calls storage  │       │
│  │              │                   │ • Updates metric │       │
│  │              │                   └─────────┬────────┘       │
│  │              │                             │                │
│  │              │                             ▼                │
│  │              │                   ┌──────────────────┐       │
│  │              │                   │ MessageStorage   │       │
│  │              │                   │                  │       │
│  │              │                   │ In-Memory List:  │       │
│  │              │                   │ • { id, content, │       │
│  │              │                   │    timestamp,    │       │
│  │              │                   │    status }      │       │
│  │              │                   └──────────────────┘       │
│  │              │                                              │
│  └──────────────┘                                              │
│                                                                │
│  Every 2 seconds: Fetch from storage and update display        │
│                                                                │
└────────────────────────────────────────────────────────────────┘
```

## Database vs In-Memory Storage (Current Implementation)

```
CURRENT (V2.0) - IN-MEMORY STORAGE:
╔════════════════════════════════════╗
║  Runtime Memory (JVM Heap)         ║
║  ┌────────────────────────────────┐║
║  │ MessageStorageService          ││
║  │ CopyOnWriteArrayList<Message>  ││
║  │ • Message 1                    ││
║  │ • Message 2                    ││
║  │ • Message 3                    ││
║  │ ...                            ││
║  └────────────────────────────────┘║
║  ✓ Thread-safe                     ║
║  ✓ Fast access                     ║
║  ✗ Lost on restart                 ║
║  ✗ Not scalable                    ║
╚════════════════════════════════════╝

FUTURE (V3.0) - DATABASE STORAGE:
╔════════════════════════════════════╗
║  Database (PostgreSQL)              ║
║  ┌────────────────────────────────┐║
║  │ messages table                 ││
║  │ • id (UUID, PK)                ││
║  │ • content (text)               ││
║  │ • timestamp (bigint)           ││
║  │ • status (varchar)             ││
║  │ • created_at (timestamp)       ││
║  └────────────────────────────────┘║
║  ✓ Persistent                      ║
║  ✓ Scalable                        ║
║  ✓ Queryable                       ║
║  ✗ Slower access                   ║
║  ✗ Additional dependencies         ║
╚════════════════════════════════════╝
```

---

These diagrams visualize the complete architecture, message flow, component structure, and data persistence model of your V2 application.
