const { test, before, after } = require("node:test");
const assert = require("node:assert/strict");
const { chromium } = require("playwright");

const baseURL = process.env.APP_URL || "http://localhost:8082";
const fallback = "Could not generate the trip plan. Please try again later.";
const rejection = "The trip plan could not pass the recommendation checks. Please revise your trip details and try again.";
let browser;

before(async () => {
    browser = await chromium.launch({ headless: process.env.SHOW_BROWSER !== "true",
        channel: process.env.BROWSER_CHANNEL || undefined });
});
after(async () => { await browser?.close(); });

for (const viewport of [{ width: 1280, height: 900 }, { width: 390, height: 844 }]) {
    test(`controlled corrections and failures at ${viewport.width}px`, async () => {
        const page = await browser.newPage({ viewport });
        const errors = [];
        page.on("pageerror", error => errors.push(error.message));
        await page.route("**/trip/plan/latest", route => route.fulfill({ status: 204 }));
        let reply;
        await page.route("**/trip/plan", route => reply === "network"
            ? route.abort("failed") : route.fulfill(reply));
        try {
            await page.goto(baseURL);
            await page.locator("#destination").fill("Italian Riviera");
            await page.locator("#days").fill("5");
            await page.locator("#travelers").fill("4");
            reply = { status: 200, json: {
                vehicle: {
                    type: "MPV",
                    model: "Family MPV; specific model subject to availability.",
                    reasoning: "Vehicle corrected by guardrail: original recommendation was too small for 4 travelers. Suggested category: MPV. Confirm seating, luggage capacity, price, and availability with the rental provider."
                }, itinerary: [], costs: {}
            } };
            await page.locator("#planBtn").click();
            const vehicle = page.locator(".plan-section").first().locator(".card");
            await vehicle.waitFor({ state: "visible" });
            assert.match(await vehicle.innerText(), /MPV.*Family MPV; specific model subject to availability\./);
            assert.match(await vehicle.innerText(), /Confirm seating, luggage capacity, price, and availability/);
            assert.doesNotMatch(await vehicle.innerText(), /Mazda|MX-5/);
            if (process.env.SHOW_BROWSER === "true") await page.waitForTimeout(2000);

            const markup = '<img src=x onerror="window.injected=true">';
            const cases = [
                [{ status: 422, json: { error: "guardrail_violation", message: rejection } }, rejection],
                [{ status: 500, json: { error: "planning_failed", message: fallback } }, fallback],
                [{ status: 422, json: { error: "guardrail_violation", message: markup } }, markup],
                [{ status: 500, contentType: "text/html", body: "<h1>private server details</h1>" }, fallback],
                [{ status: 422, body: "" }, fallback],
                [{ status: 422, json: { error: "guardrail_violation", message: "  " } }, fallback],
                [{ status: 422, json: { error: "guardrail_violation", message: {} } }, fallback],
                [{ status: 500, json: { error: "planning_failed" } }, fallback],
                [{ status: 500, json: null }, fallback],
                [{ status: 500, json: { error: "unknown", message: "private failure" } }, fallback],
                [{ status: 500, json: { error: "guardrail_violation", message: "wrong status" } }, fallback],
                ["network", fallback],
                [{ status: 200, body: "not json" }, fallback]
            ];
            for (const [response, expected] of cases) {
                await page.getByRole("button", { name: "Plan Another Trip" }).click();
                assert.equal(await page.locator("#planBtn").isEnabled(), true);
                reply = response;
                await page.locator("#planBtn").click();
                await page.locator("#planError").waitFor({ state: "visible" });
                assert.equal(await page.locator("#planError").innerText(), `Error: ${expected}`);
                assert.equal(await page.locator("#planError img").count(), 0);
                assert.equal(await page.evaluate(() => window.injected), undefined);
                assert.equal(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), true);
                if (process.env.SHOW_BROWSER === "true") await page.waitForTimeout(1500);
            }
            assert.deepEqual(errors, []);
        } finally {
            await page.close();
        }
    });
}
