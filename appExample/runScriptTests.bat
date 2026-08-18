@echo off
setlocal
set "SCRIPT_DIR=%~dp0"
cd /d "%SCRIPT_DIR%"

if not exist "lua_modules\" (
	if exist "..\lua_modules\" (
		cd /d "%SCRIPT_DIR%.."
	)
)

set "JAR="
for %%f in ("ExampleApp-*.jar") do (
	if exist "%%~f" set "JAR=%%~f"
)

if not defined JAR (
	if exist "app\" (
		for %%f in ("app\ExampleApp-*.jar") do (
			if exist "%%~f" set "JAR=%%~f"
		)
	)
)

if not defined JAR (
	echo No ExampleApp-*.jar found next to this script or in app\
	echo Build one with: gradlew :appExample:fatJar
	exit /b 1
)

java -jar "%JAR%" --script-tests %*
exit /b %ERRORLEVEL%
