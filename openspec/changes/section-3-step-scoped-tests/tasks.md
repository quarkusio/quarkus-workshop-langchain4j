## 1. Document the convention

- [ ] 1.1 Add a "Testing Conventions" subsection to root `AGENTS.md` stating that each `section-N/step-XX` module tests only the code that step introduces or changes, and does not re-test behavior inherited unchanged from earlier steps; verify the wording matches the spec's three cases (new / unchanged / modified)
- [ ] 1.2 Verify the new AGENTS.md text renders correctly and follows the repo's prose style (no bold-label bullets, natural prose) by re-reading the edited section

## 2. Audit existing tests across sections

- [ ] 2.1 List all test files under `section-*/step-*/src/test` and, for each, determine whether it targets code its step introduces or changes; record findings
- [ ] 2.2 For any test found to duplicate earlier-step coverage, note it in the audit and flag for removal or rescoping (do not change production code as part of this change)

## 3. Verify

- [ ] 3.1 Confirm no `section-N/step-XX` module contains tests that only exercise unchanged inherited behavior, by checking each test file from the audit against the convention
