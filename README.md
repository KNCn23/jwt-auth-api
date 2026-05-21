# jwt-auth-api

A production-style Spring Boot 3 authentication API featuring BCrypt password hashing, stateless JWT access tokens, rotating refresh tokens, and role-based access control. Built to be the missing auth layer for a Java backend portfolio.

## Features

| Feature | Details |
|---|---|
| **Password hashing** | BCrypt, work factor 12 |
| **Access tokens** | HS256-signed JWT, 15-minute lifetime, contains `uid` + `roles` |
| **Refresh tokens** | 48-byte URL-safe random, 7-day lifetime, persisted in DB, **rotated on every refresh** |
| **Revocation** | Logout revokes every refresh token for that user |
| **Authorization** | `@PreAuthorize("hasRole('ADMIN')")` + URL-pattern matching |
| **Validation** | Bean Validation on all DTOs (`@NotBlank`, `@Size`) |
| **Storage** | JPA + H2 in-memory (swap to PostgreSQL by changing two lines) |
| **Tests** | MockMvc integration test covering register → login → refresh |

## Endpoints

| Method | Path | Auth | Body | Returns |
|---|---|---|---|---|
| POST | `/api/auth/register` | none | `{username, password}` | `{accessToken, refreshToken, expiresInSeconds}` |
| POST | `/api/auth/login`    | none | `{username, password}` | same |
| POST | `/api/auth/refresh`  | none | `{refreshToken}` | new token pair (old refresh revoked) |
| POST | `/api/auth/logout`   | Bearer | — | revokes all refresh tokens for the user |
| GET  | `/api/me`            | Bearer | — | `{username, authorities}` |
| GET  | `/api/admin/ping`    | Bearer, ADMIN | — | `{"message": "hello, admin"}` |

## Run

```bash
mvn spring-boot:run
```

Server starts on `http://localhost:8080`. H2 console at `/h2-console` (JDBC URL: `jdbc:h2:mem:authdb`).

### Quick demo

```bash
# 1. Register
curl -s -X POST localhost:8080/api/auth/register \\
  -H 'Content-Type: application/json' \\
  -d '{"username":"alice","password":"supersecret123"}'

# Response:
# {"accessToken":"eyJ...","refreshToken":"abc...","expiresInSeconds":900}

# 2. Use the access token
TOKEN="eyJ..."
curl -s localhost:8080/api/me -H "Authorization: Bearer $TOKEN"
# {"username":"alice","authorities":[{"authority":"ROLE_USER"}]}

# 3. Refresh
curl -s -X POST localhost:8080/api/auth/refresh \\
  -H 'Content-Type: application/json' \\
  -d '{"refreshToken":"abc..."}'

# 4. Admin endpoint (returns 403 for non-admin)
curl -i localhost:8080/api/admin/ping -H "Authorization: Bearer $TOKEN"
```

## Project layout

```
src/main/java/com/kncn/jwtauth/
├── JwtAuthApiApplication.java
├── domain/
│   ├── User.java            # JPA entity + roles
│   ├── Role.java            # USER / ADMIN
│   └── RefreshToken.java    # persisted refresh tokens
├── repo/
│   ├── UserRepository.java
│   └── RefreshTokenRepository.java
├── dto/AuthDtos.java        # records for all request / response bodies
├── security/
│   ├── JwtService.java      # JJWT 0.12 — sign + parse
│   ├── JwtAuthFilter.java   # OncePerRequestFilter → SecurityContext
│   └── SecurityConfig.java  # Filter chain, BCrypt, stateless session
├── service/AuthService.java # register/login/refresh/logout logic
└── web/
    ├── AuthController.java
    └── UserController.java
```

## Configuration

Edit `src/main/resources/application.properties`:

```properties
app.jwt.secret=…                      # HS256 key, ≥ 32 bytes
app.jwt.access-token-expiration-ms=900000     # 15 min
app.jwt.refresh-token-expiration-ms=604800000 # 7 days
```

**Production note**: provide `APP_JWT_SECRET` as an environment variable, never commit a real secret. Swap H2 for PostgreSQL by replacing the `spring.datasource.*` block and adding the PG driver.

## Tests

```bash
mvn test
```

## License

MIT
