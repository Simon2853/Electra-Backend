@echo off
chcp 65001 >nul
rem ============================================================================================
rem  Levanta todo el backend de Electra, cada microservicio en su propia ventana.
rem  Requisito: MySQL corriendo (Laragon -> Start All).
rem  Para detener un servicio basta con cerrar su ventana (o Ctrl+C dentro de ella).
rem
rem  ORDEN:
rem   1. eureka-server  -> es el registro: los demás se anuncian aquí al arrancar.
rem   2. los 4 servicios de negocio (usuarios, pruebas, protocolos, archivos) -> se registran en Eureka.
rem   3. api-gateway    -> al final, para que al consultar Eureka ya encuentre a los 4 servicios.
rem ============================================================================================

cd /d "%~dp0"

rem Usa el JDK de Android Studio si no hay otro configurado.
if not defined JAVA_HOME set "JAVA_HOME=C:\Program Files\Android\Android Studio\jbr"
if not exist "%JAVA_HOME%\bin\java.exe" (
    echo No se encontro Java en "%JAVA_HOME%". Configura JAVA_HOME y vuelve a intentarlo.
    pause
    exit /b 1
)

rem ".\mvnw.cmd" (con .\) funciona aunque Windows tenga desactivada la búsqueda en la carpeta actual.
echo [1/3] Iniciando eureka-server...
start "eureka-server :8761" /D "%~dp0eureka-server" cmd /k .\mvnw.cmd spring-boot:run

echo       Esperando a que Eureka responda en http://localhost:8761 ...
set intentos=0
:esperarEureka
curl -s -o nul http://localhost:8761 && goto eurekaListo
set /a intentos+=1
if %intentos% GEQ 60 goto eurekaNoArranco
timeout /t 3 /nobreak >nul
goto esperarEureka

:eurekaListo
echo       Eureka listo.

echo [2/3] Iniciando los microservicios de negocio...
start "usuarios-service :8081"   /D "%~dp0usuarios-service"   cmd /k .\mvnw.cmd spring-boot:run
start "pruebas-service :8082"    /D "%~dp0pruebas-service"    cmd /k .\mvnw.cmd spring-boot:run
start "protocolos-service :8083" /D "%~dp0protocolos-service" cmd /k .\mvnw.cmd spring-boot:run
start "archivos-service :8084"   /D "%~dp0archivos-service"   cmd /k .\mvnw.cmd spring-boot:run

echo       Esperando a que usuarios-service responda en http://localhost:8081 ...
set intentos=0
:esperarServicios
curl -s -o nul http://localhost:8081/api/usuarios && goto serviciosListos
set /a intentos+=1
if %intentos% GEQ 60 goto usuariosNoArranco
timeout /t 3 /nobreak >nul
goto esperarServicios

:serviciosListos
echo [3/3] Iniciando api-gateway...
start "api-gateway :8080" /D "%~dp0api-gateway" cmd /k .\mvnw.cmd spring-boot:run

echo.
echo Listo. En unos 30 segundos todo deberia estar disponible:
echo   - Eureka (servicios registrados): http://localhost:8761
echo   - Gateway (la URL de la app):     http://localhost:8080
echo.
pause
exit /b 0

:eurekaNoArranco
echo.
echo Eureka no respondio en 3 minutos. Revisa el error en la ventana "eureka-server :8761".
pause
exit /b 1

:usuariosNoArranco
echo.
echo usuarios-service no respondio en 3 minutos. Revisa su ventana: lo mas comun es que
echo MySQL no este corriendo (Laragon -^> Start All).
pause
exit /b 1
