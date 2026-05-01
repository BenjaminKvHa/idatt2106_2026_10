# Nettdetektivene

A gamified web application that teaches children (grades 3–7) to recognize and handle disinformation, digital crime, and critical online situations. Built as part of IDATT2106 – Systemutvikling med smidig prosjekt at NTNU.

---

## Table of Contents

- [About the Project](#about-the-project)
- [Tech Stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Installation and Setup](#installation-and-setup)
- [Running the Application](#running-the-application)
- [Running Tests](#running-tests)
- [Project Structure](#project-structure)
- [API Documentation](#api-documentation)
- [Environment Variables](#environment-variables)
- [CI/CD](#cicd)
- [Contributors](#contributors)
- [License](#license)

---

## About the Project

Nettdetektivene is an educational game where pupils navigate a map of "stops" (e.g., Nyhetskvartalet, Fotografen, Postkontoret, Passordbanken), solving progressively harder tasks about source criticism, phishing, password safety, and AI-generated content. Teachers can create classrooms, manage pupils, create custom tasks, and track progression.

Key features:

- **Pupils**: Avatar customization, map-based progression, notebook, medals, leaderboard, "Ukens mysterium" submissions
- **Teachers**: Classroom management, pupil progression overview, task creation, notification center

---

## Tech Stack

| Layer     | Technology                        |
|-----------|-----------------------------------|
| Backend   | Java, Spring Boot                 |
| Frontend  | Vue.js (or your chosen SPA framework) |
| Database  | MySQL (or PostgreSQL — update accordingly) |
| Build     | Maven / Gradle (backend), npm / yarn (frontend) |
| CI/CD     | GitHub Actions                    |
| Testing   | JUnit, Mockito (backend), Vitest / Jest (frontend) |

> **Note**: Update this table to reflect your actual stack choices.

---

## Prerequisites

- **Java** JDK 21+ (or your version)
- **Maven** 3.9+ or **Gradle** 8+
- **Node.js** 20+ and **npm** 10+ (or **yarn**)
- **MySQL** 8.0+ (or the database you're using)
- **Git**

---

## Installation and Setup

### 1. Clone the repository

```bash
git clone https://github.com/YOUR_ORG/idatt2106_2026_XX.git
cd idatt2106_2026_XX
```

### 2. Database setup

Create the database and run the schema:

```bash
mysql -u root -p -e "CREATE DATABASE nettdetektivene;"
mysql -u root -p nettdetektivene < path/to/schema.sql
```

Optionally, seed with example data:

```bash
mysql -u root -p nettdetektivene < path/to/seed.sql
```

### 3. Backend setup

```bash
cd backend
cp .env.example .env   # Fill in your database credentials
mvn clean install       # or: ./gradlew build
```

### 4. Frontend setup

```bash
cd frontend
cp .env.example .env   # Set API base URL etc.
npm install            # or: yarn install
```

---

## Running the Application

### Backend

```bash
cd backend
mvn spring-boot:run    # or: ./gradlew bootRun
```

The API will be available at `http://localhost:8080` (default).

### Frontend

```bash
cd frontend
npm run dev            # or: yarn dev
```

The client will be available at `http://localhost:5173` (default for Vite).

---

## Running Tests

### Backend

```bash
cd backend
mvn test               # or: ./gradlew test
```

Coverage report is generated at `target/site/jacoco/index.html` (if using JaCoCo).

> Requirement: ≥ 50% server-side test coverage.

### Frontend

```bash
cd frontend
npm run test           # or: yarn test
npm run coverage       # to view coverage report
```

> Requirement: ≥ 30% client-side test coverage.

---

## Project Structure

```
idatt2106_2026_XX/
├── backend/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/          # Application source code
│   │   │   └── resources/     # application.properties, schema.sql
│   │   └── test/              # Test source code
│   └── pom.xml                # or build.gradle
├── frontend/
│   ├── src/
│   │   ├── components/        # Reusable Vue components
│   │   ├── views/             # Page-level views
│   │   ├── router/            # Vue Router config
│   │   ├── stores/            # Pinia / Vuex stores
│   │   └── assets/            # Images, styles, sounds
│   └── package.json
├── docs/                      # Additional documentation
└── README.md
```

> **Note**: Update this tree to match your actual project layout.

---

## API Documentation

REST endpoints are documented via [Swagger / OpenAPI] and available at:

```
http://localhost:8080/swagger-ui.html
```

> If you don't use Swagger, list key endpoint groups here (e.g., `/api/auth`, `/api/classrooms`, `/api/tasks`, `/api/pupils`).

---

## Environment Variables

### Backend (`backend/.env` or `application.properties`)

| Variable              | Description                    | Example                    |
|-----------------------|--------------------------------|----------------------------|
| `DB_URL`              | JDBC connection string         | `jdbc:mysql://localhost:3306/nettdetektivene` |
| `DB_USERNAME`         | Database user                  | `root`                     |
| `DB_PASSWORD`         | Database password              | `secret`                   |
| `JWT_SECRET`          | Secret key for JWT signing     | `my-secret-key`            |
| `JWT_EXPIRATION_MS`   | Access token lifetime (ms)     | `3600000`                  |

### Frontend (`frontend/.env`)

| Variable           | Description         | Example                    |
|--------------------|---------------------|----------------------------|
| `VITE_API_BASE_URL`| Backend API base URL| `http://localhost:8080/api` |

---

## CI/CD

This project uses **GitHub Actions** for Continuous Integration:

- Runs on every push and pull request to `main`/`dev`
- Builds backend and frontend
- Runs all tests and generates coverage reports
- (Optional) Deploys to staging/production

Pipeline config: `.github/workflows/ci.yml`

---

## Contributors

**Scrum Team XX** — IDATT2106 Spring 2026, NTNU

| Name             | Role       |
|------------------|------------|
| Firstname Lastname | Developer  |
| Firstname Lastname | Developer  |
| Firstname Lastname | Scrum Master |
| ...              | ...        |

---

## License

This project was developed as part of a university course at NTNU and is not licensed for commercial use.