# Language: python

Battle-tested patterns for Python 3.12+ web-api / service code. Default recommendation: FastAPI + Pydantic v2 + SQLAlchemy 2.x async + pytest.

> Use this with the matching `types/*.md` (typically `web-api.md`) and `testing/python.md`.

## Framework Choice

### Default — FastAPI

Use FastAPI for new Python web-api work when the project needs:
- async I/O support
- automatic OpenAPI generation
- explicit request / response DTOs
- lightweight application structure
- clear dependency injection via `Depends()`

FastAPI keeps API boundaries explicit. It fits service-oriented code where domain logic lives outside route handlers.

### Alternative — Django

Use Django when the project needs:
- admin UI out of the box
- full-stack server-rendered features
- Django ORM and ecosystem conventions
- batteries-included auth / sessions / forms

Do not mix Django ORM patterns into FastAPI projects. If Django is selected, follow Django app conventions instead of the folder layout below.

## Module Layout

Prefer `src-layout` for installable packages and CI consistency.

```
project-root/
├── pyproject.toml
├── src/
│   └── app/
│       ├── __init__.py
│       ├── main.py
│       ├── core/
│       │   ├── __init__.py
│       │   ├── config.py
│       │   ├── database.py
│       │   └── errors.py
│       └── users/
│           ├── __init__.py
│           ├── models.py
│           ├── schemas.py
│           ├── repository.py
│           ├── service.py
│           └── router.py
└── tests/
```

### `src-layout` vs flat layout

Use `src/` when:
- the code is packaged
- imports must behave the same locally and in CI
- the project has multiple modules or app entrypoints

Flat layout is acceptable only for small scripts or throwaway prototypes.

### Package vs flat modules

Use packages when a domain has multiple files:
- `users/models.py`
- `users/schemas.py`
- `users/repository.py`
- `users/service.py`
- `users/router.py`

Avoid one huge `users.py` once the domain has API, persistence, and business logic.

### `__init__.py` convention

Keep `__init__.py` small.
- OK: package marker, public re-export for stable API
- Avoid: side effects, database connection, logger setup, app creation

```python
# src/app/users/__init__.py
from app.users.service import UserService

__all__ = ["UserService"]
```

## Folder Convention

```
src/{root}/{domain}/
├── models.py       # SQLAlchemy ORM model
├── schemas.py      # Pydantic request / response DTOs
├── repository.py   # thin DB wrapper + Protocol boundary
├── service.py      # business use cases
└── router.py       # FastAPI routes, thin HTTP adapter
```

Shared infrastructure:

```
src/{root}/core/
├── config.py       # settings
├── database.py     # engine, sessionmaker, session dependency
└── errors.py       # common error response shape
```

## Application Entry

`main.py` wires routers and exception handlers. It should not contain business logic.

```python
# src/app/main.py
from fastapi import FastAPI

from app.core.errors import register_exception_handlers
from app.users.router import router as users_router

app = FastAPI(title="Example API")

app.include_router(users_router, prefix="/users", tags=["users"])
register_exception_handlers(app)
```

## DTO Pattern — Pydantic v2

Use Pydantic v2 `BaseModel` for request / response DTOs. Keep DTOs separate from ORM models.

```python
# src/app/users/schemas.py
from __future__ import annotations

from datetime import datetime

from pydantic import BaseModel, ConfigDict, EmailStr, Field, field_validator


class UserRegisterRequest(BaseModel):
    email: EmailStr
    name: str = Field(min_length=1, max_length=80)
    password: str = Field(min_length=12, max_length=128)

    @field_validator("name")
    @classmethod
    def normalize_name(cls, value: str) -> str:
        normalized = value.strip()
        if not normalized:
            raise ValueError("name must not be blank")
        return normalized


class UserResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    email: EmailStr
    name: str
    created_at: datetime
```

### DTO rules

- Request DTO validates external input.
- Response DTO defines the public API contract.
- ORM model should not be returned directly from a route.
- Use `model_validate(entity)` only at the route boundary or service return mapping boundary.
- Do not mix Pydantic v1 `Config.orm_mode = True` with v2 code. Use `ConfigDict(from_attributes=True)`.

## ORM Pattern — SQLAlchemy 2.x Async

Use SQLAlchemy 2.x typed mappings and `AsyncSession`.

```python
# src/app/users/models.py
from __future__ import annotations

from datetime import datetime

from sqlalchemy import DateTime, String, func
from sqlalchemy.orm import DeclarativeBase, Mapped, mapped_column


class Base(DeclarativeBase):
    pass


class User(Base):
    __tablename__ = "users"

    id: Mapped[int] = mapped_column(primary_key=True)
    email: Mapped[str] = mapped_column(String(320), unique=True, index=True)
    name: Mapped[str] = mapped_column(String(80))
    password_hash: Mapped[str] = mapped_column(String(255))
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
```

### Async session boundary

Create the engine and sessionmaker once. Create `AsyncSession` per request or per transaction boundary.

```python
# src/app/core/database.py
from __future__ import annotations

from collections.abc import AsyncIterator

from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine

from app.core.config import settings

engine = create_async_engine(settings.database_url, pool_pre_ping=True)
AsyncSessionMaker = async_sessionmaker(engine, expire_on_commit=False)


async def get_session() -> AsyncIterator[AsyncSession]:
    async with AsyncSessionMaker() as session:
        yield session
```

## Dependency Injection

Use FastAPI `Depends()` for request-scoped dependencies. Use factory functions for services.

```python
# src/app/users/router.py
from __future__ import annotations

from typing import Annotated

from fastapi import Depends
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.database import get_session
from app.users.repository import SqlAlchemyUserRepository, UserRepository
from app.users.service import UserService


def get_user_repository(
    session: Annotated[AsyncSession, Depends(get_session)],
) -> UserRepository:
    return SqlAlchemyUserRepository(session)


def get_user_service(
    repository: Annotated[UserRepository, Depends(get_user_repository)],
    session: Annotated[AsyncSession, Depends(get_session)],
) -> UserService:
    return UserService(repository=repository, session=session)
```

### DI rules

- Prefer explicit factory functions over global singletons.
- Dependencies should be easy to override in tests.
- Route handlers receive services, not repositories, unless the endpoint is truly infrastructure-only.
- Do not create database sessions at import time.

## Repository Pattern

Repository should be a thin wrapper around persistence. It should not own business decisions.

Use `Protocol` for interface boundaries. This keeps services testable without `unittest.mock.Mock`.

```python
# src/app/users/repository.py
from __future__ import annotations

from typing import Protocol

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.users.models import User


class UserRepository(Protocol):
    async def save(self, user: User) -> User:
        ...

    async def find_by_email(self, email: str) -> User | None:
        ...


class SqlAlchemyUserRepository:
    def __init__(self, session: AsyncSession) -> None:
        self._session = session

    async def save(self, user: User) -> User:
        self._session.add(user)
        await self._session.flush()
        await self._session.refresh(user)
        return user

    async def find_by_email(self, email: str) -> User | None:
        result = await self._session.execute(select(User).where(User.email == email))
        return result.scalar_one_or_none()
```

### Repository rules

- OK: CRUD, simple query composition, persistence details.
- Avoid: password hashing, permission checks, domain decisions.
- Keep methods named by domain intent when useful: `find_by_email`, `exists_by_email`.
- Do not expose raw SQLAlchemy query objects to services.

## Service Pattern

Services contain use cases. They may be classes or pure functions.

Use a class when:
- multiple dependencies are needed
- route-level DI should construct the use case
- the domain has several related operations

Use pure functions when:
- dependencies are passed explicitly
- the operation is small and stateless

```python
# src/app/users/service.py
from __future__ import annotations

from sqlalchemy.ext.asyncio import AsyncSession

from app.users.models import User
from app.users.repository import UserRepository
from app.users.schemas import UserRegisterRequest


class UserAlreadyExistsError(Exception):
    def __init__(self, email: str) -> None:
        super().__init__(f"user already exists: {email}")
        self.email = email


class UserService:
    def __init__(self, repository: UserRepository, session: AsyncSession) -> None:
        self._repository = repository
        self._session = session

    async def register(self, request: UserRegisterRequest) -> User:
        existing = await self._repository.find_by_email(str(request.email))
        if existing is not None:
            raise UserAlreadyExistsError(str(request.email))

        user = User(
            email=str(request.email),
            name=request.name,
            password_hash=hash_password(request.password),
        )

        async with self._session.begin():
            return await self._repository.save(user)


def hash_password(raw_password: str) -> str:
    # 실제 프로젝트에서는 argon2/bcrypt 같은 검증된 해시를 사용한다.
    return f"hashed:{raw_password}"
```

### Transaction rule

Prefer one transaction boundary per use case.

```python
async with session.begin():
    ...
```

Do not commit inside repository methods. Repository methods may `flush()` when generated IDs are needed.

## Router Pattern

Routers are HTTP adapters. They validate input, call service, and map output.

```python
# src/app/users/router.py
from __future__ import annotations

from typing import Annotated

from fastapi import APIRouter, Depends, status

from app.users.schemas import UserRegisterRequest, UserResponse
from app.users.service import UserService

router = APIRouter()


@router.post("", response_model=UserResponse, status_code=status.HTTP_201_CREATED)
async def register_user(
    request: UserRegisterRequest,
    service: Annotated[UserService, Depends(get_user_service)],
) -> UserResponse:
    user = await service.register(request)
    return UserResponse.model_validate(user)
```

### Router rules

- Keep route handlers thin.
- Do not open transactions in route handlers when service owns the use case.
- Do not put SQLAlchemy queries in route handlers.
- Always declare `response_model` for public API routes.

## Error Model

Use domain exception subclasses and map them through FastAPI exception handlers.

```python
# src/app/core/errors.py
from __future__ import annotations

from fastapi import FastAPI, Request, status
from fastapi.responses import JSONResponse

from app.users.service import UserAlreadyExistsError


class ErrorResponse(dict[str, object]):
    pass


def register_exception_handlers(app: FastAPI) -> None:
    @app.exception_handler(UserAlreadyExistsError)
    async def handle_user_already_exists(
        request: Request,
        exc: UserAlreadyExistsError,
    ) -> JSONResponse:
        return JSONResponse(
            status_code=status.HTTP_409_CONFLICT,
            content={
                "code": "USER_ALREADY_EXISTS",
                "message": "user already exists",
                "details": {"email": exc.email},
            },
        )
```

### Error rules

- Raise domain exceptions from services.
- Map HTTP status in API layer, not deep in domain code.
- Do not raise `HTTPException` from repositories or domain services.
- Keep error response shape stable.

## Async Decision Rule

Use `async def` when the function awaits I/O:
- database calls
- HTTP calls
- Redis / queue calls
- file I/O through async libraries

Use `def` when the work is CPU-bound or purely in-memory:
- validation helpers
- DTO mapping
- password policy checks
- small calculations

In FastAPI:
- `async def` route is preferred when calling async dependencies.
- `def` route is acceptable for sync-only work.
- Never call blocking I/O from `async def` without moving it to a threadpool or using an async client.

## Typing Defaults

Use modern typing as a design constraint.

```python
from __future__ import annotations

from collections.abc import Sequence
from typing import Protocol
```

Recommended:
- Python 3.12+
- `mypy --strict` or `pyright`
- `from __future__ import annotations`
- `Protocol` for service/repository boundaries
- concrete return types for public functions
- `Sequence[T]` for read-only collection inputs
- `list[T]` when mutation or concrete list output is required

Avoid:
- implicit `Any`
- untyped decorators
- `dict` without key/value types
- `# type: ignore` without a specific error code and reason

## Packaging

Use `pyproject.toml` as the single source of project metadata.

Prefer `uv` for new projects when fast reproducible dependency management is desired. Poetry is acceptable when the project already standardizes on it.

```toml
[project]
name = "example-api"
version = "0.1.0"
requires-python = ">=3.12"
dependencies = [
  "fastapi>=0.110",
  "pydantic[email]>=2.6",
  "sqlalchemy[asyncio]>=2.0",
  "asyncpg>=0.29",
  "uvicorn[standard]>=0.27",
]

[dependency-groups]
dev = [
  "mypy>=1.8",
  "ruff>=0.3",
  "pytest>=8",
  "pytest-asyncio>=0.23",
  "pytest-cov>=5",
]

[tool.mypy]
python_version = "3.12"
strict = true
mypy_path = "src"

[tool.ruff]
line-length = 100
target-version = "py312"
src = ["src", "tests"]
```

## End-to-End Example — User Registration

Minimal flow:

```
router -> service -> repository -> SQLAlchemy model
```

```python
# schemas.py
class UserRegisterRequest(BaseModel):
    email: EmailStr
    name: str = Field(min_length=1)
    password: str = Field(min_length=12)


class UserResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    email: EmailStr
    name: str
```

```python
# service.py
class UserService:
    def __init__(self, repository: UserRepository, session: AsyncSession) -> None:
        self._repository = repository
        self._session = session

    async def register(self, request: UserRegisterRequest) -> User:
        if await self._repository.find_by_email(str(request.email)):
            raise UserAlreadyExistsError(str(request.email))

        async with self._session.begin():
            return await self._repository.save(
                User(
                    email=str(request.email),
                    name=request.name,
                    password_hash=hash_password(request.password),
                )
            )
```

```python
# router.py
@router.post("", response_model=UserResponse, status_code=201)
async def register_user(
    request: UserRegisterRequest,
    service: Annotated[UserService, Depends(get_user_service)],
) -> UserResponse:
    user = await service.register(request)
    return UserResponse.model_validate(user)
```

## Common Anti-patterns

- Global `AsyncSession` or global database transaction object.
- Creating DB sessions at import time.
- Repository commits transactions internally.
- Business logic in FastAPI route handlers.
- Returning SQLAlchemy ORM objects without explicit response DTO.
- Raising `HTTPException` from repository or domain service.
- Sync route calling blocking database / HTTP I/O in a web-api path.
- Pydantic v1 and v2 config mixed in the same project.
- `dependency_overrides`-unfriendly global singletons.
- `Any` used to bypass typing instead of modeling the boundary.
- `# type: ignore` without a narrow error code.
- Large `main.py` containing routers, settings, DB, and services.
- Repositories that become generic ORM facades with every possible query.
- Tests that require the real production database URL.
- Hidden side effects in `__init__.py`.

