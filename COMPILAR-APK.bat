@echo off
setlocal
cd /d "%~dp0"
echo === Status C17 - compilacion de APK ===
echo.
java -version >nul 2>&1
if errorlevel 1 (
  echo ERROR: No se encontro Java en PATH.
  echo Instala un JDK 21 y abre una nueva ventana de CMD.
  echo Guia oficial: https://adoptium.net/temurin/releases/?version=21
  echo.
  pause
  exit /b 1
)
call "%~dp0gradlew.bat" assembleDebug
set "BUILD_RESULT=%ERRORLEVEL%"
echo.
if "%BUILD_RESULT%"=="0" (
  echo Compilacion terminada. Busca el APK en:
  echo app\build\outputs\apk\debug\app-debug.apk
) else (
  echo La compilacion fallo. Copia el error de arriba para revisarlo.
)
echo.
pause
exit /b %BUILD_RESULT%
