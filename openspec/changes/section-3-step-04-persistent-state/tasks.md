## 1. Renumber Section 3 docs and nav

- [ ] 1.1 Rename `docs/docs/section-3/step-08.md` → `step-09.md` and update its `# Step 08` heading to `# Step 09`
- [ ] 1.2 Rename `docs/docs/section-3/step-07.md` → `step-08.md` and update its heading
- [ ] 1.3 Rename `docs/docs/section-3/step-06.md` → `step-07.md` and update its heading
- [ ] 1.4 Rename `docs/docs/section-3/step-05.md` → `step-06.md` and update its heading
- [ ] 1.5 Rename `docs/docs/section-3/step-04.md` → `step-05.md` and update its heading
- [ ] 1.6 Update `docs/mkdocs.yml` section-3 nav block to reference nine steps (01–09)
- [ ] 1.7 Update the forward-reference in `step-03.md` ("loop-oriented orchestration concepts" in step 04) to point at the new step-05 (Voting) not step-04 (Persistence)
- [ ] 1.8 Fix the `quarkus-flow.version` in `step-03.md` dependencies snippet from `0.13.0` to `1.0.0`
- [ ] 1.9 Fix the single-app step ranges in `docs/WORKSHOP-RESTRUCTURE-PROPOSAL.md` design constraints from "1-5 / 6-7" to "1-6 / 7-8"
- [ ] 1.10 Delete root `section-3-step-04-plan.md` (old Voting step plan)
- [ ] 1.11 Create `section-3/step-09/.gitkeep` so the new step directory exists

## 2. Add project directory and dependencies

- [ ] 2.0 Remove the chat/memory code currently on disk in `section-3/step-04/src` that the revised spec excludes: `DatabaseChatMemoryStore`, `ChatMessageEntity`, `TripChatAgent`, `TripChatResource`, and the chat-triggered `TripPlannerTools.planTrip(...)`. Revert `index.html` / `app.js` to Step 03 scaffolding (keep `CHAT_ENABLED = false`), leaving only the instance-ID display from section 4 as the step-specific addition
- [ ] 2.1 Copy `section-3/step-03/` to `section-3/step-04/` as the starting point (or scaffold from scratch) and verify it builds with `./mvnw clean package -DskipTests -pl section-3/step-04`
- [ ] 2.2 Add `quarkus-flow-jpa` and `quarkus-jdbc-postgresql` to `section-3/step-04/pom.xml`; verify no version property change is needed (both are in the existing BOM)
- [ ] 2.3 Confirm `quarkus-hibernate-orm-panache` arrives transitively (no explicit entry required) by checking the effective POM

## 3. Application configuration

- [ ] 3.1 Add `quarkus.datasource.db-kind=postgresql`, `quarkus.hibernate-orm.schema-management.strategy=update`, and `quarkus.flow.persistence.auto-restore=true` (with a comment that `true` is the default) to `src/main/resources/application.properties`
- [ ] 3.2 Create `src/test/resources/application.properties` with `quarkus.datasource.devservices.db-name=tripplanner_test`, `quarkus.datasource.devservices.reuse=false`, and `quarkus.hibernate-orm.schema-management.strategy=drop-and-create`
- [ ] 3.3 Add `"env": { "TESTCONTAINERS_REUSE_ENABLE": "true" }` to the project-root `devbox.json`
- [ ] 3.4 Create `.envrc` at the project root with `export TESTCONTAINERS_REUSE_ENABLE=true`

## 4. UI: display workflow instance ID

- [ ] 4.1 Confirm the `/trip/plan` response already includes `instanceId` (from Step 03) and that the frontend receives it in `app.js`
- [ ] 4.2 Display the workflow instance ID on the results page, below the approve/reject action bar, with a short label explaining it identifies the persisted workflow instance
- [ ] 4.3 Keep the scaffolding consistent with Steps 00–03: leave `CHAT_ENABLED = false` and the disabled chat section code untouched — the instance-ID display is the only step-specific UI addition
- [ ] 4.4 Confirm no second trip-planning path is introduced; the form remains the single way to create a plan

## 5. Code consistency check (Steps 00–04)

- [x] 5.1 Confirm `TripPlannerTools` in `step-00` uses `int` for numeric parameters (`getBudgetInfo(String, int)`); ensure any tool numeric parameters remain `int` for consistency across steps
- [x] 5.2 Verify `index.html` and `app.js` base scaffolding (form page, results page, approval polling, `CHAT_ENABLED=false` flag) are identical to Step 03 except for the instance-ID display
- [x] 5.3 Document in the step which files are unchanged scaffolding vs. the step-specific additions (persistence config + instance-ID display)

## 6. Tests

- [x] 6.1 Write a `@QuarkusTest` asserting Flow instance rows exist in the database while a workflow is suspended (proves persistence half)
- [x] 6.2 Attempt `FlowDurabilityRestoreTest` (`QuarkusDevModeTest`): trigger a reload via `modifyResourceFile`, send an approval event, assert workflow completes — fall back to asserting Flow instance rows exist while suspended if the combined test proves unstable, per design.md

## 7. Documentation (`docs/docs/section-3/step-04.md`)

- [x] 7.1 Write narrative opening and concept section (single focus: Flow state survives a restart)
- [x] 7.2 Write Prerequisites tabs (covering devbox / direnv / manual env var / live-reload fallback)
- [x] 7.3 Write Dependencies section with the two new POM entries and an explanation of why Panache is transitive
- [x] 7.4 Write the configuration section covering `application.properties` snippets and the test isolation block
- [x] 7.5 Write the UI section: the instance-ID display and how to use it to verify restoration
- [x] 7.6 Write the restart section: primary path (env var + Ctrl-C restart), verification check (note instance ID in UI → `Restoring workflow instance:` log → Dev UI row → approve completes), fallback (live reload), and cleanup note for lingering containers
- [x] 7.7 Write the "Going further" section: tease workflow refinement loops as an upcoming orchestration topic
- [x] 7.8 Verify the complete step runs end-to-end: start app, submit a plan (note the instance ID), restart, confirm the same instance ID in the restore log and Dev UI, approve, confirm completion
