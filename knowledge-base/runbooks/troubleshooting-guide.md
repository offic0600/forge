# Troubleshooting Guide

## Common Issues

### 1. Backend fails to start: "Port 8080 already in use"

```bash
# Find and kill the process
lsof -ti:8080 | xargs kill -9

# Or change the port
./gradlew :web-ide:backend:bootRun --args='--server.port=8081'
```

### 2. Frontend shows blank page / API errors

**Symptoms**: Browser console shows CORS errors or 502 Bad Gateway.

**Cause**: Backend is not running or Nginx proxy is misconfigured.

**Fix**:
1. Verify backend is running: `curl http://localhost:8080/actuator/health`
2. If using Docker: `docker compose logs backend`
3. Check Nginx config: ensure `/api/` proxies to `backend:8080`

### 3. AI chat returns "Failed to get response"

**Symptoms**: Chat shows error after sending a message.

**Cause**: Missing or invalid `ANTHROPIC_API_KEY`.

**Fix**:
1. Check env var: `echo $ANTHROPIC_API_KEY`
2. For Docker: verify `ANTHROPIC_API_KEY` in docker-compose.yml
3. For local dev: set in `application-local.yml` or export in shell

### 4. Keycloak login redirect loop

**Symptoms**: Browser keeps redirecting between app and Keycloak.

**Cause**: Token exchange fails, often due to mismatched redirect URI.

**Fix**:
1. Check Keycloak client config: Valid Redirect URIs must include `http://localhost:9000/*`
2. Verify `NEXT_PUBLIC_KEYCLOAK_URL` matches the reachable Keycloak address
3. Clear browser localStorage: `localStorage.clear()`

### 5. "No files found" in Context Picker

**Symptoms**: Typing `@` in chat shows empty file list.

**Cause**: `/api/context/search` endpoint not available, or workspace has no files.

**Fix**:
1. Ensure you're in a workspace (URL should be `/workspace/<id>`)
2. Create some files first (via FileExplorer or AI)
3. Check backend logs for errors on the `/api/context/search` endpoint

### 6. File tree not refreshing after AI writes a file

**Symptoms**: AI says "I created the file" but file tree doesn't update.

**Cause**: `file_changed` SSE event not reaching the frontend.

**Fix**:
1. Check browser DevTools Network tab for SSE stream events
2. Verify `AiChatSidebar.tsx` handles `file_changed` event type
3. Verify `workspace/[id]/page.tsx` listens for `forge:file-changed` CustomEvent

### 7. Docker build fails: "Module not found"

**Symptoms**: `docker compose up --build` fails during frontend build.

**Fix**:
```bash
# Clear Docker build cache
docker compose build --no-cache

# Or rebuild specific service
docker compose build --no-cache frontend
```

### 8. Gradle build: "Could not resolve dependencies"

```bash
# Clean and rebuild
./gradlew clean build --refresh-dependencies

# Check Java version (requires JDK 21)
java -version
```

## Health Check Endpoints

| Service | Endpoint | Expected |
|---------|----------|----------|
| Backend | `GET /actuator/health` | `{"status":"UP"}` |
| Keycloak | `GET /auth/realms/forge` | Realm JSON |
| Frontend | `GET /` | HTML page |
| Nginx | `GET /health` | 200 OK |

## Log Locations (Docker)

```bash
docker compose logs -f backend    # Spring Boot logs
docker compose logs -f frontend   # Next.js logs
docker compose logs -f keycloak   # Keycloak logs
docker compose logs -f nginx      # Nginx access/error logs
```
