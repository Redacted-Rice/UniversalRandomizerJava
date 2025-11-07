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

## Building

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
are responsible for validating any modules before they load and run them. Some of the security measures are:

- Library Whitelisting: Only safe Lua libraries are loaded (math, string, table)
- Explicit Blocking: Dangerous functions are explicitly undefined
- Restricted Require: Only the randomizer module can be loaded (may relax in the future)
- Sandboxed Execution: All Lua code runs in this isolated environment

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
