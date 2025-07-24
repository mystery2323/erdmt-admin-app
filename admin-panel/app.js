
// Global variables
let devices = {};
let commandCount = 0;
let responseCount = 0;
let mediaCount = 0;
let currentPage = 'dashboard';

// Initialize app
document.addEventListener('DOMContentLoaded', function() {
    initializeAuth();
    initializeEventListeners();
});

// Authentication
function initializeAuth() {
    auth.onAuthStateChanged(user => {
        if (user) {
            currentUser = user;
            showMainApp();
            startListeners();
        } else {
            showLoginPage();
        }
    });
}

function showLoginPage() {
    document.getElementById('loginPage').classList.remove('d-none');
    document.getElementById('mainApp').classList.add('d-none');
}

function showMainApp() {
    document.getElementById('loginPage').classList.add('d-none');
    document.getElementById('mainApp').classList.remove('d-none');
    document.getElementById('userEmail').textContent = currentUser.email;
    showPage('dashboard');
}

// Event Listeners
function initializeEventListeners() {
    // Login form
    document.getElementById('loginForm').addEventListener('submit', handleLogin);
    
    // Command form
    document.getElementById('commandForm').addEventListener('submit', handleCommandSubmit);
    
    // Command type change
    document.getElementById('commandType').addEventListener('change', handleCommandTypeChange);
}

async function handleLogin(e) {
    e.preventDefault();
    
    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;
    const loginBtn = document.getElementById('loginBtn');
    const loginError = document.getElementById('loginError');
    
    // Show loading
    loginBtn.disabled = true;
    loginBtn.querySelector('.spinner-border').classList.remove('d-none');
    loginError.classList.add('d-none');
    
    try {
        await auth.signInWithEmailAndPassword(email, password);
        showToast('Successfully logged in!', 'success');
    } catch (error) {
        loginError.textContent = error.message;
        loginError.classList.remove('d-none');
    } finally {
        loginBtn.disabled = false;
        loginBtn.querySelector('.spinner-border').classList.add('d-none');
    }
}

function logout() {
    auth.signOut();
    showToast('Logged out successfully', 'info');
}

// Page Navigation
function showPage(pageId) {
    // Hide all pages
    document.querySelectorAll('.page-content').forEach(page => {
        page.classList.add('d-none');
    });
    
    // Remove active from nav links
    document.querySelectorAll('.nav-link').forEach(link => {
        link.classList.remove('active');
    });
    
    // Show target page
    document.getElementById(pageId + 'Page').classList.remove('d-none');
    
    // Add active to nav link
    event.target.classList.add('active');
    
    currentPage = pageId;
    
    // Load page data
    switch(pageId) {
        case 'dashboard':
            loadDashboard();
            break;
        case 'logs':
            loadLogs();
            break;
        case 'media':
            loadMedia();
            break;
    }
}

// Firebase Listeners
function startListeners() {
    // Listen to devices
    devicesRef.on('value', (snapshot) => {
        devices = snapshot.val() || {};
        updateDevicesList();
        updateStats();
    });
    
    // Listen to logs
    logsRef.limitToLast(100).on('value', (snapshot) => {
        if (currentPage === 'logs') {
            displayLogs(snapshot.val() || {});
        }
    });
    
    // Listen to responses for each device
    Object.keys(devices).forEach(deviceId => {
        database.ref(`responses/${deviceId}`).limitToLast(10).on('value', (snapshot) => {
            displayRecentResponses(deviceId, snapshot.val() || {});
        });
    });
}

// Dashboard Functions
function loadDashboard() {
    updateDevicesList();
    updateCommandSelect();
    updateStats();
    loadRecentResponses();
}

function updateStats() {
    const deviceCount = Object.keys(devices).length;
    document.getElementById('deviceCount').textContent = deviceCount;
    document.getElementById('commandCount').textContent = commandCount;
    document.getElementById('responseCount').textContent = responseCount;
    document.getElementById('mediaCount').textContent = mediaCount;
}

function updateDevicesList() {
    const tbody = document.getElementById('devicesTable');
    
    if (Object.keys(devices).length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted">No devices connected</td></tr>';
        return;
    }
    
    tbody.innerHTML = Object.entries(devices).map(([deviceId, device]) => `
        <tr>
            <td>
                <code>${deviceId}</code>
                <span class="badge ${device.online ? 'status-online' : 'status-offline'} ms-2">
                    ${device.online ? 'Online' : 'Offline'}
                </span>
            </td>
            <td>${device.model || 'Unknown'}</td>
            <td>
                ${device.battery ? `
                    <div class="progress" style="width: 60px; height: 8px;">
                        <div class="progress-bar ${getBatteryColor(device.battery)}" 
                             style="width: ${device.battery}%"></div>
                    </div>
                    <small class="text-muted">${device.battery}%</small>
                ` : 'N/A'}
            </td>
            <td>
                ${device.location ? `
                    <small>
                        <i class="fas fa-map-marker-alt"></i>
                        ${device.location.lat?.toFixed(4)}, ${device.location.lng?.toFixed(4)}
                    </small>
                ` : 'N/A'}
            </td>
            <td>
                <small class="text-muted">${formatTimestamp(device.lastSeen)}</small>
            </td>
            <td>
                <button class="btn btn-sm btn-primary me-1" onclick="viewDevice('${deviceId}')">
                    <i class="fas fa-eye"></i>
                </button>
                <button class="btn btn-sm btn-success" onclick="selectDevice('${deviceId}')">
                    <i class="fas fa-bullseye"></i>
                </button>
            </td>
        </tr>
    `).join('');
}

function updateCommandSelect() {
    const select = document.getElementById('targetDevice');
    select.innerHTML = '<option value="">Select Device</option>';
    
    Object.entries(devices).forEach(([deviceId, device]) => {
        if (device.online) {
            select.innerHTML += `<option value="${deviceId}">${device.model || 'Unknown'} (${deviceId})</option>`;
        }
    });
}

function getBatteryColor(battery) {
    if (battery > 50) return 'bg-success';
    if (battery > 20) return 'bg-warning';
    return 'bg-danger';
}

// Command Handling
function handleCommandTypeChange() {
    const commandType = document.getElementById('commandType').value;
    const paramsGroup = document.getElementById('paramsGroup');
    const paramsInput = document.getElementById('commandParams');
    
    if (commandType === 'shell_exec') {
        paramsGroup.style.display = 'block';
        paramsInput.placeholder = 'Enter shell command (e.g., ls -la)';
        paramsInput.required = true;
    } else if (commandType === 'toggle_icon') {
        paramsGroup.style.display = 'block';
        paramsInput.placeholder = 'Enter "show" or "hide"';
        paramsInput.required = true;
    } else {
        paramsGroup.style.display = 'none';
        paramsInput.required = false;
    }
}

async function handleCommandSubmit(e) {
    e.preventDefault();
    
    const targetDevice = document.getElementById('targetDevice').value;
    const commandType = document.getElementById('commandType').value;
    const commandParams = document.getElementById('commandParams').value;
    
    if (!targetDevice) {
        showToast('Please select a target device', 'error');
        return;
    }
    
    const command = {
        command: commandType,
        params: commandParams || null,
        timestamp: firebase.database.ServerValue.TIMESTAMP,
        id: Date.now().toString()
    };
    
    try {
        await database.ref(`commands/${targetDevice}`).push(command);
        
        // Log the command
        await database.ref('logs').push({
            level: 'info',
            message: `Command "${commandType}" sent to device ${targetDevice}`,
            timestamp: firebase.database.ServerValue.TIMESTAMP,
            deviceId: targetDevice
        });
        
        commandCount++;
        updateStats();
        
        showToast(`Command "${commandType}" sent successfully!`, 'success');
        document.getElementById('commandForm').reset();
        document.getElementById('paramsGroup').style.display = 'none';
        
    } catch (error) {
        console.error('Error sending command:', error);
        showToast('Error sending command: ' + error.message, 'error');
    }
}

function selectDevice(deviceId) {
    document.getElementById('targetDevice').value = deviceId;
    showToast(`Device ${deviceId} selected for commands`, 'info');
}

function viewDevice(deviceId) {
    const device = devices[deviceId];
    if (!device) return;
    
    let details = `Device ID: ${deviceId}\n`;
    details += `Model: ${device.model || 'Unknown'}\n`;
    details += `Status: ${device.online ? 'Online' : 'Offline'}\n`;
    details += `Battery: ${device.battery || 'N/A'}%\n`;
    details += `Last Seen: ${formatTimestamp(device.lastSeen)}\n`;
    
    if (device.location) {
        details += `Location: ${device.location.lat}, ${device.location.lng}\n`;
    }
    
    alert(details);
}

// Recent Responses
function loadRecentResponses() {
    Object.keys(devices).forEach(deviceId => {
        database.ref(`responses/${deviceId}`).limitToLast(5).once('value', (snapshot) => {
            displayRecentResponses(deviceId, snapshot.val() || {});
        });
    });
}

function displayRecentResponses(deviceId, responses) {
    const container = document.getElementById('recentResponses');
    const responseEntries = Object.entries(responses).slice(-10).reverse();
    
    if (responseEntries.length === 0) {
        container.innerHTML = '<p class="text-muted text-center">No responses yet</p>';
        return;
    }
    
    container.innerHTML = responseEntries.map(([responseId, response]) => `
        <div class="response-item fade-in">
            <div class="response-meta">
                <strong>${deviceId}</strong> • ${formatTimestamp(response.timestamp)}
            </div>
            <div><strong>Command:</strong> ${response.command}</div>
            ${response.data ? `<div class="response-data">${JSON.stringify(response.data, null, 2)}</div>` : ''}
        </div>
    `).join('');
    
    responseCount = responseEntries.length;
    updateStats();
}

// Logs Functions
function loadLogs() {
    logsRef.limitToLast(100).once('value', (snapshot) => {
        displayLogs(snapshot.val() || {});
    });
}

function displayLogs(logs) {
    const container = document.getElementById('logsContainer');
    const logEntries = Object.entries(logs).sort((a, b) => b[1].timestamp - a[1].timestamp);
    
    if (logEntries.length === 0) {
        container.innerHTML = '<p class="text-muted text-center">No logs available</p>';
        return;
    }
    
    container.innerHTML = logEntries.map(([logId, log]) => `
        <div class="log-entry">
            <div class="log-timestamp">${formatTimestamp(log.timestamp)}</div>
            <div class="log-level ${log.level}">${log.level.toUpperCase()}</div>
            <div class="log-message">${log.message}</div>
        </div>
    `).join('');
}

function refreshLogs() {
    document.getElementById('logsContainer').classList.add('loading');
    loadLogs();
    setTimeout(() => {
        document.getElementById('logsContainer').classList.remove('loading');
        showToast('Logs refreshed', 'info');
    }, 1000);
}

async function clearLogs() {
    if (confirm('Are you sure you want to clear all logs?')) {
        try {
            await logsRef.remove();
            document.getElementById('logsContainer').innerHTML = '<p class="text-muted text-center">No logs available</p>';
            showToast('Logs cleared successfully', 'success');
        } catch (error) {
            showToast('Error clearing logs: ' + error.message, 'error');
        }
    }
}

// Media Functions
async function loadMedia() {
    try {
        const storageRef = storage.ref();
        const result = await storageRef.listAll();
        
        const mediaItems = [];
        
        for (const itemRef of result.items) {
            try {
                const url = await itemRef.getDownloadURL();
                const metadata = await itemRef.getMetadata();
                
                mediaItems.push({
                    name: itemRef.name,
                    url: url,
                    size: metadata.size,
                    timeCreated: metadata.timeCreated,
                    contentType: metadata.contentType
                });
            } catch (error) {
                console.error('Error getting media item:', error);
            }
        }
        
        displayMedia(mediaItems);
        mediaCount = mediaItems.length;
        updateStats();
        
    } catch (error) {
        console.error('Error loading media:', error);
        showToast('Error loading media: ' + error.message, 'error');
    }
}

function displayMedia(mediaItems) {
    const container = document.getElementById('mediaGrid');
    
    if (mediaItems.length === 0) {
        container.innerHTML = '<div class="col-12 text-center text-muted"><p>No media files found</p></div>';
        return;
    }
    
    container.innerHTML = mediaItems.map(item => {
        const isImage = item.contentType?.startsWith('image/');
        const isVideo = item.contentType?.startsWith('video/');
        const isAudio = item.contentType?.startsWith('audio/');
        
        return `
            <div class="col-md-4 col-lg-3 mb-3">
                <div class="media-item">
                    ${isImage ? `
                        <img src="${item.url}" alt="${item.name}" onclick="openMedia('${item.url}', '${item.name}')">
                    ` : isVideo ? `
                        <video controls onclick="openMedia('${item.url}', '${item.name}')">
                            <source src="${item.url}" type="${item.contentType}">
                        </video>
                    ` : isAudio ? `
                        <div class="audio-placeholder d-flex align-items-center justify-content-center bg-light" style="height: 200px;">
                            <div class="text-center">
                                <i class="fas fa-music fa-3x text-muted mb-2"></i>
                                <div class="fw-bold">${item.name}</div>
                                <audio controls class="mt-2">
                                    <source src="${item.url}" type="${item.contentType}">
                                </audio>
                            </div>
                        </div>
                    ` : `
                        <div class="file-placeholder d-flex align-items-center justify-content-center bg-light" style="height: 200px;">
                            <div class="text-center">
                                <i class="fas fa-file fa-3x text-muted mb-2"></i>
                                <div class="fw-bold">${item.name}</div>
                            </div>
                        </div>
                    `}
                    
                    <div class="media-overlay">
                        <div class="fw-bold">${item.name}</div>
                        <small>${formatFileSize(item.size)} • ${formatTimestamp(item.timeCreated)}</small>
                        <div class="mt-2">
                            <button class="btn btn-sm btn-light me-1" onclick="downloadMedia('${item.url}', '${item.name}')">
                                <i class="fas fa-download"></i>
                            </button>
                            <button class="btn btn-sm btn-danger" onclick="deleteMedia('${item.name}')">
                                <i class="fas fa-trash"></i>
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        `;
    }).join('');
}

function openMedia(url, name) {
    window.open(url, '_blank');
}

function downloadMedia(url, name) {
    const a = document.createElement('a');
    a.href = url;
    a.download = name;
    a.click();
}

async function deleteMedia(fileName) {
    if (confirm(`Are you sure you want to delete ${fileName}?`)) {
        try {
            await storage.ref(fileName).delete();
            showToast('Media file deleted successfully', 'success');
            loadMedia();
        } catch (error) {
            showToast('Error deleting file: ' + error.message, 'error');
        }
    }
}

function refreshMedia() {
    loadMedia();
    showToast('Media refreshed', 'info');
}

// Utility Functions
function formatTimestamp(timestamp) {
    if (!timestamp) return 'Never';
    
    const date = new Date(timestamp);
    const now = new Date();
    const diffMs = now - date;
    const diffMins = Math.floor(diffMs / 60000);
    
    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return `${diffMins}m ago`;
    if (diffMins < 1440) return `${Math.floor(diffMins / 60)}h ago`;
    
    return date.toLocaleDateString() + ' ' + date.toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'});
}

function formatFileSize(bytes) {
    if (bytes === 0) return '0 Bytes';
    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

function showToast(message, type = 'info') {
    const toast = document.getElementById('liveToast');
    const toastMessage = document.getElementById('toastMessage');
    
    // Set message and style
    toastMessage.textContent = message;
    toast.className = `toast ${type}`;
    
    // Show toast
    const bsToast = new bootstrap.Toast(toast);
    bsToast.show();
}

function refreshDevices() {
    devicesRef.once('value');
    showToast('Devices refreshed', 'info');
}
