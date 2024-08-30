@echo off
set MPJ_HOME=C:\mpj
set JAVA_HOME=C:\Program Files\Java\jdk-18.0.2.1
echo Using Java at %JAVA_HOME%
"%JAVA_HOME%\bin\java" -version
"%MPJ_HOME%\bin\mpjrun.bat" %*