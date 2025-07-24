# 🛡️ ERDMT - Enhanced Remote Device Management Tool

A comprehensive Firebase-based remote device management system featuring a static web admin panel and Android WebView application for authorized device monitoring and control.

> **⚠️ IMPORTANT**: This application is for educational and authorized administrative purposes only. Use only on devices you own or have explicit permission to monitor.

## Features

### Android App Features

- **Modern WebView Integration**: Loads https://www.google.com with full JavaScript support, file uploads, and media playback
- **Comprehensive Permissions Management**: Automatic runtime permission requests for all required permissions
- **Material Design 3 UI**: Dark-themed modern interface with proper navigation and error handling
- **WebRTC Support**: Camera and microphone access for web applications
- **File Upload Support**: Complete file chooser integration for WebView
- **Geolocation Support**: GPS location access for web applications
- **Background Service**: Persistent service that starts on boot
- **Error Handling**: Comprehensive error handling with retry mechanisms
- **Responsive Design**: Optimized for all screen sizes

### Remote Command Features

- **Firebase Cloud Messaging (FCM)**: Receive and execute remote commands securely.
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

### Web Admin Panel Features

- **Modern Responsive Design**: Clean, professional interface with dark/light theme support
- **Real-time Device Management**: View connected devices, their status, and permissions
- **Remote Command Center**: Send Firebase push messages with various commands
- **Activity Monitoring**: Comprehensive logging and activity tracking
- **Dashboard Analytics**: Statistics and charts for device activity
- **Mobile Responsive**: Works perfectly on desktop, tablet, and mobile devices
- **Firebase Integration**: Direct integration with Firebase for message sending
- **Settings Management**: Configure notifications, logging, and system settings
- **Message History**: Track all sent commands and their status
- **Device Details**: Detailed information about each connected device

## Getting Started

### 1. Clone the Repository

```sh
git clone https://github.com/mystery2323/erdmt-admin-app.git
cd erdmt-admin-app
```

### 2. Setup Firebase


- Create a Firebase project in the [Firebase Console](https://console.firebase.google.com/).
- Register your Android app and download `google-services.json`, placing it in the `app/` directory.
- Enable Firebase Cloud Messaging in your project.
- For the admin panel, download the Firebase Admin SDK service account key.

### 3. Build the Project

#### Android App
- Open the project in Android Studio.
- Sync Gradle and resolve dependencies.
- Build and install on a real device (emulator support is limited for hardware features).

#### Web Admin Panel
```sh
cd admin-panel
npm install

# For development
npm run dev

# For production
npm run build
npm start
```

### 4. Setup Admin Panel

1. Copy `.env.example` to `.env` and configure your Firebase credentials:
```sh
cd admin-panel
cp .env.example .env
```

2. Edit `.env` with your Firebase project details:
```env
FIREBASE_PROJECT_ID=your-project-id
FIREBASE_PRIVATE_KEY="your-private-key"
FIREBASE_CLIENT_EMAIL=your-client-email
```

3. Start the admin panel:
```sh
npm run dev  # Development mode
# or
npm start    # Production mode
```

4. Access the admin panel at `http://localhost:3000`

### 5. Configure Permissions

The app automatically requests all necessary dangerous permissions at runtime:
- Camera access for photo capture and WebRTC
- Microphone access for audio recording and WebRTC
- Location access for geolocation services
- Storage access for file operations
- SMS, Call Log, and Contacts access for data retrieval

**Important**: Grant all permissions for full functionality. The app will show permission status and allow re-requesting if needed.

### 6. Running the App

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

## Admin Panel Usage

### Dashboard
- View connected devices count, messages sent, active permissions, and alerts
- Monitor recent activity and system status
- Real-time updates of device connections and activities

### Device Management
- View all connected devices with their status, model, and last seen time
- Check permission status for each device
- Remove devices from the system

### Push Messaging
- Send commands to specific devices or all devices
- Support for all available commands with parameter input
- View message history with timestamps and status

### Activity Logs
- Comprehensive logging of all system activities
- Filter and search through logs
- Export logs for analysis

### Settings
- Configure Firebase connection settings
- Enable/disable notifications and logging
- Theme switching (dark/light mode)

## Security & Privacy

> **Warning:** This app accesses sensitive user data and device capabilities.  
> Use only on devices you own or have explicit permission to monitor.  
> Do not distribute or use in violation of privacy laws or Google Play policies.

## Architecture

### Android App Structure
```
app/src/main/java/com/yourdomain/erdmt/
├── MainActivity.kt              # Main WebView activity with permissions
├── BackgroundService.kt         # Persistent background service
├── RemoteCommandService.kt      # Firebase messaging service
├── CameraCaptureActivity.kt     # Camera functionality
├── BootReceiver.kt             # Auto-start on boot
└── utils/                      # Utility classes for various features
```

### Admin Panel Structure
```
admin-panel/
├── index.html                  # Main admin interface
├── style.css                   # Modern responsive styling
├── script.js                   # Frontend functionality
├── server.js                   # Express.js backend
└── package.json               # Dependencies and scripts
```

## Contributing

Contributions are welcome! Please open issues or pull requests for enhancements and bug fixes.

## License

This project is for educational and authorized administrative purposes only.

## Troubleshooting

### Android App Issues
- **WebView not loading**: Check internet connection and ensure `android:usesCleartextTraffic="true"` in manifest
- **Permissions denied**: Manually grant permissions in Android Settings > Apps > ERDMT > Permissions
- **Firebase not working**: Verify `google-services.json` is in the correct location and Firebase project is configured

### Admin Panel Issues
- **Cannot connect to Firebase**: Check `.env` file configuration and Firebase service account key
- **Commands not sending**: Verify Firebase Cloud Messaging is enabled and device tokens are valid
- **UI not responsive**: Clear browser cache and ensure JavaScript is enabled

For more detailed troubleshooting, check the application logs in both the Android app and admin panel console.