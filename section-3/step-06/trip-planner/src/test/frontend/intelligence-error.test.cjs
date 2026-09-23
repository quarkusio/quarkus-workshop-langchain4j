const { test } = require("node:test");
const assert = require("node:assert/strict");
const { readFile } = require("node:fs/promises");
const { resolve } = require("node:path");
const { chromium } = require("playwright");

test("MCP and quality failures render safely in the supplied UI", async () => {
    const browser = await chromium.launch({ headless: true, channel: process.env.BROWSER_CHANNEL || undefined });
    try {
        const page = await browser.newPage();
        const resources = resolve(__dirname, "../../main/resources/META-INF/resources");
        let failure;
        await page.route("http://workshop.test/**", async route => {
            const path = new URL(route.request().url()).pathname;
            if (path === "/trip/latest") return route.fulfill({ status: 204 });
            if (path === "/trip/plan") return route.fulfill({ status: failure.status,
                contentType: "application/json", body: JSON.stringify(failure.body) });
            const name = path === "/" ? "index.html" : path.substring(1);
            try {
                return route.fulfill({ body: await readFile(resolve(resources, name)),
                    contentType: name.endsWith("js") ? "application/javascript" : name.endsWith("css") ? "text/css" : "text/html" });
            } catch { return route.fulfill({ status: 404 }); }
        });
        for (const [code, status] of [["intelligence_unavailable", 502], ["quality_not_met", 422]]) {
            for (const httpStatus of [status, 200]) {
                failure = { status: httpStatus, body: { status: "failed", requestId: "test-request",
                    error: code, message: '<img src=x onerror="window.injected=true"> Controlled failure' } };
                await page.goto("http://workshop.test/");
                await page.locator("#destination").fill("Rome");
                await page.locator("#planBtn").click();
                await page.waitForFunction(() => document.querySelector("#results").textContent.includes("Controlled failure"));
                assert.equal(await page.locator("#results img").count(), 0);
                assert.equal(await page.evaluate(() => window.injected), undefined);
            }
        }
    } finally { await browser.close(); }
});
