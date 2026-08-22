# Java + JavaFX Migration Execution Plan (ScenarioBuilder)

This plan converts the high-level checklist into commit-sized tasks that can be completed and validated incrementally.

## Target Baseline

- **Target Java**: Java 21 LTS
- **Target JavaFX**: JavaFX 21 LTS (`javafx-base`, `javafx-graphics`, `javafx-controls`, `javafx-fxml` as needed)
- **Target ControlsFX**: 11.2.x
- **Fallback baseline if blocked**: Java 17 LTS + JavaFX 17 LTS

## Working Rules

- Keep every task shippable and reviewable as one commit.
- Do not mix dependency updates with broad refactors in the same commit.
- Keep launch-script changes separate from source-code compatibility changes.
- After each commit, run at least a quick compile and startup smoke check.

## Commit-Sized Execution Plan

### Commit 1 - Capture migration baseline

- **Commit message**: `docs: add Java 21/JavaFX 21 migration execution plan`
- **Tasks**:
  - Finalize this file as the source of truth for migration scope.
  - Record current Java/JavaFX assumptions and known risk areas.
- **Primary files**:
  - `JAVA_JAVAFX_MIGRATION_CHECKLIST.md`
- **Exit criteria**:
  - Plan is approved and ordered for implementation.

### Commit 2 - Inventory current runtime assumptions (completed 2026-08-22)

- **Commit message**: `docs: inventory launcher and classpath assumptions`
- **Tasks**:
  - Document current Java invocation patterns and hardcoded paths.
  - List JavaFX-related jars currently expected by runtime.
- **Primary files**:
  - `README.md`
  - `run_GLIMPSE_GCAM-USA-8.2-windows.bat` (repo root)
  - `../GLIMPSE-ModelInterface/run_GLIMPSE-ModelInterface-Windows.bat`
  - `../GLIMPSE-ModelInterface/run-GLIMPSE-ModelInterface_Linux.sh`
- **Exit criteria**:
  - Migration has a clear before-state reference.

#### Commit 2 inventory snapshot (before migration)

- **Root launcher (Windows)**:
  - `../run_GLIMPSE_GCAM-USA-8.2-windows.bat` sets `JAVA_HOME` to `amazon-corretto-8.442.06.1-windows-x64-jre` and runs:
    - `java -Djava.util.logging.config.file -Dprism.order=sw -jar .\GLIMPSE-ScenarioBuilder\GLIMPSE-ScenarioBuilder.jar -options options_GCAM-USA-8.2-windows.txt`
  - Assumption: bundled Java 8 JRE path exists relative to repo root.
- **ModelInterface launcher (Windows)**:
  - `../GLIMPSE-ModelInterface/run_GLIMPSE-ModelInterface-Windows.bat` requires `%JAVA_HOME%\bin\java.exe` and prepends `%JAVA_HOME%\bin\server` to `PATH`.
  - Runs ModelInterface with `java -jar ./GLIMPSE-ModelInterface.jar ...`.
- **ModelInterface launcher (Linux)**:
  - `../GLIMPSE-ModelInterface/run-GLIMPSE-ModelInterface_Linux.sh` requires `$JAVA_HOME/bin/java`, prepends `$JAVA_HOME/bin/server` to `PATH`, then runs `java -jar ./GLIMPSE-ModelInterface.jar ...`.
- **ScenarioBuilder dependency assumption**:
  - `.classpath` uses `org.eclipse.jdt.launching.JRE_CONTAINER` plus explicit `libs/controlsfx-8.40.18.jar`.
  - JavaFX modules are not pinned as explicit jars in `.classpath`; they are assumed to be available via the selected Java runtime or IDE setup.
- **ModelInterface dependency assumption**:
  - `../GLIMPSE-ModelInterface/.classpath` uses `JRE_CONTAINER` and third-party Swing/charting/geotools jars; no explicit JavaFX jar dependencies are declared.

### Commit 3 - Add Java 21 compiler settings (completed 2026-08-22)

- **Commit message**: `build: set source/target release to Java 21`
- **Tasks**:
  - Update project/compiler settings to Java 21 for ScenarioBuilder.
  - Keep functional behavior unchanged in this commit.
- **Primary files**:
  - `.classpath` and/or Eclipse project metadata as applicable
  - Any local build scripts used for compile
- **Exit criteria**:
  - Project attempts compile under Java 21, with failures captured for next commits.

#### Commit 3 compile attempt snapshot

- Command run from repo root used `javac 21.0.10` with `--release 21` over ScenarioBuilder and ModelInterface source trees.
- First failure observed:
  - `GLIMPSE-ModelInterface/src/mif411/ModelInterface/UnitConversionInstance.java:29`
  - `error: unmappable character (0x92) for encoding UTF-8`
- Immediate interpretation:
  - Compiler level change is in place; next commits need encoding normalization or explicit `javac -encoding` alignment before broader API migration fixes.

### Commit 4 - Upgrade JavaFX and ControlsFX artifacts (completed 2026-08-22)

- **Commit message**: `deps: upgrade to JavaFX 21 and ControlsFX 11.2`
- **Tasks**:
  - Add JavaFX 21 artifacts for supported platforms.
  - Replace `libs/controlsfx-8.40.18.jar` with ControlsFX 11.2.x.
  - Remove superseded JavaFX 8/legacy jars only if safe.
- **Primary files**:
  - `libs/`
  - Any dependency manifest or classpath metadata in use
- **Exit criteria**:
  - Dependencies resolve locally; compile failures are API-related (not missing jars).

#### Commit 4 dependency snapshot

- Added JavaFX 21.0.4 module jars under:
  - `libs/javafx-21/win/` (`base`, `graphics`, `controls`, `fxml`)
  - `libs/javafx-21/linux/` (`base`, `graphics`, `controls`, `fxml`)
- Added `libs/controlsfx-11.2.1.jar` and replaced classpath entries previously pointing to `libs/controlsfx-8.40.18.jar`.
- Updated `.classpath` metadata to include JavaFX 21 Windows jars for local IDE compilation.
- Quick compile check (`javac --release 21` with upgraded jars) no longer reports missing JavaFX/ControlsFX artifacts; remaining failures are API-level (`FontLoader.computeStringWidth`, `Thread.destroy`).

### Commit 5 - Update launcher module flags (Windows) (completed 2026-08-22)

- **Commit message**: `build: update Windows launcher for JavaFX module path`
- **Tasks**:
  - Add `--module-path` and `--add-modules` for JavaFX 21.
  - Prefer `JAVA_HOME`/`PATH` usage over fixed Java 8 paths where practical.
- **Primary files**:
  - `run_GLIMPSE_GCAM-USA-8.2-windows.bat`
  - ScenarioBuilder launch scripts in this repo
- **Exit criteria**:
  - Windows launcher starts app to initial UI stage.

#### Commit 5 launcher snapshot

- Updated `../run_GLIMPSE_GCAM-USA-8.2-windows.bat` and `../run_GLIMPSE_GCAM-global-8.2-windows.bat` to:
  - Prefer configured `%JAVA_HOME%` when valid.
  - Fallback to legacy bundled Corretto path if present.
  - Fallback to `java.exe` from `%PATH%` as last resort.
  - Add JavaFX flags for Java 21+: `--module-path .\GLIMPSE-ScenarioBuilder\libs\javafx-21\win --add-modules=javafx.controls,javafx.fxml`.
- Added explicit existence check for `javafx-controls-21.0.4-win.jar` before launch.
- Validation scope for this commit:
  - Script-path/runtime checks verified.
  - Full GUI startup smoke remains part of Commit 9 test sign-off.

### Commit 6 - Update launcher module flags (Linux)

- **Commit message**: `build: update Linux launcher for JavaFX module path`
- **Tasks**:
  - Apply equivalent module-path/module changes to Linux scripts.
  - Keep script behavior consistent with Windows changes.
- **Primary files**:
  - `../GLIMPSE-ModelInterface/run-GLIMPSE-ModelInterface_Linux.sh`
  - Any Linux launchers used by ScenarioBuilder
- **Exit criteria**:
  - Linux launcher command line is migration-ready and documented.

### Commit 7 - Fix first-pass JavaFX/ControlsFX compile breaks

- **Commit message**: `fix: resolve JavaFX 21 and ControlsFX API compile issues`
- **Tasks**:
  - Address imports, API changes, and type incompatibilities.
  - Prioritize known UI hotspots first.
- **Primary files (expected)**:
  - `src/gui/Client.java`
  - `src/gui/DiffWindow.java`
  - `src/gui/ConsoleManager.java`
  - `src/glimpseBuilder/SetupMenuView.java`
  - `src/glimpseBuilder/SetupMenuTools.java`
  - `src/glimpseElement/PolicyTab.java`
- **Exit criteria**:
  - Clean compile under Java 21 in ScenarioBuilder.

### Commit 8 - Runtime behavior fixes

- **Commit message**: `fix: address JavaFX runtime behavior regressions`
- **Tasks**:
  - Fix runtime exceptions and threading issues (`Platform.runLater`, task boundaries).
  - Verify dialogs, table interactions, and menu actions.
- **Primary files**:
  - UI/controller files touched by runtime regressions
- **Exit criteria**:
  - Core workflows execute without runtime errors.

### Commit 9 - Smoke test and regression checklist sign-off

- **Commit message**: `test: record Java 21/JavaFX 21 smoke and regression results`
- **Tasks**:
  - Run smoke tests for startup, open/save, trash actions, and key tools.
  - Record pass/fail and known follow-ups.
- **Primary files**:
  - `JAVA_JAVAFX_MIGRATION_CHECKLIST.md`
  - Optional test notes under `build_tmp/`
- **Exit criteria**:
  - Migration status is transparent and reproducible.

### Commit 10 - Finalize docs and rollback guidance

- **Commit message**: `docs: finalize migration notes and rollback procedure`
- **Tasks**:
  - Document required environment variables and launch expectations.
  - Add short rollback instructions for Java 8 profile if needed.
- **Primary files**:
  - `README.md`
  - Launcher comments and migration docs
- **Exit criteria**:
  - Team has clear runbook for forward use and fallback.

## Definition of Done

- ScenarioBuilder compiles with Java 21.
- JavaFX 21 + ControlsFX 11.2.x are used at runtime.
- Launchers work with module path configuration.
- Smoke tests pass on at least Windows; Linux status is documented.
- Migration docs include setup, validation, and rollback notes.

## Suggested Immediate Next Commit

- Start with **Commit 5** (Windows launcher module-path wiring).