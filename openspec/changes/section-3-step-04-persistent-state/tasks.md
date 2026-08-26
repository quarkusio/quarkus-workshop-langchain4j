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

- [ ] 2.1 Copy `section-3/step-03/` to `section-3/step-04/` as the starting point (or scaffold from scratch) and verify it builds with `./mvnw clean package -DskipTests -pl section-3/step-04`
- [ ] 2.2 Add `quarkus-flow-jpa` and `quarkus-jdbc-postgresql` to `section-3/step-04/pom.xml`; verify no version property change is needed (both are in the existing BOM)
- [ ] 2.3 Confirm `quarkus-hibernate-orm-panache` arrives transitively (no explicit entry required) by checking the effective POM

## 3. Application configuration

- [ ] 3.1 Add `quarkus.datasource.db-kind=postgresql`, `quarkus.hibernate-orm.schema-management.strategy=update`, and `quarkus.flow.persistence.auto-restore=true` (with a comment that `true` is the default) to `src/main/resources/application.properties`
- [ ] 3.2 Create `src/test/resources/application.properties` with `quarkus.datasource.devservices.db-name=tripplanner_test`, `quarkus.datasource.devservices.reuse=false`, and `quarkus.hibernate-orm.schema-management.strategy=drop-and-create`
- [ ] 3.3 Add `"env": { "TESTCONTAINERS_REUSE_ENABLE": "true" }` to the project-root `devbox.json`
- [ ] 3.4 Create `.envrc` at the project root with `export TESTCONTAINERS_REUSE_ENABLE=true`

## 4. Starter code

- [ ] 4.1 Create `ChatMessageEntity` extending `PanacheEntity` with `memoryId` (String), `sequence` (int), and `message` (TEXT column); verify Hibernate creates its table on first boot
- [ ] 4.2 Create `TripChatAgent` — `@RegisterAiService(tools = TripPlannerTools.class)` annotated `@ApplicationScoped` (not the default `@RequestScoped`); add a note-box reminder in the docs about scope
- [ ] 4.3 Create `TripPlannerTools` bridging `TripChatAgent` to `TripPlannerFlowAdapter`
- [ ] 4.4 Create `TripChatResource` at `/trip/chat` accepting a session id and message body
- [ ] 4.5 Add a chat panel to `src/main/resources/META-INF/resources/index.html` alongside the existing plan form and results pages

## 5. Participant code (`DatabaseChatMemoryStore`)

- [ ] 5.1 Verify exact method names (`ChatMessageSerializer.serialize` / `ChatMessageDeserializer.deserialize`) against langchain4j 1.13.0 JAR before writing
- [ ] 5.2 Create a starter shell of `DatabaseChatMemoryStore` (class declaration, imports, empty method stubs with `@Transactional`) so participants fill in logic only
- [ ] 5.3 Implement `getMessages`: query `ChatMessageEntity` by `memoryId` ordered by `sequence`, deserialize each row's payload, and return the list
- [ ] 5.4 Implement `updateMessages`: delete all rows for the `memoryId`, then reinsert the full list with incrementing `sequence` values
- [ ] 5.5 Implement `deleteMessages`: delete all rows for the `memoryId`

## 6. Tests

- [ ] 6.1 Write `DatabaseChatMemoryStoreTest` (`@QuarkusTest`): round-trip store/read/delete, ordering preserved across `sequence`, two `memoryId` values isolated, tool-execution message survives serialization
- [ ] 6.2 Write `TripChatAgentMemoryTest` (`@QuarkusTest`): after one `/trip/chat` call, assert that `ChatMessageEntity` rows exist for that session id (assert on persistence, not model output)
- [ ] 6.3 Attempt `FlowDurabilityRestoreTest` (`QuarkusDevModeTest`): trigger a reload via `modifyResourceFile`, send an approval event, assert workflow completes — fall back to asserting Flow instance rows exist while suspended if the combined test proves unstable, per design.md

## 7. Documentation (`docs/docs/section-3/step-04.md`)

- [ ] 7.1 Write narrative opening and concept section (two persistence concerns: chat memory + Flow state)
- [ ] 7.2 Write Prerequisites tabs (covering devbox / direnv / manual env var / live-reload fallback)
- [ ] 7.3 Write Dependencies section with the two new POM entries and an explanation of why Panache is transitive
- [ ] 7.4 Write the code walk-through for `DatabaseChatMemoryStore` (highlight the three `@Transactional` methods, the delete-then-reinsert pattern, and `ChatMessageSerializer`)
- [ ] 7.5 Write the configuration section covering `application.properties` snippets and the test isolation block
- [ ] 7.6 Write the restart section: primary path (env var + Ctrl-C restart), verification check (`Restoring workflow instance:` log + Dev UI rows), fallback (live reload), and cleanup note for lingering containers
- [ ] 7.7 Write the "Going further" section: convert `TripPlanStore` to Panache; mention `chatMemoryFlushStrategySupplier` with `IMMEDIATE`
- [ ] 7.8 Verify the complete step runs end-to-end: start app, chat, submit a plan, restart, confirm workflow restore log, approve, confirm completion
