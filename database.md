# Database Schema

This document defines the local (Room) and remote (Firestore) database structure for the Lifelines mobile application.

## 1. Local Database (Room)

The local database uses an **Append-Only** strategy for case management to ensure data integrity and a perfect audit trail.

### Table: `pregnancy_cases` (Master Entry)
Stores the anchor identity of a patient. This record rarely changes.

| Column | Type | Description |
| :--- | :--- | :--- |
| `caseId` | `String (PK)` | UUID identifier for the case. |
| `patientFullName` | `String` | Full name of the patient. |
| `dateOfBirth` | `Long` | Birth date as epoch timestamp. |
| `latestUpdateId` | `String?` | Cache of the most recent `updateId` for performance. |
| `createdAt` | `Long` | Creation timestamp. |
| `createdBy` | `String` | Responder ID who opened the case. |
| `isSynced` | `Boolean` | Sync status of this master record. |

### Table: `case_updates` (Audit Log)
Every edit or screening adds a new row here. Primary source for clinical data.

| Column | Type | Description |
| :--- | :--- | :--- |
| `updateId` | `String (PK)` | UUID for this specific version. |
| `caseId` | `String (FK)` | Links to `pregnancy_cases.caseId`. |
| `version` | `Int` | Incrementing version number. |
| `updatedBy` | `String` | Responder ID who made this change. |
| `latitude` | `Double?` | GPS Latitude at time of capture. |
| `longitude` | `Double?` | GPS Longitude at time of capture. |
| `pregnancyStage` | `String` | e.g., "Trimester 1", "Month 5". |
| `status` | `String` | "ACTIVE", "RISK", "CLOSED", etc. |
| `allergies` | `String?` | Clinical allergy notes. |
| `keyRisks` | `String?` | Identified risk factors. |
| `clinicalNotes` | `String?` | Detailed summary of the checkup. |
| `mediaPath` | `String?` | Local path to media (photos/reports). |
| `capturedAt` | `Long` | Timestamp of this update. |
| `isSynced` | `Boolean` | Sync status for this log entry. |

### Table: `distress_events`
Incoming distress signals from patient devices.

| Column | Type | Description |
| :--- | :--- | :--- |
| `eventId` | `String (PK)` | Unique event ID. |
| `caseId` | `String` | ID of the case in distress. |
| `occuredAt` | `Long` | When the event happened. |
| `receivedAt` | `Long` | When the responder received it. |
| `latitude` | `Double` | Location of distress. |
| `longitude` | `Double` | Location of distress. |
| `status` | `String` | "OPEN", "ACKED", "RESOLVED", "FALSE_ALARM". |

### Table: `user_accounts`
Local cache of the responder's profile.

| Column | Type | Description |
| :--- | :--- | :--- |
| `userId` | `String (PK)` | Responder ID. |
| `fullName` | `String` | |
| `role` | `String` | "RESPONDER", "SUPERVISOR". |
| `phone` | `String?` | |
| `email` | `String` | |
| `isActive` | `Boolean` | |
| `lastLoginAt` | `Long` | |

### Table: `assignments`
Mapping of which responder is responsible for which patient.

| Column | Type | Description |
| :--- | :--- | :--- |
| `assignmentId` | `String (PK)` | Assignment identifier. |
| `caseId` | `String` | |
| `responderId` | `String` | |
| `assignedAt` | `Long` | |
| `isActive` | `Boolean` | |
| `priority` | `String` | "LOW", "MEDIUM", "HIGH". |

---

## 2. Remote Database (Firestore)

Firestore mirrors the structure above but is organized into collections for cloud synchronization.

- `/users/{userId}`: User profiles.
- `/cases/{caseId}`: Master patient records.
- `/cases/{caseId}/updates/{updateId}`: Sub-collection for the append-only audit trail.
- `/distress/{eventId}`: Global distress event tracking.
- `/assignments/{assignmentId}`: Current tasks and responsibilities.

---

## 3. NFC Payload Structure

Data written to the physical NFC tag on the patient's device for offline portability:
1. `caseId`
2. `patientFullName`
3. `dateOfBirth`
4. `pregnancyStage`
5. `allergies`
6. `keyRisks`
7. `lastCheckupSummary`
8. `lastCheckupAt`
9. `lastUpdatedAt`
