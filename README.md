# Lua Randomizer Wrapper for Java

Universal Randomizer for Java utilizing lua to allow creation of modules to arbitrarily randomize objects and their parameters.

This utilizes the Universal Randomizer Core written in Lua to perform the randomization and uses lua based modules to define the randomization to perform.

## Features

- Sandboxed Lua Environment: Execute untrusted Lua code safely with restricted access to dangerous functions
  - Pure Java: Uses LuaJ - no native Lua installation required
- Module Defined Actions: Lua modules implement actions and define their metadata to provide an API for configuring
  - Custom Arguments: Support for string, int, double, bool, lists, groups and maps with with more to come
  - Arguement Constraints: ANY, RANGE, DISCRETE_RANGE, and ENUM to allow bounding args
  - Reproducable Results: Metadata includes seed for randomization functions
- Java-Lua Bridge: Define objects and enums in Java and pass then into Lua to use and modify
  - Automatic Change Tracking: Tracks what lua modules modify and can report it
- Module Discovery: Automatically scan directories for Lua modules
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

## Usage

Please see the example app for usage. More documentation will be comming later (hopefully)

## Creating Lua Modules

See the lua modules in the example app for example structure. More documentation will be comming later (hopefully)

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

Currently has a mix of unit testing and functional testing. Additionally uses the example app to validate behavior

## Test Coverage

Coverage is separate from tests. Use `test` to run tests only, and `coverage` to run tests/application and generate coverage reports.

Commands for generating test coverage, example coverage, and combined coverage:

```bash
./gradlew coverage
./gradlew coverageExample
./gradlew coverageCombined
```

Coverage reports will be outputted in the `coverage` folder.
