# Code Consistency Audit: Section 3 Steps 00-04

Reference for keeping scaffolded code identical across steps. Decisions here are reflected in
`proposal.md`, `design.md`, and `tasks.md`.

## Baseline: What Should Be Identical

### UI Files (index.html + app.js)

Scaffolded identically across steps 00–03:
- `CHAT_ENABLED = false` flag at the top of `app.js`
- Chat UI code gated behind that flag (chat panel HTML, `sendChatMessage` function)
- Form-based planning UI (form page, results page with approve/reject)
- Workflow polling logic for approval/confirmation

Pattern: each step keeps the same base UI structure. Step-specific features are added behind
feature flags or as small, clearly-scoped additions.

### Tools Classes

- **Step 00:** `TripPlannerTools` with `getVehicleInfo(String vehicleType)` and
  `getBudgetInfo(String tripType, int days)` — numeric parameter is `int`.
- **Steps 01–03:** No Tools classes.

Type consistency: step-00 established that numeric tool parameters use `int`. Any tool added in
a later step follows the same convention.

## Decisions Applied to Step 04

### Dropped from Step 04
Step 04 is now scoped to **workflow persistence only**. The following are removed / not added:
- `DatabaseChatMemoryStore`, `ChatMessageEntity` (chat memory persistence — deferred to a later
  step if still valuable)
- `TripChatAgent`, `TripChatResource`, chat-triggered `TripPlannerTools.planTrip(...)`
- Tab navigation, separate chat page, `initializeChatWithPlan()`
- Any second trip-planning entry point

Reason: the existing approve/reject flow already fully demonstrates the persistence cycle
(suspend → persist → restart → restore → resume). Chat memory and plan refinement add
orchestration complexity without adding to the persistence lesson.

### Kept as baseline scaffolding (unchanged from step 03)
- `CHAT_ENABLED = false` and the disabled chat section code — left in place, untouched, so the
  scaffolding stays identical to steps 00–03
- Form page, results page, approval polling

### Added in Step 04 (step-specific)
- Persistence dependencies and configuration (`quarkus-flow-jpa`, `quarkus-jdbc-postgresql`,
  schema strategy, Testcontainers reuse, test isolation)
- Read-only **workflow instance ID display** on the results page, below the approve/reject
  action bar (the `instanceId` is already returned by `/trip/plan` from step 03)

## Carry-Forward Rule

When a later step needs a change to shared scaffolding (for example, flipping `CHAT_ENABLED` to
`true`, or changing a tool signature), that change must be carried forward consistently into all
subsequent steps. Additions belong only in the step that introduces them and later steps — never
retrofitted into earlier steps.

## Verification Checklist (for implementation)

- [ ] `step-04/app.js` differs from `step-03/app.js` only by the instance-ID display
- [ ] `step-04/index.html` differs from `step-03/index.html` only by the instance-ID display
- [ ] `CHAT_ENABLED = false` in step-04 (matches steps 00–03)
- [ ] No `TripChatAgent` / `TripChatResource` / chat memory classes in step-04
- [ ] Any tool numeric parameters use `int` (matches step-00 convention)
- [ ] The form is the only trip-planning path
