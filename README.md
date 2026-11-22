# Electra Backend

Microservicios de **Electra**, la app de Android para registrar pruebas eléctricas en terreno y los protocolos que las formalizan. La app ([repositorio Electra](https://github.com/Simon2853/Electra)) consume estos servicios con Retrofit a través del API Gateway.

**Tecnologías:** Kotlin · Spring Boot 4.1.3 · Spring Cloud 2025.1.4 (Eureka, Gateway, OpenFeign) · Spring Data JPA · MySQL 8 (Laragon) · springdoc-openapi (Swagger) · JUnit 5 + MockK · JaCoCo · Maven

## Integrantes

| Nombre | Rol en el proyecto |
|---|---|
| **Simón Muñoz** | Proyecto individual: diseño, desarrollo de la app Android y de los microservicios, pruebas y documentación. |

## Funcionalidades

- **usuarios-service:** registro e inicio de sesión, contraseñas cifradas con BCrypt, recuperar y cambiar contraseña, editar perfil y avatar, y roles (técnico, supervisor y administrador).
- **pruebas-service:** registrar pruebas eléctricas y consultar el historial por equipo. Cada usuario ve lo que le permite su rol (lo consulta a `usuarios-service` con Feign).
- **protocolos-service:** crear protocolos con un número correlativo único, editar observaciones y formalizarlos (solo supervisor o administrador). Un protocolo formalizado ya no se modifica.
- **archivos-service:** subir, mostrar y borrar las fotos de evidencia y de perfil, con validación del formato real de la imagen y un tamaño máximo de 5 MB.
- **api-gateway + eureka-server:** única entrada para la app (puerto 8080) y descubrimiento de los servicios por nombre.
- **En todos los servicios:** validaciones de los datos de entrada, errores uniformes con el código HTTP correcto, documentación Swagger y pruebas unitarias con cobertura JaCoCo sobre el 80 %.

> **API externa, APK firmado, llave .jks y planificación en Trello:** están en el README de la app,
> [Electra](https://github.com/Simon2853/Electra#readme). La API externa (Open-Meteo) la consume la app
> directamente y no pasa por este backend.

---

## Arquitectura

```mermaid
flowchart LR
    App["App Android<br/>(Retrofit + Coil)"] -->|HTTP| GW["api-gateway<br/>:8080"]
    GW -. pregunta dónde está cada servicio .-> EU["eureka-server<br/>:8761"]
    GW --> US["usuarios-service<br/>:8081"]
    GW --> PS["pruebas-service<br/>:8082"]
    GW --> PR["protocolos-service<br/>:8083"]
    GW --> AR["archivos-service<br/>:8084"]
    PS -- Feign --> US
    PR -- Feign --> US
    PR -- Feign --> PS
    US --- DB1[("electra_usuarios")]
    PS --- DB2[("electra_pruebas")]
    PR --- DB3[("electra_protocolos")]
    AR --- DB4[("electra_archivos")]
    AR --- DISK[/"uploads/"/]
```

**El recorrido de una petición:** la app llama a `https://<url-publica>/api/pruebas?usuario=...`. El **Gateway** revisa el prefijo de la ruta y le pregunta a **Eureka** dónde está `pruebas-service`. El servicio que recibe la petición le pregunta a `usuarios-service` (**Feign**) cuál es el rol del usuario, consulta **su propia base de datos** y responde con JSON.

| Servicio | Puerto | Responsabilidad | Base de datos |
|---|---|---|---|
| `eureka-server` | 8761 | Registro y descubrimiento de servicios | — |
| `api-gateway` | 8080 | Única entrada para la app; enruta según el prefijo | — |
| `usuarios-service` | 8081 | Cuentas, login, contraseñas (BCrypt), perfil y roles | `electra_usuarios` |
| `pruebas-service` | 8082 | Pruebas eléctricas e historial por equipo | `electra_pruebas` |
| `protocolos-service` | 8083 | Protocolos, número correlativo único, formalización | `electra_protocolos` |
| `archivos-service` | 8084 | Fotos de evidencia y de perfil | `electra_archivos` + carpeta `uploads/` |

### Decisiones técnicas

- **Una base de datos por servicio.** No hay tablas compartidas ni llaves foráneas entre servicios. Por eso `creadoPor` es texto (el login), y cuando un servicio necesita datos de un usuario se los pide a `usuarios-service` con Feign.
- **Los servicios se buscan por nombre, no por IP.** Gateway y Feign usan `lb://usuarios-service`, y Eureka les dice la dirección. Si un servicio cambia de puerto o se levantan dos copias, nada más cambia.
- **Los permisos los valida el servidor.** La app oculta los botones según el rol, pero el servidor también lo comprueba. Así, quien llame directo a la API no puede saltarse las reglas.
- **Errores uniformes.** Todos los servicios responden `{"estado", "mensaje", "campos"}` con el código HTTP correcto: 400 datos inválidos, 401 credenciales, 403 sin permiso, 404 no existe, 409 choca con el estado, 413/415 imagen inválida, 503 otro servicio caído.
- **Tolerancia a fallas.** Si `usuarios-service` está caído, los demás responden 503 con un mensaje claro en vez de caerse.

### Reglas de negocio por servicio

**usuarios-service**
- Las contraseñas se guardan cifradas con BCrypt, nunca en texto plano. Las respuestas no incluyen el hash.
- El login da el mismo error si el usuario no existe o si la contraseña es incorrecta, para no revelar qué cuentas existen.
- Para recuperar la contraseña se pide el usuario y el nombre registrado.

**pruebas-service**
- Visibilidad: el técnico ve solo sus pruebas. Supervisor y administrador ven las de todos, desde que se creó su cuenta.
- El supervisor no registra pruebas (separación de funciones).
- Una prueba no se edita, porque es evidencia. Solo pueden eliminarla quien la registró o un administrador.

**protocolos-service**
- El número `#000123` sale del id `AUTO_INCREMENT` de MySQL, así que dos técnicos que guardan al mismo tiempo nunca obtienen el mismo número.
- Crean protocolos el técnico y el administrador. Formalizan el supervisor y el administrador. Queda registrado quién formalizó y cuándo.
- Un protocolo formalizado no se edita, no cambia de estado y no se elimina (409).

**archivos-service**
- Solo acepta JPEG, PNG o WebP, de hasta 5 MB. El formato se detecta por los primeros bytes del archivo, no por la extensión.
- El id es un UUID aleatorio, para que no se puedan adivinar fotos ajenas.
- Devuelve una ruta relativa (`/api/archivos/{id}`) y la app le antepone su URL base. Así las fotos siguen cargando aunque cambie la URL pública.

---

## Requisitos

- **JDK 21 o superior.** Sirve el que trae Android Studio: `C:\Program Files\Android\Android Studio\jbr`.
- **MySQL 8 en el puerto 3306,** usuario `root` sin contraseña (lo que trae Laragon por defecto). Las bases de datos se crean solas al arrancar cada servicio.
- Maven **no** hace falta instalarlo: cada servicio trae `mvnw`.

## Cómo levantarlo

### Opción rápida: `iniciar-todo.cmd`

1. Abre **Laragon** y presiona **Start All**. Comprueba que MySQL esté corriendo.
2. Haz doble clic en **`iniciar-todo.cmd`**, en la raíz del repositorio. Abre cada microservicio en su propia ventana, **minimizada en la barra de tareas** (un clic para ver su registro si algo falla), y en este orden:
   1. `eureka-server`, y espera a que responda;
   2. `usuarios`, `pruebas`, `protocolos` y `archivos`, y espera a que respondan;
   3. `api-gateway`.
3. Para detener un servicio, cierra su ventana desde la barra de tareas. Para apagar todo el backend, ciérralas todas.

El script usa el JDK de Android Studio si `JAVA_HOME` no está configurado. Si un servicio no arranca en 3 minutos, avisa cuál fue. Lo más común es que MySQL no esté corriendo.

**Por qué ese orden:** Eureka es el registro donde cada servicio se anuncia al arrancar ("soy pruebas-service y estoy en el puerto 8082"). El Gateway le pregunta a Eureka dónde está cada servicio para reenviarle las peticiones de la app. El orden no es obligatorio, porque los servicios reintentan registrarse y el Gateway vuelve a consultar Eureka cada 30 segundos. Pero así se evitan los errores 503 mientras todo se encuentra.

### Opción manual, paso a paso (una terminal por servicio)

Cada servicio necesita **su propia terminal abierta** mientras se use: si se cierra la terminal, ese servicio se apaga.

**1. Base de datos**

Abre **Laragon**, presiona **Start All** y comprueba que MySQL quede en verde. No hay que crear nada: las bases de datos (`electra_usuarios`, `electra_pruebas`...) se crean solas.

**2. Abrir el proyecto en VS Code**

**Archivo → Abrir carpeta** → `electra-backend`. Abre una terminal con **Terminal → Nueva terminal**; debe ser PowerShell.

**3. Indicar qué Java usar, en cada terminal nueva**

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
```

**4. Levantar los servicios en este orden**, cada uno en una terminal nueva (botón **+** del panel de terminal), repitiendo el paso 3 en cada una:

| Orden | Comando | Espera hasta ver |
|---|---|---|
| 1 | `cd eureka-server; .\mvnw spring-boot:run` | `Started EurekaServerApplication` |
| 2 | `cd usuarios-service; .\mvnw spring-boot:run` | `Started UsuariosServiceApplication` |
| 3 | `cd pruebas-service; .\mvnw spring-boot:run` | `Started PruebasServiceApplication` |
| 4 | `cd protocolos-service; .\mvnw spring-boot:run` | `Started ProtocolosServiceApplication` |
| 5 | `cd archivos-service; .\mvnw spring-boot:run` | `Started ArchivosServiceApplication` |
| 6 | `cd api-gateway; .\mvnw spring-boot:run` | `Started ApiGatewayApplication` |

Los pasos 2 a 5 se pueden lanzar uno tras otro sin esperar entre ellos. Lo importante es que **Eureka vaya primero y el Gateway al final**.

> El `.\` delante de `mvnw` es necesario si Windows tiene activada la opción de seguridad `NoDefaultCurrentDirectoryInExePath`. Con `.\` funciona siempre.

**5. Comprobar que todo esté arriba**

- http://localhost:8761 → en *Instances currently registered with Eureka* deben aparecer **5**: API-GATEWAY, USUARIOS-SERVICE, PRUEBAS-SERVICE, PROTOCOLOS-SERVICE y ARCHIVOS-SERVICE.
- http://localhost:8080/api/usuarios → debe responder la lista de usuarios en JSON. Si responde 503, espera 30 segundos y vuelve a intentarlo: el Gateway todavía no se entera de que los servicios existen.

En VS Code también se pueden levantar con la extensión **Spring Boot Dashboard**.

### Apagar todo

En cada terminal presiona **Ctrl + C** (si pregunta, responde `S`), o cierra las terminales. Si se usó el script, cierra sus ventanas desde la barra de tareas. Los datos quedan guardados en MySQL.

## Usar la app con el backend

### Con el emulador de Android Studio

1. Con el backend arriba, abre el proyecto **Electra** en Android Studio.
2. Revisa la URL en `app/src/main/java/com/example/electra/data/remote/ConfiguracionApi.kt`:
   ```kotlin
   const val BASE_URL = "http://10.0.2.2:8080/"
   ```
   `10.0.2.2` es cómo el emulador ve al computador. Con el emulador no se cambia.
3. Elige el emulador y presiona **Run ▶**.
4. Cuando la app pregunte por **dispositivos cercanos**, presiona **Allow**. Es el permiso de red local que exige Android 17: sin él la app no llega al servidor.
5. Inicia sesión, o crea una cuenta desde "Regístrate".

Si aparece la franja roja "Sin conexión con el servidor", no hay que reiniciar nada: la app reintenta cada 10 segundos y, cuando el backend responde, la franja desaparece y la pantalla recarga sus datos.

### Con un teléfono físico (Gateway expuesto a Internet)

El teléfono no puede llegar a `localhost` ni a `10.0.2.2` del computador, así que se publica **solo el puerto 8080** (el Gateway) con el reenvío de puertos de VS Code:

1. En VS Code, abre el panel **Puertos** (*Ports*, junto a la Terminal) y elige **Reenviar un puerto** → `8080`. Pide iniciar sesión con GitHub.
2. Haz clic derecho sobre el puerto → **Visibilidad del puerto** → **Pública**.
3. Copia la **dirección reenviada** (algo como `https://abc123-8080.brs.devtunnels.ms`) y pégala en `ConfiguracionApi.kt`, con `/` al final:
   ```kotlin
   const val BASE_URL = "https://abc123-8080.brs.devtunnels.ms/"
   ```
4. Conecta el teléfono por USB con la depuración USB activada, elígelo en Android Studio y presiona **Run ▶**.

La URL **cambia cada vez que se crea el túnel de nuevo**: si la app deja de conectar, revisa que `BASE_URL` sea la actual. Por eso la URL está en un solo lugar y las fotos se guardan con ruta relativa: siguen cargando aunque cambie.

> Dev Tunnels muestra una página de advertencia a los navegadores. La app la omite enviando el encabezado `X-Tunnel-Skip-AntiPhishing-Page: true`, que ya va en todas las llamadas de Retrofit y de Coil.

### Problemas frecuentes

| Síntoma | Causa probable | Qué hacer |
|---|---|---|
| `mvnw no se reconoce` | Falta `.\` | Escribe `.\mvnw` |
| `JAVA_HOME is not set` | No se configuró Java en esa terminal | Repite el paso 3 |
| `Communications link failure` en un servicio | MySQL no está corriendo | Laragon → Start All, y reinicia ese servicio |
| `Port 8080 was already in use` | Ese servicio ya está corriendo en otra terminal o ventana | Cierra la otra, o usa la que ya está corriendo |
| La app dice "sin conexión" con todo arriba (emulador) | Se rechazó el permiso de red local | Ajustes del emulador → Apps → Electra → Permisos → Dispositivos cercanos → Permitir |
| La app dice "sin conexión" con todo arriba (teléfono) | `BASE_URL` apunta a un túnel antiguo o privado | Revisa que el puerto sea **Público** y que la URL sea la actual |
| Eureka muestra menos de 5 servicios | Alguno se cayó al arrancar | Revisa su terminal: el error aparece al final |
| La app recibe 503 | El Gateway todavía no ve los servicios | Espera 30 segundos |

---

## Endpoints

La documentación completa, con ejemplos y la opción de probar cada endpoint, está en el **Swagger** de cada servicio:

| Servicio | Swagger UI |
|---|---|
| usuarios | http://localhost:8081/swagger-ui.html |
| pruebas | http://localhost:8082/swagger-ui.html |
| protocolos | http://localhost:8083/swagger-ui.html |
| archivos | http://localhost:8084/swagger-ui.html |

Todas las rutas se llaman a través del Gateway (`:8080`):

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/auth/registro` | Crear una cuenta (rol `tecnico` por defecto) |
| POST | `/api/auth/login` | Iniciar sesión; devuelve los datos y el rol |
| POST | `/api/auth/restablecer` | Recuperar la contraseña con usuario y nombre |
| GET | `/api/usuarios` | Listar las cuentas |
| GET | `/api/usuarios/{usuario}` | Obtener una cuenta |
| PUT | `/api/usuarios/{usuario}/nombre` · `/contrasena` · `/avatar` · `/rol` | Editar el perfil |
| POST | `/api/pruebas` | Registrar una prueba |
| GET | `/api/pruebas?usuario=&equipo=` | Pruebas visibles para el usuario; con `equipo`, el historial |
| GET | `/api/pruebas/equipos?usuario=` | Equipos con pruebas |
| GET · DELETE | `/api/pruebas/{id}?usuario=` | Obtener o eliminar una prueba |
| POST | `/api/protocolos` | Crear un protocolo (el número lo asigna el servidor) |
| GET | `/api/protocolos?usuario=` | Protocolos visibles para el usuario |
| GET | `/api/protocolos/siguiente-numero` | Próximo número (referencial) |
| GET · DELETE | `/api/protocolos/{numero}?usuario=` | Obtener o eliminar (`{numero}`: `123`, `000123` o `%23000123`) |
| GET | `/api/protocolos/{numero}/pruebas?usuario=` | Pruebas del equipo del protocolo (vía Feign a pruebas) |
| PUT | `/api/protocolos/{numero}/estado` · `/observaciones` | Formalizar o editar las observaciones |
| POST | `/api/archivos` | Subir una imagen (multipart: `archivo`, `categoria`, `subidoPor`) |
| GET | `/api/archivos/{id}` | Descargar la imagen (la usa Coil) |
| GET | `/api/archivos/{id}/info` · `/api/archivos?subidoPor=` | Datos de una imagen o de todas las de un usuario |
| DELETE | `/api/archivos/{id}?usuario=` | Eliminar una imagen propia |

---

## Pruebas unitarias y cobertura

Cada servicio de negocio tiene pruebas de **modelo**, **servicio** (con repositorio y Feign simulados con MockK) y **controlador** (MockMvc, sin servidor ni base de datos). No necesitan MySQL ni otros servicios corriendo.

```powershell
cd usuarios-service
.\mvnw verify      # ejecuta las pruebas, genera el informe y falla si la cobertura baja del 80 %
```

El informe de JaCoCo queda en `target/site/jacoco/index.html`.

| Servicio | Pruebas | Cobertura de líneas |
|---|---|---|
| usuarios-service | 29 | 94,6 % |
| pruebas-service | 28 | 95,1 % |
| protocolos-service | 30 | 95,1 % |
| archivos-service | 23 | 93,6 % |

En la cobertura se excluyen solo las clases `*Application` (el `main` que arranca Spring), porque no tienen lógica.

## Estructura de cada servicio

```
src/main/kotlin/com/electra/<servicio>/
├── controller/   endpoints REST (traducen HTTP ↔ servicio)
├── service/      reglas de negocio
├── repository/   acceso a MySQL con Spring Data JPA
├── model/        entidades (tablas) y reglas del dominio
├── dto/          lo que entra y sale en JSON, con validaciones
├── client/       clientes Feign hacia otros servicios (pruebas y protocolos)
├── exception/    errores de negocio y su traducción a códigos HTTP
└── config/       Swagger y otros beans
```
