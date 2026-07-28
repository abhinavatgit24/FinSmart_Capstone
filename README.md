# FinSmart — AI-Powered Personal Finance Assistant

A full-stack personal finance application that helps users track spending, manage budgets and goals, analyse financial habits, calculate financial health, import bank transactions, and plan investments.

> Educational / portfolio project. Financial projections and AI responses are informational only and are not financial advice.

---

## Features

### Authentication & Profile

- Secure user registration and login
- JWT-based authentication
- Access token and refresh token flow
- User profile and preferred currency support

### Transactions

- Add, edit, delete, and view income and expense transactions
- Filter transactions by type, category, and date range
- Automatic category detection from transaction descriptions
- Categories include Food, Travel, Bills, Entertainment, Shopping, Health, Education, Salary, and Other
- Dashboard summary with income, expenses, balance, recent transactions, and category breakdown

### CSV Import

- Import transaction data from CSV files
- Supports common columns such as:
  - `date`
  - `amount`
  - `type`
  - `description`, `narration`, `particulars`, or `remarks`
  - `category`
- Supports bank-style CSV files with separate `debit` and `credit` columns
- Automatically categorises imported transactions when no category is provided

### Budgets

- Create category budgets for weekly or monthly periods
- Track budget utilisation and spending percentage
- View budget alerts:
  - On track
  - Warning
  - Exceeded

### Savings Goals

- Create and manage savings goals
- Track saved amount against target amount
- Set deadlines and view progress percentage
- Estimate whether goals are on track based on financial activity

### Analytics

- Category-wise spending and income analysis
- Monthly income, expense, and savings comparison
- Spending trend charts
- Subscription / recurring-payment detection
- Personalised insights based on spending patterns

### Financial Health Score

Financial Health Score gives an overall picture of the user's financial wellbeing based on recorded transaction history.

It considers:

- Savings ratio across overall income and expenses
- Budget adherence for active budgets
- Spending consistency across recorded months

The score is shown from 0–100:

| Score | Band |
|---:|---|
| 80–100 | Excellent |
| 60–79 | Good |
| 40–59 | Average |
| 0–39 | Poor |

If there is no transaction data, the app shows an "insufficient data" state instead of an artificial score.

### AI Finance Assistant

- Ask questions about spending, savings, budgets, goals, and financial health
- Uses Google Gemini AI
- Provides the AI with:
  - Current-month summary
  - Previous-month summary
  - Up to six months of monthly summaries
  - Category spending
  - Budgets
  - Savings goals
  - Financial health status
- Designed to answer questions such as:
  - "How much did I spend last month?"
  - "Compare my expenses this month and last month."
  - "Which category has the highest spending?"
  - "How can I improve my savings?"

### Monthly Report

- Generate a printable monthly financial report
- Includes income, expenses, savings, category-wise spending, budgets, goals, and financial health score
- Open the report in the browser and save it as PDF using the print dialog

### Grow — Financial Calculators

The Grow section provides planning calculators without bank offers, payments, or external investment integrations.

- SIP calculator
  - Monthly investment
  - Expected return
  - Investment duration
  - Optional yearly SIP increase
  - Growth projection chart

- Mutual Fund calculator
  - One-time investment
  - Expected return
  - Investment duration
  - Inflation-adjusted future value

- Fixed Deposit calculator
  - Deposit amount
  - Interest rate
  - Tenure
  - Yearly, half-yearly, quarterly, or monthly compounding
  - Estimated maturity value

- EMI / Loan calculator
  - Loan amount
  - Interest rate
  - Loan tenure
  - Processing fee
  - EMI, total interest, total payable, and repayment chart

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 17, Spring Boot 3, Spring Security |
| Database | MongoDB |
| Frontend | React 18, Vite, Tailwind CSS |
| Charts | Recharts |
| Authentication | JWT |
| AI | Google Gemini API |
| Build Tools | Maven, npm |

---

## Project Structure

```text
finsmart_phase3/
├── backend/
│   ├── src/main/java/com/finsmart/
│   │   ├── config/
│   │   ├── controller/
│   │   ├── dto/
│   │   ├── exception/
│   │   ├── model/
│   │   ├── repository/
│   │   ├── security/
│   │   ├── service/
│   │   └── util/
│   ├── src/main/resources/
│   │   └── application.properties
│   └── pom.xml
│
├── frontend/
│   ├── src/
│   │   ├── api/
│   │   ├── components/
│   │   ├── context/
│   │   ├── hooks/
│   │   ├── pages/
│   │   │   ├── analytics/
│   │   │   ├── auth/
│   │   │   ├── budgets/
│   │   │   ├── dashboard/
│   │   │   ├── goals/
│   │   │   ├── grow/
│   │   │   ├── health/
│   │   │   ├── import/
│   │   │   └── transactions/
│   │   └── utils/
│   ├── package.json
│   └── vite.config.js
│
└── README.md
```

---

## Prerequisites

Install the following before running the project:

- Java 17 or newer
- Maven 3.8 or newer
- Node.js 18 or newer
- MongoDB Atlas account or local MongoDB
- Google Gemini API key for the AI assistant

---

## Backend Setup

Open a terminal in the backend directory:

```bash
cd backend
```

Set the required environment variables.

**PowerShell**

```powershell
$env:MONGO_URI="mongodb+srv://<username>:<password>@<cluster-url>/finsmart"
$env:JWT_SECRET="replace-with-a-long-random-secret-at-least-32-characters"
$env:CORS_ORIGINS="http://localhost:5173"
$env:GEMINI_API_KEY="your-google-gemini-api-key"
$env:APP_TIME_ZONE="Asia/Kolkata"
```

Run the backend:

```bash
mvn spring-boot:run
```

The backend starts on:

```
http://localhost:8080
```

---

## Frontend Setup

Open another terminal in the frontend directory:

```bash
cd frontend
```

Install dependencies:

```bash
npm install
```

Create a `.env` file:

```
VITE_API_URL=http://localhost:8080/api
```

Start the development server:

```bash
npm run dev
```

The frontend starts on:

```
http://localhost:5173
```

---

## Environment Variables

| Variable | Layer | Description |
|---|---|---|
| `MONGO_URI` | Backend | MongoDB connection string |
| `JWT_SECRET` | Backend | JWT signing secret |
| `CORS_ORIGINS` | Backend | Allowed frontend origin |
| `GEMINI_API_KEY` | Backend | Google Gemini API key |
| `APP_TIME_ZONE` | Backend | Business calendar timezone; defaults to Asia/Kolkata |
| `VITE_API_URL` | Frontend | Backend API URL |

> Never put `GEMINI_API_KEY`, `JWT_SECRET`, or MongoDB credentials in frontend files or commit them to Git.

---

## API Overview

### Authentication

```
POST /api/auth/register
POST /api/auth/login
POST /api/auth/refresh
GET  /api/auth/me
PUT  /api/auth/me
```

### Transactions

```
POST   /api/transactions
GET    /api/transactions
GET    /api/transactions/{id}
PUT    /api/transactions/{id}
DELETE /api/transactions/{id}
GET    /api/transactions/categories
```

### Dashboard and Analytics

```
GET /api/dashboard/summary
GET /api/analytics
GET /api/analytics/subscriptions
```

### Budgets, Goals, Health, and Notifications

```
POST   /api/budgets
GET    /api/budgets
GET    /api/budgets/utilisation
DELETE /api/budgets/{id}

POST   /api/goals
GET    /api/goals
GET    /api/goals/{id}
PUT    /api/goals/{id}
DELETE /api/goals/{id}

GET /api/health/score

GET   /api/notifications
PATCH /api/notifications/{id}/read
PATCH /api/notifications/read-all
```

### Import, Reports, and AI

```
POST /api/import/csv
GET  /api/report/monthly?year=YYYY&month=MM
POST /api/ai/ask
```

Example AI request:

```json
{
  "question": "Compare my expenses this month with last month."
}
```

---

## MongoDB Date Migration

The application stores transaction calendar dates in UTC to prevent timezone-related date shifts.

If the database contains transactions created before this fix, back up MongoDB first. Then run a dry run:

```powershell
$env:APP_LEGACY_DATE_MIGRATION_ENABLED="true"
```

Review the logged number of affected records.

To apply the migration once:

```powershell
$env:APP_LEGACY_DATE_MIGRATION_ENABLED="true"
$env:APP_LEGACY_DATE_MIGRATION_APPLY="true"
```

Remove these variables after the migration completes.

---

## Disclaimer

FinSmart is a personal finance learning and planning application. Investment, return, FD, SIP, mutual fund, and EMI calculations are illustrative estimates only. They do not constitute investment, lending, tax, or financial advice.
