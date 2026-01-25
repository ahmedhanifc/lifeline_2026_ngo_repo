# Project Background

The project is set in a disaster ridden zones with limited connectivity. This application is
being crafted for an NGO responder that get dispatched to the disaster ridden zones to help the
pregnant women in the area. These people could be doctors or first aid helpers.

# Project Goal

The goal is to build An android application on the dispatched NGO responder's phone that allows him to:
1) Create a record for a pregnant woman case.
2) Update an existing record on routine checkup modifying just the note regarding additional checkup.
2a) This will get updated to the NFC chip on the pregnant's woman device chip.
read the NFC tag on the device and view the record, update information and rewrite it to the tag. The app can show the location of distress signals.
3) The responder can recieve distress signals
4) The responder can view view the location of the distress signal on the map.
5) The responder can login to an existing assigned account.
6) If the user doesn't have access to the account anymore, he can contact support.

# Application Requirements
The application is meant to be a lightweight android application.
The application will be coded on Kotlin
The device will have offline first local store on the android device.
When the connection is available, the data will be synchronized to the database server.

# Database entities
PregnancyCase

CheckupNote

DistressEvent

UserAccount

Assignment (which responder is responsible for which case)

# NFC Tag
caseId
patient basic information
pregnancy stage
key risks/allergies
last checkup summary
last updated at

# Central Database
Firebase will be used. This database has already been setup.

## Online Database AND Offline Database
PregnancyCase
- caseId
- patientFullName
- dateOfBirth
- pregnancyStage
- allergies
- lastCheckupAt
- lastCheckupSummary
- updatedAt
- status (ACTIVE, CLOSED, LOST_FOLLOWUP)

CheckupNote 
- noteId
- caseId
- responderId
- checkupAt
- noteText
- createdAt
- updatedAt

DistressEvent
- eventId
- caseId
- occuredAt
- recievedAt
- latitude
- longitude
- status (OPEN, ACKED, RESOLVED, FALSE_ALARM)

UserAccount
- userId
- fullName
- role (RESPONDER, SUPERVISOR)
- phone
- email
- isActive
- lastLoginAt
- createdAt
- updatedAt

Assignment
- assignmentId
- caseId
- responderId
- assignedAt
- isActive
- priority (LOW, MEDIUM, HIGH)
- createdAt
- updatedAt

## NFC 

caseId
patientFullName
dateOfBirth
pregnancyStage
allergies
keyRisks
lastCheckupSummary
lastCheckupAt
lastUpdatedAt
