@echo off
rem Starts the whole Cloude Wars test network, each server in its own window.
cd /d "%~dp0"
start "lobby" cmd /c lobby\start.bat
start "bedwars1" cmd /c bedwars1\start.bat
start "proxy" cmd /c proxy\start.bat

