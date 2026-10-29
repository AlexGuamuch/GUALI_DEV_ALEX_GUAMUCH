@echo off
setlocal
title Codifica con Guali - inicio
cd /d "%~dp0"

echo ==========================================================
echo   Codifica con Guali - iniciar los microservicios
echo ==========================================================
echo.

rem --- Java 25 ---
if defined JAVA_HOME (set "JAVA_CMD=%JAVA_HOME%\bin\java.exe") else (set "JAVA_CMD=java")
set "JAVA_VERSION="
for /f "tokens=3" %%v in ('call "%JAVA_CMD%" -version 2^>^&1 ^| findstr /i "version"') do set "JAVA_VERSION=%%~v"
set "JAVA_MAYOR=0"
for /f "delims=." %%m in ("%JAVA_VERSION%") do set "JAVA_MAYOR=%%m"
if %JAVA_MAYOR% LSS 25 (
  echo [ERROR] Se necesita JDK 25 o superior. Instalalo y configura JAVA_HOME, por ejemplo:
  echo         setx JAVA_HOME "C:\Program Files\Java\jdk-25"
  echo         Version encontrada:
  "%JAVA_CMD%" -version
  goto :fin
)

rem --- Configuracion ---
if not exist "configuracion.properties" (
  echo [ERROR] Falta configuracion.properties en %CD%
  goto :fin
)
echo Conexion a MariaDB: revisa configuracion.properties si algo falla.
echo.

rem --- Puertos libres ---
for %%P in (8080 8081 8082 8083) do (
  netstat -ano | findstr /R /C:":%%P .*LISTENING" >nul && (
    echo [ERROR] El puerto %%P ya esta en uso. Cierra la otra aplicacion ^(por ejemplo Docker^) o ejecuta detener.bat.
    goto :fin
  )
)

rem --- Compilar (la primera vez descarga Maven y las dependencias) ---
echo Compilando el proyecto...
call "%~dp0mvnw.cmd" -q -B -DskipTests package
if errorlevel 1 (
  echo [ERROR] No se pudo compilar el proyecto.
  goto :fin
)

rem --- Iniciar cada microservicio en su propia ventana ---
start "Guali - compilador (8083)" cmd /k call "%JAVA_CMD%" -jar services\compilador-service\target\compilador-service-2.0.0.jar
start "Guali - lecciones (8081)"  cmd /k call "%JAVA_CMD%" -jar services\lecciones-service\target\lecciones-service-2.0.0.jar
start "Guali - admin (8082)"      cmd /k call "%JAVA_CMD%" -jar services\admin-service\target\admin-service-2.0.0.jar
start "Guali - gateway (8080)"    cmd /k call "%JAVA_CMD%" -jar services\gateway\target\gateway-2.0.0.jar

echo Esperando a que los servicios esten listos...
set /a intentos=0
:esperar
set /a intentos+=1
ping -n 4 127.0.0.1 >nul
curl -s -o nul -f http://localhost:8080/api/lecciones && goto :listo
if %intentos% lss 60 goto :esperar
echo [AVISO] Los servicios tardan en responder. Revisa las ventanas "Guali - ..." para ver el error.
goto :fin

:listo
echo.
echo Listo: http://localhost:8080
echo Administracion: http://localhost:8080/login.html  (usuario y clave en configuracion.properties)
if not defined GUALI_SIN_NAVEGADOR start "" http://localhost:8080

:fin
echo.
pause
