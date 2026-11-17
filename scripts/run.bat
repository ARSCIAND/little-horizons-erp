@echo off
REM Runs the application (compile first with scripts\build.bat).
chcp 65001 > nul
java -Dfile.encoding=UTF-8 -cp "out;lib\*" com.littlehorizons.erp.Main
