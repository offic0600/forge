# Code Review Checklist

## Before Requesting Review

- [ ] All tests pass locally (`./gradlew test`)
- [ ] Baseline checks pass (`run_baseline` tool)
- [ ] No hardcoded credentials or secrets
- [ ] PR description explains the "why"

## Reviewer Checklist

### Correctness

- [ ] Logic handles edge cases (null, empty, boundary values)
- [ ] Error handling is appropriate (not swallowed, not over-caught)
- [ ] Concurrency-safe where needed (shared state, async ops)
- [ ] Database queries use parameterized inputs (no SQL injection)

### Architecture

- [ ] Changes follow existing patterns in the codebase
- [ ] No circular dependencies between modules
- [ ] New public APIs have clear contracts
- [ ] Dependencies flow downward (controller → service → repository)

### Kotlin/Spring Specifics

- [ ] Data classes for DTOs; sealed classes for state
- [ ] `@Transactional` on service methods that modify data
- [ ] Nullable types used where null is a valid state
- [ ] No blocking calls in coroutine context

### Frontend/TypeScript Specifics

- [ ] Props interfaces defined, not `any`
- [ ] Effects have proper dependency arrays
- [ ] Event handlers don't cause unnecessary re-renders
- [ ] API errors handled with user-facing feedback

### Security (OWASP Top 10)

- [ ] Input validated at API boundaries
- [ ] Authentication checked on protected endpoints
- [ ] No sensitive data in logs or error messages
- [ ] CORS configured appropriately

### Testing

- [ ] Unit tests cover new logic
- [ ] Integration tests for API endpoints
- [ ] Edge cases tested (empty input, large input, error paths)
- [ ] Test names describe the expected behavior
