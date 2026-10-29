@echo off
rem Detiene los microservicios iniciados con iniciar.bat (cierra sus ventanas).
taskkill /F /T /FI "WINDOWTITLE eq Guali - *" >nul 2>&1
echo Servicios de Codifica con Guali detenidos.
timeout /t 2 >nul
