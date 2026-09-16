const tomorrow = new Date();
tomorrow.setDate(tomorrow.getDate() + 1);
document.getElementById("startDate").value = tomorrow.toISOString().split("T")[0];

let currentRequest = null;
let currentInstanceId = null;
let pollingHandle = null;
const PLAN_ERROR_FALLBACK = "Could not generate the trip plan. Please try again later.";

// Restore a pending plan if the app was restarted while a workflow was awaiting approval.
// The optional latest-plan endpoint is unavailable in steps 00-02; leave the form visible.
(async function restoreLatestPlan() {
    try {
        const res = await fetch("/trip/plan/latest");
        if (!res.ok) return;
        const data = await res.json();
        if (data && data.instanceId && data.status === "awaiting_approval") {
            currentInstanceId = data.instanceId;
            currentRequest = data.request || { destination: "Restored plan", days: "?", travelers: "?", tripType: "", budget: "" };
            showPage("resultsPage");
            renderPlan(data.plan, "awaiting_approval");
        }
    } catch (e) {
        // endpoint not available in earlier steps — stay on form
    }
})();

function showPage(page) {
    document.getElementById("formPage").classList.remove("active");
    document.getElementById("resultsPage").classList.remove("active");
    document.getElementById(page).classList.add("active");
}

function goBackToForm() {
    stopPolling();
    showPage("formPage");
    document.getElementById("planBtn").disabled = false;
    document.getElementById("planBtn").textContent = "Generate Trip Plan";
}

async function planTrip() {
    const btn = document.getElementById("planBtn");
    btn.disabled = true;
    btn.textContent = "Planning...";

    currentRequest = {
        destination: document.getElementById("destination").value,
        startDate: document.getElementById("startDate").value,
        days: parseInt(document.getElementById("days").value, 10),
        tripType: document.getElementById("tripType").value,
        travelers: parseInt(document.getElementById("travelers").value, 10),
        budget: document.getElementById("budget").value,
        preferences: document.getElementById("preferences").value || "No specific preferences"
    };

    showPage("resultsPage");
    document.getElementById("results").innerHTML = `
        <div class="top-bar">
            <button class="btn-back" onclick="goBackToForm()">&#8592; Plan Another Trip</button>
        </div>
        <div class="spinner">
            <div class="icon">&#9203;</div>
            <p>Planning your trip...</p>
            <span class="hint">The agents are researching your destination, selecting a vehicle, and estimating costs</span>
        </div>
    `;

    try {
        const res = await fetch("/trip/plan", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(currentRequest)
        });
        if (!res.ok) {
            renderError(await readFailureMessage(res));
            return;
        }
        const data = await res.json();

        // Step 03: Response includes instanceId and status for workflow
        if (data.instanceId) {
            currentInstanceId = data.instanceId;
            renderPlan(data.plan, "awaiting_approval");
        } else {
            // Steps 01-02: Simple plan response
            renderPlan(data);
        }
    } catch (e) {
        renderError(PLAN_ERROR_FALLBACK);
    }
}

function renderPlan(plan, status, confirmation) {
    const v = plan.vehicle || {};
    const costs = plan.costs || {};
    const itinerary = plan.itinerary || [];
    const isAwaiting = status === "awaiting_approval";
    const isConfirmed = status === "confirmed";

    // Step 03: Status banner for approval workflow
    let banner = "";
    if (isAwaiting) {
        banner = `<div class="status-banner status-awaiting">The workflow is waiting for your decision.</div>`;
    } else if (isConfirmed) {
        const conf = confirmation || {};
        banner = `<div class="status-banner status-confirmed">Trip confirmed! Booking reference: <strong>${conf.bookingReference || "N/A"}</strong><br>${conf.message || ""}</div>`;
    }

    // Step 03: Action buttons for approval workflow
    const actions = isAwaiting ? `
        <div class="action-bar">
            <button class="btn-approve" id="approveBtn" onclick="submitApproval('approved')">Approve Trip</button>
            <button class="btn-reject" id="rejectBtn" onclick="submitApproval('rejected')">Reject Trip</button>
        </div>` : "";

    document.getElementById("results").innerHTML = `
        <div class="top-bar">
            <button class="btn-back" onclick="goBackToForm()">&#8592; Plan Another Trip</button>
        </div>
        ${banner}
        ${actions}
        <div class="plan-header">
            <h2>${currentRequest.destination} &mdash; ${currentRequest.days}-Day ${capitalize(currentRequest.tripType)} Trip</h2>
            <div class="meta">${currentRequest.travelers} travelers &middot; ${currentRequest.budget}</div>
        </div>

        <div class="plan-section">
            <h3>&#x1F697; Vehicle Recommendation</h3>
            <div class="card">
                <strong>${v.type || ""} &mdash; ${v.model || ""}</strong>
                <p>${v.reasoning || ""}</p>
            </div>
        </div>

        <div class="plan-section">
            <h3>&#x1F5FA;&#xFE0F; Route Overview</h3>
            <div class="card"><p>${plan.routeOverview || ""}</p></div>
        </div>

        <div class="plan-section">
            <h3>&#x1F4C5; Daily Itinerary</h3>
            ${itinerary.map(day => `
                <div class="card day-card">
                    <div class="day-header"><span class="day-num">Day ${day.day}</span> <strong>${day.title || ""}</strong></div>
                    <p>${day.description || ""}</p>
                    ${day.overnightStop ? `<div class="overnight">&#x1F3E8; ${day.overnightStop}</div>` : ""}
                </div>
            `).join("")}
        </div>

        <div class="plan-section">
            <h3>&#x1F4B6; Estimated Costs</h3>
            <div class="card costs-grid">
                ${costs.vehiclePerDay ? `<div class="cost-item"><span>Vehicle/day</span><span>${costs.vehiclePerDay}</span></div>` : ""}
                ${costs.fuel ? `<div class="cost-item"><span>Fuel</span><span>${costs.fuel}</span></div>` : ""}
                ${costs.tolls ? `<div class="cost-item"><span>Tolls</span><span>${costs.tolls}</span></div>` : ""}
                ${costs.accommodation ? `<div class="cost-item"><span>Accommodation</span><span>${costs.accommodation}</span></div>` : ""}
                ${costs.food ? `<div class="cost-item"><span>Food</span><span>${costs.food}</span></div>` : ""}
                ${costs.activities ? `<div class="cost-item"><span>Activities</span><span>${costs.activities}</span></div>` : ""}
                ${costs.total ? `<div class="cost-item total"><span>Total</span><span>${costs.total}</span></div>` : ""}
            </div>
        </div>

    `;
}

// Step 03: Approval workflow functions
async function submitApproval(status) {
    if (!currentInstanceId) return;
    disableActionButtons(true);

    try {
        const res = await fetch("/trip/approve", {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                instanceId: currentInstanceId,
                status: status,
                feedback: ""
            })
        });
        if (!res.ok) throw new Error("Could not submit approval");

        if (status === "approved") {
            startPolling();
        } else {
            renderCancelled();
        }
    } catch (e) {
        renderError(e.message);
        disableActionButtons(false);
    }
}

function startPolling() {
    stopPolling();
    pollingHandle = setInterval(pollForConfirmation, 2000);
}

function stopPolling() {
    if (pollingHandle) {
        clearInterval(pollingHandle);
        pollingHandle = null;
    }
}

async function pollForConfirmation() {
    try {
        const res = await fetch(`/trip/plan/status?instanceId=${currentInstanceId}`);
        if (res.status === 204) return;
        if (!res.ok) return;
        const planStatus = await res.json();
        if (planStatus.status === "confirmed") {
            stopPolling();
            renderPlan(planStatus.plan, "confirmed", planStatus.confirmation);
        }
    } catch (e) {
        console.error(e);
    }
}

function disableActionButtons(disabled) {
    const approveBtn = document.getElementById("approveBtn");
    const rejectBtn = document.getElementById("rejectBtn");
    if (approveBtn) approveBtn.disabled = disabled;
    if (rejectBtn) rejectBtn.disabled = disabled;
}

function renderCancelled() {
    document.getElementById("results").innerHTML = `
        <div class="top-bar">
            <button class="btn-back" onclick="goBackToForm()">&#8592; Plan Another Trip</button>
        </div>
        <div class="status-banner status-cancelled">Trip rejected. The workflow ended without finalizing a booking.</div>
    `;
}

async function readFailureMessage(response) {
    try {
        const failure = await response.json();
        if ((response.status === 422 && failure?.error === "guardrail_violation")
                || (response.status === 500 && failure?.error === "planning_failed")) {
            if (typeof failure.message === "string" && failure.message.trim()) {
                return failure.message.trim();
            }
        }
    } catch (e) {
        // Non-JSON error pages and empty responses use the same safe fallback.
    }
    return PLAN_ERROR_FALLBACK;
}

function renderError(message) {
    document.getElementById("results").innerHTML = `
        <div class="top-bar">
            <button class="btn-back" onclick="goBackToForm()">&#8592; Plan Another Trip</button>
        </div>
        <div class="spinner">
            <p id="planError" role="alert"></p>
        </div>
    `;
    document.getElementById("planError").textContent = "Error: "
            + (typeof message === "string" && message.trim() ? message : PLAN_ERROR_FALLBACK);
}

function capitalize(s) {
    return s.charAt(0).toUpperCase() + s.slice(1);
}
