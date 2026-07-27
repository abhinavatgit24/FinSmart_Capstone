# FinSmart — AI-Powered Personal Finance Assistant

Java SDE Capstone Project | April 2026

---

## Tech Stack

| Layer    | Technology                          |
|----------|-------------------------------------|
| Backend  | Java 17 · Spring Boot 3 · Spring Security · MongoDB |
| Frontend | React 18 · Vite · Tailwind CSS · Recharts |
| Database | MongoDB Atlas (free M0 cluster)     |
| Auth     | JWT (access 15 min + refresh 7 days) |

---

## Project Structure

```
finsmart/
├── backend/          # Spring Boot Maven project
│   └── src/main/java/com/finsmart/
│       ├── config/           SecurityConfig
│       ├── controller/       AuthController, TransactionController
│       ├── dto/              request/ response/
│       ├── exception/        GlobalExceptionHandler
│       ├── model/            User, Transaction
│       ├── repository/       UserRepository, TransactionRepository
│       ├── security/         JwtUtil, JwtAuthFilter, UserDetailsServiceImpl
│       └── service/          AuthService, TransactionService
│
└── frontend/         # React + Vite project
    └── src/
        ├── api/              axiosInstance, authApi, transactionApi
        ├── context/          AuthContext
        ├── hooks/            useAuth, useTransactions, useDashboard
        ├── pages/            auth/, dashboard/, transactions/
        ├── components/       layout/, ui/
        └── utils/            formatCurrency, categories
```

---

## Setup

### Prerequisites
- Java 17+
- Maven 3.8+
- Node.js 18+
- MongoDB Atlas account (free M0 cluster)

---

### 1. MongoDB Atlas Setup

1. Go to [mongodb.com/atlas](https://www.mongodb.com/atlas) → create free M0 cluster
2. Database Access → Add user (username + password)
3. Network Access → Allow 0.0.0.0/0
4. Connect → Drivers → copy connection string
5. Replace `<password>` in the URI

---

### 2. Backend Setup

```bash
cd backend

# Create application-local.properties (gitignored)
# OR set environment variables:
export MONGO_URI="mongodb+srv://<user>:<password>@cluster.mongodb.net/finsmart"
export JWT_SECRET="YourSuperSecretKeyMustBeAtLeast256BitsLongForHS256Algorithm"
export CORS_ORIGINS="http://localhost:5173"
export APP_TIME_ZONE="Asia/Kolkata"

# Run
mvn spring-boot:run
```

Backend starts on **http://localhost:8080**

### Existing database date repair

New transaction dates are stored at UTC midnight so they remain the same calendar
date in every server timezone. If this project previously ran before that fix,
back up MongoDB and run one dry-run deployment with:

```bash
APP_LEGACY_DATE_MIGRATION_ENABLED=true
```

The logs report the number of legacy IST-shifted records (`18:30:00Z`). After
verifying the backup and count, run once more with both variables below, then
remove them:

```bash
APP_LEGACY_DATE_MIGRATION_ENABLED=true
APP_LEGACY_DATE_MIGRATION_APPLY=true
```

---

### 3. Frontend Setup

```bash
cd frontend

# Install dependencies
npm install

# Copy env file
cp .env.example .env
# VITE_API_URL=/api  (proxied to localhost:8080 via vite.config.js)

# Start dev server
npm run dev
```

Frontend starts on **http://localhost:5173**

---

## API Endpoints

### Auth — `/api/auth`
| Method | Endpoint    | Auth | Description          |
|--------|-------------|------|----------------------|
| POST   | /register   | No   | Create account       |
| POST   | /login      | No   | Login, get tokens    |
| POST   | /refresh    | No   | Refresh access token |
| GET    | /me         | JWT  | Get current user     |
| PUT    | /me         | JWT  | Update profile       |

### Transactions — `/api/transactions`
| Method | Endpoint       | Auth | Description                |
|--------|----------------|------|----------------------------|
| POST   | /              | JWT  | Add transaction            |
| GET    | /              | JWT  | List (supports filters)    |
| GET    | /:id           | JWT  | Get single transaction     |
| PUT    | /:id           | JWT  | Update transaction         |
| DELETE | /:id           | JWT  | Delete transaction         |
| GET    | /dashboard     | JWT  | Dashboard summary          |
| GET    | /categories    | JWT  | Predefined category list   |

---

## Environment Variables

| Variable        | Where     | Description                     |
|-----------------|-----------|---------------------------------|
| MONGO_URI       | Backend   | MongoDB Atlas connection string |
| JWT_SECRET      | Backend   | Min 256-bit signing secret      |
| CORS_ORIGINS    | Backend   | Allowed frontend origin(s)      |
| APP_TIME_ZONE   | Backend   | Business calendar timezone (default Asia/Kolkata) |
| VITE_API_URL    | Frontend  | Backend base URL (default /api) |

---

## Phase Plan

| Phase | Feature                                          | Status      |
|-------|--------------------------------------------------|-------------|
| 1     | Auth + Transaction CRUD + Dashboard + React UI   | ✅ Complete |
| 2     | Budgets + Goals + Health Score + Analytics       | Planned     |
| 3     | CSV Import + Subscriptions + PDF Reports         | Planned     |
| 4     | AI Insights (Claude API) + Deployment            | Planned     |

---

FinSmart © 2026 — Java SDE Capstone Project
