# 🛡️ ERDMT - Enhanced Remote Device Management Tool

A comprehensive Firebase-based remote device management system featuring a static web admin panel and Android WebView application for authorized device monitoring and control.

## 🚀 Features

### 📱 Android App
- **WebView Integration**: Loads web content with full JavaScript, media, and file upload support
- **Firebase Integration**: Real-time device registration, command listening, and response handling
- **Comprehensive Permissions**: Camera, microphone, location, SMS, call logs, contacts, and storage access
- **Remote Commands**: Execute various commands remotely through Firebase
- **File Upload**: Automatic upload of captured media to Firebase Storage
- **Background Service**: Persistent service with auto-start on boot
- **Real-time Updates**: Device status, battery level, and location tracking

### 🌐 Static Admin Panel
- **Firebase Authentication**: Secure email/password login
- **Real-time Dashboard**: Live device monitoring and statistics
- **Command Center**: Send commands to individual devices or all devices
- **Device Management**: View device details, status, and permissions
- **Media Gallery**: View uploaded photos and audio files
- **System Logs**: Comprehensive logging and activity tracking
- **Responsive Design**: Works on desktop, tablet, and mobile
- **Dark/Light Theme**: Theme switching with local storage

## 📋 Prerequisites

- Firebase project with Authentication, Realtime Database, and Storage enabled
- Android Studio for building the Android app
- Web server for hosting the static admin panel (or use Replit)

## 🔧 Setup Instructions

### 1. Firebase Configuration

1. Create a Firebase project at [Firebase Console](https://console.firebase.google.com/)
2. Enable the following services:
   - **Authentication** (Email/Password provider)
   - **Realtime Database** (Start in test mode)
   - **Storage** (Start in test mode)
   - **Cloud Messaging** (for push notifications)

3. Download `google-services.json` and place it in `app/` directory

4. Create an admin user in Firebase Authentication:
   ```bash
   # Go to Firebase Console > Authentication > Users > Add user
   # Email: admin@yourproject.com
   # Password: your-secure-password
   ```

5. Set up Firebase Security Rules:

   **Realtime Database Rules:**
   ```json
   {
     "rules": {
       "devices": {
         ".read": "auth != null",
         ".write": "auth != null"
       },
       "commands": {
         ".read": "auth != null",
         ".write": "auth != null"
       },
       "responses": {
         ".read": "auth != null",
         ".write": true
       },
       "logs": {
         ".read": "auth != null",
         ".write": true
       }
     }
   }
   ```

   **Storage Rules:**
   ```javascript
   rules_version = '2';
   service firebase.storage {
     match /b/{bucket}/o {
       match /{allPaths=**} {
         allow read, write: if request.auth != null;
       }
     }
   }
   ```

### 2. Android App Setup

1. Open the project in Android Studio
2. Sync Gradle files
3. Update the package name in:
   - `app/build.gradle.kts`
   - `AndroidManifest.xml`
   - All Kotlin files

4. Build and install on a real device (emulator has limited hardware access)

### 3. Admin Panel Setup

#### Option A: Deploy on Replit
1. Upload the `admin-panel/` folder to Replit
2. Configure the run command to serve static files:
   ```bash
   python -m http.server 5000 --directory admin-panel
   ```
3. Access via the Replit URL

#### Option B: Local/Custom Server
1. Host the `admin-panel/` folder on any web server
2. Ensure HTTPS is enabled for Firebase Authentication
3. Update CORS settings if needed

### 4. Test the System

1. **Admin Panel Login**: 
   - Go to `login.html`
   - Use the admin credentials created in Firebase

2. **Device Registration**:
   - Install and run the Android app
   - Grant all permissions when prompted
   - Device should appear in admin panel dashboard

3. **Send Commands**:
   - Use the Commands page to send test commands
   - Check responses in the dashboard and logs

## 📱 Supported Commands

| Command | Description | Parameters |
|---------|-------------|------------|
| `get_info` | Get device information | None |
| `mic_record` | Record 10 seconds of audio | None |
| `camera_capture` | Capture photo | None |
| `read_sms` | Get recent SMS messages | None |
| `read_call_logs` | Get recent call logs | None |
| `read_contacts` | Get all contacts | None |
| `get_location` | Get current GPS location | None |
| `list_installed_apps` | List installed apps | None |
| `shell_exec` | Execute shell command | Command string |
| `toggle_icon` | Show/hide app icon | `show` or `hide` |

## 🏗️ Project Structure

```
erdmt-admin-app/
├── admin-panel/                 # Static web admin panel
│   ├── firebase-config.js       # Firebase configuration
│   ├── login.html              # Authentication page
│   ├── index.html              # Main dashboard
│   ├── script.js               # JavaScript functionality
│   ├── style.css               # Responsive CSS styling
│   └── README.md               # Documentation
├── app/                        # Android application
│   ├── src/main/
│   │   ├── java/com/yourdomain/erdmt/
│   │   │   ├── MainActivity.kt
│   │   │   ├── RemoteCommandService.kt
│   │   │   ├── AudioRecorder.kt
│   │   │   ├── SmsReader.kt
│   │   │   ├── CallLogReader.kt
│   │   │   └── [other utilities]
│   │   ├── res/                # Android resources
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── google-services.json    # Firebase config (add this)
└── README.md                   # Main documentation
```

## 🔒 Security & Privacy

> **⚠️ IMPORTANT DISCLAIMER**: This application accesses sensitive user data and device capabilities. Use only on devices you own or have explicit permission to monitor. Do not distribute or use in violation of privacy laws, terms of service, or Google Play policies.

### Security Features
- Firebase Authentication for admin access
- Secure HTTPS communication
- Device ID-based command targeting
- Encrypted file uploads to Firebase Storage
- Permission-based feature access

### Privacy Considerations
- All data is stored in your Firebase project
- No third-party data sharing
- Local device permissions required
- User consent for sensitive operations

## 🚀 Deployment

### Android App
1. Build APK: `./gradlew assembleRelease`
2. Sign with your keystore
3. Install on target devices
4. Grant all required permissions

### Admin Panel
1. Upload to web hosting service
2. Ensure HTTPS for Firebase Auth
3. Configure domain in Firebase Console
4. Test all functionality

### Production Checklist
- [ ] Update Firebase security rules for production
- [ ] Set up proper authentication
- [ ] Configure HTTPS certificates
- [ ] Test all commands and responses
- [ ] Verify file upload/download
- [ ] Check responsive design
- [ ] Test on multiple devices

## 🛠️ Development

### Adding New Commands
1. **Android**: Add command handler in `MainActivity.kt`
2. **Admin Panel**: Add command option in `index.html` and `script.js`
3. **Test**: Verify command execution and response

### Customizing UI
- Modify `style.css` for appearance changes
- Update `script.js` for functionality changes
- Ensure responsive design compatibility

## 📚 API Reference

### Firebase Database Structure
```json
{
  "devices": {
    "device_id": {
      "id": "device_id",
      "model": "Device Model",
      "online": true,
      "lastSeen": 1234567890,
      "battery": 85,
      "location": {"lat": 0.0, "lng": 0.0}
    }
  },
  "commands": {
    "device_id": {
      "command_id": {
        "type": "command_type",
        "params": "parameters",
        "timestamp": 1234567890
      }
    }
  },
  "responses": {
    "response_id": {
      "deviceId": "device_id",
      "message": "response_message",
      "success": true,
      "timestamp": 1234567890
    }
  },
  "logs": {
    "log_id": {
      "deviceId": "device_id",
      "type": "log_type",
      "message": "log_message",
      "level": "info",
      "timestamp": 1234567890
    }
  }
}
```

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature-name`
3. Commit changes: `git commit -am 'Add feature'`
4. Push to branch: `git push origin feature-name`
5. Submit a Pull Request

### Contribution Guidelines
- Follow existing code style
- Add comments for complex logic
- Test all changes thoroughly
- Update documentation as needed
- Ensure responsive design

## 📄 License

This project is for educational and authorized administrative purposes only. Users are responsible for compliance with local laws and regulations regarding device monitoring and data privacy.

## 🆘 Troubleshooting

### Common Issues

**Firebase Connection Failed**
- Check `firebase-config.js` settings
- Verify project ID and API keys
- Ensure correct Firebase services are enabled

**Commands Not Working**
- Verify device permissions are granted
- Check Firebase Realtime Database rules
- Confirm device is online in dashboard

**File Upload Errors**
- Check Firebase Storage rules
- Verify storage bucket configuration
- Ensure device has internet connection

**Admin Panel Login Issues**
- Verify Firebase Authentication is enabled
- Check email/password provider is configured
- Ensure user exists in Firebase Auth

### Support
For issues and questions:
1. Check the troubleshooting section
2. Review Firebase Console for errors
3. Check browser/Android logs
4. Open an issue on GitHub

## 🏷️ Version History

- **v1.0.0** - Initial release with basic Firebase integration
- **v1.1.0** - Added static admin panel and enhanced commands
- **v1.2.0** - Improved UI/UX and added media management
- **v1.3.0** - Enhanced security and permission handling

---

**Made with ❤️ for authorized device management**