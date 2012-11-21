cd /d %~dp0
set infile="%~f1"
set inmobi="%~dpn1.mobi"
set outmobi="%~dpn1-stripped.mobi"
set ext="%~x1"

if %ext% == ".epub" kindlegen.exe %infile%
kindlestrip.exe %inmobi% %outmobi%

@echo off
REM pause on error
if ERRORLEVEL 1 pause
