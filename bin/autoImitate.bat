REM @echo off

call stockEnv.bat

cd %rootDir%\bin
imitateRealtimeData.bat ..\data\copyList\600030.20200811.copylist.txt
