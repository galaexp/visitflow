# StoreFlow: Offline-First Store Visit & Follow-Up Management System

StoreFlow is a production-grade, offline-first native Android mobile application designed for field sales representatives, merchandisers, retail auditors, and store inspectors.

---

## 1. Core Architecture & Principles

### 1.1 Offline-First Source of Truth
* **Primary Database**: Local Room SQLite database (`storeflow_offline.db`).
* **Zero Network Prerequisite**: Every core operation—store directory browsing, starting/ending visits, recording observation notes, checklist inspections, photo attachments, action tracking, scheduling, Excel generation, and backup creation—functions with 100% reliability with zero internet connection or in airplane mode.
* **Persistent Outbox Pattern**: Every local write (`CREATE`, `UPDATE`, `DELETE`) enqueues a persistent event in `SyncOutboxEntity` with retry tracking and audit logging in `AuditEventEntity`. Data is never lost on network disconnect, app kill, or device restart.
* **Stateless Cloudflare Worker Relay**: The optional backend Worker provides an authenticated API endpoint to receive mobile outbox batches and relay live updates to dashboards. The device remains authoritative.

---

## 2. Technology Stack

* **Platform**: Android 14+ (minSdk 24, targetSdk 36)
* **Language**: Kotlin 2.2+ (100% Coroutines & Flow)
* **UI**: Jetpack Compose with Material 3 (corporate navy theme, light/dark mode support, edge-to-edge window insets)
* **Architecture**: MVVM with repository pattern
* **Database**: Room 2.7.0 with KSP (8 normalized tables, indices, foreign keys)
* **Image Handling**: App-private storage (`context.filesDir/photos/`) with compressed JPEG evidence and Coil image loading
* **Excel Export**: Pure Kotlin OpenXML (ECMA-376) zip container generator producing genuine `.xlsx` workbooks across 6 worksheets
* **Sharing**: Android Sharesheet with `FileProvider` content URIs
* **Backend**: Cloudflare Worker in TypeScript (`backend/worker.ts`)

---

## 3. Database Schema (Normalized Tables)

1. `stores`: Unique store identifier (UUID), unique code (`code`), name, branch, address, city, region, GPS coords, category, contact person, phone, email, assigned worker, visit frequency, preferred days, opening hours, remarks, active/archive flags.
2. `visits`: Unique visit UUID, `storeId` foreign key, worker name, start/end UTC timestamps, timezone, observation notes, issues identified, positive highlights, recommendations, priority, follow-up flags, status (`IN_PROGRESS`, `COMPLETED`, `DRAFT`, `CANCELLED`).
3. `actions`: Action UUID, `storeId` foreign key, originating `visitId`, title, description, category, priority (`LOW`, `NORMAL`, `HIGH`, `CRITICAL`), status (`OPEN`, `IN_PROGRESS`, `COMPLETED`, `DEFERRED`, `UNABLE_TO_COMPLETE`, `CANCELLED`), assigned lead, created timestamp, target due date, completion timestamp, completed by, completion evidence notes.
4. `photos`: Photo UUID, `visitId`, `actionId`, private file path, caption, captured timestamp, file size, evidence flag.
5. `scheduled_visits`: Schedule UUID, `storeId`, scheduled date/time, purpose, preparation notes, recurring frequency (`ONCE`, `DAILY`, `WEEKLY`, `BI_WEEKLY`, `MONTHLY`), completion status, linked visit ID.
6. `checklist_items`: Checklist UUID, `visitId`, inspection category, prompt text, status (`PASS`, `FAIL`, `NA`, `NEEDS_FOLLOW_UP`), notes.
7. `sync_outbox`: Event UUID, entity type (`STORE`, `VISIT`, `ACTION`, `PHOTO`, `SCHEDULE`), operation, payload JSON, status (`PENDING`, `IN_PROGRESS`, `FAILED`, `SYNCED`), retry count, last error.
8. `audit_events`: Audit UUID, entity type, record ID, action type, description details, timestamp.

---

## 4. Key Features & Workflows

### 4.1 Home Dashboard
* Reactive KPI statistics: Visits Completed Today, Open Actions, Overdue Actions, Scheduled Visits.
* Active In-Progress Visit Recovery Banner with live duration timer and instant resume.
* Queued outbox changes banner with manual "Sync Now" button.
* Attention-required overdue action preview cards.

### 4.2 Store Management
* Instant search by store name, code, city, or contact person.
* Active and Archived store directory filtering.
* Add/Edit store dialog with full validation.
* Store Details modal with one-tap "Start Visit" and "Schedule Visit".

### 4.3 Active Visit Workflow
* Real-time elapsed duration counter.
* Multi-section rich notes: General notes, Issues & Non-compliance, Positive highlights, Recommendations.
* 5-point retail inspection checklist (Pass, Fail, N/A, Follow-up).
* In-visit action item creation.
* Photograph capture (Camera via `FileProvider` or photo picker).
* "Save Draft" for safe persistence.
* "End Visit" workflow with follow-up date prompt and duration calculation.

### 4.4 Action Center
* Status filters: All, Open, Overdue, High/Critical, Completed.
* Dynamic overdue status calculation based on target due date.
* Action status transition modal with required completion notes and completed-by logging.

### 4.5 Visit Scheduling
* Planning dialog with store picker, target date, purpose, and recurrence intervals (Daily, Weekly, Bi-weekly, Monthly).
* One-tap "Start Visit" directly from scheduled appointment cards.
* Automatic generation of next recurrence upon marking a recurring appointment as complete.

### 4.6 Reports & Sharing
* **Master Excel (.xlsx) Export**: Generates genuine OpenXML multi-tab workbooks containing:
  - `Visit Summary`
  - `Visit Details`
  - `Action Items`
  - `Photo Index`
  - `Store Directory`
  - `Scheduled Visits`
* **WhatsApp / Messaging Sharing**: Generates clean formatted Markdown reports with store details, timestamps, observations, action items, and follow-up deadlines; preview & edit before sharing.
* **Offline Backup & Restore**: Creates versioned standalone JSON archive of all tables; validates schema before restoring.

---

## 5. Cloudflare Worker Setup (`backend/`)

1. Install Wrangler:
   ```bash
   cd backend
   npm install
   ```
2. Start local development server:
   ```bash
   npx wrangler dev
   ```
3. Deploy to Cloudflare:
   ```bash
   npx wrangler deploy
   ```
4. Configure in App:
   - Navigate to **Settings** in StoreFlow.
   - Enter your Worker URL (e.g., `https://storeflow-worker.<subdomain>.workers.dev`).
   - Enter `storeflow-dev-secret-token` in Bearer Auth Token.
   - Tap **Sync Now**.
