@echo off
if "%OS%" == "Windows_NT" setlocal
rem ---------------------------------------------------------------------------
rem Start script for the CATALINA Server
rem
rem $Id: startup.bat,v 1.6 2004/05/27 18:25:11 yoavs Exp $
rem ---------------------------------------------------------------------------

set CATALINA_HOME={{APPSERVER_ROOT}}

set CATALINA_BASE=%CATALINA_HOME%
set JAVA_HOME=%CATALINA_HOME%\JRE
set _RUNJAVA="%JAVA_HOME%\bin\java"

rem Guess CATALINA_HOME if not defined
set CURRENT_DIR=%cd%
if not "%CATALINA_HOME%" == "" goto gotHome
set CATALINA_HOME=%CURRENT_DIR%
if exist "%CATALINA_HOME%\bin\catalina.bat" goto okHome
cd ..
set CATALINA_HOME=%cd%
cd %CURRENT_DIR%
:gotHome
if exist "%CATALINA_HOME%\bin\catalina.bat" goto okHome
echo The CATALINA_HOME environment variable is not defined correctly
echo This environment variable is needed to run this program
goto end
:okHome

set EXECUTABLE=%CATALINA_HOME%\bin\catalina.bat

rem Check that target executable exists
if exist "%EXECUTABLE%" goto okExec
echo Cannot find %EXECUTABLE%
echo This file is needed to run this program
goto end
:okExec

rem Get remaining unshifted command line arguments and save them in the
set CMD_LINE_ARGS=
:setArgs
if ""%1""=="""" goto doneSetArgs
set CMD_LINE_ARGS=%CMD_LINE_ARGS% %1
shift
goto setArgs
:doneSetArgs

call "%EXECUTABLE%" start %CMD_LINE_ARGS%

:end
