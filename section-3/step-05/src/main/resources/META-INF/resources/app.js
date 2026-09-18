const tomorrow = new Date();
tomorrow.setDate(tomorrow.getDate() + 1);
document.getElementById("startDate").value = tomorrow.toISOString().split("T")[0];

const FETCH_TIMEOUT = 15000;
const PLANNING_TIMEOUT = 135000;
const POLLING_TIMEOUT = 120000;
const POLLING_INTERVAL = 2000;
const statuses = new Set(["planning", "awaiting_approval", "decision_submitted", "confirmed", "rejected", "failed"]);
let currentTrip = null;
let generation = 0;
let pollingHandle = null;
const requests = new Set();
let submitting = false;
let decisionUncertain = false;
let restoring = false;
let notice = "";

// The timeout covers both response headers and the response body.
async function fetchJson(url, options = {}, timeout = FETCH_TIMEOUT) {
    const controller = new AbortController();
    requests.add(controller);
    const timer = setTimeout(() => controller.abort(), timeout);
    try {
        const response = await fetch(url, { ...options, signal: controller.signal, cache: "no-store" });
        let data = null;
        if (response.status !== 204) {
            try {
                data = await response.json();
            } catch (error) {
                if (controller.signal.aborted) throw error;
            }
        }
        if (data && typeof data === "object" && "message" in data) {
            data.message = safeMessage(data, null, response.status);
        }
        return { response, data };
    } finally {
        clearTimeout(timer);
        requests.delete(controller);
    }
}

function safeMessage(data, fallback, httpStatus) {
    const expected = {
        invalid_request: 400, invalid_decision: 400, unknown_trip: 404,
        decision_not_pending: 409, guardrail_violation: 422,
        planning_failed: 500, finalization_failed: 500, wait_interrupted: 503, planning_timeout: 504
    }[data?.error];
    const matches = expected && (httpStatus === undefined || httpStatus === expected
        || (httpStatus === 200 && isEnvelope(data) && data.status === "failed" && [422, 500].includes(expected)));
    return matches && typeof data?.message === "string" && data.message.trim() ? data.message.trim() : fallback;
}

function isEnvelope(data) {
    return data && statuses.has(data.status) && typeof data.requestId === "string" && data.requestId.length > 0;
}

function stopPolling() {
    clearTimeout(pollingHandle);
    pollingHandle = null;
}

function invalidateRequests() {
    generation++;
    stopPolling();
    for (const controller of requests) controller.abort();
    requests.clear();
    submitting = false;
    restoring = false;
}

async function restoreLatestPlan() {
    const token = generation;
    restoring = true;
    try {
        const { response, data } = await fetchJson("/trip/plan/latest");
        if (token !== generation || response.status === 204) return;
        if (!response.ok || !isEnvelope(data)) throw new Error("restore");
        currentTrip = data;
        restoreForm();
        showPage("resultsPage");
        renderTrip();
        if (isPending()) startPolling();
    } catch (error) {
        if (token === generation) {
            document.getElementById("formError").textContent = "Could not restore the latest trip. Refresh to check its status before starting another trip.";
        }
    } finally {
        if (token === generation) restoring = false;
    }
}

function restoreForm() {
    for (const key of ["destination", "startDate", "days", "travelers", "tripType", "budget", "preferences"]) {
        if (currentTrip.request?.[key] != null) document.getElementById(key).value = currentTrip.request[key];
    }
}

// Do not replace form edits with a late restore response.
document.getElementById("formPage").addEventListener("input", () => {
    if (restoring) invalidateRequests();
});
window.addEventListener("pagehide", invalidateRequests);
window.addEventListener("pageshow", event => {
    if (!event.persisted || !currentTrip) return;
    // The browser may restore the page from its back/forward cache after a submission was aborted.
    decisionUncertain = currentTrip.status === "awaiting_approval";
    if (currentTrip.requestId) startPolling();
    else {
        notice = "The planning response is no longer available. The workflow may still complete. Refresh to check the latest trip before retrying.";
        renderTrip();
    }
});
restoreLatestPlan();

function showPage(page) {
    document.getElementById("formPage").classList.remove("active");
    document.getElementById("resultsPage").classList.remove("active");
    document.getElementById(page).classList.add("active");
}

function goBackToForm() {
    invalidateRequests();
    decisionUncertain = false;
    currentTrip = null;
    showPage("formPage");
    document.getElementById("planBtn").disabled = false;
    document.getElementById("planBtn").textContent = "Generate Trip Plan";
}

async function planTrip() {
    invalidateRequests();
    const token = generation;
    decisionUncertain = false;
    notice = "";
    document.getElementById("formError").textContent = "";
    const btn = document.getElementById("planBtn");
    btn.disabled = true;
    btn.textContent = "Planning...";

    const request = {
        destination: document.getElementById("destination").value,
        startDate: document.getElementById("startDate").value,
        days: parseInt(document.getElementById("days").value, 10),
        tripType: document.getElementById("tripType").value,
        travelers: parseInt(document.getElementById("travelers").value, 10),
        budget: document.getElementById("budget").value,
        preferences: document.getElementById("preferences").value
    };

    currentTrip = { request, status: "planning" };
    showPage("resultsPage");
    renderTrip();

    try {
        const { response, data } = await fetchJson("/trip/plan", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(request)
        }, PLANNING_TIMEOUT);
        if (token !== generation) return;
        if (isEnvelope(data)) currentTrip = data;
        if (!response.ok || !isEnvelope(data)) {
            notice = safeMessage(data, "Could not generate the trip plan. Refresh to check its status before retrying.");
        }
        renderTrip();
        if (isEnvelope(data) && isPending()) startPolling();
    } catch (error) {
        if (token !== generation) return;
        notice = "Could not finish waiting for the planning response. The workflow may still complete. Refresh to check the latest trip before retrying.";
        renderTrip();
    }
}

function renderTrip() {
    const { instanceId, requestId, status, plan, confirmation } = currentTrip;
    const messages = {
        planning: "Planning your trip. Waiting for the backend planning result.",
        awaiting_approval: "The workflow is waiting for your decision.",
        decision_submitted: "Decision submitted. Waiting for the workflow to finish processing it.",
        confirmed: `Simulated booking confirmed. Booking reference: ${confirmation?.bookingReference || "N/A"}. No vehicle has been reserved.`,
        rejected: "Trip rejected. The workflow ended without finalizing a booking.",
        failed: safeMessage(currentTrip, "The backend reported that the trip could not be processed. Please try again later.")
    };
    const bannerClass = status === "confirmed" ? "status-confirmed"
        : ["rejected", "failed"].includes(status) ? "status-cancelled" : "status-awaiting";
    document.getElementById("results").innerHTML = `
        <div class="top-bar">
            <button class="btn-back" onclick="goBackToForm()">&#8592; Plan Another Trip</button>
        </div>
        <div class="workflow-id" id="workflowId"></div>
        <div class="request-id" id="requestId"></div>
        <div class="status-banner ${bannerClass}" id="tripStatus" role="status"></div>
        <div class="status-banner status-cancelled" id="planError" role="alert" hidden></div>
        <div class="action-bar">
            ${status === "awaiting_approval" ? `
                <button class="btn-approve" id="approveBtn" onclick="submitApproval('approved')" ${submitting || decisionUncertain ? "disabled" : ""}>Approve Trip</button>
                <button class="btn-reject" id="rejectBtn" onclick="submitApproval('rejected')" ${submitting || decisionUncertain ? "disabled" : ""}>Reject Trip</button>` : ""}
            ${notice && (instanceId || requestId) && status !== "failed" ? '<button class="btn-check" id="checkStatusBtn" onclick="startPolling()">Check Status</button>' : ""}
        </div>
        <div id="planContent"></div>`;
    document.getElementById("workflowId").textContent = `Workflow ID: ${instanceId || "Waiting for assignment"}`;
    document.getElementById("requestId").textContent = requestId ? `Request ID: ${requestId}` : "";
    document.getElementById("tripStatus").textContent = submitting ? "Submitting your decision..."
        : decisionUncertain && status === "awaiting_approval" ? "Decision status not yet verified. Check its status before submitting again."
        : notice && !requestId ? "Planning outcome not available." : messages[status];
    if (notice) {
        document.getElementById("planError").hidden = false;
        document.getElementById("planError").textContent = notice;
    }
    if (plan) renderPlan(plan);
}

function renderPlan(plan) {
    const currentRequest = currentTrip.request || {};
    const v = plan.vehicle || {};
    const costs = plan.costs || {};
    const itinerary = plan.itinerary || [];
    document.getElementById("planContent").innerHTML = `
        <div class="plan-header">
            <h2>${escapeHtml(currentRequest.destination)} &mdash; ${escapeHtml(currentRequest.days)}-Day ${escapeHtml(capitalize(currentRequest.tripType || ""))} Trip</h2>
            <div class="meta">${escapeHtml(currentRequest.travelers)} travelers &middot; ${escapeHtml(currentRequest.budget)}</div>
        </div>

        <div class="plan-section">
            <h3>&#x1F697; Vehicle Recommendation</h3>
            <div class="card">
                <strong>${escapeHtml(v.type)} &mdash; ${escapeHtml(v.model)}</strong>
                <p>${escapeHtml(v.reasoning)}</p>
            </div>
        </div>

        <div class="plan-section">
            <h3>&#x1F5FA;&#xFE0F; Route Overview</h3>
            <div class="card"><p>${escapeHtml(plan.routeOverview)}</p></div>
        </div>

        <div class="plan-section">
            <h3>&#x1F4C5; Daily Itinerary</h3>
            ${itinerary.map(day => `
                <div class="card day-card">
                    <div class="day-header"><span class="day-num">Day ${escapeHtml(day.day)}</span> <strong>${escapeHtml(day.title)}</strong></div>
                    <p>${escapeHtml(day.description)}</p>
                    ${day.overnightStop ? `<div class="overnight">&#x1F3E8; ${escapeHtml(day.overnightStop)}</div>` : ""}
                </div>
            `).join("")}
        </div>

        <div class="plan-section">
            <h3>&#x1F4B6; Estimated Costs</h3>
            <div class="card costs-grid">
                ${costs.vehiclePerDay ? `<div class="cost-item"><span>Vehicle/day</span><span>${escapeHtml(costs.vehiclePerDay)}</span></div>` : ""}
                ${costs.fuel ? `<div class="cost-item"><span>Fuel</span><span>${escapeHtml(costs.fuel)}</span></div>` : ""}
                ${costs.tolls ? `<div class="cost-item"><span>Tolls</span><span>${escapeHtml(costs.tolls)}</span></div>` : ""}
                ${costs.accommodation ? `<div class="cost-item"><span>Accommodation</span><span>${escapeHtml(costs.accommodation)}</span></div>` : ""}
                ${costs.food ? `<div class="cost-item"><span>Food</span><span>${escapeHtml(costs.food)}</span></div>` : ""}
                ${costs.activities ? `<div class="cost-item"><span>Activities</span><span>${escapeHtml(costs.activities)}</span></div>` : ""}
                ${costs.total ? `<div class="cost-item total"><span>Total</span><span>${escapeHtml(costs.total)}</span></div>` : ""}
            </div>
        </div>

    `;
}

async function submitApproval(status) {
    if (!currentTrip?.instanceId || currentTrip.status !== "awaiting_approval" || submitting || decisionUncertain) return;
    stopPolling();
    const token = generation;
    submitting = true;
    notice = "";
    renderTrip();
    try {
        const { response, data } = await fetchJson("/trip/approve", {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                instanceId: currentTrip.instanceId,
                status: status,
                feedback: ""
            })
        });
        if (token !== generation) return;
        submitting = false;
        const matched = acceptStatus(data);
        if (!response.ok || !matched) {
            decisionUncertain = true;
            notice = safeMessage(data, "Could not submit the decision. Check the trip status before trying again.");
            renderTrip();
            return;
        }
        renderTrip();
        if (isPending()) startPolling();
    } catch (error) {
        if (token !== generation) return;
        submitting = false;
        decisionUncertain = true;
        notice = "Could not finish waiting for the decision response. The workflow may still complete. Check the trip status before submitting again.";
        renderTrip();
    }
}

function isPending() {
    return currentTrip && ["planning", "decision_submitted"].includes(currentTrip.status);
}

function acceptStatus(data) {
    if (!isEnvelope(data) || data.requestId !== currentTrip.requestId
        || (currentTrip.instanceId && data.instanceId !== currentTrip.instanceId)) return false;
    currentTrip = { ...data, request: data.request || currentTrip.request, plan: data.plan || currentTrip.plan };
    return true;
}

function startPolling() {
    if (!currentTrip?.requestId || submitting) return;
    invalidateRequests();
    const token = generation;
    const deadline = Date.now() + POLLING_TIMEOUT;
    notice = currentTrip.error === "planning_timeout"
        ? safeMessage(currentTrip, "Planning is taking longer than expected. The workflow may still complete.") : "";
    renderTrip();
    // Sequential requests avoid overlapping reads; the deadline also bounds the last fetch.
    async function poll() {
        if (token !== generation) return;
        if (Date.now() >= deadline) {
            notice = "Stopped waiting for a status update. The workflow may still complete. Check its status again later; this does not cancel the trip.";
            renderTrip();
            return;
        }
        try {
            const query = currentTrip.instanceId ? `instanceId=${encodeURIComponent(currentTrip.instanceId)}`
                : `requestId=${encodeURIComponent(currentTrip.requestId)}`;
            const { response, data } = await fetchJson(`/trip/plan/status?${query}`, {}, Math.min(FETCH_TIMEOUT, deadline - Date.now()));
            if (token !== generation) return;
            if (!response.ok || !acceptStatus(data)) {
                notice = safeMessage(data, "Could not read the trip status. Check again later; the workflow may still complete.");
                renderTrip();
                return;
            }
            if (!isPending()) notice = "";
            decisionUncertain = false;
            renderTrip();
            if (!isPending()) return;
            pollingHandle = setTimeout(poll, Math.min(POLLING_INTERVAL, Math.max(0, deadline - Date.now())));
        } catch (error) {
            if (token !== generation) return;
            notice = "Could not finish waiting for a status update. The workflow may still complete. Check its status again later; this does not cancel the trip.";
            renderTrip();
        }
    }
    poll();
}

function escapeHtml(value) {
    return String(value ?? "").replace(/[&<>"']/g, char => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[char]);
}

function capitalize(s) {
    return s.charAt(0).toUpperCase() + s.slice(1);
}
