# Handoff: `TabCafeStd` debugging status

Date: 2026-05-07

## Primary file
- `C:\Users\danlo\git\GLIMPSE-CE\GLIMPSE-ScenarioBuilder\src\glimpseElement\TabCafeStd.java`

## User goal
Support two CAFE application modes in `TabCafeStd`:
1. **Fleet Average**: preserve existing across-vintage behavior
2. **New Sales**: apply constraint only to the target vintage/year, similar in spirit to sales-specific behavior seen in other tabs such as `TabMarketShare`

## What has already been changed
### UI / metadata
Added to `TabCafeStd`:
- `Apply to:` combo box
- Options:
  - `Fleet Average`
  - `New Sales`
- Metadata persistence via:
  - `#Application mode: ...`

### Current export semantics in `saveScenarioComponent(...)`
The current implementation now:
- builds rows for each target year from the table
- derives a policy key from target year:
  - `targetYear_policyName`
- derives a market key as:
  - fleet mode: `policyKey + "Mkt"`
  - new-sales mode: `targetYear_marketName`
- loops over all model years from `vars.getAllYears()`
- applies target rows according to:
  - `New Sales`: `modelYear == targetYear`
  - `Fleet Average`: `calibrationYear <= modelYear <= targetYear`

### Hardening added
The save path was made more defensive:
- `safeParseDouble(...)`
- `safeParseInt(...)`
- skip malformed target rows
- skip invalid `load`, `intensity`, and bad `outputRatio`
- if zero valid rows are written, display warning and abort save
- print skipped count to console:
  - `Skipped invalid CAFE rows: N`

## Current symptom
Despite the hardening, the user reports:
- **sales constraint fails silently**
- **stock version crashed**
- later: **still a silent crash before period 0 begins**

This suggests one of two things:
1. The crash may still occur **outside** Java compilation and outside the guarded numeric parsing path, possibly in downstream scenario-component creation / file handling / XML generation.
2. The generated CSV structure may be accepted by the Java UI but rejected later by the model/scenario assembly layer before GCAM period 0 starts.

## Important observations from prior reasoning
- The user compared constrained MD behavior against unconstrained MA behavior and found MD clearly affected, so constraints were likely binding when they ran.
- However, changing market names alone was not sufficient for true vintage-only semantics.
- `TabMarketShare` uses a more explicit time-propagation structure (`sales` vs `all stock`) based on two time dimensions and filtering logic, whereas `TabCafeStd` is still using the `GLIMPSECAFETargets` / `GLIMPSEPFStdActivate` export pattern.
- Current implementation approximates sales-vs-stock by deciding which `year` rows get exported, but **does not introduce a second explicit vintage-year field** analogous to `adjcoef-year` in `TabMarketShare`.

## Most likely remaining failure points
### 1. Scenario-component builder or downstream parser rejects generated rows
Potential causes:
- duplicated or unexpected policy/market combinations
- `GLIMPSECAFETargets` semantics do not support the way model-year expansion is now being done
- activation rows may be inconsistent with expanded target rows

### 2. Silent failure in outer save task
Need to inspect the caller that wraps `currentTab.saveScenarioComponent()`.
Likely places to inspect next:
- `GLIMPSE-ScenarioBuilder/src/gui/ScenarioComponentCreatorDialog.java`
- anything that consumes `fileContent` / `filenameSuggestion`

A likely issue is that exceptions are swallowed by a background task and surfaced only as a generic failure.

### 3. Generated output may be too malformed for downstream use even when Java-side save succeeds
Need to inspect the exact generated `fileContent` in both modes.

## Recommended next debugging steps
### A. Add explicit logging around save completion
In `TabCafeStd.saveScenarioComponent(...)`, temporarily log:
- `writtenConstraintRows`
- `skippedInvalidRows`
- a preview of first few lines of `contentP1`
- a preview of first few lines of `contentP2`

### B. Inspect the outer save failure path
Read and trace:
- `ScenarioComponentCreatorDialog.java`
- wherever `fileContent` is written to disk
- any catch blocks that suppress the root exception

Goal: expose the actual exception text instead of a silent crash.

### C. Compare with a known-good tab
Use `TabMarketShare` as the template for how the UI/background save framework expects scenario-component generation to behave, not just how sales vs stock semantics are modeled.

### D. Export a tiny minimal test case
Suggested minimal case:
- one region
- one target year only (e.g. 2025)
- one mode at a time
- inspect raw generated CSV before model run

### E. Consider whether `GLIMPSECAFETargets` is the wrong abstraction for new-sales mode
If downstream semantics continue failing, likely next step is:
- preserve current `GLIMPSECAFETargets` path for `Fleet Average`
- implement a **separate export structure** for `New Sales`, closer to the policy-factor / PFStd style used in market-share tabs

## Current code state summary
At handoff, `TabCafeStd.java` contains:
- application mode UI and metadata support
- helper methods:
  - `isNewSalesMode()`
  - `getPolicyKeyForTargetYear(...)`
  - `getMarketKeyForTargetYear(...)`
  - `shouldApplyTargetToModelYear(...)`
  - `safeParseDouble(...)`
  - `safeParseInt(...)`
- rewritten `saveScenarioComponent(...)` with expanded year logic and row guards

## Suggested first message for next conversation
"Please inspect `ScenarioComponentCreatorDialog` and the save pipeline around `TabCafeStd.saveScenarioComponent()` to identify where the silent failure is being swallowed, then instrument the generated CSV preview for `Fleet Average` and `New Sales`."
