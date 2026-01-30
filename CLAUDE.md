# Claude Instructions

# ⚠️  CRITICAL COMMIT GUIDELINES ⚠️
## NEVER ADD CLAUDE AS AUTHOR IN ANY COMMITS
- Never include Claude as author in commits
- Always use the user's identity for git commits  
- Never add "Co-Authored-By: Claude" to commit messages
- Never add "Generated with Claude Code" to commit messages
- Keep commit messages concise and focused
- A git hook will reject commits with Claude authorship

## Development Preferences
- Follow existing code patterns and conventions
- Run linting and type checking before commits if available
- Only create documentation files when explicitly requested
- Prioritize editing existing files over creating new ones

## Security Guidelines
- Only assist with defensive security tasks
- Never create or improve code that could be used maliciously
- Focus on security analysis, detection rules, vulnerability explanations, and defensive tools

## Build Commands

### Frontend (React)
```bash
cd /Users/jeffrey.jose/cursorProjects/scalable-chat-platform/frontend
npm run build
# NEVER ADD CLAUDE AS AUTHOR
```

### Backend (Spring Boot + Gradle)
```bash
export JAVA_HOME=/Users/jeffrey.jose/Library/Java/JavaVirtualMachines/corretto-17.0.15/Contents/Home
cd /Users/jeffrey.jose/cursorProjects/scalable-chat-platform/backend
./gradlew build -x test
# NEVER ADD CLAUDE AS AUTHOR
```

### Full Build (Both)
```bash
# Backend builds both frontend and backend automatically
export JAVA_HOME=/Users/jeffrey.jose/Library/Java/JavaVirtualMachines/corretto-17.0.15/Contents/Home
cd /Users/jeffrey.jose/cursorProjects/scalable-chat-platform/backend
./gradlew build -x test
# NEVER ADD CLAUDE AS AUTHOR
```

## Common Issues & Fixes

### Java Version Issues
- **Problem**: Build fails with Java 8 vs Spring Boot 3.2.0 requiring Java 17
- **Solution**: Always set `JAVA_HOME=/Users/jeffrey.jose/Library/Java/JavaVirtualMachines/corretto-17.0.15/Contents/Home`

### API URL Double Prefix Issues
- **Problem**: Frontend calls `/api/api/endpoint` instead of `/api/endpoint`
- **Root Cause**: The `api` service in `services/api.ts` already prefixes with `/api`, so don't add `/api` to endpoint URLs
- **Fix**: Use `api.get('/admin/status')` NOT `api.get('/api/admin/status')`

### Token Storage & Authentication Issues
- **Problem**: Login page flashes on refresh, tokens not persisting
- **Root Cause**: Race condition in token initialization or using sessionStorage instead of localStorage
- **Fix**: Use synchronous token initialization and `localStorage` for persistent tokens

### Soft Delete vs Hard Delete Confusion
- **Problem**: Cleanup logic doesn't find "deleted" data
- **Root Cause**: System uses soft deletes (deletedAt timestamp) but cleanup looks for hard deletes
- **Fix**: Use `findAllActiveConversationIds()` (excludes soft-deleted) NOT `findAllConversationIds()` (includes all)

### TypeScript Interface Mismatches
- **Problem**: Frontend TypeScript errors when backend response structure changes
- **Solution**: Always update TypeScript interfaces when adding new fields to backend responses

### Missing Repository Methods
- **Problem**: Calling repository methods that don't exist
- **Solution**: Check existing methods first, add new ones with proper Spring Data naming conventions

## Architecture Notes

### Database Setup
- **PostgreSQL**: User data, conversations, participants (JPA/Hibernate)
- **MongoDB**: Chat messages (Spring Data MongoDB)
- **Soft Deletes**: Conversations use `deletedAt` timestamp, not physical deletion

### Authentication Flow
- **JWT tokens** stored in browser localStorage (persistent) or sessionStorage (temporary)
- **Token validation** on every API call via Authorization header
- **Refresh handling** via synchronous token initialization to prevent login page flash

### Cleanup Services
- **AdminDatabaseCleanupService**: Manual admin cleanup via UI
- **DatabaseCleanupService**: Scheduled automatic cleanup every 20 days
- **Three cleanup types**: Orphaned messages, soft-deleted messages, old conversations

### API Structure
- **Base URL**: `/api` prefix automatically added by api service
- **Authentication**: Bearer token in Authorization header
- **Response format**: `{success: boolean, message: string, data: T}`

## UI Components

### CreateGroupModal Search Implementation
- **Location**: `frontend/src/components/groups/CreateGroupModal.tsx:188-195`
- **Feature**: Real-time search filtering for participants when creating groups
- **Implementation**: Filters users by `displayName` and `email` using case-insensitive matching
- **State Management**: Uses `searchTerm` state with proper cleanup on form reset/cancel

## Code Quality Standards

### Backend (Java)
- **Magic Numbers**: Extract to named constants (e.g., `MESSAGE_QUEUE_CAPACITY`, `PENDING_MESSAGES_WINDOW_SECONDS`)
- **Thread Management**: Use `ScheduledExecutorService` instead of raw `Thread.sleep()` for delayed tasks
- **Lifecycle Annotations**: Always add `@PreDestroy` for cleanup methods on services with executors
- **Queue Handling**: Use bounded queues with fallback behavior instead of unbounded `LinkedBlockingQueue`
- **Logging**: Use emoji prefixes for log visibility (📤, ✅, ❌, ⚠️)

### Frontend (TypeScript/React)
- **Type Safety**: Avoid `any` type - use `unknown` with type narrowing or proper interfaces
- **Error Handling**: Use `error: unknown` in catch blocks with explicit type assertions
- **DTO Types**: Import and use proper DTO types (`ConversationDto`, `CreateGroupRequest`, etc.)
- **Hook Error Pattern**:
  ```typescript
  catch (error: unknown) {
    const axiosError = error as { response?: { data?: { message?: string } }; message?: string };
    const errorMessage = axiosError.response?.data?.message || axiosError.message || 'Default message';
  }
  ```

### Conversation/ConversationDto Mapping
- **Backend returns**: `ConversationDto` (with `name: string | null`)
- **Frontend uses**: `Conversation` (with `name: string`)
- **Casting**: Use `as Conversation` when passing `ConversationDto` to components expecting `Conversation`