# Implementation Plan: Patient Screening & Append-Only Audit Trail

This document outlines the strategy for implementing patient screening views and an "Append-Only" audit system for record management in the Lifelines mobile app.

## 1. Architectural Strategy: The Append-Only Principle
Instead of updating existing rows (destructive updates), every change by a responder will result in a **new entry**. This ensures a perfect audit trail, allowing admins to see exactly how a case evolved over time.

### Why Append-Only?
- **Data Integrity**: No clinical data is ever lost.
- **Sync Reliability**: Easier to sync "deltas" or new logs to the server than complex row merges.
- **Resilience**: If a responder makes a mistake, the previous state is still in the local DB.

---

## 2. Database Design (Room)

To achieve this, we will move from a single "flat" table to a **Master-Log** relationship.

### A. The Master Entry: `pregnancy_cases`
This table stores the "Anchor" of the patient. It rarely changes once created.

| Column | Type | Description |
| :--- | :--- | :--- |
| `caseId` | `UUID (PK)` | The permanent identifier for this patient/case. |
| `patientFullName` | `String` | Static identity. |
| `dateOfBirth` | `Long` | Static identity. |
| `latestUpdateId` | `UUID?` | FK cache of the most recent update for performance. |
| `createdAt` | `Long` | Initial creation timestamp. |
| `createdBy` | `String` | Responder ID who opened the case. |
| `isSynced` | `Boolean` | Sync status for the identity record. |

### B. The Audit Log: `case_updates` (Append-Only)
Every "Edit" or "Screening" adds a row here.

| Column | Type | Description |
| :--- | :--- | :--- |
| `updateId` | `UUID (PK)` | Unique ID for this specific version. |
| `caseId` | `UUID (FK)` | Links back to the Master Entry. |
| `version` | `Int` | Simple incrementing version number (1, 2, 3...). |
| `updatedBy` | `String` | Responder ID who made this specific change. |
| `latitude` | `Double?` | GPS Lat where the screening occurred. |
| `longitude` | `Double?` | GPS Long where the screening occurred. |
| `pregnancyStage` | `String` | e.g., "Trimester 1" |
| `status` | `String` | "ACTIVE", "RISK", etc. |
| `allergies` | `String?` | Known allergies at time of update. |
| `keyRisks` | `String?` | Identified risk factors (e.g., "High BP"). |
| `clinicalNotes` | `String?` | Detailed observation notes. |
| `mediaPath` | `String?` | Local path to captured photos or medical reports. |
| `capturedAt` | `Long` | Timestamp of this specific update. |
| `isSynced` | `Boolean` | Sync status for this specific log. |

---

## 3. Detailed Edge Case Strategies

### 1. Location Tracking (`latitude`/`longitude`)
- **How it works**: When the responder clicks "Submit", the `ViewModel` uses the Android **FusedLocationProviderClient** to grab the last known coordinates.
- **Purpose**: NGO admins can visualize a heatmap of screenings on a map to ensure coverage of remote villages and verify field presence.
- **Privacy**: Only captured during the submission event, not continuous tracking.

### 2. Simple Conflict Resolution
- **Approach**: "Last-In-Wins" with full history.
- **Scenario**: If two responders edit the same case while offline, both entries are saved as new rows with their own timestamps.
- **Outcome**: The UI simply displays the record with the most recent `capturedAt` or highest `version`. No data is overwritten, so admins can still see both versions.

### 3. Media & Attachments (`mediaPath`)
- **Storage Strategy**: 
    - **Physical File**: Save images to the app's private directory (`context.filesDir`).
    - **DB Pointer**: The `mediaPath` column stores only the relative string path to that file.
- **Syncing**: The sync engine first uploads the file to cloud storage, then updates the remote database with the cloud URL.

### 4. Camera Integration
- **Intent-Based Strategy**: Use `ActivityResultContracts.TakePicture()` to launch the system camera. This avoids the complexity of building a custom camera UI and ensures stability.
- **Privacy (FileProvider)**: 
    - Photos are saved directly into the app's internal cache/files folder.
    - Uses a `FileProvider` (XML config in Manifest) to grant the Camera app temporary permission to write to our private folder.
    - **Result**: Photos do **not** appear in the user's public gallery, protecting patient privacy.
- **UI Flow**: 
    1. Click "Take Photo".
    2. Create temp file & URI.
    3. Open System Camera.
    4. On success, show thumbnail and save path to the `case_updates` row.

### C. The "Current State" View
We will use a Room `@Query` to fetch the **latest** version for the main list:
```sql
SELECT * FROM case_updates 
WHERE caseId = :cid 
ORDER BY version DESC LIMIT 1
```

---

## 4. Implementation Steps

### Phase 1: Data Layer Migration
1. Create `CaseUpdateEntity`.
2. Update `PregnancyCaseRepository` to handle "Double Insertion" (Insert Master + Initial Update).
3. Implement `getHistoryForCase(caseId)` to show the audit trail.

### Phase 2: The Screening View (UI)
- **The "Timeline" Component**: A vertical line showing "Created" -> "Month 3 Update" -> "Urgent Alert".
- **The "Compare" Mode**: Highlight what changed between Version 2 and Version 3 (e.g., Status changed from Green to Red).

### Phase 3: The "Edit" Flow
- When the user clicks "Edit", the UI pre-fills the form with the **Latest Version** data.
- Clicking "Save" calls `repository.insertUpdate()`, which pushes a new row with `version + 1`.

---

## 5. Discussion Points
- **Performance**: As the audit log grows, querying the "latest" for 1000 patients might slow down. **Resolved**: We maintain a `latestUpdateId` field in the Master table for fast indexing and O(1) list fetching.
- **Storage**: Mobile storage is limited. *Solution: We might only keep the last 5 versions locally but keep the full history on the cloud.*

---
**Status**: Planning
**Next Step**: Refactor `PregnancyCaseEntity` into `Master` + `Update` structure.
