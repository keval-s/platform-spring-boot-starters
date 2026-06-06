# Coding Conventions

This document outlines the coding conventions for our Java projects, ensuring consistency and maintainability across the
codebase.

## Java Coding Conventions

- Only use `var` in the following scenarios:
  - When the type is explicitly repeated on the right-hand side of the assignment (e.g.,
    `var list = new ArrayList<String>();`).
  - When the variable is initialized with a constructor or method call that clearly indicates its type.
  - When the variable is used in a context where its type can be easily inferred (e.g., loop iterators,
    try-with-resources).
- Do not use shortform or abbreviated variable names. Always prefer descriptive names that convey the purpose and type
  of the variable.
- Prefer record types for simple data carriers (DTOs, configuration properties) to enhance immutability and reduce boilerplate.
- In lambda expressions, use descriptive parameter names that clarify their role and the data they represent:
- In loop iterators, use plural names for collections and singular names for individual items to enhance readability.
- All packages in this project should start with `com.kevshah.platform` for starter modules and `com.kevshah.example`
  for example modules, following lowercase naming conventions with dots as separators.

### Javadoc Conventions

- All public API elements (public classes, methods, and fields) must have Javadoc comments that
  describe their purpose and behaviour.
- Comments should be concise but informative, ideally not exceeding 3-4 sentences per element.
- Do not use **Markdown Javadoc** (Java 23+ `///` line comments) — use `/** ... */` block comments instead.

#### Javadoc for public methods

Every public method must have a Javadoc comment. Use the following structure:

1. **Summary sentence** — one line describing what the method does. Start with a third-person
   verb (e.g., *Returns*, *Registers*, *Builds*).
2. **Optional detail paragraph** — separated by a `<p>`, providing additional
   context when the summary alone is insufficient.
3. **`@param` tags** — one per parameter, in declaration order. Omit for zero-parameter methods.
4. **`@return` tag** — always present when the return type is not `void`.
5. **`@throws` tag** — one per checked or documented unchecked exception.

```java
/**
 * Resolves the base URL for the named REST client.
 * <p>
 * The URL is sourced from {@code platform.rest.client.<name>.base-url}. If no value
 * is configured the method falls back to the provided default.
 *
 * @param name       the logical client name
 * @param defaultUrl the fallback URL used when no configuration is present
 * @return the resolved base URL, never {@code null}
 * @throws IllegalArgumentException if {@code name} is blank
 */
public String resolveBaseUrl(String name, String defaultUrl) { /* ... */ }
```

#### Javadoc for records

For `record` types, document each component (field) using `@param` tags in the **class-level**
Javadoc comment. Do **not** place a separate Javadoc comment on the individual record components.

```java
/**
 * Holds the timeout configuration for outbound REST calls.
 * <p>
 * @param connectTimeoutMs maximum time in milliseconds to establish a connection
 * @param readTimeoutMs    maximum time in milliseconds to wait for a response
 */
public record TimeoutProperties(int connectTimeoutMs, int readTimeoutMs) {
}
```

## Spring Boot Conventions

- Always use constructor injection for dependencies in Spring-managed beans. Avoid field injection and setter injection
  unless necessary for optional dependencies.
- **Auto-configuration:** Use `@AutoConfiguration` (not `@Configuration`) and guard beans
  with the most specific `@ConditionalOn*` annotation available.
- When defining configuration properties, always opt for immutable `record` types with a single constructor, and use the
  `@ConstructorBinding` annotation to enable constructor-based binding.
- Always use yml files for Spring configuration, and follow a consistent structure for property naming and organization.
  Use the `platform.<short-name>` prefix for all properties in starter modules.
- **Error handling:** Use `@RestControllerAdvice` for global exception handling in RESTful web services, and define
  specific exception handlers for anticipated error conditions.
- **Logging:** Use the `org.slf4j.Logger` interface for logging, and follow a consistent logging strategy (e.g., log at
  appropriate levels, include contextual information, avoid logging sensitive data). Use structured logging where
  possible, and provide configuration options for enabling/disabling logging and controlling log