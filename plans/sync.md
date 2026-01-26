# Cloud Synchronization Plan (Manual Sync)

## Evaluation of Current Structure

The application currently follows a clean architecture with a clear separation between the data layer and UI.

### Strengths for Synchronization:
- **UUIDs**: Both `PregnancyCaseEntity` and `CaseUpdateEntity` use UUID-based strings for primary keys (`caseId`, `updateId`). This prevents collisions.
- **Sync Flags**: Entities already include an `isSynced: Boolean` flag.
- **Timestamps**: `createdAt` and `capturedAt` are stored as `Long` timestamps.
- **Firestore Presence**: `firebase-firestore` is already available in the project.

### Simplified Approach (Manual Sync):
- **No Background Workers**: We will skip `WorkManager` for now to keep the implementation simple and predictable for the user.
- **Explicit Trigger**: The user will initiate sync via a button in the UI.
- **Single Session Sync**: Sync will only run while the app is in the foreground and the sync operation is active.

---

## Implementation Plan

### 1. Infrastructure Setup
- **DI (Hilt)**: 
    - Provide `FirebaseFirestore` in `AppModule`.
    - Inject `FirebaseFirestore` into the Repository.

### 2. Database & Repository Updates
- **Dao Enhancement**: 
    - Add queries to count unsynced records.
    - Add queries to fetch unsynced records.
- **Repository Interface**:
    - Add `suspend fun syncAll(): Result<Unit>` method.
    - This method will handle the batch upload to Firestore and update local flags.

### 3. Manual Sync Logic
The `syncAll()` implementation will:
1.  Check for network connectivity (simple check).
2.  Fetch all unsynced `PregnancyCase` records and upload to Firestore `cases`.
3.  Fetch all unsynced `CaseUpdate` records and upload to Firestore `case_updates`.
4.  On success for each batch, update local `isSynced = true`.

### 4. UI Integration
- Add a "Sync Now" button to the Dashboard or Settings screen.
- Show a loading indicator and a summary (e.g., "5 records synced").

## Verification Plan

### Automated Verification
- **Unit Tests**: Test the mapping logic between Room and Firestore.

### Manual Verification
1.  **Offline Work**: Create records offline.
2.  **Explicit Sync**: Press the "Sync Now" button and verify data appears in Firestore Console.
3.  **Success Feedback**: Verify the UI showing successful sync count.
cloud.