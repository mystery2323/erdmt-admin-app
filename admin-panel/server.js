const express = require('express');
const cors = require('cors');
const path = require('path');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 3000;

// Middleware
app.use(cors());
app.use(express.json());
app.use(express.static('.'));

// Serve the admin panel
app.get('/', (req, res) => {
    res.sendFile(path.join(__dirname, 'index.html'));
});

// API Routes
app.get('/api/devices', (req, res) => {
    // In a real implementation, this would fetch from a database
    res.json({
        success: true,
        devices: [
            {
                id: 'device_001',
                model: 'Samsung Galaxy S21',
                online: true,
                lastSeen: new Date(),
                permissions: ['CAMERA', 'MICROPHONE', 'LOCATION', 'STORAGE', 'SMS']
            }
        ]
    });
});

app.post('/api/send-message', (req, res) => {
    const { target, command, params } = req.body;
    
    // In a real implementation, this would send via Firebase Admin SDK
    console.log('Sending message:', { target, command, params });
    
    // Simulate Firebase message sending
    setTimeout(() => {
        res.json({
            success: true,
            messageId: Date.now().toString(),
            message: 'Command sent successfully'
        });
    }, 1000);
});

app.get('/api/logs', (req, res) => {
    // In a real implementation, this would fetch from a database
    res.json({
        success: true,
        logs: [
            {
                id: '1',
                timestamp: new Date(),
                level: 'info',
                message: 'WebView service started'
            },
            {
                id: '2',
                timestamp: new Date(Date.now() - 60000),
                level: 'success',
                message: 'Device permissions granted'
            }
        ]
    });
});

// Error handling middleware
app.use((err, req, res, next) => {
    console.error(err.stack);
    res.status(500).json({
        success: false,
        message: 'Internal server error'
    });
});

// Start server
app.listen(PORT, () => {
    console.log(`ERDMT Admin Panel server running on port ${PORT}`);
    console.log(`Open http://localhost:${PORT} to access the admin panel`);
});