# Testing Strategy

This document outlines the testing approach for the Chat Platform backend.

## 🧪 Test Types

### 1. Unit Tests (Current)
**Purpose**: Test individual components in isolation
**Framework**: JUnit 5
**Dependencies**: None (Plain Java objects)

```java
@Test
void testConversationCreation() {
    Conversation conv = new Conversation("id", ConversationType.GROUP, "name", "user");
    assertEquals("id", conv.getId());
}
```

### 2. Repository Tests (Future)
**Purpose**: Test JPA repositories with H2 database
**Framework**: @DataJpaTest
**Dependencies**: H2 in-memory database

```java
@DataJpaTest
@EnableAutoConfiguration(exclude = {
    MongoAutoConfiguration.class,
    RedisAutoConfiguration.class
})
class ConversationRepositoryTest {
    @Autowired
    private ConversationRepository repository;
}
```

### 3. Integration Tests (Future)
**Purpose**: Test complete workflows with real services
**Framework**: @SpringBootTest + Testcontainers
**Dependencies**: Docker containers (PostgreSQL, MongoDB, Redis)

```java
@SpringBootTest
@Testcontainers
class ConversationIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");
}
```

## 🔧 Test Configuration

### Unit Tests
- **File**: `application-test.yml`
- **Database**: H2 in-memory
- **External Services**: Disabled/Mocked
- **Fast**: ✅ No network dependencies

### Integration Tests
- **File**: `application-integration.yml` (future)
- **Database**: Testcontainers PostgreSQL/MongoDB
- **External Services**: Real Docker containers
- **Comprehensive**: ✅ Full system testing

## 🚨 Current Issues

### Build System
The project uses **Gradle** as the build system:

```bash
# Run all tests
./gradlew test

# Run with coverage
./gradlew test jacocoTestReport

# Run integration tests
./gradlew integrationTest
```

**Java 17 Required**: Ensure JAVA_HOME points to Java 17+ installation

## 📋 Test Guidelines

### 1. Test Naming
- Use descriptive method names: `testConversationCreation()`
- Follow Given-When-Then structure
- One assertion per concept

### 2. Test Independence
- Each test should be isolated
- No shared state between tests
- Use `@DirtyContext` if needed for Spring tests

### 3. Test Data
- Use meaningful test data
- Avoid magic numbers/strings
- Create test data builders for complex objects

### 4. Performance
- Unit tests should be fast (< 100ms)
- Integration tests can be slower but should complete < 30s
- Use `@MockBean` for external dependencies in slice tests

## 🔄 Migration Path

### Phase 1: Basic Unit Tests ✅
- Model validation tests
- Business logic tests
- No external dependencies

### Phase 2: Repository Tests (Next)
- Enable when Maven repository access is available
- Test JPA queries and relationships
- Use H2 in-memory database

### Phase 3: Integration Tests (Future)
- Add when complex scenarios need testing
- Use Testcontainers for real databases
- Test complete user workflows

## 📊 Test Metrics

### Coverage Goals
- **Unit Tests**: > 80% line coverage
- **Integration Tests**: > 70% feature coverage
- **Critical Paths**: 100% coverage

### Performance Targets
- **Unit Test Suite**: < 10 seconds
- **Repository Test Suite**: < 30 seconds  
- **Integration Test Suite**: < 2 minutes

## 🛠️ Tools & Libraries

- **JUnit 5**: Test framework
- **Mockito**: Mocking framework (via spring-boot-starter-test)
- **AssertJ**: Fluent assertions (via spring-boot-starter-test)
- **Testcontainers**: Integration testing with Docker
- **H2**: In-memory database for tests
- **Spring Boot Test**: Test slice annotations

## 📚 References

- [Spring Boot Testing Guide](https://spring.io/guides/gs/testing-web/)
- [Testcontainers Documentation](https://testcontainers.com/)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)