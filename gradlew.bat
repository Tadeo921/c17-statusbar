@rem Gradle wrapper for Windows - corrected failure handling
@echo off
setlocal EnableExtensions

set "DIRNAME=%~dp0"
if "%DIRNAME%"=="" set "DIRNAME=."
set "APP_BASE_NAME=%~n0"
set "APP_HOME=%DIRNAME%"
for %%i in ("%APP_HOME%") do set "APP_HOME=%%~fi"
set DEFAULT_JVM_OPTS="-Xmx64m" "-Xms64m"

@rem Prefer JAVA_HOME when it points to an installed JDK
if defined JAVA_HOME goto findJavaFromJavaHome

@rem Otherwise, look for java.exe on PATH
set "JAVA_EXE=java.exe"
"%JAVA_EXE%" -version >NUL 2>&1
if %ERRORLEVEL% equ 0 goto execute

echo. 1>&2
echo ERROR: Java is not installed or java.exe is not in PATH. 1>&2
echo Install a JDK (Java 21 LTS is recommended), then open a new CMD window. 1>&2
echo. 1>&2
exit /b 1

:findJavaFromJavaHome
set "JAVA_HOME=%JAVA_HOME:"=%"
set "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
if exist "%JAVA_EXE%" goto execute

echo. 1>&2
echo ERROR: JAVA_HOME points to an invalid directory: "%JAVA_HOME%" 1>&2
echo Set JAVA_HOME to the JDK root folder, not its bin folder or java.exe. 1>&2
echo. 1>&2
exit /b 1

:execute
endlocal & "%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %GRADLE_OPTS% "-Dorg.gradle.appname=%APP_BASE_NAME%" -jar "%APP_HOME%\gradle\wrapper\gradle-wrapper.jar" %* & call :exitWithErrorLevel

:exitWithErrorLevel
"%COMSPEC%" /c exit %ERRORLEVEL%
