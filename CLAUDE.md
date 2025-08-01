# Claude Instructions

## Commit Guidelines
- Never include Claude as author in commits
- Always use the user's identity for git commits
- Never add "Co-Authored-By: Claude" to commit messages
- Keep commit messages concise and focused

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
```

### Backend (Spring Boot + Gradle)
```bash
export JAVA_HOME=/Users/jeffrey.jose/Library/Java/JavaVirtualMachines/corretto-17.0.15/Contents/Home
cd /Users/jeffrey.jose/cursorProjects/scalable-chat-platform/backend
./gradlew build -x test
```

### Full Build (Both)
```bash
# Backend builds both frontend and backend automatically
export JAVA_HOME=/Users/jeffrey.jose/Library/Java/JavaVirtualMachines/corretto-17.0.15/Contents/Home
cd /Users/jeffrey.jose/cursorProjects/scalable-chat-platform/backend
./gradlew build -x test
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