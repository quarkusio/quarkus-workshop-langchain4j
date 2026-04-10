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

    // Add event listeners for form submissions
    setupEventListeners();

    // Set up sorting functionality
    setupSorting();
});

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
        tableBody.innerHTML = '<tr><td colspan="7">No cars match your filter criteria</td></tr>';
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

        // Build action cell based on status
        let actionCell = '';
        if (car.status === 'RENTED' || car.status === 'AT_CLEANING' || car.status === 'IN_MAINTENANCE') {
            actionCell = `
                <td>
                    <form onsubmit="processFeedback(event, ${car.id}, '${car.status}')">
                        <input type="text" class="feedback-input" id="feedback-${car.id}" placeholder="Enter feedback">
                        <button type="submit" class="return-button">Return</button>
                    </form>
                </td>`;
        } else if (car.status === 'PENDING_DISPOSITION') {
            actionCell = `<td>${car.condition || 'Pending analysis'}</td>`;
        } else {
            actionCell = `<td></td>`;
        }

        row.innerHTML = `
            <td>${car.id}</td>
            <td>${car.make}</td>
            <td>${car.model}</td>
            <td>${car.year}</td>
            <td>${car.condition || 'N/A'}</td>
            <td><span class="status-pill ${statusPillClass}">${getStatusDisplay(car.status)}</span></td>
            ${actionCell}
        `;

        tableBody.appendChild(row);
    });
}

// Function to process feedback and return a car
function processFeedback(event, carId, status) {
    event.preventDefault();
    const feedback = document.getElementById(`feedback-${carId}`).value;
    const button = event.target.querySelector('button');

    button.disabled = true;
    button.classList.add('loading');
    const originalText = button.textContent;
    button.textContent = 'Processing...';

    const statusLabels = {
        'RENTED': 'rental',
        'AT_CLEANING': 'cleaning',
        'IN_MAINTENANCE': 'maintenance'
    };

    fetch(`/car-management/return/${carId}?feedback=${encodeURIComponent(feedback)}`, { method: 'POST' })
    .then(response => {
        if (!response.ok) throw new Error('Network response was not ok');
        return response.text();
    })
    .then(data => {
        lastUpdatedCarId = carId;
        showNotification(`Car successfully returned from ${statusLabels[status]}`);
        loadAllCars();
    })
    .catch(error => {
        console.error(`Error returning car from ${statusLabels[status]}:`, error);
        displayError(`Failed to process ${statusLabels[status]} return. Please try again.`);
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
