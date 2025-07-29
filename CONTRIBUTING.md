# Contributing to Scalable Chat Platform

Thank you for your interest in contributing to the Scalable Chat Platform! This document provides guidelines and information for contributors.

## Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Setup](#development-setup)
- [Contributing Process](#contributing-process)
- [Coding Standards](#coding-standards)
- [Testing](#testing)
- [Documentation](#documentation)
- [Commit Messages](#commit-messages)
- [Pull Request Process](#pull-request-process)
- [Issue Reporting](#issue-reporting)
- [Security](#security)

## Code of Conduct

This project adheres to a code of conduct that we expect all contributors to follow. Please be respectful and constructive in all interactions.

### Expected Behavior
- Use welcoming and inclusive language
- Be respectful of differing viewpoints and experiences
- Gracefully accept constructive criticism
- Focus on what is best for the community
- Show empathy towards other community members

## Getting Started

### Prerequisites

Before contributing, ensure you have:
- Java 17+ installed
- Node.js 18+ installed  
- Maven 3.8+ installed
- Docker and Docker Compose
- Git configured with your name and email

### First-Time Setup

1. **Fork the repository** on GitHub
2. **Clone your fork** locally:
   ```bash
   git clone https://github.com/YOUR_USERNAME/scalable-chat-platform.git
   cd scalable-chat-platform
   ```
3. **Add upstream remote**:
   ```bash
   git remote add upstream https://github.com/ORIGINAL_OWNER/scalable-chat-platform.git
   ```
4. **Set up development environment**:
   ```bash
   ./start-dev.sh
   ```

## Development Setup

Follow the [Development Setup Guide](docs/development/setup.md) for detailed instructions.

### Quick Development Start
```bash
# Start infrastructure
./start-dev.sh

# Start backend (Terminal 2)
cd backend && mvn spring-boot:run

# Start frontend (Terminal 3)
cd frontend && npm install && npm run start:network
```

## Contributing Process

### 1. Choose an Issue
- Look for issues labeled `good first issue` for beginners
- Check existing issues before creating new ones
- Comment on issues you'd like to work on

### 2. Create a Branch
```bash
git checkout -b feature/your-feature-name
# or
git checkout -b fix/issue-number-description
```

### 3. Make Changes
- Follow the coding standards below
- Write tests for new functionality
- Update documentation as needed
- Test your changes locally

### 4. Commit Your Changes
Follow our [commit message guidelines](#commit-messages):
```bash
git add .
git commit -m "feat: add user presence indicators"
```

### 5. Push and Create PR
```bash
git push origin your-branch-name
```
Then create a Pull Request through GitHub.

## Coding Standards

### Java (Backend)
- **Style**: Follow Google Java Style Guide
- **Version**: Use Java 17+ features where appropriate
- **Architecture**: Follow existing layered architecture
- **Dependencies**: Minimize external dependencies

```java
// Example: Use records for DTOs
public record MessageDto(
    String id,
    String content,
    String senderId,
    Instant timestamp
) {}

// Example: Use pattern matching
public String getMessageStatus(Message message) {
    return switch (message.getStatus()) {
        case SENT -> "✓";
        case DELIVERED -> "✓✓";
        case READ -> "✓✓";
        default -> "";
    };
}
```

### TypeScript/React (Frontend)
- **Style**: Use Prettier and ESLint configurations
- **Components**: Prefer functional components with hooks
- **Types**: Use TypeScript strictly, avoid `any`
- **State**: Use React hooks appropriately

```typescript
// Example: Properly typed component
interface MessageProps {
  message: Message;
  onReply: (messageId: string) => void;
}

const MessageComponent: React.FC<MessageProps> = ({ message, onReply }) => {
  const handleReply = useCallback(() => {
    onReply(message.id);
  }, [message.id, onReply]);

  return (
    <div className="message">
      <p>{message.content}</p>
      <button onClick={handleReply}>Reply</button>
    </div>
  );
};
```

### General Guidelines
- **Comments**: Write clear, necessary comments
- **Naming**: Use descriptive names for variables and functions
- **Functions**: Keep functions small and focused
- **Error Handling**: Handle errors gracefully
- **Security**: Never commit secrets or sensitive data

## Testing

### Backend Testing
```bash
# Run all tests
cd backend && mvn test

# Run specific test class
mvn test -Dtest=MessageServiceTest

# Run with coverage
mvn test jacoco:report
```

### Frontend Testing
```bash
# Run all tests
cd frontend && npm test

# Run with coverage
npm test -- --coverage

# Run E2E tests (if available)
npm run test:e2e
```

### Testing Requirements
- **Unit Tests**: Write tests for new services and utilities
- **Integration Tests**: Test API endpoints and database interactions
- **Component Tests**: Test React components with React Testing Library
- **Coverage**: Maintain reasonable test coverage (aim for >80%)

## Documentation

### Documentation Requirements
- Update README.md if you change setup process
- Add JSDoc comments for complex functions
- Update API documentation for new endpoints
- Add comments for non-obvious code

### Documentation Style
```java
/**
 * Sends a message to a conversation and distributes it to all participants.
 * 
 * @param conversationId The ID of the target conversation
 * @param senderId The ID of the user sending the message
 * @param content The message content
 * @return The created message with generated ID
 * @throws ConversationNotFoundException if conversation doesn't exist
 */
public Message sendMessage(String conversationId, String senderId, String content) {
    // Implementation
}
```

## Commit Messages

We follow the [Conventional Commits](https://www.conventionalcommits.org/) specification:

### Format
```
<type>[optional scope]: <description>

[optional body]

[optional footer(s)]
```

### Types
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation changes
- `style`: Code style changes (formatting, etc.)
- `refactor`: Code refactoring
- `test`: Adding or updating tests
- `chore`: Maintenance tasks

### Examples
```bash
# Feature
git commit -m "feat(chat): add read receipts for messages"

# Bug fix
git commit -m "fix(websocket): handle connection timeouts gracefully"

# Documentation
git commit -m "docs: update API documentation for auth endpoints"

# Breaking change
git commit -m "feat(api)!: change message format to include metadata"
```

## Pull Request Process

### Before Creating a PR
- [ ] Ensure your branch is up to date with main
- [ ] Run all tests locally
- [ ] Run linting and formatting
- [ ] Update documentation if needed
- [ ] Test your changes manually

### PR Requirements
1. **Title**: Use conventional commit format
2. **Description**: Clearly describe what and why
3. **Tests**: Include tests for new functionality
4. **Documentation**: Update relevant documentation
5. **Small PRs**: Keep changes focused and reviewable

### PR Template
```markdown
## Description
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Testing
- [ ] Unit tests pass
- [ ] Integration tests pass
- [ ] Manual testing completed

## Checklist
- [ ] Code follows style guidelines
- [ ] Self review completed
- [ ] Documentation updated
- [ ] No sensitive data committed
```

### Review Process
1. **Automated Checks**: CI/CD pipeline must pass
2. **Code Review**: At least one approving review required
3. **Discussion**: Address all review comments
4. **Merge**: Maintainers will merge approved PRs

## Issue Reporting

### Bug Reports
Use the bug report template and include:
- **Environment**: OS, browser, versions
- **Steps to Reproduce**: Clear, numbered steps
- **Expected Behavior**: What should happen
- **Actual Behavior**: What actually happens
- **Screenshots**: If applicable
- **Additional Context**: Any other relevant information

### Feature Requests
Use the feature request template and include:
- **Problem**: What problem does this solve?
- **Solution**: Proposed solution
- **Alternatives**: Alternative solutions considered
- **Use Cases**: How would this be used?

### Issue Labels
- `bug`: Something isn't working
- `enhancement`: New feature or request
- `documentation`: Improvements to documentation
- `good first issue`: Good for newcomers
- `help wanted`: Extra attention needed
- `priority/high`: High priority items

## Security

### Reporting Security Issues
**Do not create public issues for security vulnerabilities.**

Instead:
1. Email security issues to: [security-email]
2. Include detailed description of the vulnerability
3. Provide steps to reproduce if possible
4. Allow reasonable time for fixes before disclosure

### Security Guidelines
- Never commit secrets, API keys, or passwords
- Use environment variables for configuration
- Validate all user inputs
- Follow OWASP security practices
- Keep dependencies updated

## Development Tips

### Useful Commands
```bash
# Update dependencies
cd backend && mvn versions:use-latest-versions
cd frontend && npm update

# Format code
cd backend && mvn spotless:apply
cd frontend && npm run format

# Lint code
cd frontend && npm run lint

# Clean build
cd backend && mvn clean install
cd frontend && npm run build
```

### IDE Setup
- **IntelliJ IDEA**: Import as Maven project, enable annotation processing
- **VS Code**: Install Java Extension Pack and React extensions
- **Eclipse**: Import as existing Maven project

### Debugging
- **Backend**: Use your IDE's debugger or add logging
- **Frontend**: Use browser DevTools and React DevTools
- **Network**: Monitor WebSocket connections in browser
- **Database**: Use database clients to inspect data

## Getting Help

### Community Resources
- **Documentation**: Check [docs/](docs/) directory
- **Issues**: Search existing issues first
- **Discussions**: Use GitHub Discussions for questions

### Contact
- **General Questions**: Create a GitHub Discussion
- **Bug Reports**: Create a GitHub Issue
- **Security Issues**: Email [security-email]

## Recognition

Contributors are recognized in:
- GitHub contributor statistics
- Release notes for significant contributions
- Special recognition for major features

Thank you for contributing to the Scalable Chat Platform! 🚀