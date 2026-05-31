---
name: write-tests
description: Guide for writing unit and integration tests following project conventions (JUnit 6, AssertJ, OkHttp MockWebServer, Testcontainers).
---

## Overview

All tests are written using **JUnit 6 (Jupiter)**, **AssertJ** for fluent assertions, and
**Mockito** for mocking. Tests are organised into two categories:

- **Unit tests** — test a single class in isolation; dependencies are mocked with Mockito.
- **Integration tests** — test the wired-up Spring context; use `@SpringBootTest` and
  Testcontainers for any database or external infrastructure.

Use **OkHttp `MockWebServer`** whenever the code under test makes outbound HTTP calls.

This project contains two types of modules:

- **Starter/library modules** (under `starters/`) — these are Spring Boot auto-configuration
  libraries with no `@SpringBootApplication` class. They require a different integration-test
  approach than runnable applications.
- **Example/runnable application modules** (under `examples/`) — these have a
  `@SpringBootApplication` main class and can use the standard `@SpringBootTest` approach.

---

## 1. File & Package Layout

| Test kind | Source root | Naming rule |
|---|---|---|
| Unit test | `src/test/java` | `<ClassUnderTest>Test` |
| Integration test | `src/test/java` | `<ClassUnderTest>IT` or `<Feature>IntegrationTest` |
| Auto-configuration test | `src/test/java` | `<AutoConfigClass>Test` |
| Test fixture / helper | `src/test/java` | `<Name>Fixture` or `<Name>TestHelper` |

Place each test class in the **same package** as the class under test (but under
`src/test/java`).

---

## 2. Method Naming Convention

Use the pattern:

```
<methodOrBehaviour>_<stateOrInput>_<expectedOutcome>
```

Examples:

```java
findById_existingId_returnsUser()
findById_unknownId_throwsNotFoundException()
save_nullInput_throwsIllegalArgumentException()
process_serverReturns500_throwsServiceUnavailableException()
```

Rules:
- Use **camelCase** with underscores only as the two separators shown above.
- Keep names descriptive but concise — avoid filler words like `should` or `test`.
- For parameterised tests, append `_<paramDescription>` to the base name, or rely on
  `@MethodSource` / `@CsvSource` display names.

---

## 3. Test Structure — Given / When / Then

Every test body must be divided into exactly **three comment-delimited sections**.

### Standard three-section layout

```java
@Test
void findById_existingId_returnsUser() {
    // Given
    var userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    when(userRepository.findById(userId)).thenReturn(Optional.of(new User(userId, "Alice")));

    // When
    var result = userService.findById(userId);

    // Then
    assertThat(result.name()).isEqualTo("Alice");
}
```

### Combined When/Then (acceptable for simple assertions)

When the act and assert steps are trivially short, they may be collapsed under a single
`// When/Then` comment:

```java
@Test
void findById_unknownId_throwsNotFoundException() {
    // Given
    var userId = UUID.randomUUID();
    when(userRepository.findById(userId)).thenReturn(Optional.empty());

    // When/Then
    assertThatThrownBy(() -> userService.findById(userId))
            .isInstanceOf(UserNotFoundException.class)
            .hasMessageContaining(userId.toString());
}
```

Rules:
- **Never** omit all section comments — at minimum `// Given` and `// When/Then` must appear.
- Keep the `// Given` block free of assertions.
- Keep the `// When` / `// When/Then` block to a **single method call** on the class under
  test where possible.

---

## 4. Unit Tests

### Class-level setup

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    UserService userService;
}
```

- Annotate the class with `@ExtendWith(MockitoExtension.class)`.
- Declare each dependency as a `@Mock` field.
- Inject the class under test with `@InjectMocks`.
- Prefer constructor injection in production code so `@InjectMocks` works reliably.

### Mocking with Mockito

Use the standard Mockito API (`when` / `thenReturn` / `thenThrow` / `verify`):

```java
// Stubbing a return value
when(userRepository.findById(userId)).thenReturn(Optional.of(user));

// Stubbing a void method
doNothing().when(eventPublisher).publish(any());

// Stubbing an exception
when(userRepository.findById(userId)).thenThrow(new DataAccessException("db error") {});

// Verifying an interaction
verify(userRepository).save(user);
verify(eventPublisher, never()).publish(any());
```

Imports to use:

```java
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
```

### Assertions with AssertJ

```java
// Value assertions
assertThat(result).isNotNull();
assertThat(result.name()).isEqualTo("Alice");
assertThat(result.roles()).containsExactlyInAnyOrder(Role.ADMIN, Role.USER);

// Exception assertions
assertThatThrownBy(() -> userService.findById(unknownId))
        .isInstanceOf(UserNotFoundException.class)
        .hasMessageContaining("not found");

// Soft assertions (when checking multiple fields at once)
assertSoftly(softly -> {
    softly.assertThat(result.id()).isNotNull();
    softly.assertThat(result.name()).isEqualTo("Alice");
    softly.assertThat(result.active()).isTrue();
});
```

Import to use:

```java
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
```

---

## 5. Integration Tests

Integration test approach differs depending on whether the module is a **starter library**
or a **runnable example application**.

### 5a. Starter / Library Modules — Auto-Configuration Testing

Starter modules have **no** `@SpringBootApplication` class, so plain `@SpringBootTest`
cannot be used directly for auto-configuration tests.
Use `ApplicationContextRunner` instead — it spins up a lightweight application context
without a servlet container and is the idiomatic way to test `@AutoConfiguration` classes
and their `@ConditionalOn*` conditions.

```java
class RestServerAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RestServerAutoConfiguration.class));

    @Test
    void loggingFilter_loggingEnabled_beanIsPresent() {
        // Given/When
        contextRunner
                .withPropertyValues("platform.rest.server.logging.enabled=true")
                .run(context -> {
                    // Then
                    assertThat(context).hasSingleBean(StandardRequestResponseLoggingFilter.class);
                });
    }

    @Test
    void loggingFilter_loggingDisabled_beanIsAbsent() {
        // Given/When
        contextRunner
                .withPropertyValues("platform.rest.server.logging.enabled=false")
                .run(context -> {
                    // Then
                    assertThat(context).doesNotHaveBean(StandardRequestResponseLoggingFilter.class);
                });
    }
}
```

Imports to use:

```java
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.assertj.core.api.Assertions.assertThat;
```

For web-layer auto-configuration, use `WebApplicationContextRunner` or
`ReactiveWebApplicationContextRunner` instead of `ApplicationContextRunner`.

### 5b. Starter / Library Modules — Full Spring Context Integration Tests

When a full Spring context is genuinely needed (e.g., testing filter chain integration or
property binding end-to-end), create a minimal `@SpringBootApplication` class in
`src/test/java` and reference it explicitly:

```java
// src/test/java/com/kevshah/platform/starter/rest/server/TestApplication.java
package com.kevshah.platform.starter.rest.server;

import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
class TestApplication {}
```

Then reference it in the test:

```java
@SpringBootTest(classes = TestApplication.class)
@ActiveProfiles("test")
class RestServerFilterIntegrationTest {

    @Autowired
    StandardRequestResponseLoggingFilter loggingFilter;
}
```

- Never place the `TestApplication` class in `src/main/java`.
- The test application class is package-private (no `public` modifier) to signal it is
  test-only infrastructure.

### 5c. Runnable Application Modules (`examples/`)

Runnable example applications have a real `@SpringBootApplication` main class, so
`@SpringBootTest` works without any extra setup:

```java
@SpringBootTest
@ActiveProfiles("test")
class OrdersAPIRestControllerIT {

    @Autowired
    MockMvc mockMvc;
}
```

For controller slice tests use `@WebMvcTest`:

```java
@WebMvcTest(OrdersAPIRestController.class)
class OrdersAPIRestControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void createOrder_validRequest_returns201() throws Exception {
        // Given
        var requestBody = """
                { "customerId": "cust-1", "items": [{ "sku": "SKU-1", "qty": 1 }] }
                """;

        // When/Then
        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"));
    }
}
```

### 5d. Database Integration Tests (any module)

```java
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class UserRepositoryIT {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17-alpine");

    @DynamicPropertySource
    static void configureDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    UserRepository userRepository;
}
```

- Declare the container as a `static` field annotated with `@Container`.
- Wire dynamic connection properties via `@DynamicPropertySource`.
- Use `@Testcontainers` on the class so JUnit manages container lifecycle.
- In library modules, pair with the `TestApplication` helper described in §5b.

---

## 6. Testing Outbound HTTP Calls with OkHttp MockWebServer

Use `MockWebServer` (from `com.squareup.okhttp3:mockwebserver`) whenever the class under
test makes outbound HTTP calls through a `RestClient`, `WebClient`, or similar.

### Setup and teardown

```java
class ExternalPaymentClientTest {

    MockWebServer mockWebServer;
    ExternalPaymentClient client;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        var baseUrl = mockWebServer.url("/").toString();
        client = new ExternalPaymentClient(baseUrl);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }
}
```

- Start the server in `@BeforeEach` and shut it down in `@AfterEach`.
- Pass the dynamic `mockWebServer.url("/")` to the client under test; do **not** hardcode
  a port.

### Enqueuing responses

```java
@Test
void charge_successfulResponse_returnsChargeId() throws Exception {
    // Given
    var responseBody = """
            { "chargeId": "ch_abc123", "status": "SUCCEEDED" }
            """;
    mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody(responseBody));

    // When
    var result = client.charge(new ChargeRequest("tok_visa", 5000));

    // Then
    assertThat(result.chargeId()).isEqualTo("ch_abc123");
    assertThat(result.status()).isEqualTo("SUCCEEDED");
}
```

### Asserting the outbound request

```java
@Test
void charge_requestBody_containsExpectedPayload() throws Exception {
    // Given
    mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .setHeader("Content-Type", "application/json")
            .setBody("""{ "chargeId": "ch_abc123", "status": "SUCCEEDED" }"""));

    // When
    client.charge(new ChargeRequest("tok_visa", 5000));

    // Then
    var recorded = mockWebServer.takeRequest();
    assertThat(recorded.getMethod()).isEqualTo("POST");
    assertThat(recorded.getPath()).isEqualTo("/v1/charges");
    assertThat(recorded.getBody().readUtf8()).contains("\"token\":\"tok_visa\"");
}
```

### Simulating error responses

```java
@Test
void charge_serverReturns503_throwsServiceUnavailableException() {
    // Given
    mockWebServer.enqueue(new MockResponse().setResponseCode(503));

    // When/Then
    assertThatThrownBy(() -> client.charge(new ChargeRequest("tok_visa", 5000)))
            .isInstanceOf(ServiceUnavailableException.class);
}
```

---

## 7. Parameterised Tests

Use `@ParameterizedTest` with `@MethodSource` for complex inputs or `@CsvSource` for
simple value tables:

```java
@ParameterizedTest
@CsvSource({
    "ACTIVE,   true",
    "INACTIVE, false",
    "BANNED,   false"
})
void isEligible_variousStatuses_returnsExpected(String status, boolean expected) {
    // Given
    var user = new User(UUID.randomUUID(), "Alice", UserStatus.valueOf(status));

    // When
    var result = userService.isEligible(user);

    // Then
    assertThat(result).isEqualTo(expected);
}
```

---

## 8. Nested Tests with `@Nested`

Use `@Nested` inner classes to group related tests around a shared context, state, or
subject-method — keeping the outer class uncluttered while making the test report easier
to read.

### When to use `@Nested`

- A single class has **multiple methods**, each with several distinct scenarios.
- A method has **two or more meaningfully different states** (e.g., *entity exists* vs.
  *entity not found*) that each require their own `@BeforeEach` set-up.
- You want to share common `@BeforeEach` / `@AfterEach` fixtures within a group **without**
  polluting the rest of the test class.

Do **not** reach for `@Nested` just for a single happy-path test — a flat test method is
simpler.

### Naming convention

Name the nested class after the **method or behaviour under focus**, using `UpperCamelCase`:

```
When<State>           →  WhenUserExists, WhenDatabaseIsUnavailable
<MethodName>          →  FindById, Save, Delete
<MethodName>_<State>  →  FindById_WhenFound, FindById_WhenMissing  (if nesting two levels deep)
```

### Example — grouping by method and state

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    UserService userService;

    @Nested
    class FindById {

        @Nested
        class WhenUserExists {

            User existingUser;

            @BeforeEach
            void setUp() {
                existingUser = new User(
                        UUID.fromString("00000000-0000-0000-0000-000000000001"), "Alice");
                when(userRepository.findById(existingUser.id()))
                        .thenReturn(Optional.of(existingUser));
            }

            @Test
            void returnsUser() {
                // When
                var result = userService.findById(existingUser.id());

                // Then
                assertThat(result.name()).isEqualTo("Alice");
            }

            @Test
            void doesNotQueryTwice() {
                // When
                userService.findById(existingUser.id());
                userService.findById(existingUser.id());

                // Then
                verify(userRepository, times(2)).findById(existingUser.id());
            }
        }

        @Nested
        class WhenUserDoesNotExist {

            UUID unknownId;

            @BeforeEach
            void setUp() {
                unknownId = UUID.randomUUID();
                when(userRepository.findById(unknownId)).thenReturn(Optional.empty());
            }

            @Test
            void throwsUserNotFoundException() {
                // When/Then
                assertThatThrownBy(() -> userService.findById(unknownId))
                        .isInstanceOf(UserNotFoundException.class)
                        .hasMessageContaining(unknownId.toString());
            }
        }
    }

    @Nested
    class Save {

        @Test
        void nullInput_throwsIllegalArgumentException() {
            // When/Then
            assertThatThrownBy(() -> userService.save(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
```

### Rules

- Nested classes must be **non-static** inner classes so they can access outer `@Mock`
  fields.
- The outer class still holds all `@Mock` and `@InjectMocks` declarations; nested classes
  only add **local** fixtures via their own `@BeforeEach`.
- Each nested class may have its own `@BeforeEach` and `@AfterEach`; they are called
  **after** the outer class lifecycle methods.
- Keep nesting to **two levels** at most (outer class → method group → state group).
  Deeper nesting hurts readability.
- Test method names inside `@Nested` classes should still follow the
  `<behaviour>_<state>_<outcome>` pattern **or** — because the class name already supplies
  the context — a shorter `<outcome>` form is acceptable:
  `returnsUser()`, `throwsNotFoundException()`, `persistsRecord()`.

---

## 9. Quick-Reference Checklist

Before committing a test, verify:

- [ ] Class name ends in `Test` (unit) or `IT` / `IntegrationTest` (integration).
- [ ] Method name follows `<behaviour>_<state>_<outcome>` convention.
- [ ] Every test body has `// Given`, `// When`, `// Then` (or `// When/Then`) comments.
- [ ] Assertions use AssertJ — no JUnit `assertEquals` / `assertTrue` calls.
- [ ] Mocking uses Mockito `when(…).thenReturn(…)` / `verify(…)` — no BDDMockito
  `given(…).willReturn(…)` or `then(…).should()` calls.
- [ ] All static imports are explicit — no wildcard (`.*`) imports for any test dependency.
- [ ] Outbound HTTP calls are exercised through `MockWebServer`, not a live endpoint.
- [ ] Integration tests that touch a database use Testcontainers, not H2 or mocked repos.
- [ ] No hardcoded secrets, ports, or environment-specific URLs.
- [ ] `@Nested` classes are used when a method or feature has two or more meaningfully
  different states/scenarios that benefit from shared `@BeforeEach` set-up or grouping.
- [ ] `@Nested` classes are non-static inner classes and nesting is kept to two levels.
- [ ] Test method names inside `@Nested` classes use either the full
  `<behaviour>_<state>_<outcome>` pattern or a shorter `<outcome>`-only form when the
  class name already supplies the context.
- [ ] **Starter/library modules:** auto-configuration conditions are tested with
  `ApplicationContextRunner`, not `@SpringBootTest`.
- [ ] **Starter/library modules:** if `@SpringBootTest` is genuinely needed, a
  package-private `TestApplication` class exists in `src/test/java`.
- [ ] **Example/runnable modules:** `@SpringBootTest` is used for integration tests;
  `@WebMvcTest` is used for controller slice tests.
