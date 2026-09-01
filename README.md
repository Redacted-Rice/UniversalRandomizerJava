# Lua Randomizer Wrapper for Java

Universal Randomizer for Java utilizing lua to allow creation of modules to arbitrarily randomize objects and their parameters.

This utilizes the Universal Randomizer Core written in Lua to perform the randomization and uses lua based modules to define the randomization to perform.

## Features

- Sandboxed Lua Environment: Execute untrusted Lua code safely with restricted access to dangerous functions
  - Pure Java: Uses LuaJ - no native Lua installation required
- Module Defined Actions: Lua modules implement actions and define their metadata to provide an API for configuring
  - Custom Arguments: Support for string, int, double, bool, lists, and tables
  - Arguement Constraints: ANY, RANGE, DISCRETE_RANGE, and ENUM to allow bounding args
  - Reproducable Results: Metadata includes seed for randomization functions
- Java-Lua Bridge: Define objects and enums in Java and pass then into Lua to use and modify
  - Automatic Change Tracking: Tracks what lua modules modify and can report it
- Module Discovery: Automatically scan directories for Lua modules
- Dependency Metadata: `requires`, `provides`, and `needs` validated at load and execution time
- Error Handling: Logging for any Lua errors encountered

Note: Performances, both speed and for large data sets, was not considered

## Requirements

- Java 21
- Gradle 8.5+

## License

MIT License - Feel free to use in your projects!

## Version

0.5.0

## Setup & Building

First ensure that the UniversalRandomizerCore repo is initialized. If this is not done, it will error trying to run as it won't find the randomizer files

```bash
git submodule update --init
```

Then build with

```bash
./gradlew build
```

## Example App

This includes an example app showing how this can be used. To run the example:

```bash
./gradlew runExample
```

You should see the results output to the console.

The example also ships a few Lua script tests under `appExample/script_tests`, next to `lua_modules`. These are a small sample of how a host app can pin module behavior with in-memory fixtures. Run them with:

```bash
./gradlew runExampleScriptTests
```

You can also pass `--script-tests` when running the example app directly (same working directory as `runExample`).

## Usage

Please see the example app for usage. More documentation will be comming later (hopefully)

## Creating Lua Modules

See the lua modules in the example app for example structure.

### Module metadata: requires, provides, needs

**requires** — versioned dependencies on the host app, URJava, or other loaded modules/scripts. Checked when modules load (`loadModules` / `validateAllRequirements`).

```lua
requires = {
    ExampleApp = "1.0.0",
    other_module = "0.1",
},
```

**provides / needs** — typed dynamic variables a module produces or consumes. URJ validates names and types only - your Lua code sets and reads the values. Type names are matched case insensitively. A module cannot satisfy its own needs - another loaded module/script must provide a compatible value. Load time checks that every need has such a provider somewhere. Before a batch runs, execution order is checked so each need is preceded by a compatible provide. `executeModules` returns an empty list on order failure (details in `IssueTracker`).

```lua
provides = { { name = "evoLineId", type = "integer" } },
needs = { { name = "numMoves", type = "integer" } },
```

### Enum display names and exclusions

Register human-readable labels on enum values:

```lua
context.registerEnum("EnergyType", {
  "FIRE", "WATER", "COLORLESS",
  displayNames = { FIRE = "Fire", WATER = "Water", COLORLESS = "Colorless" },
})
```

Filter enum arguments with `exclude` (and optional `values` allowlist):

```lua
definition = { type = "enum", constraint = "EnergyType", exclude = { "COLORLESS" } },
```

Arguments also support optional `displayName` and `description` for UI tooling.

## Security

This wrapper restricts Lua to provide a safe area to run untrusted scripts. This should not be relied upon though and users
are responsible for validating any modules before they load and run them. This section explains the security measures taken at a high level.

### Filesystem Restrictions
- Path Validation: Lua scripts can only access files within explicitly allowed directories
- Symlink Resolution: Symlinks are resolved and handled like full paths
- Blocked File Operations: Functions like `dofile`, `load`, and `loadstring` are either blocked or restricted

### Module and Library Protection
- Blocked Modules: Dangerous modules (`io`, `os`, `luajava`) are completely removed
- Debug Library Restriction: Only `debug.traceback()` is available for error messages to aid in debugging scripts
- Package System Protection: `package.path`, `package.loaded`, and module loaders are protected from modification
- Require Validation: Module loading is restricted to allowed paths and blocks dangerous modules

### Function-Level Security
- Dangerous Functions Blocked: `rawget`, `rawset`, `collectgarbage`, `getfenv`, `setfenv` removed
- Metatable Protection: Prevents modification of global environment and protected table metatables
- Global Environment Protection: Scripts cannot create new global variables

### Resource Limits
- Memory Limiting: Configurable memory usage limits with delta-based tracking
- Execution Timeout: Configurable timeout limits to prevent infinite loops
- Monitoring Thread: Separate thread monitors resource usage during script execution.

## Error Handling

Lua parsing and execution errors are captured and sent to the logger. This can be sent to a file or seen system out/err

## Testing

Tests can be run with the following

```bash
./gradlew test
```

Currently has a mix of unit testing and functional testing. Additionally uses the example app to validate behavior. Example Lua script tests live in `appExample/script_tests` and run via `./gradlew runExampleScriptTests`.

## Test Coverage

Coverage is separate from tests. Use `test` to run tests only, and `coverage` to run tests/application and generate coverage reports.

Commands for generating test coverage, example coverage, and combined coverage:

```bash
./gradlew coverage
./gradlew coverageExample
./gradlew coverageCombined
```

Coverage reports will be outputted in the `coverage` folder.
