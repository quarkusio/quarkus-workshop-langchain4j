// Opt-in: LIVE_APP_URL=http://localhost:8080 LIVE_CAPTURE_DIR=/existing/directory
// Optional LIVE_DOCS_CAPTURE_DIR writes validated desktop screenshots for the chapter.
// node --test src/test/frontend/live.test.cjs (requires Playwright on NODE_PATH).
// This creates two real workflows and invokes the configured model.
const { test } = require("node:test");
const assert = require("node:assert/strict");
const { readFile, writeFile } = require("node:fs/promises");
const { join } = require("node:path");
const { chromium } = require("playwright");

function verifyKafka(evidence) {
    const events = Object.entries(evidence.kafka).flatMap(([topic, snapshot]) => snapshot.messages.map(record => ({
        topic, offset: record.offset, timestamp: record.timestamp, headers: record.headers, data: JSON.parse(record.value.value)
    })));
    return evidence.trips.map(trip => {
        const request = events.filter(event => event.topic === "flow-in" && event.headers.ce_id === trip.requestId);
        const decisions = events.filter(event => event.topic === "flow-in" && event.headers.ce_flowinstanceid === trip.instanceId);
        const outcomes = events.filter(event => event.topic === "flow-out" && event.headers.ce_flowinstanceid === trip.instanceId)
            .sort((a, b) => a.offset - b.offset);
        assert.equal(request.length, 1);
        assert.equal(request[0].headers.ce_type, "com.tripplanner.trip.requested");
        assert.deepEqual(request[0].data.request, trip.request);
        assert.equal(decisions.length, 1);
        assert.equal(decisions[0].headers.ce_type, "com.tripplanner.trip.approval.done");
        assert.equal(decisions[0].data.instanceId, trip.instanceId);
        assert.equal(decisions[0].data.status, trip.decision);
        const terminal = trip.decision === "approved" ? "com.tripplanner.booking.finalized" : "com.tripplanner.trip.rejected";
        assert.deepEqual(outcomes.map(event => event.headers.ce_type), ["com.tripplanner.trip.approval.requested", terminal]);
        for (const event of outcomes) {
            assert.equal(event.data.requestId, trip.requestId);
            assert.equal(event.data.instanceId, trip.instanceId);
            assert.deepEqual(event.data.request, trip.request);
        }
        const waitingEvents = trip.waitingKafka.messages.filter(record => record.headers.ce_flowinstanceid === trip.instanceId);
        assert.equal(waitingEvents.length, 1);
        assert.equal(waitingEvents[0].headers.ce_type, "com.tripplanner.trip.approval.requested");
        assert.equal(trip.planningResponseFinished, true);
        assert.equal(trip.waitingAfterHttpFinished.status, "awaiting_approval");
        assert.equal(trip.restored, true);
        assert.equal(trip.terminalRestored, true);
        if (trip.decision === "rejected") assert.equal(outcomes[1].data.confirmation, null);
        const chain = [request[0], outcomes[0], decisions[0], outcomes[1]];
        for (let index = 1; index < chain.length; index++) assert.ok(chain[index].timestamp >= chain[index - 1].timestamp);
        return { requestId: trip.requestId, instanceId: trip.instanceId, decision: trip.decision,
            bookingFinalizedEvents: outcomes.filter(event => event.headers.ce_type === "com.tripplanner.booking.finalized").length,
            events: chain.map(event => ({ type: event.headers.ce_type, topic: event.topic, offset: event.offset,
                taskId: event.headers.ce_flowtaskid, timestamp: event.timestamp })) };
    });
}

test("live restore, approval and rejection journey", { skip: !process.env.LIVE_APP_URL, timeout: 600000 }, async () => {
    const browser = await chromium.launch({ headless: true, channel: process.env.BROWSER_CHANNEL || undefined });
    const page = await browser.newPage({ viewport: { width: 1280, height: 900 } });
    const evidence = { appURL: process.env.LIVE_APP_URL, started: new Date().toISOString(), trips: [], captures: [],
        modelIds: [], toolCalls: [], pricingExecutions: [], diagnosticLimitations: [] };
    const errors = [];
    page.on("pageerror", error => errors.push(error.message));
    async function devTool(name, args = {}) {
        const response = await fetch(`${process.env.LIVE_APP_URL}/q/dev-mcp`, {
            method: "POST", signal: AbortSignal.timeout(10000),
            headers: { "Content-Type": "application/json", Accept: "application/json, text/event-stream" },
            body: JSON.stringify({ jsonrpc: "2.0", id: 1, method: "tools/call", params: { name, arguments: args } })
        });
        const data = await response.json();
        if (!response.ok || data.error || data.result?.isError) throw new Error(`Dev MCP ${name} unavailable`);
        return JSON.parse(data.result.content[0].text);
    }
    const seenLogs = new Set();
    const calls = new Map();
    const toolResults = new Set();
    async function collectLogs() {
        try {
            for (const line of await devTool("devui-logstream_logHistory")) {
                if (seenLogs.has(line)) continue;
                seenLogs.add(line);
                const calculation = line.match(/Rental calculation executed: category=(\w+), days=(\d+), total=(\d+) EUR/);
                if (calculation) evidence.pricingExecutions.push({ category: calculation[1], days: Number(calculation[2]), totalEUR: Number(calculation[3]) });
                let body;
                try { body = JSON.parse(line.slice(line.indexOf("{"))); } catch { continue; }
                if (typeof body.model === "string" && !evidence.modelIds.includes(body.model)) evidence.modelIds.push(body.model);
                const messages = [...(body.messages || []), ...(body.choices || []).map(choice => choice.message).filter(Boolean)];
                for (const message of messages) {
                    if (message.role === "tool") toolResults.add(message.tool_call_id);
                    for (const call of message.tool_calls || []) {
                        if (!["activate_skill", "estimateRental"].includes(call.function?.name)) continue;
                        let args;
                        try { args = JSON.parse(call.function.arguments); } catch { continue; }
                        // Persist selected arguments only, never request headers or full prompts.
                        calls.set(call.id, { id: call.id, name: call.function.name, arguments: call.function.name === "estimateRental"
                            ? { category: args.category, days: args.days } : args });
                    }
                }
            }
        } catch {
            if (!evidence.diagnosticLimitations.includes("Log history unavailable")) evidence.diagnosticLimitations.push("Log history unavailable");
        }
    }
    let collecting = true;
    const logCollector = (async () => {
        while (collecting) {
            await collectLogs();
            if (collecting) await new Promise(resolve => setTimeout(resolve, 1500));
        }
    })();
    async function capture(label) {
        if (!process.env.LIVE_CAPTURE_DIR) return;
        for (const width of [1280, 390]) {
            await page.setViewportSize({ width, height: 900 });
            assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), true);
            const path = join(process.env.LIVE_CAPTURE_DIR, `step03-live-${label}-${width}.png`);
            await page.screenshot({ path, fullPage: true });
            evidence.captures.push(path);
            const docsLabel = { "awaiting-approved": "awaiting-approval", confirmed: "confirmed", rejected: "rejected" }[label];
            if (width === 1280 && docsLabel && process.env.LIVE_DOCS_CAPTURE_DIR) {
                const docsPath = join(process.env.LIVE_DOCS_CAPTURE_DIR, `section-3-step-03-${docsLabel}.png`);
                await page.screenshot({ path: docsPath, fullPage: true });
                evidence.captures.push(docsPath);
            }
        }
        await page.setViewportSize({ width: 1280, height: 900 });
    }
    try {
        await page.goto(process.env.LIVE_APP_URL);
        await page.waitForFunction(() => !restoring);
        for (const decision of ["approved", "rejected"]) {
            if (await page.locator("#resultsPage").isVisible()) await page.getByRole("button", { name: "Plan Another Trip" }).click();
            const inputs = { destination: "Italian Riviera", startDate: "2026-10-12", days: "7", travelers: "4",
                preferences: "Coastal towns and good food, with short driving days and time to rest." };
            for (const [key, value] of Object.entries(inputs)) await page.locator(`#${key}`).fill(value);
            await page.locator("#tripType").selectOption("family");
            await page.locator("#budget").selectOption({ index: 2 });
            const planned = page.waitForResponse(response => response.url().endsWith("/trip/plan")
                && response.request().method() === "POST", { timeout: 145000 });
            await page.locator("#planBtn").click();
            const response = await planned;
            const data = await response.json();
            assert.equal(await response.finished(), null);
            const trip = { requestId: data.requestId, instanceId: data.instanceId, planningHttpStatus: response.status(),
                planningResponseFinished: true, initialStatus: data.status, request: data.request, decision, states: [data.status] };
            evidence.trips.push(trip);
            assert.equal(response.status(), 200, data.message || "Planning did not return success");
            assert.equal(data.status, "awaiting_approval");
            const waitingResponse = await page.request.get(`${process.env.LIVE_APP_URL}/trip/plan/status?instanceId=${encodeURIComponent(data.instanceId)}`, { timeout: 15000 });
            const waiting = await waitingResponse.json();
            assert.equal(waiting.status, "awaiting_approval");
            assert.equal(waiting.requestId, data.requestId);
            trip.waitingAfterHttpFinished = { requestId: waiting.requestId, instanceId: waiting.instanceId, status: waiting.status };
            try { trip.waitingKafka = await devTool("quarkus-kafka-client_topicMessages", { topicName: "flow-out" }); }
            catch { evidence.diagnosticLimitations.push("Waiting Kafka snapshot unavailable"); }
            await page.locator("#approveBtn").waitFor();
            await capture(`awaiting-${decision}`);
            await page.reload();
            await page.locator("#approveBtn").waitFor();
            assert.equal(await page.locator("#workflowId").innerText(), `Workflow ID: ${data.instanceId}`);
            for (const [key, value] of Object.entries(data.request)) assert.equal(await page.locator(`#${key}`).inputValue(), String(value));
            trip.restored = true;
            const accepted = page.waitForResponse(response => response.url().endsWith("/trip/approve"));
            await page.locator(decision === "approved" ? "#approveBtn" : "#rejectBtn").click();
            const decisionResponse = await accepted;
            assert.equal(decisionResponse.status(), 202);
            const submitted = await decisionResponse.json();
            assert.equal(submitted.instanceId, data.instanceId);
            assert.equal(submitted.requestId, data.requestId);
            assert.equal(submitted.status, "decision_submitted");
            trip.states.push(submitted.status);
            await page.waitForFunction(() => ["confirmed", "rejected", "failed"].includes(currentTrip?.status), null, { timeout: 130000 });
            const final = await page.evaluate(() => currentTrip);
            trip.states.push(final.status);
            assert.equal(final.status, decision === "approved" ? "confirmed" : "rejected", final.message || "Unexpected final result");
            assert.equal(final.instanceId, data.instanceId);
            assert.equal(final.requestId, data.requestId);
            trip.confirmation = final.confirmation;
            if (decision === "rejected") assert.equal(final.confirmation, null);
            if (decision === "approved") assert.match(await page.locator("#tripStatus").innerText(), /Simulated booking.*No vehicle has been reserved/);
            await page.reload();
            await page.waitForFunction(expected => currentTrip?.status === expected, final.status);
            assert.equal(await page.locator("#approveBtn").count(), 0);
            assert.equal(await page.locator("#workflowId").innerText(), `Workflow ID: ${data.instanceId}`);
            trip.terminalRestored = true;
            await capture(final.status);
        }
        try {
            evidence.kafka = {};
            for (const topicName of ["flow-in", "flow-out"]) evidence.kafka[topicName] = await devTool("quarkus-kafka-client_topicMessages", { topicName });
        } catch { evidence.diagnosticLimitations.push("Final Kafka snapshot unavailable"); }
        if (evidence.kafka?.["flow-in"] && evidence.kafka?.["flow-out"]) evidence.verifiedKafkaTraces = verifyKafka(evidence);
        assert.deepEqual(errors, []);
        evidence.passed = true;
    } catch (error) {
        evidence.passed = false;
        evidence.failure = error.message;
        await capture("incomplete");
        throw error;
    } finally {
        collecting = false;
        await logCollector;
        await collectLogs();
        evidence.toolCalls = [...calls.values()].map(call => ({ ...call, resultObserved: toolResults.has(call.id) }));
        evidence.finished = new Date().toISOString();
        if (process.env.LIVE_CAPTURE_DIR) await writeFile(join(process.env.LIVE_CAPTURE_DIR, "step03-live-evidence.json"), JSON.stringify(evidence, null, 2));
        await browser.close();
    }
});

// Recheck captured live evidence without creating more workflows or invoking the model.
test("saved live Kafka evidence and current Dev UI pricing traces", {
    skip: !process.env.LIVE_EVIDENCE_FILE, timeout: 45000
}, async () => {
    const evidence = JSON.parse(await readFile(process.env.LIVE_EVIDENCE_FILE, "utf8"));
    assert.equal(evidence.passed, true);
    const verification = { verifiedAt: new Date().toISOString(), source: process.env.LIVE_EVIDENCE_FILE,
        kafkaTraces: verifyKafka(evidence), diagnosticLimitations: [] };
    const browser = await chromium.launch({ headless: true, channel: process.env.BROWSER_CHANNEL || undefined });
    try {
        const page = await browser.newPage();
        await page.goto(`${evidence.appURL}/q/dev-ui/quarkus-langchain4j-agentic/executions`, { timeout: 15000 });
        const body = page.frameLocator("iframe").locator("body");
        await body.filter({ hasText: "estimateRental" }).waitFor({ timeout: 10000 });
        const report = await body.textContent();
        verification.skillActivations = [...report.matchAll(/Tool activate_skill( failed)?[\d.]+m?s(\{[^}]+\})/g)]
            .map(match => ({ failed: Boolean(match[1]), arguments: JSON.parse(match[2]) }));
        verification.rentalToolResults = [...report.matchAll(/Tool estimateRental[\s\S]*?(\{\s*"category"\s*:[^{}]+"rentalTotal"\s*:[^{}]+\})/g)]
            .map(match => JSON.parse(match[1]));
        assert.equal(verification.rentalToolResults.length, 2);
        for (const result of verification.rentalToolResults) assert.deepEqual(result,
            { category: "suv", days: 7, currency: "EUR", dailyRate: 80, rentalTotal: 560 });
        assert.ok(verification.skillActivations.some(call => !call.failed && call.arguments.skill_name === "family-trip"));
        assert.ok(verification.skillActivations.some(call => !call.failed && call.arguments.skill_name === "vehicle-selection"));
        const response = await page.request.post(`${evidence.appURL}/q/dev-mcp`, {
            timeout: 10000, headers: { Accept: "application/json, text/event-stream" },
            data: { jsonrpc: "2.0", id: 1, method: "tools/call", params: { name: "devui-configuration_searchConfig",
                arguments: { query: "chat-model.model-name", extension: "langchain4j", limit: 5 } } }
        });
        const configs = JSON.parse((await response.json()).result.content[0].text);
        verification.configuredModel = configs.find(config => config.name === "quarkus.langchain4j.openai.chat-model.model-name").currentValue;
        verification.diagnosticLimitations.push("Dev MCP log history returned only pre-run startup entries; tool results were verified in the Dev UI execution report.");
        verification.passed = true;
        if (process.env.LIVE_CAPTURE_DIR) await writeFile(join(process.env.LIVE_CAPTURE_DIR, "step03-live-verification.json"), JSON.stringify(verification, null, 2));
    } finally {
        await browser.close();
    }
});
