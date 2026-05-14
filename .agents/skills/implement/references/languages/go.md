# Language: go

Battle-tested patterns for Go 1.22+ services. Use this with matching `types/*.md`, `testing/go.md`, and `verify/go.md`.

> Default assumption: HTTP API or worker service with explicit layers, standard `context.Context`, and tests built around fakes or testcontainers.

## Module Layout

Recommended single-module layout:

```text
.
├── cmd/
│   └── api/
│       └── main.go              # process entrypoint only
├── internal/
│   └── user/
│       ├── handler/             # transport adapters: HTTP, gRPC, queue
│       ├── usecase/             # business logic and consumer-owned interfaces
│       └── repository/          # DB/cache implementations
├── pkg/                         # public library packages only when truly reusable
├── sql/                         # migrations or sqlc query files
├── go.mod
└── go.sum
```

Rules:
- `cmd/{app}/main.go` wires configuration, logger, DB, repositories, usecases, and server.
- `internal/` is the default for product code. Go enforces import privacy: packages outside the parent tree cannot import `internal/...`.
- `pkg/` is not a dumping ground. Use it only for stable, reusable libraries that are safe for external import.
- Keep domain slices under `internal/{domain}/...` unless the project already uses technical slices.

**Existing `pkg/`-centric projects.** Some established Go projects place product code under `pkg/` instead of `internal/`. This may predate the modern `internal/` convention or be a deliberate choice (e.g., exposing internal packages for tooling, a library that doubles as a service). Treat this as a **REFINEMENT** of the convention, NOT a CONFLICT:

- Respect the existing layout. Add new code where the project's peers already live.
- Do NOT migrate `pkg/X` → `internal/X` as part of a feature change. Migration is a separate refactor requiring explicit user approval and a dedicated plan.
- The Phase 0 hard gate of `/implement` still applies. Just resolve `{package-root}` to the project's actual choice (`internal/` OR `pkg/`).

## Layered Structure

Default flow:

```text
handler  ->  usecase  ->  repository implementation
HTTP         business     sqlc/sqlx/GORM/Redis/external client
```

Responsibilities:
- `handler`: decode transport input, validate DTO, call usecase, map errors to transport response.
- `usecase`: own business rules, transactions, idempotency, authorization decisions, and repository interfaces.
- `repository`: implement persistence details. No HTTP DTOs and no transport-specific errors.

Do not let repository code decide API status codes. Do not let handlers reach into SQL helpers directly.

## Interface Boundary

Go interface ownership is opposite to many Java/Spring designs.

Rule: define the interface in the consumer package, usually `usecase`, not in the producer package.

```text
[OK] internal/user/usecase.UserRepository interface
[OK] internal/user/repository.PostgresRepository implements usecase.UserRepository
[NO] internal/user/repository.Repository exported by producer and imported everywhere
```

Why:
- Consumers declare only the methods they need.
- Tests can provide small fakes without implementing producer-wide interfaces.
- Producer packages stay concrete and simple.

## DTO Pattern

Use plain structs with tags. Keep DTOs separate from domain entities.

```go
package handler

type RegisterUserRequest struct {
    Email string `json:"email" validate:"required,email"`
    Name  string `json:"name" validate:"required,min=1,max=80"`
}

type RegisterUserResponse struct {
    ID    string `json:"id"`
    Email string `json:"email"`
    Name  string `json:"name"`
}
```

Validation:
- Use `github.com/go-playground/validator/v10` at the transport boundary.
- Convert valid DTOs into usecase input structs.
- Do not pass HTTP request DTOs into repository packages.

```go
package usecase

type RegisterUserInput struct {
    Email string
    Name  string
}
```

## Error Handling

Use explicit errors. Libraries return errors; applications decide how to present them.

Recommended layers:
- Sentinel errors for stable categories: `ErrUserNotFound`, `ErrEmailAlreadyUsed`.
- Domain error type when the caller needs structured details.
- `errors.Is` for category checks.
- `errors.As` for extracting typed errors.

```go
package usecase

import "errors"

var (
    ErrUserNotFound     = errors.New("user not found")
    ErrEmailAlreadyUsed = errors.New("email already used")
)

type ValidationError struct {
    Field   string
    Message string
}

func (e *ValidationError) Error() string {
    return e.Field + ": " + e.Message
}
```

Wrap lower-level errors with context:

```go
if err := r.queries.CreateUser(ctx, params); err != nil {
    return User{}, fmt.Errorf("create user: %w", err)
}
```

Map errors at the edge:

```go
switch {
case errors.Is(err, usecase.ErrEmailAlreadyUsed):
    http.Error(w, "email already used", http.StatusConflict)
default:
    http.Error(w, "internal server error", http.StatusInternalServerError)
}
```

Never `panic` for normal business errors.

## Context Propagation

Every I/O-capable method receives `context.Context` as the first argument.

```go
func (s *Service) Register(ctx context.Context, in RegisterUserInput) (User, error)
func (r *PostgresRepository) Save(ctx context.Context, user User) error
```

Rules:
- First parameter: `ctx context.Context`.
- Do not store `context.Context` in structs.
- Do not pass `nil`; use `context.Background()` only at process boundaries or tests.
- Propagate request cancellation to DB, Redis, HTTP clients, and queue clients.
- Avoid global state; inject dependencies through constructors.

## Repository Pattern

Define the repository interface in the usecase package:

```go
package usecase

import "context"

type UserRepository interface {
    Save(ctx context.Context, user User) error
    FindByEmail(ctx context.Context, email string) (User, error)
    FindByID(ctx context.Context, id string) (User, error)
}
```

Implement it in the repository package:

```go
package repository

type PostgresRepository struct {
    db      *sql.DB
    queries *dbgen.Queries // sqlc generated package
}

func NewPostgresRepository(db *sql.DB) *PostgresRepository {
    return &PostgresRepository{db: db, queries: dbgen.New(db)}
}
```

Implementation options:
- `sqlc`: preferred when SQL shape matters and compile-time query types are valuable.
- `sqlx`: good for hand-written SQL with lightweight scanning helpers.
- `GORM`: acceptable when the project already standardizes on ORM behavior; keep it behind repository interfaces.

Transaction handling belongs in usecase or a small transaction manager abstraction, not hidden deep inside random repository methods.

## Dependency Injection

Default: manual constructor injection.

```go
userRepo := repository.NewPostgresRepository(db)
userSvc := usecase.NewService(userRepo, clock, ids)
userHandler := handler.NewHandler(userSvc, validator.New())
```

Constructor rules:
- Accept interfaces only when the package consumes behavior.
- Return concrete structs unless there is a strong reason to hide them.
- Keep constructors boring and side-effect free.

Use `wire`, `fx`, or another DI container only when the project is large enough that manual wiring is demonstrably noisy. Do not introduce a DI framework for a few constructors.

## Folder Convention

Preferred domain-first layout:

```text
internal/user/
├── handler/
│   ├── dto.go
│   └── http.go
├── usecase/
│   ├── errors.go
│   ├── repository.go
│   ├── service.go
│   └── user.go
└── repository/
    ├── postgres.go
    └── mapper.go
```

Keep generated packages separate when using sqlc:

```text
internal/user/repository/dbgen/     # generated by sqlc
sql/queries/user.sql
sql/schema/001_create_users.sql
```

## Code Example: User Registration

### Entity

```go
package usecase

import (
    "strings"
    "time"
)

type User struct {
    ID        string
    Email     string
    Name      string
    CreatedAt time.Time
}

func NewUser(id, email, name string, now time.Time) (User, error) {
    email = strings.TrimSpace(strings.ToLower(email))
    name = strings.TrimSpace(name)

    if email == "" {
        return User{}, &ValidationError{Field: "email", Message: "email is required"}
    }
    if name == "" {
        return User{}, &ValidationError{Field: "name", Message: "name is required"}
    }

    return User{ID: id, Email: email, Name: name, CreatedAt: now}, nil
}
```

### Repository Interface

```go
package usecase

import "context"

type UserRepository interface {
    Save(ctx context.Context, user User) error
    FindByEmail(ctx context.Context, email string) (User, error)
}
```

### Repository Implementation

```go
package repository

import (
    "context"
    "database/sql"
    "errors"
    "fmt"

    "example.com/app/internal/user/usecase"
)

type PostgresRepository struct {
    db *sql.DB
}

func NewPostgresRepository(db *sql.DB) *PostgresRepository {
    return &PostgresRepository{db: db}
}

func (r *PostgresRepository) Save(ctx context.Context, user usecase.User) error {
    const q = `insert into users (id, email, name, created_at) values ($1, $2, $3, $4)`
    if _, err := r.db.ExecContext(ctx, q, user.ID, user.Email, user.Name, user.CreatedAt); err != nil {
        if isUniqueViolation(err) {
            return usecase.ErrEmailAlreadyUsed
        }
        return fmt.Errorf("save user: %w", err)
    }
    return nil
}

func (r *PostgresRepository) FindByEmail(ctx context.Context, email string) (usecase.User, error) {
    const q = `select id, email, name, created_at from users where email = $1`
    var user usecase.User
    err := r.db.QueryRowContext(ctx, q, email).Scan(&user.ID, &user.Email, &user.Name, &user.CreatedAt)
    if errors.Is(err, sql.ErrNoRows) {
        return usecase.User{}, usecase.ErrUserNotFound
    }
    if err != nil {
        return usecase.User{}, fmt.Errorf("find user by email: %w", err)
    }
    return user, nil
}
```

### Usecase

```go
package usecase

import (
    "context"
    "errors"
    "fmt"
    "time"
)

type IDGenerator interface {
    NewID() string
}

type Clock interface {
    Now() time.Time
}

type Service struct {
    repo UserRepository
    ids  IDGenerator
    clk  Clock
}

func NewService(repo UserRepository, ids IDGenerator, clk Clock) *Service {
    return &Service{repo: repo, ids: ids, clk: clk}
}

func (s *Service) Register(ctx context.Context, in RegisterUserInput) (User, error) {
    if _, err := s.repo.FindByEmail(ctx, in.Email); err == nil {
        return User{}, ErrEmailAlreadyUsed
    } else if !errors.Is(err, ErrUserNotFound) {
        return User{}, fmt.Errorf("check duplicate email: %w", err)
    }

    user, err := NewUser(s.ids.NewID(), in.Email, in.Name, s.clk.Now())
    if err != nil {
        return User{}, err
    }
    if err := s.repo.Save(ctx, user); err != nil {
        return User{}, err
    }
    return user, nil
}
```

### HTTP Handler

```go
package handler

import (
    "encoding/json"
    "errors"
    "net/http"

    "github.com/go-playground/validator/v10"

    "example.com/app/internal/user/usecase"
)

type Handler struct {
    svc      *usecase.Service
    validate *validator.Validate
}

func NewHandler(svc *usecase.Service, validate *validator.Validate) *Handler {
    return &Handler{svc: svc, validate: validate}
}

func (h *Handler) Register(w http.ResponseWriter, r *http.Request) {
    var req RegisterUserRequest
    if err := json.NewDecoder(r.Body).Decode(&req); err != nil {
        http.Error(w, "invalid json", http.StatusBadRequest)
        return
    }
    if err := h.validate.Struct(req); err != nil {
        http.Error(w, "invalid request", http.StatusBadRequest)
        return
    }

    user, err := h.svc.Register(r.Context(), usecase.RegisterUserInput{
        Email: req.Email,
        Name:  req.Name,
    })
    if err != nil {
        writeRegisterError(w, err)
        return
    }

    w.Header().Set("Content-Type", "application/json")
    w.WriteHeader(http.StatusCreated)
    _ = json.NewEncoder(w).Encode(RegisterUserResponse{
        ID:    user.ID,
        Email: user.Email,
        Name:  user.Name,
    })
}

func writeRegisterError(w http.ResponseWriter, err error) {
    switch {
    case errors.Is(err, usecase.ErrEmailAlreadyUsed):
        http.Error(w, "email already used", http.StatusConflict)
    default:
        http.Error(w, "internal server error", http.StatusInternalServerError)
    }
}
```

## Build and Tooling

Common commands:

```bash
go test ./...
go test ./... -tags=integration
go test -race ./...
go vet ./...
golangci-lint run ./...
```

Project defaults:
- Run commands from module root.
- Prefer `./...` scope for verification.
- Keep `go.mod` minimal; unused dependencies are a smell.
- Run `gofmt` and `goimports` before review.

## Anti-Patterns

Avoid:
- Global mutable variables for DB, logger, config, or clients.
- `panic` in libraries or business logic for recoverable errors.
- Missing `context.Context` on I/O paths.
- Defining broad interfaces in producer packages.
- Passing HTTP DTOs into usecase or repository packages.
- Creating `pkg/` packages before there is a real external reuse need.
- Hiding transactions inside unrelated helper methods.
- Returning raw SQL driver errors across usecase boundaries.

