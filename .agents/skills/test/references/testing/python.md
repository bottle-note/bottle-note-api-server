# Testing: python

Battle-tested testing patterns for Python 3.12+ projects. Default recommendation: pytest + fixtures + typed fakes + testcontainers for integration.

> Use this with `languages/python.md` and `verify/python.md`.

## Framework Choice

Use `pytest` as the default test framework.

Prefer pytest over `unittest` because:
- fixture composition is simpler
- markers cleanly separate unit / integration / docs tests
- async testing support is straightforward
- parameterization is concise
- plugins such as `pytest-cov`, `pytest-asyncio`, and `testcontainers` integrate well

`unittest` is acceptable only when an existing codebase already standardizes on it.

## Test Classification

Use pytest markers to classify test cost and required infrastructure.

```python
import pytest


@pytest.mark.unit
def test_hash_password_when_valid_password_returns_hash() -> None:
    ...


@pytest.mark.integration
def test_user_repository_when_saved_can_find_by_email() -> None:
    ...


@pytest.mark.docs
def test_openapi_schema_when_generated_matches_contract() -> None:
    ...
```

### Marker policy

- `unit`: no network, no real DB, no Docker
- `integration`: real infrastructure through testcontainers or local test service
- `docs`: docs, examples, OpenAPI schema, generated contract checks
- `e2e`: optional, external process or full stack

## pytest Configuration

Declare markers and strict marker behavior in `pyproject.toml`.

```toml
[tool.pytest.ini_options]
testpaths = ["tests"]
python_files = ["test_*.py", "*_test.py"]
addopts = [
  "--strict-markers",
  "--strict-config",
]
markers = [
  "unit: fast tests without external infrastructure",
  "integration: tests that require Docker or real infrastructure",
  "docs: documentation and generated contract tests",
  "e2e: full-stack end-to-end tests",
]
asyncio_mode = "auto"
```

`--strict-markers` prevents typo markers from silently creating new categories.

## Test Layout

Recommended structure:

```
tests/
├── conftest.py
├── unit/
│   └── users/
│       └── test_user_service.py
├── integration/
│   └── users/
│       └── test_user_repository.py
└── api/
    └── test_users_router.py
```

Use test names that describe behavior:

```python
def test_register_when_email_exists_raises_user_already_exists() -> None:
    ...
```

## Fixture Pattern

Use `conftest.py` for shared fixtures. Keep default scope as `function`.

```python
# tests/conftest.py
from __future__ import annotations

import pytest

from app.users.models import User


@pytest.fixture
def user_email() -> str:
    return "user@example.com"


@pytest.fixture
def existing_user(user_email: str) -> User:
    return User(
        id=1,
        email=user_email,
        name="Existing User",
        password_hash="hashed-password",
    )
```

### Fixture scope rules

- `function`: default for mutable data and repositories.
- `module`: rarely, when setup is expensive but state can be isolated.
- `session`: Docker containers, schema creation, immutable config.
- Avoid global mutable fixtures.

## InMemory Pattern

Use typed fake repositories that implement the same `Protocol` as the real repository.

This is preferred over `unittest.mock.Mock` for domain tests because:
- type checking catches interface drift
- behavior is explicit
- state is visible
- tests read like real use cases

```python
# tests/unit/users/fakes.py
from __future__ import annotations

from app.users.models import User
from app.users.repository import UserRepository


class InMemoryUserRepository(UserRepository):
    def __init__(self) -> None:
        self._users_by_email: dict[str, User] = {}
        self._next_id = 1

    async def save(self, user: User) -> User:
        if user.id is None:
            user.id = self._next_id
            self._next_id += 1
        self._users_by_email[user.email] = user
        return user

    async def find_by_email(self, email: str) -> User | None:
        return self._users_by_email.get(email)
```

## Unit Test Example — Fake Repository

```python
# tests/unit/users/test_user_service.py
from __future__ import annotations

import pytest

from app.users.schemas import UserRegisterRequest
from app.users.service import UserAlreadyExistsError, UserService
from tests.unit.users.fakes import InMemoryUserRepository


class FakeSession:
    def begin(self) -> "FakeSession":
        return self

    async def __aenter__(self) -> None:
        return None

    async def __aexit__(self, exc_type: object, exc: object, tb: object) -> None:
        return None


@pytest.mark.unit
@pytest.mark.asyncio
async def test_register_when_new_email_saves_user() -> None:
    repository = InMemoryUserRepository()
    service = UserService(repository=repository, session=FakeSession())

    user = await service.register(
        UserRegisterRequest(
            email="new@example.com",
            name="New User",
            password="very-secure-password",
        )
    )

    assert user.id == 1
    assert user.email == "new@example.com"


@pytest.mark.unit
@pytest.mark.asyncio
async def test_register_when_email_exists_raises_user_already_exists() -> None:
    repository = InMemoryUserRepository()
    service = UserService(repository=repository, session=FakeSession())

    request = UserRegisterRequest(
        email="dup@example.com",
        name="First User",
        password="very-secure-password",
    )
    await service.register(request)

    with pytest.raises(UserAlreadyExistsError):
        await service.register(request)
```

## Integration Testing

Use `testcontainers-python` for real Postgres / Redis / external infrastructure behavior.

Install examples:
- `testcontainers[postgresql]`
- `asyncpg`
- `sqlalchemy[asyncio]`

Use session-scoped containers and function-scoped database cleanup.

```python
# tests/integration/conftest.py
from __future__ import annotations

from collections.abc import AsyncIterator, Iterator

import pytest
from sqlalchemy.ext.asyncio import AsyncEngine, AsyncSession, async_sessionmaker, create_async_engine
from testcontainers.postgres import PostgresContainer

from app.users.models import Base


@pytest.fixture(scope="session")
def postgres_container() -> Iterator[PostgresContainer]:
    with PostgresContainer("postgres:16-alpine") as postgres:
        yield postgres


@pytest.fixture(scope="session")
async def async_engine(postgres_container: PostgresContainer) -> AsyncIterator[AsyncEngine]:
    sync_url = postgres_container.get_connection_url()
    async_url = sync_url.replace("postgresql+psycopg2://", "postgresql+asyncpg://")
    engine = create_async_engine(async_url)

    async with engine.begin() as connection:
        await connection.run_sync(Base.metadata.create_all)

    yield engine

    await engine.dispose()


@pytest.fixture
async def session(async_engine: AsyncEngine) -> AsyncIterator[AsyncSession]:
    maker = async_sessionmaker(async_engine, expire_on_commit=False)

    async with maker() as session:
        async with session.begin():
            yield session
            await session.rollback()
```

## Integration Test Example — Repository

```python
# tests/integration/users/test_user_repository.py
from __future__ import annotations

import pytest
from sqlalchemy.ext.asyncio import AsyncSession

from app.users.models import User
from app.users.repository import SqlAlchemyUserRepository


@pytest.mark.integration
@pytest.mark.asyncio
async def test_find_by_email_when_user_saved_returns_user(session: AsyncSession) -> None:
    repository = SqlAlchemyUserRepository(session)
    user = User(
        email="saved@example.com",
        name="Saved User",
        password_hash="hashed-password",
    )

    await repository.save(user)
    found = await repository.find_by_email("saved@example.com")

    assert found is not None
    assert found.email == "saved@example.com"
```

## HTTP Testing

### FastAPI `TestClient`

Use `TestClient` for sync-style API tests when route dependencies can run under the test client's event loop.

```python
from fastapi.testclient import TestClient

from app.main import app


def test_health_when_called_returns_ok() -> None:
    client = TestClient(app)

    response = client.get("/health")

    assert response.status_code == 200
```

### `httpx.AsyncClient`

Use `httpx.AsyncClient` when the test itself is async or when async fixtures are already in use.

```python
from __future__ import annotations

import pytest
from httpx import ASGITransport, AsyncClient

from app.main import app


@pytest.mark.asyncio
async def test_health_when_called_returns_ok() -> None:
    transport = ASGITransport(app=app)

    async with AsyncClient(transport=transport, base_url="http://test") as client:
        response = await client.get("/health")

    assert response.status_code == 200
```

## Auth in Tests

Override FastAPI dependencies directly.

```python
from __future__ import annotations

from fastapi.testclient import TestClient

from app.auth.dependencies import get_current_user
from app.main import app
from app.users.schemas import CurrentUser


def test_me_when_authenticated_returns_current_user() -> None:
    fake_user = CurrentUser(id=1, email="user@example.com")
    app.dependency_overrides[get_current_user] = lambda: fake_user

    try:
        client = TestClient(app)
        response = client.get("/me")

        assert response.status_code == 200
        assert response.json()["email"] == "user@example.com"
    finally:
        app.dependency_overrides.clear()
```

### Override rules

- Always clear `app.dependency_overrides` after the test.
- Prefer fixture-managed overrides for repeated patterns.
- Do not patch auth internals with `Mock` when a dependency override is enough.

## Factory Pattern

Use plain factory functions for simple projects.

```python
# tests/factories.py
from __future__ import annotations

from app.users.models import User


def make_user(
    *,
    id: int | None = None,
    email: str = "user@example.com",
    name: str = "Test User",
    password_hash: str = "hashed-password",
) -> User:
    return User(id=id, email=email, name=name, password_hash=password_hash)
```

Use `factory-boy` when:
- many models share nested defaults
- large object graphs are common
- integration tests need DB-backed factories

Do not hide important test setup inside overly clever factories.

## Async Tests

Use `pytest-asyncio`.

```python
import pytest


@pytest.mark.asyncio
async def test_async_operation_when_called_returns_value() -> None:
    result = await async_operation()

    assert result == "value"
```

With `asyncio_mode = "auto"`, explicit `@pytest.mark.asyncio` may be optional, but keeping it improves readability for async tests.

## Coverage

Use `pytest-cov` in CI.

Example commands:
- `pytest -m unit --cov=app --cov-report=term-missing`
- `pytest --cov=app --cov-report=xml --cov-fail-under=80`

Coverage gates should protect core behavior, not force meaningless tests.

## Property-Based Tests

Use `hypothesis` for invariants:
- parsing / normalization
- money / date calculations
- validators
- idempotent transformations

```python
from hypothesis import given
from hypothesis import strategies as st


@given(st.text(min_size=1))
def test_normalize_name_when_called_is_idempotent(value: str) -> None:
    once = normalize_name(value)
    twice = normalize_name(once)

    assert once == twice
```

## Common Anti-patterns

- Overusing `from unittest.mock import Mock` for domain collaborators.
- Mocking the method under test.
- Global fixture state shared across tests.
- Session-scoped mutable repositories.
- Integration tests hitting production or developer-local databases.
- Docker-dependent tests marked as `unit`.
- Tests relying on execution order.
- Sleeping in tests instead of controlling time or polling with timeout.
- Leaving `app.dependency_overrides` dirty after a test.
- Factories that create hidden external resources.
- Assertions that only check status code and ignore response body.
- Coverage gates without meaningful assertions.
- `pytest.mark.skip` used to hide broken tests without a tracked reason.

