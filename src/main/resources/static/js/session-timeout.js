(function() {
    'use strict';
    
    let sessionWarningShown = false;
    let sessionExpiringSoon = false;
    
    function checkSessionStatus() {
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
            console.warn('Session status check failed:', error);
        });
    }
    
    function showSessionWarning(remainingMinutes) {
        if (sessionWarningShown) {
            return;
        }
        
        sessionWarningShown = true;
        
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
        
        const bootstrapModal = new bootstrap.Modal(modal);
        bootstrapModal.show();
        
        setTimeout(() => {
            if (modal.parentNode) {
                bootstrapModal.hide();
                modal.remove();
            }
        }, 10000);
    }
    
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
                
                const modal = document.getElementById('sessionWarningModal');
                if (modal) {
                    const bootstrapModal = bootstrap.Modal.getInstance(modal);
                    if (bootstrapModal) {
                        bootstrapModal.hide();
                    }
                    modal.remove();
                }
                
                showToast('Session extended successfully!', 'success');
            }
        })
        .catch(error => {
            console.error('Failed to extend session:', error);
            showToast('Failed to extend session. Please login again.', 'danger');
        });
    }
    
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
        
        toast.addEventListener('hidden.bs.toast', () => {
            toast.remove();
        });
    }
    
    function createToastContainer() {
        const container = document.createElement('div');
        container.id = 'toastContainer';
        container.className = 'toast-container position-fixed top-0 end-0 p-3';
        container.style.zIndex = '9999';
        document.body.appendChild(container);
        return container;
    }
    
    function startSessionMonitoring() {
        setInterval(checkSessionStatus, 120000);
        
        ['click', 'keypress', 'scroll', 'mousemove'].forEach(event => {
            document.addEventListener(event, () => {
                if (sessionExpiringSoon) {
                    sessionExpiringSoon = false;
                    sessionWarningShown = false;
                }
            }, { passive: true });
        });
    }
    
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', startSessionMonitoring);
    } else {
        startSessionMonitoring();
    }
    
    window.extendSession = extendSession;
    
})();
