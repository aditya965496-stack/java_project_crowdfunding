# Online Crowdfunding Platform

[![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4.1-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Vaadin](https://img.shields.io/badge/Vaadin_Flow-24.6.5-00B4F0?logo=vaadin&logoColor=white)](https://vaadin.com/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

A full-stack **Java** crowdfunding platform where creators can launch and manage fundraising campaigns, backers can contribute securely, and administrators oversee platform compliance.

Built entirely in Java using **Spring Boot** (backend) and **Vaadin Flow** (frontend) — a unified, single-language architecture requiring no separate frontend framework or JavaScript build toolchains.

---

## ✨ Features

### 👑 Admin
- **User & Role Management**: Create, view, update, and manage platform users and roles (`ADMIN`, `CREATOR`, `CONTRIBUTOR`).
- **Campaign Governance**: Review, approve, or reject newly submitted campaigns before they go live.
- **Payment Verification**: Dedicated verification view for pending UPI and Card transactions.
- **System Settings**: Dynamically configure platform fee percentage, minimum contributions, and campaign duration limits.
- **Real-Time Monitoring**: Live audit and platform activity log view.

### 🚀 Campaign Creator
- **Campaign Creation**: Rich form for setting funding goals, target deadlines, categories, and media uploads.
- **Progress Tracking & Analytics**: Real-time funding progress bars, percentage targets, and backer counts.
- **Campaign Updates**: Post project milestones and status updates directly to backers.
- **Direct Messaging**: Communicate directly with backers and answer project queries.

### 🤝 Contributor / Backer
- **Campaign Discovery**: Browse, filter by category, and search active fundraising campaigns.
- **Flexible Payments**: Support campaigns via simulated Card or UPI payment gateways.
- **Contribution History**: Track past pledges, funding status, and download payment receipts.
- **Profile Management**: Update user profile information and track backed initiatives.

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| **Language** | Java 17+ |
| **Framework** | Spring Boot 3.4.1 (Spring MVC, Spring Security, Spring Data JPA) |
| **UI / Frontend** | Vaadin Flow 24.6.5 (Pure Java Component-based UI) |
| **Database** | H2 (In-memory / File-based for dev) & MySQL (Production-ready) |
| **Data Access** | Spring Data JPA (Hibernate) + Custom JDBC DAOs (Performance) |
| **Security** | Spring Security with BCrypt hashing, Session management & In-memory Rate Limiting |
| **Build Tool** | Apache Maven 3.9+ |
| **Testing** | JUnit 5, Mockito, Spring Boot Test |

---

## 📂 Project Structure

```
d:\java project\
├── frontend/                          # Vaadin theme definitions and templates
│   ├── generated/                     # Vaadin auto-generated assets (git-ignored)
│   └── index.html                     # HTML root template
│
├── src/
│   ├── main/
│   │   ├── java/com/crowdfund/
│   │   │   ├── CrowdFundApplication.java   # Spring Boot Application entrypoint
│   │   │   │
│   │   │   ├── concurrency/           # Thread-safe async processing
│   │   │   │   └── ConcurrentPaymentVerificationQueue.java
│   │   │   │
│   │   │   ├── config/                # App, Security, and Data Initialization
│   │   │   │   ├── CoreDataInitializer.java    # Seeds admin user & system settings
│   │   │   │   ├── DemoDataSeeder.java         # Optional mock data seeder
│   │   │   │   ├── SecurityConfig.java         # Spring Security & route authorization
│   │   │   │   ├── VaadinAppServiceInitListener.java
│   │   │   │   └── WebMvcConfig.java
│   │   │   │
│   │   │   ├── controller/            # REST API endpoints & Webhooks
│   │   │   │   └── PaymentWebhookController.java
│   │   │   │
│   │   │   ├── dao/                   # High-throughput JDBC access
│   │   │   │   ├── GenericDao.java
│   │   │   │   └── jdbc/
│   │   │   │       ├── ContributionJdbcDao.java
│   │   │   │       └── DatabaseConnectionManager.java
│   │   │   │
│   │   │   ├── dto/                   # Data Transfer Objects & Payloads
│   │   │   │   ├── PaginatedResult.java
│   │   │   │   ├── PaymentIntent.java
│   │   │   │   ├── PaymentRequest.java
│   │   │   │   └── PaymentWebhookPayload.java
│   │   │   │
│   │   │   ├── entity/                # Domain JPA Entities & Enums
│   │   │   │   ├── ActivityLog.java
│   │   │   │   ├── Campaign.java
│   │   │   │   ├── CampaignStatus.java         # PENDING, ACTIVE, FUNDED, EXPIRED, CANCELLED
│   │   │   │   ├── CampaignUpdate.java
│   │   │   │   ├── Contribution.java
│   │   │   │   ├── ContributionStatus.java     # PENDING, COMPLETED, FAILED, REFUNDED
│   │   │   │   ├── Message.java
│   │   │   │   ├── Role.java                   # ADMIN, CREATOR, CONTRIBUTOR
│   │   │   │   ├── SystemSetting.java
│   │   │   │   └── User.java
│   │   │   │
│   │   │   ├── exception/             # Centralized Custom Exceptions & Handlers
│   │   │   │   ├── BusinessRuleException.java
│   │   │   │   ├── CrowdFundException.java
│   │   │   │   ├── DatabaseOperationException.java
│   │   │   │   ├── GlobalRestExceptionHandler.java
│   │   │   │   ├── PaymentVerificationException.java
│   │   │   │   └── ResourceNotFoundException.java
│   │   │   │
│   │   │   ├── repository/            # Spring Data JPA Repositories
│   │   │   │   ├── ActivityLogRepository.java
│   │   │   │   ├── CampaignRepository.java
│   │   │   │   ├── CampaignUpdateRepository.java
│   │   │   │   ├── ContributionRepository.java
│   │   │   │   ├── MessageRepository.java
│   │   │   │   ├── SystemSettingRepository.java
│   │   │   │   └── UserRepository.java
│   │   │   │
│   │   │   ├── security/              # Auth Services, Rate Limiting & UserDetails
│   │   │   │   ├── AuthenticatedUser.java
│   │   │   │   ├── RateLimiterService.java
│   │   │   │   ├── RateLimitingFilter.java
│   │   │   │   └── UserDetailsServiceImpl.java
│   │   │   │
│   │   │   ├── service/               # Core Business Logic & Strategy Pattern
│   │   │   │   ├── ActivityLogService.java
│   │   │   │   ├── CampaignService.java
│   │   │   │   ├── ContributionService.java
│   │   │   │   ├── FileUploadService.java
│   │   │   │   ├── PaymentService.java
│   │   │   │   ├── SimulatedPaymentService.java
│   │   │   │   ├── SystemSettingService.java
│   │   │   │   ├── UserService.java
│   │   │   │   └── payment/           # Payment Strategy Implementations
│   │   │   │       ├── CardPaymentProcessor.java
│   │   │   │       ├── PaymentContext.java
│   │   │   │       ├── PaymentExecutionResult.java
│   │   │   │       ├── PaymentProcessor.java
│   │   │   │       ├── PaymentProcessorFactory.java
│   │   │   │       └── UpiPaymentProcessor.java
│   │   │   │
│   │   │   ├── servlet/               # Servlets
│   │   │   │   └── PaymentReceiptServlet.java   # Receipt download streaming
│   │   │   │
│   │   │   ├── ui/                    # Vaadin Flow Pure Java UI Components
│   │   │   │   ├── common/            # Shared layouts, Login, Registration
│   │   │   │   │   ├── CustomRouteNotFoundErrorView.java
│   │   │   │   │   ├── LoginView.java
│   │   │   │   │   ├── MainLayout.java
│   │   │   │   │   ├── RegisterView.java
│   │   │   │   │   └── RootView.java
│   │   │   │   ├── admin/             # Admin views (Users, Campaigns, Verification, Logs)
│   │   │   │   │   ├── ActivityMonitorView.java
│   │   │   │   │   ├── AdminDashboardView.java
│   │   │   │   │   ├── CampaignManagementView.java
│   │   │   │   │   ├── PaymentVerificationView.java
│   │   │   │   │   ├── SystemSettingsView.java
│   │   │   │   │   └── UserManagementView.java
│   │   │   │   ├── creator/           # Creator views (Campaign Creation, Dashboard, Messages)
│   │   │   │   │   ├── CampaignFormView.java
│   │   │   │   │   ├── CreatorCampaignsView.java
│   │   │   │   │   ├── CreatorDashboardView.java
│   │   │   │   │   └── CreatorMessagesView.java
│   │   │   │   └── contributor/       # Backer views (Browse, History, Updates, Profile)
│   │   │   │       ├── BrowseCampaignsView.java
│   │   │   │       ├── CampaignUpdatesView.java
│   │   │   │       ├── ContributionHistoryView.java
│   │   │   │       ├── ContributorDashboardView.java
│   │   │   │       └── ProfileView.java
│   │   │   │
│   │   │   └── util/                  # Utilities (InputSanitizer, Validation)
│   │   │       └── InputSanitizer.java
│   │   │
│   │   └── resources/
│   │       ├── application.properties         # Default profile (H2 Database)
│   │       └── application-mysql.properties   # Production MySQL profile
│   │
│   └── test/java/com/crowdfund/       # Unit & Integration Tests
│       ├── CrowdFundApplicationTest.java
│       ├── controller/PaymentWebhookControllerTest.java
│       ├── security/
│       │   ├── RateLimiterServiceTest.java
│       │   └── SecurityAuthorizationTest.java
│       ├── service/
│       │   ├── CampaignServiceTest.java
│       │   ├── ContributionServiceTest.java
│       │   ├── FileUploadServiceTest.java
│       │   ├── PaymentServiceTest.java
│       │   ├── SystemSettingServiceTest.java
│       │   └── UserServiceTest.java
│       ├── ui/SlowNetworkAndMobileLayoutTest.java
│       └── util/InputSanitizerTest.java
│
├── .env.example                       # Template for environment configuration
├── .gitignore                         # Excluded build artifacts & database locks
├── pom.xml                            # Project Object Model dependencies & plugins
└── README.md                          # Project documentation
```

---

## 🚀 Getting Started

### Prerequisites
- **Java Development Kit (JDK)**: Version 17 or higher
- **Maven**: Version 3.8+ (or use the included `./mvnw` wrapper)
- **MySQL** (optional; embedded H2 database is enabled by default)

### 1. Clone the Repository
```bash
git clone https://github.com/aditya965496-stack/java_project_crowdfunding.git
cd java_project_crowdfunding
```

### 2. Configure Environment
Copy `.env.example` to `.env` if you need custom credentials:
```bash
cp .env.example .env
```

### 3. Run the Application

#### Option A: Running with Default H2 Database (Recommended for Local Dev)
```bash
./mvnw spring-boot:run
```
*(On Windows PowerShell / CMD: `mvnw.cmd spring-boot:run`)*

The application will start at: **`http://localhost:8080`**

#### Option B: Running with MySQL Profile
Ensure MySQL is running and database exists, then start with:
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql
```

---

## 🔑 Default Credentials & Demo Accounts

On initial startup, default accounts are seeded automatically:

| Role | Email | Password | Access Level |
|---|---|---|---|
| **Admin** | `admin@crowdfund.com` | `Admin@Secure2026!` | Full administrative control & verification |
| **Creator** | `rahul@example.com` | `password` | Create & manage campaigns, post updates |
| **Contributor** | `amit@example.com` | `password` | Back campaigns, view pledges & receipts |

> **Note**: Custom admin credentials can be overridden via `CROWDFUND_ADMIN_EMAIL` and `CROWDFUND_ADMIN_PASSWORD` in your `.env` or system properties.

---

## 🧪 Running Tests

Execute the unit and integration test suite:
```bash
./mvnw clean test
```

---

## 🛡️ Security Highlights
- **Role-Based Access Control**: Strict view and endpoint restrictions via Spring Security.
- **In-Memory Rate Limiting**: Token-bucket rate limiting on authentication and webhook endpoints.
- **Input Sanitization**: Defense against XSS and injection attacks on user-submitted text.
- **Payment Verification Queue**: Thread-safe concurrent queue for processing and verifying asynchronous transactions.

---

## 📄 License


This project was developed as part of an academic/hackathon submission. All rights reserved.
This project is licensed under the [MIT License](LICENSE).
