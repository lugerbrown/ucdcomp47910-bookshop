/**
 * Session timeout warning and management for CWE-613 mitigation.
 * Provides user notifications about session expiration and automatic logout handling.
 */

(function() {
    'use strict';
    
    let sessionWarningShown = false;
    let sessionExpiringSoon = false;
    
    // Check for session expiration headers on each request
    function checkSessionHeaders() {
        // This would typically be done via AJAX or by checking response headers
        // For now, we'll use a polling approach to check session status
        checkSessionStatus();
    }
    
    // Check session status via AJAX
    function checkSessionStatus() {
        // Only check if user is authenticated
        if (!document.body.classList.contains('authenticated')) {
            return;
        }
        
        fetch('/api/session/status', {
            method: 'GET',
            credentials: 'same-origin',
            headers: {
                'X-Requested-With': 'XMLHttpRequest'
            }
        })
        .then(response => {
            if (response.headers.get('X-Session-Expiring') === 'true') {
                const remainingMinutes = response.headers.get('X-Session-Remaining-Minutes');
                if (remainingMinutes && !sessionExpiringSoon) {
                    sessionExpiringSoon = true;
                    showSessionWarning(parseInt(remainingMinutes));
                }
            }
        })
        .catch(error => {
            // If session check fails, assume session is invalid
            console.warn('Session status check failed:', error);
        });
    }
    
    // Show session expiration warning
    function showSessionWarning(remainingMinutes) {
        if (sessionWarningShown) {
            return;
        }
        
        sessionWarningShown = true;
        
        // Create warning modal
        const modal = document.createElement('div');
        modal.className = 'modal fade';
        modal.id = 'sessionWarningModal';
        modal.innerHTML = `
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header bg-warning">
                        <h5 class="modal-title">
                            <i class="bi bi-exclamation-triangle"></i> Session Expiring Soon
                        </h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body">
                        <p>Your session will expire in <strong>${remainingMinutes}</strong> minutes due to inactivity.</p>
                        <p>To continue working, please click "Extend Session" below.</p>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Dismiss</button>
                        <button type="button" class="btn btn-primary" onclick="extendSession()">Extend Session</button>
                    </button>
                </div>
            </div>
        `;
        
        document.body.appendChild(modal);
        
        // Show modal
        const bootstrapModal = new bootstrap.Modal(modal);
        bootstrapModal.show();
        
        // Auto-hide after 10 seconds
        setTimeout(() => {
            if (modal.parentNode) {
                bootstrapModal.hide();
                modal.remove();
            }
        }, 10000);
    }
    
    // Extend session by making a request
    function extendSession() {
        fetch('/api/session/extend', {
            method: 'POST',
            credentials: 'same-origin',
            headers: {
                'X-Requested-With': 'XMLHttpRequest'
            }
        })
        .then(response => {
            if (response.ok) {
                sessionExpiringSoon = false;
                sessionWarningShown = false;
                
                // Hide modal
                const modal = document.getElementById('sessionWarningModal');
                if (modal) {
                    const bootstrapModal = bootstrap.Modal.getInstance(modal);
                    if (bootstrapModal) {
                        bootstrapModal.hide();
                    }
                    modal.remove();
                }
                
                // Show success message
                showToast('Session extended successfully!', 'success');
            }
        })
        .catch(error => {
            console.error('Failed to extend session:', error);
            showToast('Failed to extend session. Please login again.', 'danger');
        });
    }
    
    // Show toast notification
    function showToast(message, type = 'info') {
        const toastContainer = document.getElementById('toastContainer') || createToastContainer();
        
        const toast = document.createElement('div');
        toast.className = `toast align-items-center text-white bg-${type} border-0`;
        toast.innerHTML = `
            <div class="d-flex">
                <div class="toast-body">${message}</div>
                <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast"></button>
            </div>
        `;
        
        toastContainer.appendChild(toast);
        
        const bootstrapToast = new bootstrap.Toast(toast);
        bootstrapToast.show();
        
        // Auto-remove after toast is hidden
        toast.addEventListener('hidden.bs.toast', () => {
            toast.remove();
        });
    }
    
    // Create toast container if it doesn't exist
    function createToastContainer() {
        const container = document.createElement('div');
        container.id = 'toastContainer';
        container.className = 'toast-container position-fixed top-0 end-0 p-3';
        container.style.zIndex = '9999';
        document.body.appendChild(container);
        return container;
    }
    
    // Check session status periodically
    function startSessionMonitoring() {
        // Check every 2 minutes
        setInterval(checkSessionStatus, 120000);
        
        // Also check on user activity
        ['click', 'keypress', 'scroll', 'mousemove'].forEach(event => {
            document.addEventListener(event, () => {
                if (sessionExpiringSoon) {
                    // Reset warning if user is active
                    sessionExpiringSoon = false;
                    sessionWarningShown = false;
                }
            }, { passive: true });
        });
    }
    
    // Initialize when DOM is ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', startSessionMonitoring);
    } else {
        startSessionMonitoring();
    }
    
    // Make functions globally available
    window.extendSession = extendSession;
    
})();
