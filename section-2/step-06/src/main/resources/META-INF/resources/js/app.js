// Car Management UI JavaScript

// Global variables for sorting and filtering
let currentSortColumn = 'id';
let currentSortDirection = 'asc';
let carsData = []; // Store the cars data globally for sorting
let currentFilterText = '';
let currentFilterField = 'all';
let lastUpdatedCarId = null; // Track the last updated car for highlighting

// Wait for the DOM to be fully loaded
document.addEventListener('DOMContentLoaded', function() {
    // Load all cars and populate the tables
    loadAllCars();
    
    // Add event listeners for form submissions and tabs
    setupEventListeners();
    
    // Set up tab functionality
    setupTabs();
    
    // Set up sorting functionality
    setupSorting();
    
    // Start polling for approvals (always active now with modal)
    startApprovalPolling();
});

// Function to set up tab functionality
function setupTabs() {
    const tabButtons = document.querySelectorAll('.tab-button');
    
    tabButtons.forEach(button => {
        button.addEventListener('click', () => {
            // Remove active class from all buttons and content
            document.querySelectorAll('.tab-button').forEach(btn => {
                btn.classList.remove('active');
            });
            document.querySelectorAll('.tab-content').forEach(content => {
                content.classList.remove('active');
            });
            
            // Add active class to clicked button
            button.classList.add('active');
            
            // Show corresponding content
            const tabId = button.getAttribute('data-tab');
            const tabContent = document.getElementById(tabId + '-section');
            if (tabContent) {
                tabContent.classList.add('active');
            }
        });
    });
}

// Function to load all cars from the API
function loadAllCars() {
    fetch('/cars')
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(cars => {
            // Store cars data globally for sorting
            carsData = cars;
            
            // Sort the data if a sort is active
            sortCars();
            
            // Process the cars data
            populateFleetStatusTable(carsData);
            populateRentalReturnTable(carsData.filter(car => car.status === 'RENTED'));
            populateCleaningTable(carsData.filter(car => car.status === 'AT_CLEANING'));
            populateMaintenanceTable(carsData.filter(car => car.status === 'IN_MAINTENANCE'));
            populateDispositionTable(carsData.filter(car => car.status === 'PENDING_DISPOSITION'));
        })
        .catch(error => {
            console.error('Error fetching cars:', error);
            displayError('Failed to load car data. Please try again later.');
        });
}

// Function to set up sorting functionality
function setupSorting() {
    const sortableHeaders = document.querySelectorAll('.sortable');
    
    sortableHeaders.forEach(header => {
        header.addEventListener('click', function() {
            const column = this.getAttribute('data-sort');
            
            // If clicking the same column, toggle direction
            if (column === currentSortColumn) {
                currentSortDirection = currentSortDirection === 'asc' ? 'desc' : 'asc';
            } else {
                // New column, default to ascending
                currentSortColumn = column;
                currentSortDirection = 'asc';
            }
            
            // Update header classes for visual indication
            updateSortHeaders();
            
            // Sort and redisplay data
            sortCars();
            populateFleetStatusTable(carsData);
        });
    });
}

// Function to update sort header classes
function updateSortHeaders() {
    // Remove all sort classes
    document.querySelectorAll('.sortable').forEach(header => {
        header.classList.remove('sort-asc', 'sort-desc');
    });
    
    // Add class to current sort column
    const currentHeader = document.querySelector(`.sortable[data-sort="${currentSortColumn}"]`);
    if (currentHeader) {
        currentHeader.classList.add(currentSortDirection === 'asc' ? 'sort-asc' : 'sort-desc');
    }
}

// Function to sort cars based on current sort settings
function sortCars() {
    carsData.sort((a, b) => {
        let valueA, valueB;
        
        // Handle special case for status which needs to be displayed text
        if (currentSortColumn === 'status') {
            valueA = getStatusDisplay(a.status);
            valueB = getStatusDisplay(b.status);
        } else {
            valueA = a[currentSortColumn];
            valueB = b[currentSortColumn];
        }
        
        // Handle numeric values
        if (currentSortColumn === 'id' || currentSortColumn === 'year') {
            valueA = Number(valueA) || 0;
            valueB = Number(valueB) || 0;
        }
        
        // Compare values based on direction
        if (valueA < valueB) {
            return currentSortDirection === 'asc' ? -1 : 1;
        }
        if (valueA > valueB) {
            return currentSortDirection === 'asc' ? 1 : -1;
        }
        return 0;
    });
}

// Function to filter cars based on current filter settings
function filterCars() {
    if (!currentFilterText) {
        return carsData; // Return all cars if no filter text
    }
    
    return carsData.filter(car => {
        // Convert filter text to lowercase for case-insensitive comparison
        const filterText = currentFilterText.toLowerCase();
        
        // If filtering on a specific field
        if (currentFilterField !== 'all') {
            let fieldValue = car[currentFilterField];
            
            // Handle special case for status which needs to be displayed text
            if (currentFilterField === 'status') {
                fieldValue = getStatusDisplay(fieldValue);
            }
            
            // Convert to string and check if it contains the filter text
            return String(fieldValue).toLowerCase().includes(filterText);
        }
        
        // If filtering across all fields
        return (
            String(car.id).toLowerCase().includes(filterText) ||
            car.make.toLowerCase().includes(filterText) ||
            car.model.toLowerCase().includes(filterText) ||
            String(car.year).toLowerCase().includes(filterText) ||
            (car.condition && car.condition.toLowerCase().includes(filterText)) ||
            getStatusDisplay(car.status).toLowerCase().includes(filterText)
        );
    });
}

// Function to populate the Fleet Status table
function populateFleetStatusTable(cars) {
    const tableBody = document.getElementById('fleet-status-table-body');
    tableBody.innerHTML = ''; // Clear existing rows
    
    // Apply filter if there's filter text
    const filteredCars = currentFilterText ? filterCars() : cars;
    
    if (filteredCars.length === 0) {
        tableBody.innerHTML = '<tr><td colspan="6">No cars match your filter criteria</td></tr>';
        return;
    }
    
    filteredCars.forEach(car => {
        const row = document.createElement('tr');
        
        // Highlight the row if it was just updated
        if (car.id === lastUpdatedCarId) {
            row.classList.add('highlight-row');
            // Clear the highlight after animation completes
            setTimeout(() => {
                lastUpdatedCarId = null;
            }, 3000);
        }
        
        // Get status pill class based on car status
        const statusPillClass = getStatusPillClass(car.status);
        
        row.innerHTML = `
            <td>${car.id}</td>
            <td>${car.make}</td>
            <td>${car.model}</td>
            <td>${car.year}</td>
            <td>${car.condition || 'N/A'}</td>
            <td><span class="status-pill ${statusPillClass}">${getStatusDisplay(car.status)}</span></td>
        `;
        
        tableBody.appendChild(row);
    });
}

// Function to populate the Rental Return table
function populateRentalReturnTable(cars) {
    const tableBody = document.getElementById('rental-return-table-body');
    tableBody.innerHTML = ''; // Clear existing rows
    
    if (cars.length === 0) {
        tableBody.innerHTML = '<tr><td colspan="6">No cars currently rented</td></tr>';
        return;
    }
    
    cars.forEach(car => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${car.id}</td>
            <td>${car.make}</td>
            <td>${car.model}</td>
            <td>${car.year}</td>
            <td>
                <form id="rentalReturnForm" onsubmit="returnFromRental(event, ${car.id})">
                    <input type="text" class="feedback-input" id="rental-feedback-${car.id}" placeholder="Enter feedback">
                    <button type="submit" class="return-button">Return</button>
                </form>
            </td>
        `;
        
        tableBody.appendChild(row);
    });
}

// Function to populate the Cleaning table
function populateCleaningTable(cars) {
    const tableBody = document.getElementById('cleaning-table-body');
    tableBody.innerHTML = ''; // Clear existing rows
    
    if (cars.length === 0) {
        tableBody.innerHTML = '<tr><td colspan="6">No cars currently at cleaning</td></tr>';
        return;
    }
    
    cars.forEach(car => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${car.id}</td>
            <td>${car.make}</td>
            <td>${car.model}</td>
            <td>${car.year}</td>
            <td>
                <form id="rentalReturnForm" onsubmit="returnFromCleaning(event, ${car.id})">
                    <input type="text" class="feedback-input" id="cleaning-feedback-${car.id}" placeholder="Enter feedback">
                    <button class="return-button">Return</button>
                </form>
            </td>
        `;
        
        tableBody.appendChild(row);
    });
}

// Function to populate the Maintenance table
function populateMaintenanceTable(cars) {
    const tableBody = document.getElementById('maintenance-table-body');
    tableBody.innerHTML = ''; // Clear existing rows
    
    if (cars.length === 0) {
        tableBody.innerHTML = '<tr><td colspan="6">No cars currently in maintenance</td></tr>';
        return;
    }
    
    cars.forEach(car => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${car.id}</td>
            <td>${car.make}</td>
            <td>${car.model}</td>
            <td>${car.year}</td>
            <td>
                <form id="rentalReturnForm" onsubmit="returnFromMaintenance(event, ${car.id})">
                    <input type="text" class="feedback-input" id="maintenance-feedback-${car.id}" placeholder="Enter feedback">
                    <button class="return-button">Return</button>
                </form>
            </td>
        `;
        
        tableBody.appendChild(row);
    });
}

// Function to populate the Disposition table
function populateDispositionTable(cars) {
    const tableBody = document.getElementById('disposition-table-body');
    tableBody.innerHTML = ''; // Clear existing rows
    
    if (cars.length === 0) {
        tableBody.innerHTML = '<tr><td colspan="5">No cars pending disposition</td></tr>';
        return;
    }
    
    cars.forEach(car => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${car.id}</td>
            <td>${car.make}</td>
            <td>${car.model}</td>
            <td>${car.year}</td>
            <td>${car.condition || 'Pending analysis'}</td>
        `;
        
        tableBody.appendChild(row);
    });
}

// Function to return a car from rental
function returnFromRental(event, carId) {
    event.preventDefault();
    const feedback = document.getElementById(`rental-feedback-${carId}`).value;
    const button = event.target.querySelector('button[type="submit"]');
    
    // Add loading state
    button.disabled = true;
    button.classList.add('loading');
    const originalText = button.textContent;
    button.textContent = 'Processing...';
    
    fetch(`/car-management/rental-return/${carId}?rentalFeedback=${encodeURIComponent(feedback)}`, {
        method: 'POST'
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        return response.text();
    })
    .then(data => {
        lastUpdatedCarId = carId; // Mark this car for highlighting
        showNotification('Car successfully returned from rental');
        loadAllCars(); // Refresh all tables
    })
    .catch(error => {
        console.error('Error returning car from rental:', error);
        displayError('Failed to process rental return. Please try again.');
        // Re-enable button on error
        button.disabled = false;
        button.classList.remove('loading');
        button.textContent = originalText;
    });
}

// Function to return a car from cleaning
function returnFromCleaning(event, carId) {
    event.preventDefault();
    const feedback = document.getElementById(`cleaning-feedback-${carId}`).value;
    const button = event.target.querySelector('button');
    
    // Add loading state
    button.disabled = true;
    button.classList.add('loading');
    const originalText = button.textContent;
    button.textContent = 'Processing...';
    
    fetch(`/car-management/cleaningReturn/${carId}?cleaningFeedback=${encodeURIComponent(feedback)}`, {
        method: 'POST'
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        return response.text();
    })
    .then(data => {
        lastUpdatedCarId = carId; // Mark this car for highlighting
        showNotification('Car successfully returned from cleaning');
        loadAllCars(); // Refresh all tables
    })
    .catch(error => {
        console.error('Error returning car from cleaning:', error);
        displayError('Failed to process cleaning return. Please try again.');
        // Re-enable button on error
        button.disabled = false;
        button.classList.remove('loading');
        button.textContent = originalText;
    });
}

// Function to return a car from maintenance
function returnFromMaintenance(event, carId) {
    event.preventDefault();
    const feedback = document.getElementById(`maintenance-feedback-${carId}`).value;
    const button = event.target.querySelector('button');
    
    // Add loading state
    button.disabled = true;
    button.classList.add('loading');
    const originalText = button.textContent;
    button.textContent = 'Processing...';
    
    fetch(`/car-management/maintenance-return/${carId}?maintenanceFeedback=${encodeURIComponent(feedback)}`, {
        method: 'POST'
    })
    .then(response => {
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        return response.text();
    })
    .then(data => {
        lastUpdatedCarId = carId; // Mark this car for highlighting
        showNotification('Car successfully returned from maintenance');
        loadAllCars(); // Refresh all tables
    })
    .catch(error => {
        console.error('Error returning car from maintenance:', error);
        displayError('Failed to process maintenance return. Please try again.');
        // Re-enable button on error
        button.disabled = false;
        button.classList.remove('loading');
        button.textContent = originalText;
    });
}

// Helper function to get CSS class based on car status
function getStatusClass(status) {
    switch(status) {
        case 'RENTED':
            return 'status-rented';
        case 'AT_CLEANING':
            return 'status-cleaning';
        case 'IN_MAINTENANCE':
            return 'status-maintenance';
        case 'AVAILABLE':
            return 'status-available';
        case 'PENDING_DISPOSITION':
            return 'status-disposition';
        default:
            return '';
    }
}

// Helper function to get status pill class based on car status
function getStatusPillClass(status) {
    switch(status) {
        case 'RENTED':
            return 'status-pill-rented';
        case 'AT_CLEANING':
            return 'status-pill-cleaning';
        case 'IN_MAINTENANCE':
            return 'status-pill-maintenance';
        case 'AVAILABLE':
            return 'status-pill-available';
        case 'PENDING_DISPOSITION':
            return 'status-pill-disposition';
        default:
            return '';
    }
}

// Helper function to get display text for car status
function getStatusDisplay(status) {
    switch(status) {
        case 'RENTED':
            return 'Rented';
        case 'AT_CLEANING':
            return 'At Cleaning';
        case 'IN_MAINTENANCE':
            return 'In Maintenance';
        case 'AVAILABLE':
            return 'Available to Rent';
        case 'PENDING_DISPOSITION':
            return 'Pending Disposition';
        default:
            return status;
    }
}

// Function to set up event listeners
function setupEventListeners() {
    // Add refresh button event listener
    const refreshButton = document.getElementById('refresh-button');
    if (refreshButton) {
        refreshButton.addEventListener('click', loadAllCars);
    }
    
    // Add filter input event listener
    const filterInput = document.getElementById('fleet-filter');
    if (filterInput) {
        filterInput.addEventListener('input', function() {
            currentFilterText = this.value;
            populateFleetStatusTable(carsData);
        });
    }
    
    // Add filter field select event listener
    const filterField = document.getElementById('filter-field');
    if (filterField) {
        filterField.addEventListener('change', function() {
            currentFilterField = this.value;
            populateFleetStatusTable(carsData);
        });
    }
    
    // Add clear filter button event listener
    const clearFilterButton = document.getElementById('clear-filter');
    if (clearFilterButton) {
        clearFilterButton.addEventListener('click', function() {
            const filterInput = document.getElementById('fleet-filter');
            const filterField = document.getElementById('filter-field');
            
            // Reset filter values
            currentFilterText = '';
            currentFilterField = 'all';
            
            // Reset UI elements
            if (filterInput) filterInput.value = '';
            if (filterField) filterField.value = 'all';
            
            // Refresh table
            populateFleetStatusTable(carsData);
        });
    }
}

// Function to display error messages
function displayError(message) {
    const errorDiv = document.getElementById('error-message');
    if (errorDiv) {
        errorDiv.textContent = message;
        errorDiv.style.display = 'block';
        
        // Hide after 5 seconds
        setTimeout(() => {
            errorDiv.style.display = 'none';
        }, 5000);
    } else {
        alert(message);
    }
}

// Function to show notification messages
function showNotification(message) {
    const notificationDiv = document.getElementById('notification');
    if (notificationDiv) {
        notificationDiv.textContent = message;
        notificationDiv.style.display = 'block';
        
        // Hide after 3 seconds
        setTimeout(() => {
            notificationDiv.style.display = 'none';
        }, 3000);
    }
}



// Poll for pending approvals every 2 seconds
let approvalPollingInterval = null;
let lastApprovalCount = 0;
let isModalOpen = false;

// ============================================================================
// HUMAN-IN-THE-LOOP APPROVAL FUNCTIONS
// ============================================================================

// Load and display pending approvals in modal
async function loadPendingApprovals() {
    try {
        const response = await fetch('/api/approvals/pending');
        const proposals = await response.json();
        
        const floatBtn = document.getElementById('approval-notification-btn');
        const countBadge = floatBtn.querySelector('.approval-count-badge');
        
        // Show browser notification if new approvals arrived
        if (proposals.length > lastApprovalCount && lastApprovalCount >= 0) {
            if (proposals.length > 0) {
                showBrowserNotification('🚨 Approval Required',
                    `${proposals.length} vehicle disposition${proposals.length > 1 ? 's' : ''} awaiting your approval`);
            }
        }
        lastApprovalCount = proposals.length;
        
        // Update floating button
        if (proposals.length > 0) {
            floatBtn.style.display = 'flex';
            countBadge.textContent = proposals.length;
        } else {
            floatBtn.style.display = 'none';
            // Close modal if no more approvals
            if (isModalOpen) {
                closeApprovalModal();
            }
        }
        
        // Only update modal content if modal is NOT open (prevents flashing)
        if (!isModalOpen) {
            const modalBody = document.getElementById('approval-modal-body');
            if (!proposals || proposals.length === 0) {
                modalBody.innerHTML = '<p style="text-align: center; padding: 40px; color: #666;">No pending approvals at this time.</p>';
            } else {
                modalBody.innerHTML = '';
                proposals.forEach(proposal => {
                    const card = createApprovalCard(proposal);
                    modalBody.appendChild(card);
                });
            }
        }
    } catch (error) {
        console.error('Error loading pending approvals:', error);
    }
}

// Open approval modal
function openApprovalModal() {
    isModalOpen = true;
    const modal = document.getElementById('approval-modal');
    modal.style.display = 'flex';
    
    // Load content when opening
    loadModalContent();
}

// Close approval modal
function closeApprovalModal() {
    isModalOpen = false;
    document.getElementById('approval-modal').style.display = 'none';
}

// Load modal content (called when opening modal)
async function loadModalContent() {
    try {
        const response = await fetch('/api/approvals/pending');
        const proposals = await response.json();
        const modalBody = document.getElementById('approval-modal-body');
        
        if (!proposals || proposals.length === 0) {
            modalBody.innerHTML = '<p style="text-align: center; padding: 40px; color: #666;">No pending approvals at this time.</p>';
        } else {
            modalBody.innerHTML = '';
            proposals.forEach(proposal => {
                const card = createApprovalCard(proposal);
                modalBody.appendChild(card);
            });
        }
    } catch (error) {
        console.error('Error loading modal content:', error);
    }
}

// Show browser notification (requires permission)
function showBrowserNotification(title, body) {
    if (!("Notification" in window)) {
        return;
    }
    
    if (Notification.permission === "granted") {
        new Notification(title, { body, icon: '/favicon.ico' });
    } else if (Notification.permission !== "denied") {
        Notification.requestPermission().then(permission => {
            if (permission === "granted") {
                new Notification(title, { body, icon: '/favicon.ico' });
            }
        });
    }
}

// Create an approval card UI element for a proposal
function createApprovalCard(proposal) {
    const card = document.createElement('div');
    card.className = 'approval-card';
    card.id = `approval-${proposal.id}`;
    
    card.innerHTML = `
        <div class="approval-card-header">
            <div class="vehicle-title">
                <span class="vehicle-icon">🚗</span>
                <h3>${proposal.carYear} ${proposal.carMake} ${proposal.carModel}</h3>
            </div>
            <div class="vehicle-value">${proposal.carValue}</div>
        </div>
        
        <div class="approval-card-body">
            <div class="info-row">
                <span class="info-label">Car #${proposal.carNumber}</span>
                <span class="info-label">Condition: ${proposal.carCondition}</span>
            </div>
            
            <div class="damage-section">
                <div class="section-title">Damage Report</div>
                <div class="damage-text">${proposal.rentalFeedback || 'No feedback provided'}</div>
            </div>
            
            <div class="proposal-section">
                <div class="section-title">AI Recommendation</div>
                <div class="proposal-action">
                    <span class="action-badge">${proposal.proposedDisposition}</span>
                    <span class="action-reason">${proposal.dispositionReason}</span>
                </div>
            </div>
        </div>
        
        <div class="approval-card-footer">
            ${getApprovalButtons(proposal)}
        </div>
    `;
    
    return card;
}

// Get approval buttons - simplified to always show Keep vs Dispose
function getApprovalButtons(proposal) {
    return `
        <button class="btn-approve" onclick="handleProposalDecision(${proposal.id}, 'KEEP_CAR')">
            ✅ Keep & Repair
        </button>
        <button class="btn-reject" onclick="handleProposalDecision(${proposal.id}, 'DISPOSE_CAR')">
            🗑️ Dispose
        </button>
    `;
}

// Handle approval/rejection decision for a proposal
async function handleProposalDecision(proposalId, decision) {
    try {
        const reasonInput = document.getElementById(`reason-${proposalId}`);
        const reason = reasonInput ? reasonInput.value.trim() : '';
        
        const response = await fetch(`/api/approvals/${proposalId}/decide`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                decision: decision, // KEEP_CAR or DISPOSE_CAR
                reason: reason || `${decision === 'KEEP_CAR' ? 'Keep and repair' : 'Dispose'} decision by human reviewer`,
                approvedBy: 'Workshop User'
            })
        });
        
        if (response.ok) {
            const actionText = decision === 'KEEP_CAR' ? 'KEEP & REPAIR' : 'DISPOSE';
            showNotification(`✅ Decision: ${actionText} - Workflow will complete shortly`, 'success');
            
            // Remove the approval card with animation
            const card = document.getElementById(`approval-${proposalId}`);
            if (card) {
                card.style.opacity = '0';
                card.style.transform = 'scale(0.95)';
                setTimeout(() => {
                    card.remove();
                    // Reload approvals to update the display
                    loadPendingApprovals();
                    // Don't reload cars immediately - let the next automatic refresh handle it
                    // This prevents the UI from flickering between states
                }, 300);
            }
        } else {
            const error = await response.json();
            showNotification(`❌ Error: ${error.error || 'Failed to record decision'}`, 'error');
        }
    } catch (error) {
        console.error('Error handling proposal decision:', error);
        showNotification('❌ Error recording decision', 'error');
    }
}

// Start polling for pending approvals
function startApprovalPolling() {
    // Request notification permission on first load
    if ("Notification" in window && Notification.permission === "default") {
        Notification.requestPermission();
    }
    
    // Load immediately
    loadPendingApprovals();
    
    // Then poll every 2 seconds
    if (approvalPollingInterval) {
        clearInterval(approvalPollingInterval);
    }
    approvalPollingInterval = setInterval(loadPendingApprovals, 2000);
}

// Stop polling for pending approvals
function stopApprovalPolling() {
    if (approvalPollingInterval) {
        clearInterval(approvalPollingInterval);
        approvalPollingInterval = null;
    }
}
