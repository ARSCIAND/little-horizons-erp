@echo off
REM Compiles the project into out\ (run from the project root).
if exist out rmdir /s /q out
mkdir out
dir /s /b src\*.java > out\sources.txt
javac -encoding UTF-8 --release 17 -d out -cp "lib\*" @out\sources.txt
if errorlevel 1 exit /b 1
del out\sources.txt
echo Build OK -^> out\
