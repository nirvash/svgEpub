@echo OFF
set FF=%~dp1
set LANG=en
set PATH=%FF%\bin;%FF%\bin\Xming-6.9.0.31;%PATH%
set DISPLAY=:9.0
set XLOCALEDIR=%FF%\bin\Xming-6.9.0.31\locale
set AUTOTRACE=potrace
set HOME=%FF%

rem start /B "" "%FF%\bin\Xming-6.9.0.31\Xming.exe" :9 -multiwindow -clipboard -silent-dup-error -notrayicon
rem "%FF%\bin\Xming_close.exe" -wait

echo HOME=%HOME%
echo FF=%FF%
echo cd %~dp2
echo "%FF%\bin\fontforge.exe" -nosplash -script %3 %4 %5

echo cd %~dp2
echo %3 %4 %5
cd %~dp2


"%FF%\bin\fontforge.exe" -nosplash -script %3 %4 %5

rem "%FF%\bin\Xming_close.exe" -close
