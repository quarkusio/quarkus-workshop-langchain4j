// Run with Playwright on NODE_PATH: node --test src/test/frontend/app.test.cjs
// API responses are controlled; the actual supplied HTML/JS runs in Chromium.
const { test, before, after } = require("node:test");
const assert = require("node:assert/strict");
const { readFile } = require("node:fs/promises");
const { createServer } = require("node:http");
const { resolve } = require("node:path");
const { chromium } = require("playwright");

const resources = resolve(__dirname, "../../main/resources/META-INF/resources");
const request = { destination: "Italian Riviera", startDate: "2026-10-12", days: 7,
    travelers: 4, tripType: "family", budget: "moderate (\u20ac1000-\u20ac2500)", preferences: "Coastal towns and good food" };
const plan = {
    vehicle: { type: "MPV", model: "Family MPV; specific model subject to availability.",
        reasoning: "Vehicle corrected by guardrail. Confirm seating, luggage capacity, price, and availability with the rental provider." },
    routeOverview: "Coastal towns with short driving days",
    itinerary: [{ day: 1, title: "Arrival", description: "Explore on foot", overnightStop: "Genoa" }],
    costs: { vehiclePerDay: "EUR 90", total: "EUR 1600" }
};
const envelope = (status = "awaiting_approval", extra = {}) => ({
    requestId: "request-123", instanceId: "workflow-123", request, status, plan,
    confirmation: status === "confirmed" ? { bookingReference: "SIM-123" } : null,
    error: null, message: null, ...extra
});
let browser, server, baseURL;

before(async () => {
    server = createServer(async (req, res) => {
        if (req.headers["x-test-hang-body"]) {
            res.writeHead(200, { "Content-Type": "application/json" });
            res.write('{"requestId":');
            return; // Deliberately leave the body open to exercise the fetch deadline.
        }
        const name = req.url === "/" ? "index.html" : req.url === "/app.js" ? "app.js" : null;
        if (!name) { res.writeHead(404); res.end(); return; }
        res.setHeader("Content-Type", name.endsWith(".js") ? "text/javascript" : "text/html");
        res.end(await readFile(resolve(resources, name)));
    });
    await new Promise(done => server.listen(0, "127.0.0.1", done));
    baseURL = `http://127.0.0.1:${server.address().port}`;
    browser = await chromium.launch({ headless: true, channel: process.env.BROWSER_CHANNEL || undefined });
});
after(async () => {
    await browser?.close();
    server?.closeAllConnections();
    await new Promise(done => server ? server.close(done) : done());
});

async function setup(t, latest = envelope(), viewport = { width: 1280, height: 900 }) {
    const page = await browser.newPage({ viewport });
    const errors = [];
    page.on("pageerror", error => errors.push(error.message));
    t.after(async () => { await page.close(); assert.deepEqual(errors, []); });
    await page.clock.install();
    await page.clock.pauseAt(new Date(Date.now() + 1000));
    const api = { latest, status: latest, reads: [], decisions: [], posts: [], decision: null, post: null };
    await page.route("**/trip/**", async route => {
        const req = route.request();
        const url = new URL(req.url());
        let reply;
        if (url.pathname === "/trip/plan/latest") reply = api.latest == null ? { status: 204 } : { json: api.latest };
        if (url.pathname === "/trip/plan/status") { api.reads.push(url.search); reply = { json: api.status }; }
        if (url.pathname === "/trip/approve") { api.decisions.push(req.postDataJSON()); reply = api.decision; }
        if (url.pathname === "/trip/plan") { api.posts.push(req.postDataJSON()); reply = api.post; }
        if (typeof reply === "function") return reply(route);
        await route.fulfill(reply || { status: 500 });
    });
    await page.goto(baseURL);
    await page.waitForFunction(() => !restoring);
    return { page, api };
}

async function statusIs(page, text) {
    await page.waitForFunction(text => document.getElementById("tripStatus")?.textContent.includes(text), text);
}

async function hasNotice(page, pattern) {
    await page.locator("#planError").waitFor({ state: "visible" });
    assert.match(await page.locator("#planError").innerText(), pattern);
}

async function retainsPlan(page) {
    assert.match(await page.locator("#planContent").innerText(), /Family MPV; specific model subject to availability/);
}

for (const width of [1280, 390]) {
    test(`restore original inputs and complete both decisions at ${width}px`, async t => {
        const { page, api } = await setup(t, envelope(), { width, height: 900 });
        for (const decision of ["approved", "rejected"]) {
            api.latest = envelope();
            await page.reload();
            await statusIs(page, "waiting for your decision");
            assert.equal(await page.locator("#workflowId").innerText(), "Workflow ID: workflow-123");
            for (const [key, value] of Object.entries(request)) assert.equal(await page.locator(`#${key}`).inputValue(), String(value));
            await retainsPlan(page);
            let release;
            api.decision = route => new Promise(resolve => { release = async () => {
                await route.fulfill({ status: 202, json: envelope("decision_submitted") }); resolve();
            }; });
            api.status = envelope("decision_submitted");
            await page.locator(decision === "approved" ? "#approveBtn" : "#rejectBtn").click();
            await statusIs(page, "Submitting your decision");
            assert.equal(await page.locator("#approveBtn").isDisabled(), true);
            assert.equal(await page.locator("#rejectBtn").isDisabled(), true);
            await page.waitForFunction(() => submitting);
            assert.deepEqual(api.decisions.at(-1), { instanceId: "workflow-123", status: decision, feedback: "" });
            await release();
            await statusIs(page, "Decision submitted");
            await page.waitForFunction(() => pollingHandle !== null);
            assert.doesNotMatch(await page.locator("#tripStatus").innerText(), /confirmed|Trip rejected/);
            const terminal = decision === "approved" ? "confirmed" : "rejected";
            api.status = envelope(terminal);
            await page.clock.runFor(2000);
            await statusIs(page, terminal === "confirmed" ? "Simulated booking confirmed" : "Trip rejected");
            await retainsPlan(page);
            assert.ok(api.reads.every(query => query === "?instanceId=workflow-123"));
            const count = api.reads.length;
            await page.clock.runFor(10000);
            assert.equal(api.reads.length, count);
            api.latest = api.status;
            await page.reload();
            await statusIs(page, terminal === "confirmed" ? "No vehicle has been reserved" : "Trip rejected");
            assert.equal(await page.locator("#approveBtn").count(), 0);
            assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), true);
            const bounds = await page.locator("#workflowId").boundingBox();
            assert.ok(bounds.x >= 0 && bounds.x + bounds.width <= width);
        }
    });
}

for (const state of ["planning", "awaiting_approval", "decision_submitted", "confirmed", "rejected", "failed"]) {
    test(`latest restores ${state}`, async t => {
        const latest = envelope(state, state === "planning" ? { plan: null, instanceId: null }
            : state === "failed" ? { error: "planning_failed", message: "The trip could not be processed." } : {});
        const { page, api } = await setup(t, latest);
        await page.locator("#tripStatus").waitFor();
        assert.equal(await page.evaluate(() => currentTrip.status), state);
        assert.equal(await page.locator("#requestId").innerText(), "Request ID: request-123");
        if (state === "planning" || state === "decision_submitted") {
            await page.waitForFunction(() => pollingHandle !== null);
            assert.equal(api.reads[0], state === "planning" ? "?requestId=request-123" : "?instanceId=workflow-123");
            api.status = envelope("failed", { error: "planning_failed", message: "Controlled workflow failure." });
            await page.clock.runFor(2000);
            await statusIs(page, "Controlled workflow failure.");
        } else assert.equal(api.reads.length, 0);
    });
}

test("204 is empty, planning uses envelope and preserves the submitted original request", async t => {
    const { page, api } = await setup(t, null);
    assert.equal(await page.locator("#formPage").isVisible(), true);
    await page.locator("#destination").fill(request.destination);
    api.post = { json: envelope() };
    await page.locator("#planBtn").click();
    await statusIs(page, "waiting for your decision");
    assert.equal(api.posts[0].destination, request.destination);
    assert.equal(api.posts[0].preferences, "");
    await retainsPlan(page);
    await page.getByRole("button", { name: "Plan Another Trip" }).click();
    assert.equal(await page.locator("#planBtn").isEnabled(), true);
});

test("planning HTTP errors render safe text or fallback, including 504 correlation recovery", async t => {
    const { page, api } = await setup(t, null);
    const markup = '<img src=x onerror="window.injected=true">';
    for (const reply of [
        { status: 422, json: envelope("failed", { plan: null, error: "quality_not_met", message: "Safe server message" }) },
        { status: 422, json: envelope("failed", { plan: null, error: "guardrail_violation", message: markup }) },
        { status: 500, json: envelope("failed", { plan: null, error: "planning_failed", message: "Safe server message" }) },
        { status: 500, json: { error: "unknown", message: "private failure" } },
        { status: 500, json: { error: "guardrail_violation", message: "wrong status" } },
        { status: 500, json: envelope("failed", { error: "guardrail_violation", message: "wrong status" }) },
        { status: 500, contentType: "text/html", body: "private server details" },
        { status: 422, json: { message: {} } },
        { status: 422, json: { message: "  " } },
        { status: 500, json: null },
        { status: 500, body: "" },
        { status: 200, body: "not JSON" }
    ]) {
        api.post = reply;
        await page.locator("#planBtn").click();
        await hasNotice(page, /img|Safe server message|Could not generate the trip plan/);
        assert.equal(await page.locator("#planError img").count(), 0);
        assert.equal(await page.evaluate(() => window.injected), undefined);
        assert.doesNotMatch(await page.locator("#results").innerText(), /private server details|private failure|wrong status/);
        await page.getByRole("button", { name: "Plan Another Trip" }).click();
    }
    api.post = { status: 504, json: envelope("planning", { plan: null, instanceId: null,
        error: "planning_timeout", message: "Planning is taking longer than expected. The workflow may still complete." }) };
    api.status = envelope("planning", { plan: null, instanceId: null });
    await page.locator("#planBtn").click();
    await page.waitForFunction(() => pollingHandle !== null);
    assert.equal(api.reads.at(-1), "?requestId=request-123");
    api.status = envelope();
    await page.clock.runFor(2000);
    await statusIs(page, "waiting for your decision");
    assert.equal(await page.locator("#workflowId").innerText(), "Workflow ID: workflow-123");
});

test("submission errors preserve the plan and require a status check before resubmission", async t => {
    const { page, api } = await setup(t);
    for (const [code, error] of [[400, "invalid_decision"], [404, "unknown_trip"],
        [409, "decision_not_pending"], [500, "finalization_failed"]]) {
        api.decision = { status: code, json: { error, message: `Safe decision error ${code}` } };
        await page.locator("#approveBtn").click();
        await hasNotice(page, new RegExp(`Safe decision error ${code}`));
        await retainsPlan(page);
        assert.equal(await page.locator("#approveBtn").isDisabled(), true);
        await page.locator("#checkStatusBtn").click();
        await page.waitForFunction(() => !decisionUncertain);
        assert.equal(await page.locator("#approveBtn").isEnabled(), true);
    }
    api.decision = { status: 500, json: envelope("failed", { error: "finalization_failed", message: "Decision publication failed." }) };
    await page.locator("#rejectBtn").click();
    await statusIs(page, "Decision publication failed.");
    await retainsPlan(page);
});

test("failed status is HTTP 200, with safe fallback, and preserves the reviewed plan", async t => {
    const { page, api } = await setup(t);
    api.decision = { status: 202, json: envelope("decision_submitted") };
    api.status = envelope("failed", { plan: null, error: "finalization_failed", message: {} });
    await page.locator("#approveBtn").click();
    await statusIs(page, "backend reported");
    await retainsPlan(page);
    assert.equal(await page.locator("#approveBtn").count(), 0);
});

test("status read errors preserve the plan and render messages as text", async t => {
    const { page, api } = await setup(t);
    api.decision = { status: 202, json: envelope("decision_submitted") };
    const markup = '<img src=x onerror="window.injected=true">';
    let reply = { status: 404, json: { error: "unknown_trip", message: markup } };
    await page.route("**/trip/plan/status?*", route => reply === "network" ? route.abort("failed") : route.fulfill(reply));
    await page.locator("#rejectBtn").click();
    await hasNotice(page, /img/);
    assert.equal(await page.locator("#planError img").count(), 0);
    assert.equal(await page.evaluate(() => window.injected), undefined);
    for (const response of [{ status: 404, body: "private details" }, { status: 204 }, "network"]) {
        reply = response;
        await page.locator("#checkStatusBtn").click();
        await hasNotice(page, /Could not.*status/);
        await retainsPlan(page);
        assert.equal(await page.evaluate(() => currentTrip.status), "decision_submitted");
    }
});

test("returning from browser history rechecks an interrupted submission", async t => {
    const { page, api } = await setup(t);
    api.decision = () => new Promise(() => {});
    await page.locator("#approveBtn").click();
    await statusIs(page, "Submitting your decision");
    api.status = envelope("confirmed");
    await page.evaluate(() => {
        window.dispatchEvent(new PageTransitionEvent("pagehide", { persisted: true }));
        window.dispatchEvent(new PageTransitionEvent("pageshow", { persisted: true }));
    });
    await statusIs(page, "Simulated booking confirmed");
    await retainsPlan(page);
});

test("polling has a fixed deadline, preserves the plan, and can resume checking", async t => {
    const { page, api } = await setup(t, envelope("decision_submitted"));
    await page.waitForFunction(() => pollingHandle !== null);
    // Move past the deadline without issuing 60 real-time requests.
    await page.clock.fastForward(120001);
    await hasNotice(page, /Stopped waiting.*may still complete.*does not cancel/);
    await retainsPlan(page);
    assert.equal(await page.evaluate(() => currentTrip.status), "decision_submitted");
    const count = api.reads.length;
    await page.clock.fastForward(120001);
    assert.equal(api.reads.length, count);
    api.status = envelope("rejected");
    await page.locator("#checkStatusBtn").click();
    await statusIs(page, "Trip rejected");
});

for (const operation of ["restore", "plan", "decision", "status", "body"]) {
    test(`${operation} network wait is bounded`, { timeout: 10000 }, async t => {
        const { page, api } = await setup(t, operation === "plan" || operation === "restore" || operation === "body" ? null : envelope());
        let received;
        const requestStarted = new Promise(resolve => { received = resolve; });
        const hang = route => { received(); return new Promise(() => {}); };
        if (operation === "restore" || operation === "body") {
            await page.route("**/trip/plan/latest", operation === "body" ? route => {
                received();
                return route.continue({ headers: { ...route.request().headers(), "x-test-hang-body": "1" } });
            } : hang);
            await page.reload();
        } else if (operation === "plan") {
            api.post = hang;
            await page.locator("#planBtn").click();
        } else if (operation === "decision") {
            api.decision = hang;
            await page.locator("#rejectBtn").click();
        } else {
            await page.route("**/trip/plan/status?*", hang);
            api.decision = { status: 202, json: envelope("decision_submitted") };
            await page.locator("#approveBtn").click();
        }
        await requestStarted;
        await page.clock.fastForward(operation === "plan" ? 135001 : 15001);
        if (operation === "restore" || operation === "body") {
            await page.waitForFunction(() => document.getElementById("formError").textContent.includes("Could not restore"));
        } else {
            await hasNotice(page, /may still complete/);
            if (operation !== "plan") await retainsPlan(page);
            assert.notEqual(await page.evaluate(() => currentTrip.status), "failed");
            assert.notEqual(await page.evaluate(() => currentTrip.status), "rejected");
        }
    });
}

test("unrelated correlated status cannot replace the displayed trip", async t => {
    const { page, api } = await setup(t, envelope("decision_submitted"));
    await page.waitForFunction(() => pollingHandle !== null);
    api.status = envelope("confirmed", { requestId: "other-request", instanceId: "other-workflow" });
    await page.clock.runFor(2000);
    await hasNotice(page, /Could not read the trip status/);
    assert.equal(await page.locator("#workflowId").innerText(), "Workflow ID: workflow-123");
    assert.equal(await page.evaluate(() => currentTrip.status), "decision_submitted");
});

for (const operation of ["restore", "plan", "decision", "status"]) {
    test(`late ${operation} response cannot replace a new trip`, async t => {
        const { page, api } = await setup(t, operation === "restore" || operation === "plan" ? null : envelope());
        // Deliberately ignore aborts: the generation guard must still reject the response.
        await page.evaluate(operation => {
            const original = window.fetch;
            const path = { restore: "/trip/plan/latest", plan: "/trip/plan", decision: "/trip/approve", status: "/trip/plan/status" }[operation];
            window.fetch = (url, options) => String(url).split("?")[0] === path
                ? new Promise(resolve => { window.releaseOld = data => resolve(new Response(JSON.stringify(data), { status: 200 })); })
                : original(url, options);
            window.restoreFetch = () => { window.fetch = original; };
        }, operation);
        if (operation === "restore") await page.evaluate(() => { restoreLatestPlan(); });
        else if (operation === "plan") await page.locator("#planBtn").click();
        else if (operation === "decision") await page.locator("#approveBtn").click();
        else {
            api.decision = { status: 202, json: envelope("decision_submitted") };
            await page.locator("#approveBtn").click();
        }
        await page.waitForFunction(() => typeof window.releaseOld === "function");
        if (operation === "restore") await page.locator("#destination").fill("New trip");
        else await page.getByRole("button", { name: "Plan Another Trip" }).click();
        await page.evaluate(() => window.restoreFetch());
        api.post = { json: envelope("awaiting_approval", { requestId: "new-request", instanceId: "new-workflow", request: { ...request, destination: "New trip" } }) };
        await page.locator("#planBtn").click();
        await statusIs(page, "waiting for your decision");
        await page.evaluate(data => window.releaseOld(data), envelope("confirmed"));
        assert.equal(await page.locator("#workflowId").innerText(), "Workflow ID: new-workflow");
        assert.match(await page.locator(".plan-header").innerText(), /New trip/);
        assert.equal(await page.evaluate(() => currentTrip.status), "awaiting_approval");
    });
}
