## Phase 0: Proof of Concept (The "Distress Signal")

### 1. Goal
Detect a specific distress string sent from a Caretaker node and display an alert with the embedded location.

**Target Message Format:**
`SOS - Caretaker device | Location: [LAT], [LONG]`

### 2. Technical Specifications
To connect to the Meshtastic node via BLE, we will use:
- **Service UUID:** `6ba1b210-2884-a114-11e2-b883083e5767`
- **Notify (From-Node) UUID:** `8ba1b210-2884-a114-11e2-b883083e5767`
- **Port:** `TEXT_MESSAGE_APP` (Port 1)

### 3. PoC Workflow
1. **The Signal:** Device A broadcasts the text string: `"SOS - Caretaker device | Location: 25.3533, 51.4865"`.
2. **The Receipt:** Our Android app receives the `MeshPacket` via BLE Notify.
3. **The Extraction:**
   - Decode using Protobuf.
   - Parse the string using Regex to find `Location: (.*), (.*)`.
4. **The Alert:**
   - Trigger a high-priority notification.
   - Show a dialog: "Emergency! SOS Received at [LAT, LONG]".
   - (Next step would be to place the Red Marker here).

### 4. Implementation Requirements
- **Protobuf:** `mesh.proto` and `portnums.proto`.
- **Regex Helper:** A robust function to pull lat/long from the string.
- **BLE Service:** A service to maintain the background connection to the NGO node.
