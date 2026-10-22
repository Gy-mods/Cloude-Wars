@echo off
rem Starts the Cloude Wars test server (Paper 1.21.11, port 25575). Type "stop" in this window to shut it down.
cd /d "%~dp0"
java -Xms1G -Xmx2G -jar paper-1.21.11-132.jar --nogui
pause
