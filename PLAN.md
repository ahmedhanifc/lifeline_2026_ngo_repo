# Implementation Plan - Lifelines Responder App

## Design System & Aesthetics
*   **Theme**: Clean, clinical Blue/White palette (`#3B6EB4` primary blue).
*   **Layout**: Card-based interface with soft shadows and rounded corners (Material 3).
*   **Navigation**: Bottom Navigation Bar (Home, Records, Sync, Profile).
*   **Typography**: Clean sans-serif (Inter or Roboto).

## Phase 1: Foundation & Data Layer (Offline-First)

### 1.1 Project Configuration
- [ ] **Dependencies**: Add missing dependencies:
    - Room Database (bundled with KSP/Kapt)
    - Hilt (Dependency Injection)
    - Google Maps Compose
    - Navigation Compose
    - Coroutines/Flow
- [ ] **Folder Structure**: Set up clean architecture packages:
    - `data/` (local, remote, repository)
    - `domain/` (models, usecases)
    - `ui/` (screens, components, theme)
    - `di/` (Hilt modules)

### 1.2 Local Database (Room)
- [ ] **Entities**: Create data classes annotated with `@Entity`:
    - `PregnancyCaseEntity`
    - `CheckupNoteEntity`
    - `DistressEventEntity`
    - `UserAccountEntity`
    - `AssignmentEntity`
- [ ] **DAOs**: Define data access objects for each entity with `Flow` return types.
- [ ] **Database Class**: Setup `AppDatabase` with migrations support.

### 1.3 Remote Data (Firebase)
- [ ] **Firestore Helpers**: create mapper functions to convert between Firestore documents and Domain/Entity models.
- [ ] **Authentication**: Setup `FirebaseAuth` repository for login flow.

### 1.4 Repository Layer (Synchronization)
- [ ] **Unified Repository**: Create repositories that manage data flow:
    - *Read*: Always observe Local Database (Single Source of Truth).
    - *Write*: Write to Local Database first, then trigger background sync to Firestore.
- [ ] **Sync Mechanism**: Implement a basic sync strategy (e.g., `WorkManager`) to push local changes when online and pull remote updates.

---

## Phase 2: Core UI & Navigation

### 2.1 Navigation
- [ ] Setup `NavHost` with routes:
    - `Login`
    - `Dashboard` (Home)
    - `CaseDetails`
    - `Map`

### 2.2 Authentication Screen
- [ ] **Login UI**: Match mockup design.
    - [ ] Logo header ("NGO Responder" with heart pulse icon).
    - [ ] Input fields for Email/Password with internal labels.
    - [ ] Password visibility toggle.
    - [ ] Social Login Buttons (Google/Facebook placeholders).
- [ ] **Logic**: Authenticate via Firebase, then fetch User Profile and Assignments.

### 2.3 Dashboard (Home)
- [ ] **Top Header**: "Welcome back, {Name}" with notification bell.
- [ ] **Feature Cards**: Implement the vertical list of interactive cards:
    - [ ] `Local Records`: Show count of records stored on device.
    - [ ] `Scan NFC Tag`: Quick action for immediate proximity reading.
    - [ ] `Create New Record`: Direct link to new case form.
    - [ ] `View Records`: Searchable list of all cases.
    - [ ] `Sync Data`: Manual trigger for Firestore sync.
- [ ] **Recent Updates Section**:
    - [ ] Horizontal/Vertical list of the last 3-5 modified records.
    - [ ] Display Patient Name, Status (e.g., "Under Treatment"), and Relative Time.

### 2.4 Navigation Shell
- [ ] **Bottom Navigation**: Persistent bar with icons for Quick Access.

---

## Phase 3: Feature - Case Management

### 3.1 Case Details Logic
- [ ] **View Mode**: Display patient info, risk factors, and pregnancy stage.
- [ ] **History**: Show list of previous `CheckupNotes`.

### 3.2 Add/Update Case
- [ ] **Forms**: Inputs for `PregnancyCase` fields.
- [ ] **Checkup Note**: Simple text input to append a new note (linked to `CaseId`).

---

## Phase 4: Hardware Integration

### 4.1 NFC Module
- [ ] **NFC Manager**: Class to handle `NfcAdapter`.
- [ ] **NDEF Handling**:
    - *Read*: Parse NDEF records into a temporary `NfcPatientData` object.
    - *Write*: Serialize patient status + last checkup into NDEF records and write to tag.
- [ ] **UI Integration**: "Scan Tag" button/dialog on Dashboard and Case Details.

### 4.2 Maps & Distress Signals (Technical Detail)
- [ ] **Dependencies**:
    - `com.google.maps.android:maps-compose` (Jetpack Compose wrapper)
    - `com.google.android.gms:play-services-location` (FusedLocationProvider)
- [ ] **Offline Architecture**:
    - **Single Source of Truth**: The Map UI will *only* observe the local Room database (`DistressEventEntity`), never Firestore directly. This ensures markers load immediately even without internet.
    - **Sync Worker**: A background `WorkManager` job will fetch new `DistressEvents` from Firestore and upsert them into Room when connectivity is available.
- [ ] **Map Implementation**:
    - Implement `GoogleMap` composable with `MapProperties` (enable user location).
    - **Marker Logic**:
        - Iterate through the `List<DistressEvent>` state flow from the DB.
        - Render `Marker` for each event.
        - **Color Coding**: Red for `OPEN`, Yellow for `ACKED`, Green for `RESOLVED`.
    - **User Location**: Use `FusedLocationProviderClient` to get the responder's current coordinates for navigation calculations.
- [ ] **Distress Actions**:
    - Clicking a marker opens a BottomSheet with details.
    - "Acknowledge" button updates the local DB state to `ACKED`.
    - Sync Engine pushes this update to Firestore when online.

---

## Phase 5: Polish & Optimization

- [ ] **Offline UX**: Visual indicators when app is offline/syncing.
- [ ] **Error Handling**: Graceful degradation when sensors/network fail.
- [ ] **Theming**: Apply "Lifelines" visual identity (styling, typography).
