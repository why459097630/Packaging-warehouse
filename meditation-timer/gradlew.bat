@echo off
set DIR=%~dp0
set JAVA_EXE=%JAVA_HOME%\bin\java.exe

"%JAVA_EXE%" -classpath "%DIR%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
