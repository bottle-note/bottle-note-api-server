# Testing: go

Production-oriented patterns for Go tests using standard `testing`, `testify`, fakes, `httptest`, and `testcontainers-go`.

> Default stance: use real code and small hand-written fakes. Reach for generated mocks only when the interface is large, stable, and expensive to fake manually.

**Naming caveat — judge by behavior, not name.** A struct named `mockX` or `MockY` is not automatically an anti-pattern. Classify by what it does:

| Implementation | Classification |
|----------------|----------------|
| Uses an expectation framework (`gomock.Controller`, `EXPECT().Foo().Return(...)`, `assert.AssertExpectations`) | True **mock** — implementation coupling, refactor-fragile |
| Plain hand-written struct (map / counter / fixed return, no expectation framework) | **Fake** that happens to be named `mock*` — REFINEMENT of Fake-first, not CONFLICT |

When reviewing existing tests, do NOT mechanically rename `mockX` → `fakeX`. Rename only if the naming actively confuses maintainers, and treat the rename as a separate refactor (not as part of feature work).

## Test Classification

Go commonly uses build tags instead of framework-level tags.

| Type | File naming | Build tag | Run via |
|------|-------------|-----------|---------|
| Unit | `*_test.go` | none | `go test ./...` |
| Integration | `*_integration_test.go` | `//go:build integration` | `go test -tags=integration ./...` |
| E2E | `*_e2e_test.go` | `//go:build e2e` | `go test -tags=e2e ./...` |
| Race | any test | none | `go test -race ./...` |

Integration file header:

```go
//go:build integration

package repository_test
```

Keep unit tests runnable without Docker, network, or external credentials.

## Framework

Default stack:
- Standard `testing` package for test lifecycle.
- `github.com/stretchr/testify/require` for fatal preconditions.
- `github.com/stretchr/testify/assert` for non-fatal comparisons.
- `github.com/google/go-cmp/cmp` for readable struct diffs.

Rule of thumb:
- Use `require.NoError(t, err)` when continuing would make the test misleading.
- Use `assert.Equal(t, want, got)` when multiple independent assertions are useful.
- Use `cmp.Diff(want, got)` for nested structs, slices, and maps.

## Naming Convention

Use exported-style test names because `go test` discovers `TestXxx`.

```go
func TestService_Register(t *testing.T) {}
func TestHandler_Register(t *testing.T) {}
func TestPostgresRepository_SaveAndFindByEmail(t *testing.T) {}
```

For scenarios, prefer table-driven tests:

```go
func TestNewUser(t *testing.T) {
    tests := []struct {
        name     string
        email    string
        userName string
        wantErr  bool
    }{
        {name: "valid user", email: "a@example.com", userName: "Alice"},
        {name: "empty email", email: "", userName: "Alice", wantErr: true},
    }

    for _, tt := range tests {
        tt := tt
        t.Run(tt.name, func(t *testing.T) {
            t.Parallel()

            _, err := NewUser("user-1", tt.email, tt.userName, time.Now())
            if tt.wantErr {
                require.Error(t, err)
                return
            }
            require.NoError(t, err)
        })
    }
}
```

Always rebind `tt := tt` before parallel subtests for compatibility with older Go versions and clearer intent.

## Subtests

Use `t.Run` to group related behavior without hiding individual failures.

```go
func TestService_Register(t *testing.T) {
    t.Run("creates user when email is new", func(t *testing.T) {})
    t.Run("returns conflict when email exists", func(t *testing.T) {})
}
```

Guidelines:
- Put `t.Parallel()` inside each test or subtest only after test-local setup is complete.
- Avoid parallel tests when they mutate process-wide state such as env vars, global loggers, or current working directory.
- Use `t.Setenv` instead of `os.Setenv`; it automatically restores values.
- Use `t.TempDir` for filesystem tests.

## InMemory Repository Pattern

Implement the consumer-owned interface directly. Keep it concurrency-safe if tests may run in parallel.

```go
type FakeUserRepository struct {
    mu      sync.Mutex
    byID    map[string]usecase.User
    byEmail map[string]string
}

func NewFakeUserRepository() *FakeUserRepository {
    return &FakeUserRepository{
        byID:    make(map[string]usecase.User),
        byEmail: make(map[string]string),
    }
}

func (r *FakeUserRepository) Save(ctx context.Context, user usecase.User) error {
    r.mu.Lock()
    defer r.mu.Unlock()

    if existingID, ok := r.byEmail[user.Email]; ok && existingID != user.ID {
        return usecase.ErrEmailAlreadyUsed
    }
    r.byID[user.ID] = user
    r.byEmail[user.Email] = user.ID
    return nil
}

func (r *FakeUserRepository) FindByEmail(ctx context.Context, email string) (usecase.User, error) {
    r.mu.Lock()
    defer r.mu.Unlock()

    id, ok := r.byEmail[email]
    if !ok {
        return usecase.User{}, usecase.ErrUserNotFound
    }
    return r.byID[id], nil
}
```

Fake rules:
- Model the behavior the usecase depends on, not the entire database.
- Preserve important constraints such as uniqueness.
- Guard maps with `sync.Mutex` when tests use `t.Parallel()`.
- Add helper methods only for test setup or assertions.

## Unit Test Example

```go
func TestService_Register(t *testing.T) {
    t.Parallel()

    repo := NewFakeUserRepository()
    ids := fixedIDGenerator{id: "user-1"}
    clk := fixedClock{now: time.Date(2026, 1, 2, 3, 4, 5, 0, time.UTC)}
    svc := usecase.NewService(repo, ids, clk)

    user, err := svc.Register(context.Background(), usecase.RegisterUserInput{
        Email: "A@Example.com",
        Name:  "Alice",
    })

    require.NoError(t, err)
    assert.Equal(t, "user-1", user.ID)
    assert.Equal(t, "a@example.com", user.Email)

    saved, err := repo.FindByEmail(context.Background(), "a@example.com")
    require.NoError(t, err)
    assert.Equal(t, user.ID, saved.ID)
}
```

Prefer deterministic helpers for time and IDs:

```go
type fixedIDGenerator struct{ id string }

func (g fixedIDGenerator) NewID() string { return g.id }

type fixedClock struct{ now time.Time }

func (c fixedClock) Now() time.Time { return c.now }
```

## HTTP Testing

Use standard `net/http/httptest` first.

### Handler Unit Test with Recorder

```go
func TestHandler_Register(t *testing.T) {
    t.Parallel()

    repo := NewFakeUserRepository()
    svc := usecase.NewService(repo, fixedIDGenerator{"user-1"}, fixedClock{time.Now()})
    h := handler.NewHandler(svc, validator.New())

    body := strings.NewReader(`{"email":"a@example.com","name":"Alice"}`)
    req := httptest.NewRequest(http.MethodPost, "/users", body)
    rec := httptest.NewRecorder()

    h.Register(rec, req)

    require.Equal(t, http.StatusCreated, rec.Code)

    var got handler.RegisterUserResponse
    require.NoError(t, json.NewDecoder(rec.Body).Decode(&got))
    assert.Equal(t, "user-1", got.ID)
}
```

### HTTP Integration Test with Server

```go
func TestUserRoutes(t *testing.T) {
    t.Parallel()

    srv := httptest.NewServer(newTestRouter(t))
    t.Cleanup(srv.Close)

    res, err := http.Post(
        srv.URL+"/users",
        "application/json",
        strings.NewReader(`{"email":"a@example.com","name":"Alice"}`),
    )
    require.NoError(t, err)
    defer res.Body.Close()

    assert.Equal(t, http.StatusCreated, res.StatusCode)
}
```

Use `httptest.NewServer` when you need real routing, middleware, cookies, redirects, or an actual `http.Client`.

## Integration Base: testcontainers-go

Use Testcontainers for real Postgres, Redis, or broker behavior. Keep it behind the `integration` build tag.

```go
//go:build integration

package repository_test

import (
    "context"
    "database/sql"
    "os"
    "testing"

    "github.com/testcontainers/testcontainers-go"
    "github.com/testcontainers/testcontainers-go/modules/postgres"
    "github.com/testcontainers/testcontainers-go/wait"
)

var testDB *sql.DB

func TestMain(m *testing.M) {
    ctx := context.Background()

    container, err := postgres.RunContainer(ctx,
        postgres.WithImage("postgres:16-alpine"),
        postgres.WithDatabase("app_test"),
        postgres.WithUsername("app"),
        postgres.WithPassword("secret"),
        testcontainers.WithWaitStrategy(wait.ForListeningPort("5432/tcp")),
    )
    if err != nil {
        panic(err)
    }

    dsn, err := container.ConnectionString(ctx, "sslmode=disable")
    if err != nil {
        panic(err)
    }
    testDB, err = sql.Open("postgres", dsn)
    if err != nil {
        panic(err)
    }

    code := m.Run()

    _ = testDB.Close()
    _ = container.Terminate(ctx)
    os.Exit(code)
}
```

Repository integration test:

```go
func TestPostgresRepository_SaveAndFindByEmail(t *testing.T) {
    t.Parallel()

    truncateTables(t, testDB)
    repo := repository.NewPostgresRepository(testDB)

    user := usecase.User{
        ID:        "user-1",
        Email:     "a@example.com",
        Name:      "Alice",
        CreatedAt: time.Now(),
    }
    require.NoError(t, repo.Save(context.Background(), user))

    got, err := repo.FindByEmail(context.Background(), "a@example.com")
    require.NoError(t, err)
    assert.Equal(t, user.ID, got.ID)
}
```

If tests run in parallel against one database, isolate with unique schemas, unique IDs, or per-test cleanup that cannot conflict.

## Mocking Policy

Default: hand-written fake.

Use `gomock` only when:
- The interface is large enough that a fake becomes noisy.
- The contract is stable.
- The test needs precise call ordering or argument matching.

Avoid mocks for tiny interfaces. A 2-method repository fake is usually clearer, less brittle, and closer to behavior.

## Fixtures and Golden Files

Use `testdata/` for fixture files. Go tooling ignores this directory for package builds.

```text
internal/user/handler/testdata/register_success.json
internal/user/handler/testdata/register_validation_error.json
```

Golden test pattern:

```go
got := renderResponse(data)
want := readFile(t, "testdata/register_success.json")
if diff := cmp.Diff(want, got); diff != "" {
    t.Fatalf("response mismatch (-want +got):\n%s", diff)
}
```

Keep golden updates explicit. Do not auto-update golden files in normal test runs.

## Coverage and Race Detector

Commands:

```bash
go test -cover -coverprofile=cover.out ./...
go tool cover -func=cover.out
go tool cover -html=cover.out
go test -race ./...
```

Guidelines:
- Coverage is a signal, not the goal. Prioritize business branches and error paths.
- Run `-race` for code using goroutines, maps, caches, background workers, or shared fakes.
- Race detector is slower; use it in L2/L3 verification or CI.

## Anti-Patterns

Avoid:
- Using mocks for tiny interfaces that are easier to fake by hand.
- Tests that pass only when run in a specific order.
- `t.Parallel()` with shared mutable maps and no mutex.
- Integration tests without build tags.
- Sleeping for timing assertions instead of using channels, contexts, or fake clocks.
- Ignoring response body close in HTTP client tests.
- Calling external services in unit tests.
- Comparing large structs with unreadable `reflect.DeepEqual` failures when `go-cmp` would show a diff.

