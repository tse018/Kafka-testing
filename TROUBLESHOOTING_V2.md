# V2 Troubleshooting Guide

## Common Issues & Solutions

### 1. Frontend Connection Issues

#### Issue: "Cannot GET http://localhost:5173"
**Problem:** Frontend development server isn't running

**Solution:**
```bash
cd frontend
npm install
npm run dev
```
Frontend should open at `http://localhost:5173`

---

#### Issue: Frontend shows "API is not responding"
**Problem:** Backend is not running or CORS is blocked

**Solution:**
1. **Check Backend**
   ```bash
   # Terminal 1
   mvn spring-boot:run
   # Should see "Started KafkaApplication"
   ```

2. **Verify Connection**
   ```bash
   curl http://localhost:8080/api/messages/health
   # Should return 200 with success: true
   ```

3. **Check Console Errors** (Browser DevTools)
   - Press F12
   - Go to Console tab
   - Look for CORS errors or network errors
   - Check Network tab for failed requests

4. **Verify CORS Config**
   - Backend CORS allows `http://localhost:5173`
   - See: [CorsConfig.java](src/main/java/com/kafka/app/config/CorsConfig.java)

---

#### Issue: Messages sent but don't appear in UI
**Problem:** Messages not being stored or fetched

**Solution:**
1. **Check Kafka connection**
   ```bash
   docker-compose ps
   # Should show kafka and zookeeper running
   ```

2. **Verify Storage Service**
   - Check logs for "Message consumed and stored successfully"
   - Ensure MessageStorageService is autowired

3. **Check API endpoint**
   ```bash
   curl http://localhost:8080/api/messages
   # Should return JSON array with messages
   ```

4. **Restart services**
   ```bash
   # Stop and restart backend
   # Stop and restart frontend
   ```

---

### 2. Backend Issues

#### Issue: Maven build fails
**Problem:** Compilation errors or dependency issues

**Solution:**
```bash
# Clean cache
mvn clean

# Verify Java version (should be 17+)
java -version

# Try installing again
mvn install

# Check for specific errors
mvn compile -X
```

---

#### Issue: "Kafka Connection Refused"
**Problem:** Kafka broker not running

**Solution:**
```bash
# Check Docker status
docker-compose ps

# Start if not running
docker-compose up -d

# Verify Kafka logs
docker-compose logs kafka

# Expected: "Started KafkaServer"
```

---

#### Issue: "Error consuming message"
**Problem:** Consumer group issue or Kafka topic problem

**Solution:**
1. **Check topics exist**
   ```bash
   docker exec -it <kafka-container> \
     kafka-topics --bootstrap-server localhost:9092 --list
   # Should show: messages
   ```

2. **Check consumer group**
   ```bash
   docker exec -it <kafka-container> \
     kafka-consumer-groups --bootstrap-server localhost:9092 --list
   # Should show: kafka-group
   ```

3. **Reset consumer group (if stuck)**
   ```bash
   docker exec -it <kafka-container> \
     kafka-consumer-groups --bootstrap-server localhost:9092 \
     --group kafka-group --reset-offsets --all-topics \
     --to-earliest --execute
   ```

---

#### Issue: Metrics not showing
**Problem:** Actuator not enabled or metrics not generated

**Solution:**
1. **Check endpoints are exposed**
   - application.properties has:
     ```properties
     management.endpoints.web.exposure.include=health,metrics,prometheus
     ```

2. **Verify endpoints**
   ```bash
   curl http://localhost:8080/actuator
   curl http://localhost:8080/actuator/health
   curl http://localhost:8080/actuator/metrics
   ```

3. **Generate metrics (send a message)**
   - Use frontend or curl to send message
   - Metrics appear after first use

---

### 3. Frontend Issues

#### Issue: "npm ERR! Cannot find module"
**Problem:** Dependencies not installed

**Solution:**
```bash
cd frontend
rm -rf node_modules
rm package-lock.json
npm install
```

---

#### Issue: Vite dev server won't start
**Problem:** Port 5173 already in use or config issue

**Solution:**
```bash
# Check if port is in use
lsof -i :5173

# Kill process if needed
kill -9 <PID>

# Or change port in vite.config.js
```

---

#### Issue: Styling looks broken
**Problem:** CSS not loading properly

**Solution:**
1. **Clear browser cache** (Ctrl+Shift+Delete)
2. **Hard refresh** (Ctrl+Shift+R or Cmd+Shift+R)
3. **Check dev console** for CSS errors
4. **Rebuild frontend**
   ```bash
   npm run build
   npm run preview
   ```

---

#### Issue: Messages keep disappearing on refresh
**Problem:** In-memory storage is lost on server restart

**Solution:**
- This is expected behavior with V2.0 (in-memory storage)
- **Planned for V3.0:** Database persistence
- **Temporary workaround:** Don't restart backend during testing

---

### 4. Network Issues

#### Issue: Cannot reach localhost:8080 from frontend
**Problem:** Port 8080 blocked or backend not accessible

**Solution:**
```bash
# Test connectivity
curl -v http://localhost:8080/api/messages/health

# Check if service listening
netstat -tuln | grep 8080
lsof -i :8080

# Verify firewall isn't blocking
# (usually not an issue on localhost)
```

---

#### Issue: CORS error in browser console
**Problem:** Origin not allowed by backend

**Solution:**
1. **Check browser error message**
   - Should show which origin was blocked

2. **Update CorsConfig.java**
   ```java
   registry.addMapping("/api/**")
           .allowedOrigins("http://localhost:5173")  // Add your origin
           .allowedMethods("GET", "POST", "DELETE", "OPTIONS")
           .allowedHeaders("*")
           .allowCredentials(true);
   ```

3. **Rebuild and restart**
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

---

### 5. Data Issues

#### Issue: Duplicate messages appearing
**Problem:** Consumer processing same message multiple times

**Solution:**
```bash
# Reset consumer offset
docker exec -it <kafka> kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group kafka-group \
  --reset-offsets --to-latest \
  --all-topics --execute

# Restart backend
# Kill and restart mvn spring-boot:run
```

---

#### Issue: Message content truncated or malformed
**Problem:** JSON parsing error or encoding issue

**Solution:**
1. **Check message format**
   ```bash
   curl -X POST "http://localhost:8080/api/messages/send?message=Hello%20World"
   # URL encode spaces as %20
   ```

2. **Use JSON endpoint instead**
   ```bash
   curl -X POST http://localhost:8080/api/messages/send-json \
     -H "Content-Type: application/json" \
     -d '{"message":"Hello World"}'
   ```

3. **Check Kafka logs**
   ```bash
   docker-compose logs kafka | tail -50
   ```

---

### 6. Performance Issues

#### Issue: UI becomes slow with many messages
**Problem:** Too many messages in memory

**Solution:**
1. **Clear old messages**
   ```bash
   curl -X DELETE http://localhost:8080/api/messages
   ```

2. **Implement pagination** (planned for V3.0)

3. **Increase message list height temporarily**
   - Edit App.css
   - Change `.messages-list { max-height: 500px; }` to larger value

---

#### Issue: Polling causes high CPU usage
**Problem:** 2-second polling interval too aggressive

**Solution:**
Edit [App.jsx](frontend/src/App.jsx):
```javascript
// Change this line:
const interval = setInterval(fetchMessages, 2000)  // 2 seconds

// To:
const interval = setInterval(fetchMessages, 5000)  // 5 seconds
```

---

### 7. Kafka-Specific Issues

#### Issue: "Topic does not exist"
**Problem:** Messages topic wasn't created

**Solution:**
```bash
# Create topic manually
docker exec -it <kafka-container> \
  kafka-topics --create \
  --topic messages \
  --bootstrap-server localhost:9092 \
  --partitions 1 \
  --replication-factor 1

# Verify
docker exec -it <kafka-container> \
  kafka-topics --list --bootstrap-server localhost:9092
```

---

#### Issue: Consumer lag or offset issues
**Problem:** Consumer stuck at old offset

**Solution:**
```bash
# Check current offset
docker exec -it <kafka> kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group kafka-group \
  --describe

# Reset to latest
docker exec -it <kafka> kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --group kafka-group \
  --reset-offsets --to-latest \
  --all-topics --execute
```

---

### 8. Logging Issues

#### Issue: No debug logs appearing
**Problem:** Logging level not set to DEBUG

**Solution:**
Check `src/main/resources/application.properties`:
```properties
logging.level.com.kafka.app=DEBUG
```

Restart backend:
```bash
mvn spring-boot:run
```

---

#### Issue: Too many logs - want to reduce noise
**Problem:** INFO level showing too much

**Solution:**
Update `application.properties`:
```properties
logging.level.root=WARN
logging.level.com.kafka.app=INFO  # Changed from DEBUG
```

---

### 9. Docker Issues

#### Issue: "docker-compose: command not found"
**Problem:** Docker Compose not installed

**Solution:**
```bash
# Install Docker Desktop (includes Compose)
# Or install separately:
brew install docker-compose  # macOS
apt-get install docker-compose  # Linux
```

---

#### Issue: Containers won't start
**Problem:** Port already in use or Docker daemon not running

**Solution:**
```bash
# Check Docker is running
docker ps

# If not, start Docker Desktop

# Kill conflicting processes
lsof -i :2181  # Zookeeper
lsof -i :9092  # Kafka

# Remove old containers
docker-compose down -v

# Start fresh
docker-compose up -d
```

---

## Diagnostic Checklist

Use this checklist when something breaks:

### ✓ Is Docker running?
```bash
docker-compose ps
# Both kafka and zookeeper should be "Up"
```

### ✓ Is backend running?
```bash
curl http://localhost:8080/api/messages/health
# Should return: {"success":true,"message":"API is healthy","data":"Running"}
```

### ✓ Is frontend running?
```bash
# Browser: http://localhost:5173
# Should load without errors
```

### ✓ Can they communicate?
```bash
# In frontend console (F12)
fetch('http://localhost:8080/api/messages/health')
  .then(r => r.json())
  .then(d => console.log(d))
# Should print response object
```

### ✓ Is Kafka working?
```bash
curl http://localhost:8080/api/messages/send?message=test
# Send message via API
# Check backend logs for "Message consumed and stored"
```

### ✓ Is storage working?
```bash
curl http://localhost:8080/api/messages
# Should see test message in response
```

---

## Emergency Solutions

### "Everything is broken, start over"

```bash
# 1. Stop everything
docker-compose down -v
pkill -f "mvn spring-boot:run"
pkill -f "vite"

# 2. Clean builds
mvn clean
cd frontend && rm -rf node_modules package-lock.json

# 3. Start fresh
docker-compose up -d
mvn clean install
mvn spring-boot:run

# In another terminal:
cd frontend
npm install
npm run dev
```

### Still not working?

1. **Check logs carefully**
   - Backend: Look for actual error messages
   - Frontend: Browser DevTools F12 → Console
   - Docker: `docker-compose logs -f`

2. **Search issue online**
   - Copy exact error message
   - Include technology (Spring Boot, React, Kafka)

3. **Restart entire system**
   - Close all terminals
   - Restart Docker Desktop
   - Start fresh from top

---

## Getting Help

When asking for help, include:
1. **What you're trying to do**
2. **What error you see** (exact message)
3. **What you've already tried**
4. **Command output** (what you ran and what it returned)
5. **System info** (OS, Java version, Node version, Docker version)

**Example:**
```
I'm trying to send a message from the React frontend.
Error: "message is not receiving in the backend"

I've tried:
- Restarted backend and frontend
- Checked CORS config

Environment:
- macOS 13.6
- Java 17.0.1
- Node 18.12
- Docker Desktop 4.15

When I click Send, the backend logs show nothing.
The browser console shows no errors.
```

---

## Version Information

**Application Version:** 2.0.0  
**Java Version Required:** 17+  
**Node Version Required:** 16+  
**Spring Boot Version:** 3.2.1  
**React Version:** 19.2.0  
**Kafka Version:** Latest (via docker-compose)

---

Last Updated: January 17, 2026
