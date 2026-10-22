@echo off
rem Cloude Wars test network: Velocity proxy, players connect to 127.0.0.1:25575. Type "shutdown" to stop it.
cd /d "%~dp0"
title proxy
"C:\Program Files\Eclipse Adoptium\jdk-25.0.2.10-hotspot\bin\java.exe" -Xms512M -Xmx512M -jar velocity-4.3.0-44.jar
pause

