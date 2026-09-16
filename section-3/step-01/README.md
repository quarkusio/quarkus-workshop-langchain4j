# Step 01: Agent skills

The Miles of Smiles trip planner uses Quarkus LangChain4j agents to recommend a vehicle, create an itinerary, and estimate costs. Markdown skills supply vehicle and trip-specific guidance through `activate_skill`. The supplied page renders the plan from `POST /trip/plan`; no extra endpoint or UI component is needed to inspect rest days and driving descriptions.

Compared with Step 00, this step adds only the skills dependency, Markdown guidance, agent skill annotations and activation instructions, and skills configuration. Vehicle selection and itinerary planning still run in parallel before cost estimation. The workflow, four-field `TripPlan` response, and UI are unchanged. There is no chat, approval, status, or latest-plan endpoint. Step 02 keeps this baseline and adds guardrails, guarded rental pricing, and safe error display.

Follow the [workshop chapter](../../docs/docs/section-3/step-01.md) for setup and the seven-day family-trip exercise. Run `./mvnw quarkus:dev` with the configured model credentials available to the process. Keep credentials out of captures and shared logs.

The family skill starts with exactly one rest day for trips of seven or more days. The main experiment changes only that instruction to exactly two rest days, repeats exactly the same form inputs without a manual restart, and checks both the newly returned skill content and the daily cards. Restore the one-rest-day baseline afterwards. Family-versus-adventure selection is a separate experiment with different inputs.

## Test

Run `./mvnw test -Dtest=TripPlanContractTest` for the same deterministic model, workflow, and HTTP contract checks as Step 00. These fixed responses verify plan assembly and the JSON contract, not skill activation or model adherence. Run `./mvnw test` with `OPENAI_API_KEY` unset to skip the existing live-model `TripPlannerResourceTest`. Do not run `clean` while dev mode is running.

The investigation below records earlier runs with a minimum-rest-day instruction. The current exercise uses exactly one rest day, changed to exactly two; the historical observations are retained as recorded.

## Reload investigation and fix, September 14, 2026

The stale-content blocker was resolved with one dev-only configuration entry in `src/main/resources/application.properties`:

```properties
%dev.quarkus.live-reload.watched-resources=skills/family-trip/SKILL.md
```

This is a standard Quarkus setting, not a Skills-extension reload option. It watches the exact resource-relative file used in the exercise. Other skill files are not watched by this entry; additional filenames can be comma-separated, but directories are not supported. It causes an automatic application restart inside the same running JVM, with no Java edit, dependency upgrade, or manual restart command. In-memory agent conversations and execution history are recreated with the application.

Source inspection explained why the resource copy alone was insufficient. The installed extension's `SkillsProcessor` registers a runtime-initialized, application-scoped `SkillsToolProvider` but no `HotDeploymentWatchedFileBuildItem`. `SkillsRecorder` reads the configured directories when that provider is created. The upstream `ClassPathSkillLoader` reads `SKILL.md` into a skill object, and `ActivateSkillToolExecutor` returns `skill.content()` from its stored map; it does not reread the file on activation. `SkillsConfiguration` exposes only `directories`. Changing from a classpath directory to a filesystem directory would still use the recorder's construction-time loading, not a per-call reload setting.

Quarkus's `RuntimeUpdatesProcessor` distinguishes copying changed resources from restarting the application. `DevModeBuildStep.watchChanges()` turns `quarkus.live-reload.watched-resources` entries into restart-triggering watched files. A probe without that setting found matching two-rest-day content in source and `target/classes`, HTTP 200 from the application, and no new skill-loading or live-reload messages. The previous new activation that returned old text was therefore a stale provider snapshot, not merely an LLM repeating its prior answer.

Conversation reuse was a separate factor in the original experiment. The captured second-run requests included earlier tool results, including the baseline's `call_tO1iXxmENJARjD6nSueFThwT`. However, the new call `call_gEQGzWzHq8cIzTWZqW52cGgv` also returned the old one-rest-day text, so retained conversation history could not explain away the stale tool result. In the fixed comparison, the first itinerary-model request in each run contained only a `user` message, with no earlier assistant/tool messages. The automatic reload gave the second run fresh application-side conversation state; identical form inputs do not imply identical model context across a reload.

The repeat used the same configured `gpt-4o` model and the original seven-day family form values. Both request bodies had SHA-256 `53e6e786fe9ad6a810176c65f770a98d1085ca2af2ee93651263934cc44d94cc`. The baseline started planning at `15:59:11Z`, returned HTTP 200 and seven cards, and successfully activated `family-trip` with call `call_6jDLgWhFK1rAc9Flodiuttcc`. Its result contained the one-rest-day minimum. Day 4 explicitly described a driving-free rest day at the same overnight base; other cards included approximate driving times and some stops but lacked the requested break timing and duration.

After changing only the skill minimum to two, an ordinary HTTP request triggered reload before the next model request. At `16:00:33.760Z` (`18:00:33.760` in the local logs), `SkillsRecorder` reported loading four skills. Quarkus then logged `Live reload total time: 0.348s`. The next plan request began at `16:00:35Z`. The port-8083 listener remained JVM PID `92244` before and after the experiment. No force-restart tool, process restart, Java edit, or further configuration edit occurred between these two plans.

The edited request returned HTTP 200 and seven cards. New successful activation `call_oTurvOip6nAaY46Y5HPWvHVR` returned the two-rest-day minimum together with the driving and break guidance. Days 4 and 6 were titled `Rest day - no driving`, retained their preceding overnight bases, and described walking or train-accessible activities without car excursions. This run made the change visible in the existing itinerary. It still omitted approximate driving times and timed breaks, and did not repeat the exact rest-day label in both descriptions. Successful reload and activation do not establish full adherence or verified travel times.

The runtime probe asserted source/classpath content equality, successful HTTP responses, seven rendered cards, and a fresh successful family activation containing the expected minimum for each run. Sanitized evidence is in the session's temporary directory under `step01-reload-*.json`, with itinerary captures named `step01-reload-configured-*-cards.png`. These checks used the real configured model; no responses were mocked. The source skill was restored to the one-rest-day baseline after the experiment, leaving only the supported dev-mode watcher as the runtime fix.

Restoration also triggered automatic reload (`0.459s`). Both skill copies then matched the baseline SHA-256 `4a1a3c325c485413975c4ef9c2d4fef231efdc2cc011a746067ba56d89809745`. The updated chapter passed MkDocs, all three Mermaid renders, and desktop/mobile checks at 1280px and 390px with no broken images or horizontal overflow. The existing REST test was not rerun in this follow-up; its earlier timeout is recorded below. After verification, MCP again reported stopping Step 01 without closing its listener. A targeted termination of the verified Step 01 JVM stopped port 8083, confirmed with `lsof`. Step 03 was not used, changed, or stopped by this investigation.

The source references below are pinned to the artifacts inspected for this investigation; project dependency versions remain defined by `pom.xml`.

- [SkillsProcessor](https://github.com/quarkiverse/quarkus-langchain4j/blob/1.14.0.CR3/skills/deployment/src/main/java/io/quarkiverse/langchain4j/skills/deployment/SkillsProcessor.java), [SkillsRecorder](https://github.com/quarkiverse/quarkus-langchain4j/blob/1.14.0.CR3/skills/runtime/src/main/java/io/quarkiverse/langchain4j/skills/runtime/SkillsRecorder.java), and [SkillsConfiguration](https://github.com/quarkiverse/quarkus-langchain4j/blob/1.14.0.CR3/skills/runtime/src/main/java/io/quarkiverse/langchain4j/skills/runtime/SkillsConfiguration.java)
- [ClassPathSkillLoader](https://github.com/langchain4j/langchain4j/blob/1.20.0/langchain4j-skills/src/main/java/dev/langchain4j/skills/ClassPathSkillLoader.java) and [ActivateSkillToolExecutor](https://github.com/langchain4j/langchain4j/blob/1.20.0/langchain4j-skills/src/main/java/dev/langchain4j/skills/ActivateSkillToolExecutor.java)
- [LiveReloadConfig](https://github.com/quarkusio/quarkus/blob/3.39.3/core/runtime/src/main/java/io/quarkus/runtime/LiveReloadConfig.java), [DevModeBuildStep](https://github.com/quarkusio/quarkus/blob/3.39.3/core/deployment/src/main/java/io/quarkus/deployment/steps/DevModeBuildStep.java), and [RuntimeUpdatesProcessor](https://github.com/quarkusio/quarkus/blob/3.39.3/core/deployment/src/main/java/io/quarkus/deployment/dev/RuntimeUpdatesProcessor.java)

## Initial verification without the watcher

The configured model was `gpt-4o`; response logs identified `gpt-4o-2024-08-06`. Step 01 ran on port 8083. Step 03 on port 8080 was not used or restarted. Browser automation submitted the existing form and read the rendered cards. Dev MCP log history supplied the activation calls and tool results; request headers and credentials were excluded from the captured evidence.

An initial edit adding driving-description guidance was not reflected in the returned skill. A preparatory Dev MCP force restart loaded that guidance before the recorded baseline. The managed restart wrapper timed out; invoking the discovered restart tool directly through Step 01's Dev MCP endpoint succeeded. No restart was requested between the following baseline and edited runs.

Both requests used Italian Riviera, start date `2026-10-12`, seven days, four travelers, Family Vacation, Moderate (`EUR 1000-2500`), and `We love coastal towns and good food`. The actual form request bodies had the same SHA-256: `53e6e786fe9ad6a810176c65f770a98d1085ca2af2ee93651263934cc44d94cc`.

The baseline at `15:45:05Z` returned HTTP 200 and seven cards. After an invalid `Family Trip Planning` request, `activate_skill` call `call_tO1iXxmENJARjD6nSueFThwT` successfully returned `family-trip`, including the one-rest-day instruction and the new driving-time, break, and driving-free-day guidance. Days 3 and 5 said `Rest day - no driving` in their descriptions and retained the previous overnight base. Days 2, 4, and 6 stated driving times of one hour, 1.5 hours, and 30 minutes. The model did not supply planned breaks, mark these times explicitly as approximate, or use the requested driving-free label in both rest-day titles. Arrival/departure transport and some local transport remained unspecified. These are partial results, not fully verified adherence or checked journey times.

After changing only the rest-day minimum from one to two, the second request at `15:46:01Z` also returned HTTP 200 and seven cards. New `family-trip` call `call_gEQGzWzHq8cIzTWZqW52cGgv` succeeded but still returned the one-rest-day instruction. Both the source skill and `target/classes/skills/family-trip/SKILL.md` contained the two-rest-day edit at that point. Earlier tool results were also present in conversation history, but the stale content was confirmed in this new call's result. The cards again described days 3 and 5 as driving-free, with the same driving times and missing breaks. Since the baseline already had two such descriptions and the new guidance was not returned, this does not demonstrate an effect from the edit.

This initial run did not meet the hot-reload completion criterion. The follow-up above added the watched-resource setting and verified the updated content in a new activation without a manual restart. No Java, response schema, or frontend changes were needed. The source skill was restored to the one-rest-day baseline after each experiment.

Desktop (1280px) and mobile (390px) screenshots and sanitized JSON evidence were captured outside the repository in the session's temporary directory as `step01-baseline-*` and `step01-edited-*`. Seven daily cards were present in each run, with no horizontal overflow at the mobile viewport. These captures are local verification artifacts, not workshop screenshots or evidence of verified travel advice.

The separate Swiss Alps adventure request also returned HTTP 200 and seven rendered cards. The captured Dev MCP log history did not provide matching adventure activation evidence, so successful `adventure-trip` selection was not verified. Its itinerary is not a substitute for that missing trace.

The existing `TripPlannerResourceTest` was run through Dev MCP. It failed with `Read timed out` after approximately 30 seconds, including on a retry with no concurrent browser request. The browser exercise used a longer wait and completed successfully. No test or timeout configuration was changed within this documentation-only scope. This test checks that vehicle and itinerary fields exist; it does not assert skill activation, reload, or adherence.

The MkDocs build passed with source-include path checks enabled. Its messages about missing anchors in Section 1 Step 06 and Section 2 Step 09 are outside this change. All three existing Mermaid diagrams in this chapter also rendered successfully with `mmdc`. The generated chapter was checked in Chrome at 1280px and 390px, with no broken images or horizontal overflow. MCP reported Step 01 stopped afterwards, but the follow-up found the same serving JVM still listening on 8083; lifecycle wrapper status alone was not reliable shutdown evidence. Step 03 was left untouched.

## References

- [Quarkus LangChain4j skills](https://docs.quarkiverse.io/quarkus-langchain4j/dev/skills.html)
- [Quarkus LangChain4j Dev UI](https://docs.quarkiverse.io/quarkus-langchain4j/dev/dev-ui.html)
- [Quarkus dev mode](https://quarkus.io/guides/dev-mode-differences)
