// Global state
let currentTheme = localStorage.getItem('theme') || 'light';
let devices = [];
let messageHistory = [];
let activityLogs = [];

// Initialize app
document.addEventListener('DOMContentLoaded', function() {
    initializeTheme();
    initializeNavigation();
    initializeEventListeners();
    loadDashboardData();
    startDataRefresh();
});

// Theme Management
function initializeTheme() {
    document.documentElement.setAttribute('data-theme', currentTheme);
    updateThemeIcon();
}

function toggleTheme() {
    currentTheme = currentTheme === 'light' ? 'dark' : 'light';
    document.documentElement.setAttribute('data-theme', currentTheme);
    localStorage.setItem('theme', currentTheme);
    updateThemeIcon();
    showToast('Theme changed to ' + currentTheme + ' mode', 'info');
}

function updateThemeIcon() {
    const themeToggle = document.getElementById('themeToggle');
    const icon = themeToggle.querySelector('i');
    icon.className = currentTheme === 'light' ? 'fas fa-moon' : 'fas fa-sun';
}

// Navigation
function initializeNavigation() {
    const menuItems = document.querySelectorAll('.menu-item');
    const pages = document.querySelectorAll('.page');
    const pageTitle = document.getElementById('pageTitle');
    
    menuItems.forEach(item => {
        item.addEventListener('click', () => {
            const targetPage = item.getAttribute('data-page');
            
            // Update active menu item
            menuItems.forEach(mi => mi.classList.remove('active'));
            item.classList.add('active');
            
            // Show target page
            pages.forEach(page => page.classList.remove('active'));
            document.getElementById(targetPage).classList.add('active');
            
            // Update page title
            pageTitle.textContent = item.querySelector('span').textContent;
            
            // Load page-specific data
            loadPageData(targetPage);
            
            // Close mobile menu
            closeMobileMenu();
        });
    });
}

function loadPageData(page) {
    switch(page) {
        case 'dashboard':
            loadDashboardData();
            break;
        case 'devices':
            loadDevicesData();
            break;
        case 'messaging':
            loadMessagingData();
            break;
        case 'logs':
            loadLogsData();
            break;
        case 'settings':
            loadSettingsData();
            break;
    }
}

// Event Listeners
function initializeEventListeners() {
    // Theme toggle
    document.getElementById('themeToggle').addEventListener('click', toggleTheme);
    
    // Mobile menu
    document.getElementById('mobileMenuBtn').addEventListener('click', toggleMobileMenu);
    document.getElementById('sidebarToggle').addEventListener('click', toggleMobileMenu);
    
    // Message form
    document.getElementById('messageForm').addEventListener('submit', handleMessageSubmit);
    document.getElementById('commandType').addEventListener('change', handleCommandTypeChange);
    
    // Refresh buttons
    document.getElementById('refreshDevices')?.addEventListener('click', loadDevicesData);
    document.getElementById('refreshLogs')?.addEventListener('click', loadLogsData);
    
    // Clear logs
    document.getElementById('clearLogs')?.addEventListener('click', clearLogs);
    
    // Settings checkboxes
    document.getElementById('enableNotifications')?.addEventListener('change', updateSettings);
    document.getElementById('enableLogs')?.addEventListener('change', updateSettings);
}

// Mobile Menu
function toggleMobileMenu() {
    const sidebar = document.getElementById('sidebar');
    sidebar.classList.toggle('open');
}

function closeMobileMenu() {
    const sidebar = document.getElementById('sidebar');
    sidebar.classList.remove('open');
}

// Dashboard Data
function loadDashboardData() {
    // Simulate loading dashboard stats
    updateDashboardStats();
    loadRecentActivity();
}

function updateDashboardStats() {
    document.getElementById('connectedDevices').textContent = devices.length;
    document.getElementById('messagesSent').textContent = messageHistory.length;
    document.getElementById('activePermissions').textContent = calculateActivePermissions();
    document.getElementById('alertsCount').textContent = '0';
}

function calculateActivePermissions() {
    return devices.reduce((total, device) => {
        return total + (device.permissions ? device.permissions.length : 0);
    }, 0);
}

function loadRecentActivity() {
    const activityContainer = document.getElementById('recentActivity');
    const recentActivities = activityLogs.slice(-5).reverse();
    
    if (recentActivities.length === 0) {
        activityContainer.innerHTML = `
            <div class="activity-item">
                <div class="activity-icon">
                    <i class="fas fa-info-circle"></i>
                </div>
                <div class="activity-content">
                    <p>No recent activity</p>
                    <span class="activity-time">-</span>
                </div>
            </div>
        `;
        return;
    }
    
    activityContainer.innerHTML = recentActivities.map(activity => `
        <div class="activity-item">
            <div class="activity-icon">
                <i class="${getActivityIcon(activity.type)}"></i>
            </div>
            <div class="activity-content">
                <p>${activity.message}</p>
                <span class="activity-time">${formatTimeAgo(activity.timestamp)}</span>
            </div>
        </div>
    `).join('');
}

// Devices Data
function loadDevicesData() {
    const devicesTable = document.getElementById('devicesTable');
    
    if (devices.length === 0) {
        devicesTable.innerHTML = `
            <tr>
                <td colspan="6" class="text-center">No devices connected</td>
            </tr>
        `;
        return;
    }
    
    devicesTable.innerHTML = devices.map(device => `
        <tr>
            <td>${device.id}</td>
            <td>${device.model}</td>
            <td><span class="status-badge ${device.online ? 'status-success' : 'status-error'}">${device.online ? 'Online' : 'Offline'}</span></td>
            <td>${formatTimeAgo(device.lastSeen)}</td>
            <td>${device.permissions ? device.permissions.length : 0}/9</td>
            <td>
                <button class="btn-secondary btn-sm" onclick="viewDevice('${device.id}')">View</button>
                <button class="btn-danger btn-sm" onclick="removeDevice('${device.id}')">Remove</button>
            </td>
        </tr>
    `).join('');
}

// Messaging
function loadMessagingData() {
    updateTargetDeviceSelect();
    loadMessageHistory();
}

function updateTargetDeviceSelect() {
    const select = document.getElementById('targetDevice');
    select.innerHTML = '<option value="all">All Devices</option>';
    
    devices.forEach(device => {
        select.innerHTML += `<option value="${device.id}">${device.model} (${device.id})</option>`;
    });
}

function handleCommandTypeChange() {
    const commandType = document.getElementById('commandType').value;
    const paramsGroup = document.getElementById('paramsGroup');
    
    // Show params field for commands that need parameters
    if (commandType === 'shell_exec' || commandType === 'toggle_icon') {
        paramsGroup.style.display = 'block';
        const paramsInput = document.getElementById('commandParams');
        
        if (commandType === 'shell_exec') {
            paramsInput.placeholder = 'Enter shell command (e.g., ls -la)';
        } else if (commandType === 'toggle_icon') {
            paramsInput.placeholder = 'Enter "show" or "hide"';
        }
    } else {
        paramsGroup.style.display = 'none';
    }
}

function handleMessageSubmit(e) {
    e.preventDefault();
    
    const targetDevice = document.getElementById('targetDevice').value;
    const commandType = document.getElementById('commandType').value;
    const commandParams = document.getElementById('commandParams').value;
    
    const message = {
        id: Date.now().toString(),
        target: targetDevice,
        command: commandType,
        params: commandParams || null,
        timestamp: new Date(),
        status: 'sent'
    };
    
    // Add to message history
    messageHistory.unshift(message);
    
    // Add to activity log
    addActivityLog('message', `Command "${commandType}" sent to ${targetDevice === 'all' ? 'all devices' : targetDevice}`);
    
    // Send message via Firebase (simulated)
    sendFirebaseMessage(message);
    
    // Update UI
    loadMessageHistory();
    showToast('Command sent successfully!', 'success');
    
    // Reset form
    document.getElementById('messageForm').reset();
    document.getElementById('paramsGroup').style.display = 'none';
}

function loadMessageHistory() {
    const messageHistoryContainer = document.getElementById('messageHistory');
    
    if (messageHistory.length === 0) {
        messageHistoryContainer.innerHTML = `
            <div class="message-item">
                <div class="message-content">
                    <p>No messages sent yet</p>
                </div>
            </div>
        `;
        return;
    }
    
    messageHistoryContainer.innerHTML = messageHistory.slice(0, 10).map(message => `
        <div class="message-item">
            <div class="message-content">
                <p><strong>Command:</strong> ${message.command}</p>
                <p><strong>Target:</strong> ${message.target === 'all' ? 'All Devices' : message.target}</p>
                ${message.params ? `<p><strong>Params:</strong> ${message.params}</p>` : ''}
                <span class="message-time">${formatTimeAgo(message.timestamp)}</span>
            </div>
            <span class="message-status status-${message.status}">${message.status}</span>
        </div>
    `).join('');
}

// Logs
function loadLogsData() {
    const logsContainer = document.getElementById('logsContainer');
    
    if (activityLogs.length === 0) {
        logsContainer.innerHTML = `
            <div class="log-entry">
                <span class="log-time">${new Date().toLocaleString()}</span>
                <span class="log-level log-info">INFO</span>
                <span class="log-message">No activity logs available</span>
            </div>
        `;
        return;
    }
    
    logsContainer.innerHTML = activityLogs.slice().reverse().map(log => `
        <div class="log-entry">
            <span class="log-time">${log.timestamp.toLocaleString()}</span>
            <span class="log-level log-${log.level}">${log.level.toUpperCase()}</span>
            <span class="log-message">${log.message}</span>
        </div>
    `).join('');
}

function clearLogs() {
    if (confirm('Are you sure you want to clear all logs?')) {
        activityLogs = [];
        loadLogsData();
        showToast('Logs cleared successfully!', 'info');
    }
}

// Settings
function loadSettingsData() {
    // Settings are already loaded in HTML
    // This function can be used to load dynamic settings
}

function updateSettings() {
    const enableNotifications = document.getElementById('enableNotifications').checked;
    const enableLogs = document.getElementById('enableLogs').checked;
    
    // Save settings to localStorage
    localStorage.setItem('enableNotifications', enableNotifications);
    localStorage.setItem('enableLogs', enableLogs);
    
    showToast('Settings updated successfully!', 'success');
}

// Firebase Integration (Simulated)
function sendFirebaseMessage(message) {
    // In a real implementation, this would send the message via Firebase Admin SDK
    console.log('Sending Firebase message:', message);
    
    // Simulate API call
    setTimeout(() => {
        addActivityLog('firebase', `Firebase message sent: ${message.command}`);
    }, 1000);
}

// Utility Functions
function addActivityLog(type, message, level = 'info') {
    const log = {
        id: Date.now().toString(),
        type: type,
        message: message,
        level: level,
        timestamp: new Date()
    };
    
    activityLogs.push(log);
    
    // Keep only last 1000 logs
    if (activityLogs.length > 1000) {
        activityLogs = activityLogs.slice(-1000);
    }
}

function getActivityIcon(type) {
    const icons = {
        device: 'fas fa-mobile-alt',
        message: 'fas fa-paper-plane',
        firebase: 'fas fa-cloud',
        permission: 'fas fa-shield-alt',
        error: 'fas fa-exclamation-triangle',
        default: 'fas fa-info-circle'
    };
    
    return icons[type] || icons.default;
}

function formatTimeAgo(timestamp) {
    const now = new Date();
    const time = new Date(timestamp);
    const diffInSeconds = Math.floor((now - time) / 1000);
    
    if (diffInSeconds < 60) {
        return `${diffInSeconds} seconds ago`;
    } else if (diffInSeconds < 3600) {
        const minutes = Math.floor(diffInSeconds / 60);
        return `${minutes} minute${minutes > 1 ? 's' : ''} ago`;
    } else if (diffInSeconds < 86400) {
        const hours = Math.floor(diffInSeconds / 3600);
        return `${hours} hour${hours > 1 ? 's' : ''} ago`;
    } else {
        const days = Math.floor(diffInSeconds / 86400);
        return `${days} day${days > 1 ? 's' : ''} ago`;
    }
}

function showToast(message, type = 'info') {
    const toastContainer = document.getElementById('toastContainer');
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.textContent = message;
    
    toastContainer.appendChild(toast);
    
    // Show toast
    setTimeout(() => {
        toast.classList.add('show');
    }, 100);
    
    // Hide and remove toast
    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => {
            toastContainer.removeChild(toast);
        }, 300);
    }, 3000);
}

// Device Management
function viewDevice(deviceId) {
    const device = devices.find(d => d.id === deviceId);
    if (device) {
        showToast(`Viewing device: ${device.model}`, 'info');
        // In a real app, this would open a device details modal
    }
}

function removeDevice(deviceId) {
    if (confirm('Are you sure you want to remove this device?')) {
        devices = devices.filter(d => d.id !== deviceId);
        loadDevicesData();
        updateDashboardStats();
        showToast('Device removed successfully!', 'success');
        addActivityLog('device', `Device ${deviceId} removed`);
    }
}

// Data Refresh
function startDataRefresh() {
    // Simulate periodic data updates
    setInterval(() => {
        // Simulate new device connections
        if (Math.random() < 0.1 && devices.length < 5) {
            addSimulatedDevice();
        }
        
        // Update dashboard if on dashboard page
        const activePage = document.querySelector('.page.active');
        if (activePage && activePage.id === 'dashboard') {
            updateDashboardStats();
        }
    }, 10000); // Every 10 seconds
}

function addSimulatedDevice() {
    const deviceModels = ['Samsung Galaxy S21', 'Google Pixel 6', 'OnePlus 9', 'Xiaomi Mi 11', 'iPhone 13'];
    const randomModel = deviceModels[Math.floor(Math.random() * deviceModels.length)];
    
    const device = {
        id: `device_${Date.now()}`,
        model: randomModel,
        online: true,
        lastSeen: new Date(),
        permissions: ['CAMERA', 'MICROPHONE', 'LOCATION', 'STORAGE', 'SMS']
    };
    
    devices.push(device);
    addActivityLog('device', `New device connected: ${device.model}`);
    
    if (document.querySelector('.devices-page.active')) {
        loadDevicesData();
    }
}

// Initialize with some sample data
function initializeSampleData() {
    // Add sample activity logs
    addActivityLog('system', 'Admin panel started', 'success');
    addActivityLog('firebase', 'Firebase connection established', 'success');
    addActivityLog('system', 'WebView service initialized', 'info');
    
    // Add sample device
    if (devices.length === 0) {
        devices.push({
            id: 'device_sample_001',
            model: 'Samsung Galaxy S21',
            online: true,
            lastSeen: new Date(Date.now() - 300000), // 5 minutes ago
            permissions: ['CAMERA', 'MICROPHONE', 'LOCATION', 'STORAGE', 'SMS', 'CONTACTS', 'CALL_LOG']
        });
        
        addActivityLog('device', 'Sample device connected: Samsung Galaxy S21');
    }
}

// Initialize sample data when page loads
document.addEventListener('DOMContentLoaded', function() {
    setTimeout(initializeSampleData, 1000);
});