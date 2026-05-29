# Railway Deployment Guide — Trust Platform Backend

## Overview

This Spring Boot application is deployed on Railway using a multi-stage Docker build. The Dockerfile activates the `prod` Spring profile (`-Dspring.profiles.active=prod`), which loads `application.properties` as the base and `application-prod.properties` as the production override layer.

---

## Required Environment Variables

Set these in your Railway service's **Variables** tab before deploying.

### Database (MySQL)
| Variable        | Description                          |
|-----------------|--------------------------------------|
| `MYSQLHOST`     | MySQL host (provided by Railway)     |
| `MYSQLPORT`     | MySQL port (default: 3306)           |
| `MYSQLDATABASE` | Database name                        |
| `MYSQLUSER`     | Database username                    |
| `MYSQLPASSWORD` | Database password                    |

> Railway's MySQL plugin automatically injects these variables when you attach a MySQL database to your service.

### Authentication
| Variable               | Description                                      |
|------------------------|--------------------------------------------------|
| `JWT_SECRET`           | HS256 secret key (min 32 chars, base64 encoded)  |
| `JWT_EXPIRATION`       | Access token TTL in ms (default: 900000 = 15min) |
| `JWT_REFRESH_EXPIRATION` | Refresh token TTL in ms (default: 2592000000 = 30d) |

### Application
| Variable          | Description                                    |
|-------------------|------------------------------------------------|
| `FRONTEND_URL`    | Frontend origin for CORS (e.g. `https://your-app.vercel.app`) |
| `PORT`            | HTTP port (Railway sets this automatically)    |

### Email (Optional)
| Variable        | Description                          |
|-----------------|--------------------------------------|
| `MAIL_PASSWORD` | Gmail App Password for SMTP          |

### Payments (Optional)
| Variable             | Description              |
|----------------------|--------------------------|
| `RAZORPAY_KEY_ID`    | Razorpay API key ID      |
| `RAZORPAY_KEY_SECRET`| Razorpay API key secret  |

### Storage (Optional)
| Variable                   | Description                              |
|----------------------------|------------------------------------------|
| `APP_RECEIPTS_STORAGE_PATH`| Path for PDF receipt storage (default: `./receipts`) |

---

## Schema Initialization

`spring.jpa.hibernate.ddl-auto=update` is set in both `application.properties` and `application-prod.properties`. This means:

- **First deploy**: Hibernate automatically creates all tables (`audit_logs`, `users`, `donations`, `events`, etc.) from the `@Entity` definitions.
- **Subsequent deploys**: Hibernate only adds new columns or tables — existing data is never dropped or modified.
- **DataInitializer** runs after the schema is ready and seeds the initial admin account, sample events, donations, volunteer applications, audit logs, and notifications.

No manual SQL execution is required.

---

## Deployment Flow

1. Railway detects a push to the configured branch.
2. Docker build runs: Maven compiles the JAR (`./mvnw package -DskipTests`).
3. Container starts with the `prod` Spring profile active.
4. Hibernate (`ddl-auto=update`) creates any missing tables.
5. Spring context fully initializes.
6. `DataInitializer` seeds initial data (idempotent — safe to run multiple times).
7. `DatabaseSchemaRefiner` applies any ALTER TABLE refinements.
8. Application is ready on port 8080.

---

## Verifying a Successful Deployment

Monitor the Railway deployment logs for these lines (in order):

```
Tomcat started on port 8080
[DataInitializer] Starting enterprise platform seeding execution...
[DataInitializer] Enterprise platform seeding execution successfully completed.
Started TrustPlatformBackendApplication
```

Then verify the health endpoint:

```bash
curl https://<your-railway-domain>/api/public/health
```

And test the admin login:

```bash
curl -X POST https://<your-railway-domain>/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@trust.org","password":"admin123"}'
```

---

## Default Seed Accounts

| Role      | Email                  | Password       |
|-----------|------------------------|----------------|
| Admin     | admin@trust.org        | admin123       |
| Donor     | donor@trust.org        | donor123       |
| Volunteer | volunteer@trust.org    | volunteer123   |
| User      | user@trust.org         | user123        |

> **Change these passwords immediately after first login in production.**

---

## Rollback

If a deployment fails:

1. Revert the commit in Git and push to trigger a new Railway build.
2. Or use Railway's **Deployments** tab to redeploy a previous successful build.

Since `ddl-auto=update` never drops tables or data, rollbacks are safe — the schema may have extra columns from the failed deploy, but the application will function correctly.

---

## Redis (Optional)

The application uses Redis for caching if a Redis connection is available. If no Redis instance is configured, it automatically falls back to an in-memory `ConcurrentMapCacheManager`. To add Redis, attach a Redis plugin in Railway and set the `REDIS_URL` variable.
