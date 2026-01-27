# Lifelines 2026 – NGO Responder Application

## Project Background

This project is designed for disaster- and conflict-affected zones where connectivity, power, and medical infrastructure are unreliable. The application is built for NGO responders who are dispatched into these environments to support pregnant women and their caretakers. Responders may be doctors, midwives, or trained first aid personnel operating under significant resource constraints and time pressure.

The application is intended to function reliably in the field, where internet access may be intermittent or completely unavailable, and where maintaining continuity of care depends on local, offline access to accurate patient records.

## Project Goal

The goal of this project is to build a lightweight Android application that runs on an NGO responder’s phone and supports the full lifecycle of a pregnancy case in crisis settings. The application allows a responder to create a new pregnancy record during an initial checkup, update that record during routine follow-ups, and persist critical information directly onto the NFC tag mounted on the pregnant woman’s caretaker device.

During a visit, the responder can scan the NFC tag to read the existing medical record, view key details such as pregnancy stage, risks, and allergies, and update the record with new checkup notes or changes in status. These updates are written back to the NFC tag so the information remains physically tied to the patient, even when no connectivity is available.

In addition to routine care, the application allows responders to receive distress signals broadcast from caretaker devices. When a distress signal is received, the responder can view its location on a map and act accordingly. Responders authenticate using pre-assigned accounts managed by the NGO. If access issues arise, account recovery is handled through organizational support rather than in-app account creation.

## Application Requirements and Design

The application is intentionally lightweight and is developed in Kotlin using Android Studio. It follows an offline-first design, meaning all core functionality works without an active internet connection. Data is stored locally on the device, allowing responders to continue working in the field regardless of network conditions.

When connectivity becomes available, the application synchronizes local data with a central database to support supervision, continuity across teams, and long-term record keeping. The responder does not need to manually manage synchronization; it occurs opportunistically when conditions allow.

## Data Model and Entities

The application is built around a small set of clearly defined entities that represent the operational workflow of maternal care in crisis settings.

A `PregnancyCase` represents the core patient record and includes identifiers, patient details, pregnancy stage, key risks, allergies, last checkup summary, timestamps, and a status flag indicating whether the case is active, closed, or lost to follow-up.

A `CheckupNote` captures routine follow-up information recorded by a responder during a visit. Each note is linked to a pregnancy case and includes the responder identity, checkup time, and free-text observations.

A `DistressEvent` represents an emergency alert sent from a caretaker device. It includes timestamps, location coordinates, and a status that tracks whether the event is open, acknowledged, resolved, or marked as a false alarm.

A `UserAccount` represents an NGO responder or supervisor, including role, contact information, and account status. Responders cannot create accounts themselves and must be provisioned by the NGO.

An `Assignment` links responders to pregnancy cases, defining responsibility and priority levels to support coordination in the field.

## NFC Record Structure

The NFC tag mounted on the caretaker device stores a compact but critical subset of the pregnancy case. This includes the case identifier, patient name, date of birth, pregnancy stage, allergies, key risks, last checkup summary, last checkup timestamp, and last updated timestamp. This ensures that essential information remains accessible even if the responder’s phone or backend systems are unavailable.

## Storage and Synchronization

The application uses a dual-layer storage approach. Locally, data is stored on the Android device using a persistent local database to support offline operation. Centrally, a cloud database is used to aggregate data across responders and deployments when connectivity permits. The central database has already been set up and is used only for synchronization and supervision, not for core field functionality.

The database structure organizes users, cases, checkups, distress events, and assignments in a hierarchical format that mirrors the application’s data model and supports incremental updates.


## Running and Testing

The application is developed and run using Android Studio.

## License

This project is released under the MIT License.  
You are free to use, modify, and distribute this software for academic, non-commercial, and commercial purposes, provided that the original copyright notice and license text are included in all copies or substantial portions of the software.

See the `LICENSE` file in this repository for the full license text.


For development and debugging, you can connect a physical Android device using ADB:

```bash
adb devices

# Account Information
test@yopmail.com
123456
