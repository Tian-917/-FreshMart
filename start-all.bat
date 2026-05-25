@echo off
chcp 936 >nul
title FreshMart One-Click Start
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\start-all.ps1"
echo.
echo Close this window does NOT stop services.
pause
