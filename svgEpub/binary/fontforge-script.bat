@echo off
set FF=%~dp1
set PATH=%FF%cygwin\bin;%SystemRoot%\system32;%SystemRoot%;%SystemRoot%\system32\Wbem
set CYGWIN=nodosfilewarning
set HOME=%FF%
set LANG=C.UTF-8
set TZ=
set DISPLAY=:9.0
set AUTOTRACE=potrace

echo FF=%FF%
for /F %%i in ('%FF%\cygwin\bin\cygpath.exe -u "%~f3"') do @set f3="%%i"
for /F %%i in ('%FF%\cygwin\bin\cygpath.exe -u "%~f4"') do @set f4="%%i"
for /F %%i in ('%FF%\cygwin\bin\cygpath.exe -u %2%5') do @set f5="%%i"
echo f4=%f4%
echo f5=%f5%

rem start /B XWin.exe :9 -multiwindow -nomultimonitors -silent-dup-error
rem pushd %~dp2
echo pushd %~dp1
echo %0 %*
echo "%FF%\cygwin\bin\fontforge.exe" -nosplash -script %f3% %f4% %f5%
rem xwin-close.exe -wait
"%FF%\cygwin\bin\fontforge.exe" -nosplash -script %f3% %f4% %f5%
rem xwin-close.exe -close
rem popd
