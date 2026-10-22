@echo off
rem Cloude Wars test network: lobby server (Paper 1.21.11). Type "stop" in this window to shut it down.
cd /d "%~dp0"
title lobby
java -Xms1G -Xmx2G -jar paper-1.21.11-132.jar --nogui
pause

