# 🎓 Little Horizons ERP — Sistema de Gestión Educativa

ERP de consola en **Java + MySQL** para el **Colegio Internacional Little Horizons**: centraliza la gestión académica, extracurricular, financiera (con facturación electrónica simulada según la DIAN) y de recursos del colegio, siguiendo una arquitectura **MVC** con capa **DAO**.

---

## Tabla de contenido

1. [Título del proyecto](#-little-horizons-erp--sistema-de-gestión-educativa)
2. [Descripción](#2-descripción)
3. [Autores](#3-autores)
4. [Tecnologías utilizadas](#4-tecnologías-utilizadas)
5. [Requerimientos previos](#5-requerimientos-previos)
6. [Configuración de la base de datos](#6-configuración-de-la-base-de-datos)
7. [Instalación y ejecución](#7-instalación-y-ejecución)
8. [Estructura del proyecto](#8-estructura-del-proyecto)
9. [Principios y patrones de diseño aplicados](#9-principios-y-patrones-de-diseño-aplicados)
10. [Funcionalidades principales](#10-funcionalidades-principales)
11. [Manejo de errores y logs](#11-manejo-de-errores-y-logs)
12. [Capturas de pantalla](#12-capturas-de-pantalla)
13. [Problemas conocidos y limitaciones](#13-problemas-conocidos-y-limitaciones)
14. [Futuras mejoras](#14-futuras-mejoras)
15. [Licencia](#15-licencia)
16. [Contacto](#16-contacto)
17. [Anexo A. Verificación de requisitos del enunciado](#anexo-a-verificación-de-requisitos-del-enunciado)
18. [Anexo B. Recorrido de prueba por módulo](#anexo-b-recorrido-de-prueba-por-módulo)
19. [Anexo C. Examen: buscador avanzado de estudiantes](#anexo-c-examen-buscador-avanzado-de-estudiantes)

---

## 2. Descripción

### Propósito

El colegio atiende a más de 500 estudiantes (preescolar a grado 11°) con 40 profesores, y hasta ahora manejaba su información en cuadernos y hojas de cálculo dispersas. Este ERP reemplaza esos registros por una única base de datos relacional y una aplicación de consola que conecta las áreas académica, administrativa y financiera.

### Problemas que resuelve

| Problema en el colegio | Solución en el ERP |
|---|---|
| Notas en cuadernos que se pierden o dañan | Registro de calificaciones (0.0–5.0) por materia y período, con historial completo desde el ingreso del estudiante |
| Sin registro centralizado de clubes y eventos | Catálogo de clubes con **validación de cupo máximo**, inscripciones, eventos y posiciones en competencias |
| Facturación manual, lenta y fuera de norma | Registro de pagos con **factura electrónica en archivo `.txt`** (CUFE con SHA‑384, código QR y resolución DIAN simulados) |
| Libros y equipos que "se pierden" | Inventario con código único y **sistema de préstamos** a estudiantes y profesores con control de vencidos |
| Profesores sin visibilidad del rendimiento | Promedios automáticos, ranking por curso, alertas de estudiantes que **necesitan apoyo** y **patrones de inasistencia** |

### Funcionalidades principales (resumen)

- **Gestión académica:** estudiantes, acudientes (hermanos comparten acudiente), matrículas por año, inscripción en materias, notas, promedios y asistencia.
- **Profesores:** directorio, asignación de materias por sección y horarios con detección de cruces.
- **Extracurricular:** clubes con cupo, eventos con competencia y resultados.
- **Financiero:** conceptos de pago, valores personalizados (becas, descuento por hermanos), pagos, facturación electrónica y reporte de morosos.
- **Recursos:** inventario, préstamos, devoluciones y reporte de préstamos vencidos.
- **Reportes:** promedios por curso, ranking, alertas académicas, inasistencia, participación y panel general.

---

## 3. Autores

| Nombre completo | Correo electrónico | GitHub | Rol |
|---|---|---|---|
| Anaya | andrescamiloanayag@gmail.com | [@ARSCIAND](https://github.com/ARSCIAND) | Desarrollo full‑stack (BD, backend, consola) |

> Curso de Software — Módulo de Java. Proyecto final: ERP educativo para el Colegio Internacional Little Horizons.

---

## 4. Tecnologías utilizadas

| Categoría | Tecnología |
|---|---|
| Lenguaje | **Java 17** (compatible con JDK 17, 21 y superiores). Usa `record`, `switch` con flechas y *text blocks* |
| IDE | **IntelliJ IDEA** (Community o Ultimate). El repositorio incluye la configuración del módulo y una *Run Configuration* llamada `Main` |
| Base de datos | **MySQL 8.0 o superior** (requerido por los `CHECK` constraints) |
| Acceso a datos | **JDBC** puro con `PreparedStatement` y *try‑with‑resources* |
| Librerías externas | **MySQL Connector/J 9.x** (`com.mysql:mysql-connector-j`) — único JAR externo |
| Facturas | Archivos de texto UTF‑8 (`.txt`) generados con `java.nio.file`, **sin dependencias**; el QR simulado se dibuja con caracteres de bloque |
| Hash CUFE / QR | `java.security.MessageDigest` (SHA‑384 y SHA‑256) del JDK |
| Control de versiones | Git + GitHub (repositorio privado) |

---

## 5. Requerimientos previos

Antes de empezar, el equipo debe tener instalado lo siguiente:

| Software | Versión mínima | Cómo verificarlo | Dónde descargarlo |
|---|---|---|---|
| JDK (Java Development Kit) | **17** (probado con 17 y 21) | `java -version` y `javac -version` | <https://adoptium.net> |
| **MySQL Server** | **8.0 o superior** ⚠️ | `mysql --version` o el servicio `MySQL80`/`MySQL84` en `services.msc` | <https://dev.mysql.com/downloads/mysql/> |
| MySQL Connector/J (driver JDBC) | 8.0 o superior (se usa 9.1.0) | archivo `.jar` dentro de `lib/` | se descarga con un script (paso 6.3) |
| IntelliJ IDEA | 2022.3 o superior (Community sirve) | — | <https://www.jetbrains.com/idea/download/> |
| Cliente SQL | DBeaver, MySQL Workbench o consola `mysql` | — | <https://dbeaver.io/download/> |
| Git | 2.x | `git --version` | <https://git-scm.com/downloads> |

> ⚠️ **Se requiere MySQL 8.0 o superior.** En MySQL 5.7 los `CHECK` constraints se ignoran, y entonces la base aceptaría notas fuera del rango 0.0–5.0 o montos negativos.

### Datos del entorno de desarrollo

El proyecto se desarrolló y probó con esta configuración. Si su equipo usa los mismos datos, **no necesita cambiar nada** en `config/db.properties`:

| Dato | Valor usado |
|---|---|
| Sistema operativo | Windows 11 |
| JDK | 21 |
| MySQL | 8.4 LTS (servicio `MySQL84`), también probado en 8.0 |
| Host / puerto | `localhost` / `3306` |
| Usuario de MySQL | `root` |
| **Contraseña de MySQL** | **`1234`** |
| Base de datos | `erp_little_horizons` |

---

## 6. Configuración de la base de datos

### 6.1. Creación de la base de datos

Todo está en un único archivo: [`docs/database.sql`](docs/database.sql). Este archivo crea la base `erp_little_horizons` desde cero y la llena con datos de prueba.

**Opción A: DBeaver (recomendada)**

1. Abrir DBeaver y conectarse al MySQL local (host `localhost`, puerto `3306`, usuario `root`, contraseña `1234` o la suya).
   - Si la conexión falla con *"Public Key Retrieval is not allowed"*: editar la conexión → pestaña **Driver properties** → poner `allowPublicKeyRetrieval` en `true`.
2. Abrir el archivo `docs/database.sql` de una de estas formas:
   - Arrastrarlo desde el Explorador de Windows y soltarlo sobre DBeaver, **o**
   - abrirlo con el Bloc de notas, copiar todo (Ctrl+A, Ctrl+C), y pegarlo en un editor SQL nuevo de la conexión (clic derecho sobre la conexión → *Editor SQL* → *Nuevo script SQL*).
3. Verificar que arriba del editor esté seleccionada la conexión `localhost`.
4. Ejecutar el **script completo** con **Alt + X**. No usar Ctrl + Enter, porque solo ejecuta una sentencia.
5. DBeaver muestra la advertencia *"You are about to execute a DROP statement"*: pulsar **Aceptar**. El script borra la base solo si ya existía.
6. En la pestaña *Salida* puede aparecer *"Can't drop database 'erp_little_horizons'"*. **No es un error**: es un aviso de MySQL que indica que la base aún no existía.
7. Al terminar, clic derecho sobre la conexión → **Refrescar**.

**Opción B: consola de MySQL**

```bash
mysql -u root -p --default-character-set=utf8mb4 < docs/database.sql
```

**✅ Cómo saber que quedó bien:** debe existir la base `erp_little_horizons` con **24 tablas**, y la tabla `estudiantes` debe tener **42 registros**. Se puede comprobar con:

```sql
SELECT COUNT(*) FROM erp_little_horizons.estudiantes;   -- resultado esperado: 42
```

> El script se puede ejecutar todas las veces que se quiera; cada vez deja la base en su estado inicial de prueba.

#### Contenido del script

El archivo tiene dos secciones:

- **Sección 1 – DDL (estructura)**
  - **1A. Script oficial del anexo, copiado sin ningún cambio** (versión 2.0: `DECIMAL(2,1)` para notas y tablas separadas `prestamos_estudiante` / `prestamos_profesor`): 22 tablas.
  - **1B. Extensiones**, que solo **agregan** al esquema oficial: 2 tablas nuevas y 5 restricciones `CHECK`.
- **Sección 2 – DML (datos de prueba)**: registros coherentes para el año académico 2026 (calendario A).

**Extensiones justificadas (Sección 1B).** El anexo permite agregar elementos siempre que se justifiquen:

| Extensión | Justificación |
|---|---|
| Tabla `horarios_clase` | El requerimiento 1.2 pide registrar los días y horas de cada asignación para evitar cruces; el script oficial no tenía dónde guardarlos. |
| Tabla `valores_personalizados` | El requerimiento 3.1 pide valores diferenciados por estudiante (descuento por hermanos, becas); el script oficial no lo soportaba. |
| 5 `CHECK` adicionales (`ALTER TABLE … ADD CONSTRAINT`) | Montos > 0, cupo de club > 0 y `fecha_devolucion_esperada >= fecha_prestamo` en las dos tablas de préstamos. |

Ninguna tabla ni columna del script oficial se modifica. Por ejemplo, `facturas.nombre_archivo_pdf` conserva su nombre original y guarda el nombre del archivo `.txt` de cada factura. Además, los `INSERT` iniciales del anexo (grados, categorías y conceptos) se cargan en la Sección 2 con los mismos valores, para separar claramente DDL y DML.

> **Compatibilidad con el script oficial.** La aplicación también funciona si la base se crea **solo** con el script del anexo, sin la Sección 1B. En ese caso avisa al iniciar y desactiva únicamente los horarios de clase y los valores personalizados (las pensiones se cobran a su valor base). Todo lo demás opera con normalidad.

**Volumen de datos de prueba** (el enunciado pide mínimo 30 en cada tabla principal):

| Tabla | Registros | Tabla | Registros |
|---|---:|---|---:|
| `estudiantes` | 42 | `pagos` | 604 |
| `acudientes` | 38 | `clubes` | 31 |
| `profesores` | 30 | `recursos` | 40 |
| `materias` | 61 | `registros_academicos` | 955 |
| `matriculas` | 151 | `inscripciones_materia` | 792 |
| `asignaciones_profesor` | 61 | `horarios_clase` | 181 |
| `asistencias` | 456 | `membresias_club` | 67 |
| `eventos` | 12 | `participantes_evento` | 69 |
| `prestamos_estudiante` | 31 | `prestamos_profesor` | 12 |

Los datos son coherentes entre sí. Por ejemplo, 12 familias tienen dos hijos que comparten acudiente y reciben un descuento del 10 % en la pensión, y las edades corresponden al grado de cada estudiante. Además, las notas de 2025 aparecen como historial de quienes ya estudiaban en el colegio. Por otro lado, hay casos preparados para probar los reportes: 6 estudiantes en mora, 4 con patrón de inasistencia, préstamos vencidos y un club (Robótica) con el cupo lleno. La tabla `facturas` empieza vacía a propósito, para que cada factura tenga su archivo `.txt` real.

### 6.2. Configuración de credenciales

La aplicación lee la conexión del archivo [`config/db.properties`](config/db.properties). Viene configurado así:

```properties
db.url=jdbc:mysql://localhost:3306/erp_little_horizons?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=America/Bogota&characterEncoding=UTF-8
db.user=root
db.password=1234

app.invoiceDir=invoices
app.logFile=error_log.txt
app.academicYear=2026
```

- Si el usuario de MySQL es `root` con contraseña `1234`, **no hay que cambiar nada**.
- Si la contraseña es otra, basta con cambiar la línea `db.password=` y guardar el archivo.
- `app.academicYear=2026` fija el año académico de los datos de prueba. Sin esa línea, el programa usaría el año actual del computador.

> **¿No recuerda la contraseña de `root`?** Se puede crear un usuario solo para este proyecto. Desde DBeaver, conectado como `root`, se ejecuta:
> ```sql
> CREATE USER IF NOT EXISTS 'erp'@'localhost' IDENTIFIED BY 'erp123';
> GRANT ALL PRIVILEGES ON erp_little_horizons.* TO 'erp'@'localhost';
> FLUSH PRIVILEGES;
> ```
> Y en `config/db.properties` se pone `db.user=erp` y `db.password=erp123`.

### 6.3. Instalación del driver JDBC

El único JAR externo es **MySQL Connector/J**, y debe quedar dentro de la carpeta `lib/`. No viene en el repositorio porque es una librería de terceros que se descarga aparte.

**Windows (PowerShell):**

1. Abrir la carpeta principal del proyecto (la que contiene `config`, `docs`, `lib`, `scripts`, `src`). No entrar a ninguna subcarpeta.
2. En la barra de direcciones del Explorador escribir `powershell` y pulsar Enter.
3. Ejecutar:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\download-driver.ps1
```

Respuesta esperada: `Driver saved to lib\mysql-connector-j-9.1.0.jar`.

**Linux / macOS:**

```bash
./scripts/download-driver.sh
```

**Alternativas, si el script falla:**
- Descargar manualmente <https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/9.1.0/mysql-connector-j-9.1.0.jar> y copiarlo en `lib/`.
- Si en el equipo ya se usó Maven con MySQL, el driver suele estar en `%USERPROFILE%\.m2\repository\com\mysql\mysql-connector-j\` y basta con copiar ese `.jar` a `lib/`.

El módulo de IntelliJ está configurado para cargar **cualquier `.jar` que esté en `lib/`**, así que no hay que agregar la librería a mano.

**✅ Cómo saber que quedó bien:** `dir lib` (Windows) o `ls lib` (Linux/macOS) muestra `mysql-connector-j-9.1.0.jar`.

---

## 7. Instalación y ejecución

### Resumen

```
1. Clonar el repositorio
2. Ejecutar docs/database.sql en MySQL            (sección 6.1)
3. Revisar usuario/contraseña en config/db.properties (sección 6.2)
4. Descargar el driver a lib/                     (sección 6.3)
5. Abrir en IntelliJ, configurar el SDK y ejecutar Main
```

### 7.1. Clonar el repositorio

```bash
git clone https://github.com/ARSCIAND/little-horizons-erp.git
cd little-horizons-erp
```

También se puede descargar desde GitHub con **Code → Download ZIP** y descomprimirlo. En ese caso se abre la carpeta que contiene `src`, no la carpeta que la envuelve.

### 7.2. Abrir el proyecto en el IDE

1. En IntelliJ IDEA: **File → Open…** y seleccionar la carpeta `little-horizons-erp`.
2. Si pregunta, elegir **Trust Project**.
3. **Configurar el SDK.** Si aparece el aviso *"The SDK is not specified for module little-horizons-erp"* o *"Project SDK is not defined"*:
   1. Clic en **Configure…**, o ir a **File → Project Structure** (Ctrl + Alt + Shift + S).
   2. En **Project → SDK** elegir un JDK **17 o superior**. Si la lista está vacía: **Add SDK → Download JDK…** → versión 21 → **Download**.
   3. En **Modules → little-horizons-erp → Dependencies**, el *Module SDK* debe ser **Project SDK**.
   4. **Apply → OK**.
4. Esperar a que IntelliJ termine de indexar (barra de progreso abajo a la derecha).

### 7.3. Compilar

- **IntelliJ:** *Build → Build Project* (Ctrl + F9). Se crea la carpeta `out/`, que no se sube al repositorio.
- **Terminal, sin IntelliJ:**

```bash
./scripts/build.sh        # Linux / macOS
scripts\build.bat         # Windows
```

### 7.4. Ejecutar

- **IntelliJ:** arriba a la derecha, junto al botón ▶ verde, seleccionar la configuración **`Main`** (viene incluida en el repositorio) y pulsar ▶. Si no aparece, abrir `src/com/littlehorizons/erp/Main.java` → clic derecho → **Run 'Main.main()'**.
- **Terminal:**

```bash
./scripts/run.sh          # Linux / macOS
scripts\run.bat           # Windows (activa UTF-8 con chcp 65001)
```

> El directorio de trabajo debe ser la raíz del proyecto, porque de ahí se leen `config/db.properties` y se escriben `invoices/` y `error_log.txt`. La configuración `Main` de IntelliJ ya lo trae así.

**✅ Resultado esperado.** En la consola aparece el menú principal:

```
  ╔══════════════════════════════════════════════╗
  ║   ERP Colegio Little Horizons                ║
  ║   Sistema de Gestión Educativa               ║
  ╚══════════════════════════════════════════════╝

  1. Gestión de Estudiantes
  2. Gestión Académica
  3. Gestión de Profesores
  4. Actividades Extracurriculares
  5. Gestión Financiera
  6. Gestión de Recursos
  7. Reportes y Estadísticas
  8. Salir

  Seleccione una opción:
```

En IntelliJ hay que hacer clic dentro de la consola para poder escribir.

### 7.5. Prueba rápida (5 minutos)

| # | Qué se escribe | Resultado esperado |
|---|---|---|
| 1 | `1` → `2` → `1` | Tabla con los 42 estudiantes |
| 2 | Enter → `0` | Regresa al menú principal |
| 3 | `7` → `6` | Panel general: recaudo del año, **6 estudiantes en mora**, préstamos vencidos |
| 4 | Enter → `0` → `5` → `6` → estudiante `4` → concepto `2` → `s` → (si pregunta por el acudiente) `s` → Enter → `2` → `4` → `Pensión noviembre 2026` → `s` | `✔ Factura electrónica LHFE-000001 generada` y la ruta del archivo |
| 5 | Abrir `invoices/factura_000001.txt` | Factura con datos del colegio, acudiente, detalle, IVA, total, CUFE y QR |
| 6 | Enter → `0` → `4` → `4` → estudiante `1` → club `2` (Robótica) | `✖ El club alcanzó su cupo máximo (10 estudiantes).` |
| 7 | En cualquier formulario escribir `X` | `ℹ Operación cancelada. No se guardaron cambios.` |
| 8 | `0` → `8` → `s` | El programa se cierra |

> **Sobre las fechas.** Los datos de prueba corresponden a 2026 y las cifras de esta tabla se obtuvieron en noviembre de 2026. Los reportes de mora y de préstamos vencidos se calculan contra la fecha actual del computador, así que en otra fecha las cantidades pueden variar.

En el [Anexo B](#anexo-b-recorrido-de-prueba-por-módulo) hay un recorrido completo por todos los módulos.

### 7.6. Solución de problemas

| Mensaje | Causa | Solución |
|---|---|---|
| `No se encontró el driver JDBC de MySQL` | Falta el `.jar` en `lib/` | Repetir el paso 6.3 |
| `Usuario o contraseña de MySQL incorrectos` | Credenciales distintas a las de `config/db.properties` | Corregir `db.user` / `db.password` (paso 6.2) |
| `La base de datos erp_little_horizons no existe` | No se ejecutó el script | Ejecutar `docs/database.sql` con **Alt + X** (paso 6.1) |
| `No fue posible conectarse a MySQL` | El servicio MySQL está detenido | `services.msc` → `MySQL84`/`MySQL80` → **Iniciar** |
| `The SDK is not specified for module…` | IntelliJ sin JDK asignado | Paso 7.2, punto 3 |
| `Public Key Retrieval is not allowed` (en DBeaver) | Opción del driver de DBeaver | `allowPublicKeyRetrieval = true` en *Driver properties* |
| Aparecen `?` en lugar de tildes o recuadros | La terminal de Windows no está en UTF-8 | Usar `scripts\run.bat` o la consola de IntelliJ |
| `ℹ La base de datos no tiene las tablas de extensión…` | Se ejecutó solo el script del anexo | Funciona igual; ejecutar `docs/database.sql` completo para habilitar horarios y valores personalizados |

Cualquier otro error queda registrado con todo su detalle en `error_log.txt`, en la raíz del proyecto.

---

## 8. Estructura del proyecto

```
little-horizons-erp/
├── config/db.properties              # Credenciales y rutas
├── docs/
│   ├── database.sql                  # DDL + DML
│   └── evidence/                     # Facturas de ejemplo (.txt)
├── invoices/                         # Facturas generadas en ejecución (factura_000001.txt)
├── lib/                              # mysql-connector-j-x.x.x.jar
├── scripts/                          # build / run / download-driver (.sh, .bat, .ps1)
├── little-horizons-erp.iml           # Módulo IntelliJ
└── src/com/littlehorizons/erp/
    ├── Main.java                     # Punto de entrada y raíz de composición
    ├── config/        (2)            # AppConfig, SchoolInfo
    ├── model/         (23)           # Entidades del dominio
    │   ├── enums/     (19)           # Enumeraciones mapeadas a los ENUM de MySQL
    │   └── report/    (9)            # Records (DTO) para reportes
    ├── dao/           (20)           # Acceso a datos (JDBC)
    ├── controller/    (7)            # Lógica y reglas de negocio
    ├── view/          (10)           # Interfaces de consola
    ├── service/
    │   └── invoice/   (5)            # Facturación electrónica (CUFE, QR, archivo .txt)
    ├── exception/     (4)            # Excepciones de la aplicación
    └── util/          (6)            # Conexión, log, entrada validada, tablas, formatos
```

**Total:** 107 clases Java. Todo el código (clases, métodos, variables y comentarios) está en inglés. Solo los textos que ve el usuario están en español, y los nombres de tablas y columnas siguen el script oficial.

### Arquitectura MVC + DAO

```
 ┌──────────── VIEW (view/) ────────────┐     ┌──────── CONTROLLER (controller/) ────────┐     ┌──────── MODEL ────────┐
 │ MainMenuView → StudentView,           │ ──▶ │ StudentController, AcademicController,   │ ──▶ │ dao/  (JDBC, SQL)     │
 │ AcademicView, FinanceView, ...        │     │ FinanceController, ReportController...   │     │ model/ (entidades)    │
 │ Solo lee/valida entradas e imprime    │ ◀── │ Reglas de negocio, cálculos con Streams  │ ◀── │ service/invoice (TXT) │
 └───────────────────────────────────────┘     └──────────────────────────────────────────┘     └───────────────────────┘
```

| Paquete | Responsabilidad |
|---|---|
| `model` | Entidades del dominio (`Student`, `Guardian`, `Teacher`, `AcademicRecord`, `Payment`, `Loan`, …). `Person` es la superclase abstracta de estudiantes, acudientes y profesores. |
| `model.enums` | Enumeraciones en inglés que implementan `DbEnum` y conocen su valor exacto en la BD (`AttendanceStatus.MEDICAL_EXCUSE ↔ 'Excusa Médica'`). |
| `model.report` | `record` inmutables para reportes (`SubjectAverage`, `DelinquentAccount`, `AbsenceAlert`, …). |
| `dao` | Un DAO por agregado. `BaseDao` centraliza conexión, `PreparedStatement`, mapeo con lambdas, transacciones y traducción de errores SQL. |
| `controller` | Reglas de negocio: cupos, cruces de horario, validaciones de notas, morosidad, préstamos, promedios. No imprime nada. |
| `view` | Menús y formularios de consola. `ConsoleView` dibuja el menú, ejecuta la opción y convierte excepciones en mensajes. `Lookup` ofrece buscadores reutilizables. |
| `service.invoice` | Flujo de factura electrónica: recolecta datos → calcula IVA y CUFE → genera el archivo `.txt` → guarda en `facturas`. |
| `util` | `DatabaseConnection`, `ErrorLogger`, `InputReader` (entradas validadas), `TablePrinter`, `Formats`, `NumberToWords`. |
| `config` | `AppConfig` (lee `db.properties`) y `SchoolInfo` (datos legales y DIAN del colegio). |

---

## 9. Principios y patrones de diseño aplicados

### 9.1. Principios SOLID

**S — Responsabilidad única.** Cada capa tiene una sola razón para cambiar. Por ejemplo, `FinanceController` decide *cuándo* se emite una factura, `InvoiceService` *cómo* se arma, `TextInvoiceGenerator` *cómo se escribe* y `InvoiceDao` *cómo se guarda*. De igual forma, `InputReader` solo valida entradas y `TablePrinter` solo dibuja tablas.

**O — Abierto/Cerrado.** Para agregar otro formato de factura (por ejemplo, PDF o XML UBL) basta con crear otra implementación de `InvoiceGenerator` y registrarla en `Main`, sin modificar el servicio:

```java
new InvoiceService(paymentDao, studentDao, guardianDao, invoiceDao,
        List.of(new TextInvoiceGenerator()));
```

Asimismo, para agregar una opción a un menú solo hay que añadir un `MenuOption` con su lambda; `ConsoleView.show()` no cambia.

**L — Sustitución de Liskov.** `Student`, `Guardian` y `Teacher` extienden `Person` y pueden usarse donde se espera una persona (`getFullName()`, `getDocumentLabel()`). Del mismo modo, cualquier `InvoiceGenerator` es intercambiable dentro de `InvoiceService`.

**I — Segregación de interfaces.** `CrudDao<T>` define solo `findById`, `findAll`, `save` y `update`. El borrado está en una interfaz aparte, `DeletableDao`, que implementan únicamente `StudentDao`, `EventDao` y `AssignmentDao`, porque la mayoría de los registros escolares no deben borrarse: cambian de estado.

**D — Inversión de dependencias.** Ninguna clase crea sus dependencias. `Main` actúa como raíz de composición y las inyecta por constructor. Así, los controladores reciben sus DAOs y `InvoiceService` recibe una `List<InvoiceGenerator>` (abstracción), no clases concretas.

```java
FinanceController financeController =
        new FinanceController(conceptDao, paymentDao, studentDao, guardianDao, invoiceDao, invoiceService);
```

### 9.2. Patrones de diseño

| Patrón | Dónde | Justificación |
|---|---|---|
| **Singleton** | `DatabaseConnection`, `ErrorLogger` (doble verificación con `volatile`), `AppConfig` (eager) | Una sola configuración y un solo escritor del log, compartidos por toda la app. `DatabaseConnection` entrega una conexión nueva por operación para que cada DAO la cierre con *try‑with‑resources*. |
| **DAO** | Paquete `dao` | Aísla el SQL del resto del sistema; los controladores no conocen JDBC. |
| **Template Method** | `BaseDao` (consultar / insertar / transacción) y `ConsoleView.show()` | El algoritmo común (abrir conexión, enlazar parámetros, mapear, cerrar; o dibujar menú, ejecutar, manejar errores) se escribe una sola vez. |
| **Strategy** | `InvoiceGenerator` → `TextInvoiceGenerator` | El formato de la factura es intercambiable sin tocar `InvoiceService`. |
| **Command (con lambdas)** | `MenuOption(String label, Runnable action)` | Cada opción de menú encapsula su acción. |
| **Inyección de dependencias** | `Main` | Bajo acoplamiento y piezas fáciles de probar. |
| **DTO inmutable** | `record` en `model.report` e `InvoiceData` | Datos de reportes y facturas sin estado mutable. |
| **Facade** | `InvoiceService.issueFor(paymentId)` | Una sola llamada orquesta 4 DAOs, el cálculo del CUFE y el generador del archivo. |

### Uso de lambdas y Stream API

Los cálculos se hacen con *streams* en los controladores. Algunos ejemplos:

```java
// Promedio del curso por materia (ReportController.courseAverages)
records.stream().filter(periodFilter(period))
       .collect(Collectors.groupingBy(r -> r.getSubjectCode() + "|" + r.getSubjectName()))
       ...summarizingDouble(AcademicRecord::getGradeValue)

// Más de 3 ausencias en la misma semana (ReportController.absencePatterns)
.collect(Collectors.groupingBy(r -> Map.entry(r.getStudentId(), weekStart.apply(r)), Collectors.counting()))
.entrySet().stream().filter(e -> e.getValue() > WEEKLY_ABSENCE_THRESHOLD)

// Pensiones exigibles con más de un mes de vencidas (FinanceController)
IntStream.rangeClosed(FEBRUARY, NOVEMBER).mapToObj(m -> LocalDate.of(year, m, 5))
         .filter(due -> due.plusMonths(1).isBefore(today)).count();
```

Además, los DAOs mapean filas con lambdas (`ps -> ps.setInt(1, id)`, `this::map`) y `TablePrinter` define sus columnas con `Function<T, Object>`.

---

## 10. Funcionalidades principales

### 1. Gestión de Estudiantes
- Registro completo (documento, nombres, nacimiento, género, contacto, tipo de sangre, alergias, condiciones médicas) con **matrícula automática** en el grado y **acudiente obligatorio**. Si el acudiente ya existe se reutiliza, lo que permite que los hermanos compartan acudiente.
- Listado por estado, búsqueda por nombre o documento y **ficha completa** con acudientes e historial de matrículas.
- Actualización de datos y cambio de estado (Activo, Graduado, Retirado, Suspendido) manteniendo la coherencia: al retirar se cierran la matrícula y las inscripciones, y solo se gradúa desde grado Once.
- Matrícula en un nuevo año (no permite bajar de grado) y eliminación solo de registros sin historial.
- Acudientes: registro, vinculación (principal / contacto de emergencia), desvinculación (siempre queda al menos uno) y listado de estudiantes a cargo.

### 2. Gestión Académica
- Materias por grado: listado, registro (código único `XXX-NN`) y activación.
- Inscripción de un estudiante o de un **grado completo** en las materias de un período.
- **Notas 0.0–5.0 con un decimal** (acepta `4.5` o `4,5`), individuales o por **curso completo**. Solo puede registrarlas el profesor asignado y el estudiante debe estar inscrito.
- **Nota final automática** (promedio de los períodos I–IV).
- **Boletín:** notas por período, **promedio por materia**, **promedio general por período y del año**, y nivel de desempeño (Superior, Alto, Básico, Bajo).
- **Historial académico** completo por año desde el ingreso.
- **Asistencia** diaria por materia (Presente, Ausente, Tardanza, Excusa Médica) y consulta por rangos de fechas.

### 3. Gestión de Profesores
- Directorio con documento y correo únicos, actualización y estados (Activo, Inactivo, Licencia).
- Asignación de materias por sección: un profesor dicta varias materias y una materia puede tener varios profesores (uno por sección).
- **Horarios** con detección de cruces del **profesor** y del **grado**, y horario semanal por profesor o por grado.

### 4. Actividades Extracurriculares
- Catálogo de clubes con ocupación (`miembros/cupo`), creación y edición.
- Inscripción con **validación de cupo máximo**, hecha en una transacción con `SELECT … FOR UPDATE`. También se valida que no haya duplicados y que el estudiante esté activo.
- Retiro con fecha y motivo, y consulta de miembros o de los clubes de un estudiante.
- Eventos (Académico, Cultural, Deportivo, Social, Científico): calendario, creación, participantes y **resultados** (1er, 2do, 3er lugar o Participante). No se permiten posiciones repetidas ni resultados de eventos futuros.

### 5. Gestión Financiera
- Catálogo de conceptos (matrícula, pensión, uniforme, transporte, etc.) con valor base.
- **Valores personalizados** por estudiante y año (beca, descuento por hermanos) que se aplican al cobrar.
- **Registro de pago + factura electrónica automática:** número consecutivo `LHFE-000001`, recibo `RC-2026-000001`, datos del colegio y del acudiente, detalle, subtotal, IVA, total y valor en letras. Incluye **CUFE** (SHA‑384), **QR** y resolución DIAN simulados. Genera `invoices/factura_000001.txt` y muestra la ruta.
- IVA: los servicios educativos están excluidos (art. 476 E.T.); solo el concepto `UNIFORME` lleva 19 %, discriminado del valor pagado.
- Historial por estudiante y **por acudiente** (todos sus hijos), con totales.
- **Reporte de morosos:** pensiones (febrero a noviembre, vencen el día 5) pendientes por **más de un mes**, con deuda y teléfono del acudiente.
- Recaudo del año por concepto y por medio de pago.

### 6. Gestión de Recursos
- Inventario filtrable por categoría y estado; registro con **código único automático** (`LIB-016`, `LAB-011`, …).
- Cambios de estado (mantenimiento, baja). El estado "Prestado" solo lo asigna el sistema.
- **Préstamos** a estudiantes y profesores en tablas separadas. Se valida disponibilidad (transacción con bloqueo), prestatario activo, sin vencidos y máximo 3 préstamos abiertos por estudiante.
- **Devolución:** registra la fecha real y deja el recurso disponible.
- **Reporte de préstamos vencidos** con días de retraso (actualiza automáticamente "Activo → Vencido").

### 7. Reportes y Estadísticas
- **Promedio general del curso en cada materia** (mínimo, máximo, estudiantes).
- Ranking de un grado por promedio.
- **Estudiantes que necesitan apoyo** (materias con promedio < 3.0).
- **Patrones de inasistencia** (más de 3 ausencias en una semana).
- Participación extracurricular (clubes, eventos, podios).
- Panel general: estudiantes por estado y grado, recaudo, cartera vencida, préstamos y alertas.

### Experiencia de usuario
- Menú principal por módulos y submenús agrupados por secciones.
- **Toda entrada se valida** (números, fechas `AAAA-MM-DD`, horas, correos, teléfonos, documentos, notas, montos, s/n). Ante un error se muestra un mensaje claro y se vuelve a preguntar.
- Mensajes de éxito `✔`, error `✖` e información `ℹ`; confirmación antes de guardar operaciones importantes.
- **Navegación:** `0` vuelve al menú principal desde cualquier submenú. Además, escribir **`X`** y Enter en **cualquier campo de cualquier formulario** cancela la operación y regresa al menú sin guardar nada (cada formulario lo recuerda en su encabezado).

---

## 11. Manejo de errores y logs

### Sistema de logging

`util/ErrorLogger` (Singleton, *thread‑safe*) agrega entradas al archivo **`error_log.txt`** en la raíz del proyecto (ruta configurable con `app.logFile`). Cada entrada tiene fecha y hora, nivel (`INFO`, `WARN`, `ERROR`), componente, mensaje y, en los errores, la **traza completa** de la excepción:

```
[2026-11-11 17:40:12] ERROR DatabaseConnection - Connection test failed for jdbc:mysql://localhost:3306/...
java.sql.SQLException: Access denied for user 'root'@'localhost' (using password: YES)
    at ...
[2026-11-11 17:41:25] INFO  InvoiceService - Invoice LHFE-000002 issued for payment 606
```

Los errores de SQL registran además el SQL resumido, el `SQLState` y el código de MySQL.

### Estrategia de manejo de excepciones

| Excepción | Origen | Tratamiento |
|---|---|---|
| `SQLException` | JDBC | Se captura en `BaseDao`, se registra en el log y se convierte en `DataAccessException` con un mensaje comprensible: duplicado (1062), registro con dependencias (1451), referencia inexistente (1452) o violación de `CHECK` (3819). |
| `BusinessException` | Controladores | Regla de negocio incumplida (cupo lleno, recurso prestado, cruce de horario…). Su mensaje se muestra tal cual. |
| `OperationCancelledException` | Vistas | El usuario escribió `0` o no confirmó; no se guarda nada. |
| `InputClosedException` | `InputReader` | Fin de la entrada estándar; la aplicación se cierra de forma ordenada. |
| `RuntimeException` inesperada | Cualquiera | `ConsoleView.run()` la registra con su traza y muestra un aviso genérico; el usuario vuelve al menú sin que el programa termine. |

Además:

- **Todas** las consultas usan `PreparedStatement` (sin concatenar datos del usuario), y conexiones, sentencias y `ResultSet` se cierran con **try‑with‑resources**.
- Las operaciones de varios pasos son **transaccionales** (`BaseDao.inTransaction`): préstamo y devolución con actualización del recurso, cupo de club, vinculación de acudiente principal, asistencia y valores personalizados. Ante cualquier error se hace `rollback`.
- Si la factura no puede escribirse, el pago se conserva, se registra un `WARN` y el usuario puede generarla después desde *Generar factura de un pago existente*.

---

## 12. Capturas de pantalla

**Menú principal**

```
  ╔══════════════════════════════════════════════╗
  ║   ERP Colegio Little Horizons                ║
  ║   Sistema de Gestión Educativa               ║
  ╚══════════════════════════════════════════════╝

  1. Gestión de Estudiantes
  2. Gestión Académica
  3. Gestión de Profesores
  4. Actividades Extracurriculares
  5. Gestión Financiera
  6. Gestión de Recursos
  7. Reportes y Estadísticas
  8. Salir

  Seleccione una opción: _
```

**Boletín de un estudiante (Gestión Académica → Boletín)**

```
  ▸ BOLETÍN 2026 - EMILIA LUCÍA JAIMES LÓPEZ (SÉPTIMO)
  +--------+----------------------------+-----+-----+-----+-----+----------+-------+-----------+
  | Código | Materia                    | I   | II  | III | IV  | Promedio | Final | Desempeño |
  +========+============================+=====+=====+=====+=====+==========+=======+===========+
  | CNA-07 | Ciencias Naturales Séptimo | 3.9 | 3.2 | 3.7 | 3.8 | 3.7      | -     | Básico    |
  | MAT-07 | Matemáticas Séptimo        | 4.1 | 3.3 | 5.0 | 3.4 | 4.0      | -     | Alto      |
  | SOC-07 | Ciencias Sociales Séptimo  | 4.5 | 4.4 | 3.6 | 4.6 | 4.3      | -     | Alto      |
  ...
    PROMEDIO GENERAL DEL AÑO:        3.8 (Básico)
```

**Validación de cupo y de cruce de horario**

```
  ✖ El club alcanzó su cupo máximo (10 estudiantes).
  ✖ Conflicto: el profesor ya dicta Matemáticas Primero (Primero) el Martes de 09:00 a 09:55.
```

**Factura electrónica generada** (`invoices/factura_000002.txt`; más ejemplos en [`docs/evidence/`](docs/evidence/))

```
==============================================================================
                 COLEGIO INTERNACIONAL LITTLE HORIZONS S.A.S.
          NIT 901.456.789-3 - Responsable de IVA - Persona jurídica
   Avenida Libertadores # 10-55, Barrio Caobos, Cúcuta, Norte de Santander
          Tel: (607) 583 4400  |  facturacion@littlehorizons.edu.co
==============================================================================
                 FACTURA ELECTRÓNICA DE VENTA N° LHFE-000002
Resolución DIAN 18764000123456 del 2026-01-15 - Rango LHFE-000001 a LHFE-100000
                 Vigencia: 2026-01-15 a 2028-01-15 (simulada)
------------------------------------------------------------------------------
Fecha de emisión : 2026-11-11 17:58:20
Forma de pago    : Contado        Medio de pago: Efectivo
Recibo de caja   : RC-2026-000606
------------------------------------------------------------------------------
ADQUIRIENTE
  Nombre    : Lorena Andrea Villamizar Leal
  Documento : CC 1088295444
  Dirección : Diagonal 26 # 23-62 Popular, Cúcuta
  Teléfono  : 3056607058   Correo: lorena.villamizar91@gmail.com
  Estudiante: Miguel Ángel Quintero Villamizar (Sexto)
------------------------------------------------------------------------------
N°  Descripción                                Cant  Vr. unitario      Subtotal
------------------------------------------------------------------------------
1   Uniforme - Uniforme de diario                 1     $ 210.084     $ 210.084
------------------------------------------------------------------------------
                                                     Subtotal:       $ 210.084
                                                    IVA (19%):        $ 39.916
                                                TOTAL A PAGAR:       $ 250.000
Son: DOSCIENTOS CINCUENTA MIL PESOS M/CTE
------------------------------------------------------------------------------
CUFE: cb8bc406df243ed19d3e2a86246df9696ae84e95d87253278c11e8384f73ba9c0916c23c72284f9b170bb37565e301c0
Consulta (QR): https://catalogo-vpfe.dian.gov.co/document/searchqr?documentkey=cb8bc406df243ed19d3e2a86246df9696ae84e95d87253278c11e8384f73ba9c0916c23c72284f9b170bb37565e301c0

...  (sigue el código QR simulado dibujado en texto)
```

---

## 13. Problemas conocidos y limitaciones

- El CUFE, el QR, la clave técnica y la resolución son **simulados**: la factura tiene la estructura de la DIAN, pero no se transmite ni tiene validez fiscal. El QR reproduce la estructura visual de un QR versión 3, aunque no es escaneable.
- La morosidad se calcula por **cantidad** de pensiones pagadas en el año frente a las exigibles, no por mes específico, porque `pagos` no tiene una columna de mes.
- Cada pago genera una factura con un solo concepto (cantidad 1).
- No hay autenticación ni roles de usuario: cualquier persona con acceso a la consola opera todos los módulos.
- En la consola clásica de Windows (`cmd`) los caracteres de los recuadros requieren UTF‑8. `scripts\run.bat` ejecuta `chcp 65001`, mientras que la consola de IntelliJ funciona sin cambios.

## 14. Futuras mejoras

- Inicio de sesión con roles (rector, secretaría, docente, tesorería, biblioteca).
- Facturas con varios conceptos y abonos parciales, columna de mes en `pagos` y envío de facturas por correo al acudiente.
- Integración real con un proveedor tecnológico de facturación electrónica (XML UBL 2.1 firmado).
- Pruebas unitarias con JUnit 5 para controladores y DAOs (con Testcontainers para MySQL).
- Pool de conexiones (HikariCP) y migración a Maven o Gradle.
- Interfaz gráfica (JavaFX) o web (Spring Boot) reutilizando las capas *model*, *dao* y *controller*.

## 15. Licencia

Distribuido bajo la licencia **MIT**; consulte el archivo [`LICENSE`](LICENSE). Proyecto académico: los datos de prueba (personas, documentos, teléfonos y correos) son **ficticios**.

## 16. Contacto

| | |
|---|---|
| Autor | Anaya |
| Correo | andrescamiloanayag@gmail.com |
| GitHub | <https://github.com/ARSCIAND> |
| Repositorio | <https://github.com/ARSCIAND/little-horizons-erp> (privado) |

---

## Anexo A. Verificación de requisitos del enunciado

Esta tabla relaciona cada requisito del enunciado y del checklist de entrega con el lugar del proyecto donde se cumple. Las rutas de código son relativas a `src/com/littlehorizons/erp/`.

### Módulos funcionales

| Requisito | Dónde se cumple |
|---|---|
| 1.1 Registro de estudiantes con datos personales y contactos de emergencia | `controller/StudentController.registerStudent`, `view/StudentView` (opción 1.1) |
| 1.1 Acudientes; un acudiente con varios estudiantes (hermanos) | tabla `estudiante_acudiente`, `StudentController.linkGuardian` / `guardiansWithStudents` |
| 1.1 Matrícula por grado y año con estado (Activo, Graduado, Retirado) | `StudentController.registerStudent`, `enrollInYear`, `changeStatus` |
| 1.1 Inscripción en materias por período | `AcademicController.enrollStudentInGradeSubjects`, `enrollGradeInPeriod` |
| 1.2 Directorio de profesores | `controller/TeacherController`, `view/TeacherView` |
| 1.2 Asignación de materias (N a N, por sección) | `TeacherController.assignSubject` |
| 1.2 Horarios sin conflictos básicos | `TeacherController.addSchedule` (cruces de profesor y de grado), tabla `horarios_clase` |
| 1.3 Notas 0.0–5.0 con un decimal por materia y período (I–IV, Final) | `AcademicController.recordGrade`, `computeFinalGrades`, `util/InputReader.readGrade`, `CHECK` en `registros_academicos` |
| 1.3 Promedio por materia, general del estudiante y del curso | `AcademicController.getReportCard`, `getStudentAverage`, `ReportController.courseAverages` |
| 1.3 Historial académico desde el ingreso | `AcademicController.getAcademicHistory` |
| 1.4 Asistencia diaria (Presente, Ausente, Tardanza, Excusa Médica) | `AcademicController.recordAttendance` |
| 1.4 Reporte de inasistencias (> 3 ausencias en una semana) | `ReportController.absencePatterns` |
| 2.1 Clubes con cupo máximo, profesor encargado, ingreso y estado | `controller/ExtracurricularController.joinClub`, `dao/ClubDao.addMember` (transacción con `SELECT … FOR UPDATE`) |
| 2.2 Eventos, participantes y posiciones en competencias | `ExtracurricularController.addParticipant`, `recordResult` |
| 3.1 Catálogo de conceptos y valores por estudiante | `FinanceController.createConcept`, `setCustomPrice`, `resolvePrice`, tabla `valores_personalizados` |
| 3.2 Registro de pagos (estudiante, concepto, monto, fecha, método, recibo) | `FinanceController.registerPayment` |
| 3.2 Historial por estudiante y por acudiente | `FinanceController.historyByStudent`, `historyByGuardian` |
| 3.2 Morosos (pagos pendientes por más de un mes) | `FinanceController.delinquentAccounts`, `monthsOverdueThreshold` |
| 3.3 Factura electrónica DIAN: datos del colegio y del cliente, consecutivo, detalle, IVA, CUFE, QR y resolución | `service/invoice/InvoiceService.issueFor`, `CufeGenerator` (SHA‑384), `SimulatedQrGenerator`, `TextInvoiceGenerator` |
| 3.3 Archivo con nombre único (`factura_000123`) y mensaje con la ruta | `InvoiceData.baseFileName`, `view/FinanceView.printIssued` |
| 4.1 Inventario con código único, categoría, estado y ubicación | `controller/ResourceController.registerResource` (código automático `LIB-001`, `LAB-001`…) |
| 4.2 Préstamos a estudiantes y a profesores | `ResourceController.lend`, `dao/LoanDao.createLoan` (tablas separadas) |
| 4.2 Devolución que deja el recurso "Disponible" | `ResourceController.registerReturn`, `LoanDao.registerReturn` |
| 4.2 Reporte de préstamos vencidos | `ResourceController.overdueLoans`, `LoanDao.markOverdue` |
| 5.1 Arquitectura MVC | paquetes `model`, `view`, `controller` (+ `dao`, `service`, `util`); ver sección 8 |
| 5.2 Menú principal por módulos | `view/MainMenuView` |
| 5.2 Validación de entradas con mensaje claro y reintento | `util/InputReader` (todas las lecturas) |
| 5.2 Mensajes de confirmación y error | `view/ConsoleView.success` / `error` / `confirmOrCancel` |
| 5.2 Navegación: volver al menú anterior o principal | `0` en cada submenú; `X` en cualquier formulario (`InputReader.CANCEL_KEY`) |

### Checklist de entrega

| Ítem del checklist | Estado y evidencia |
|---|---|
| Proyecto Java estándar | ✅ Proyecto IntelliJ (`little-horizons-erp.iml`), sin Maven ni Gradle; compila con `javac` (`scripts/build.*`) |
| Estructura MVC | ✅ Sección 8 |
| Código en inglés (clases, variables, métodos, comentarios) | ✅ Todo el código de `src/`. Solo los textos visibles para el usuario están en español |
| Convenciones de nombres Java | ✅ `PascalCase` (clases), `camelCase` (métodos y variables), `UPPER_SNAKE_CASE` (constantes, p. ej. `MAX_OPEN_LOANS_PER_STUDENT`, `CANCEL_KEY`) |
| Base `erp_little_horizons` | ✅ `docs/database.sql` |
| `database.sql` con DDL completo basado en la versión corregida | ✅ Sección 1A, copia exacta del anexo |
| DML con mínimo 30 registros por tabla principal | ✅ Tabla de volumen en la sección 6.1 |
| Datos coherentes y realistas | ✅ Nombres colombianos, direcciones de Cúcuta, montos en pesos, integridad referencial |
| MySQL 8.0 o superior | ✅ Probado en MySQL 8.0 y 8.4 |
| Validación de entradas, mensajes claros, navegación intuitiva | ✅ `InputReader`, `ConsoleView` |
| Manejo de errores con `try-catch` | ✅ `BaseDao`, `ConsoleView.run`, `Main` |
| Logs en `error_log.txt` | ✅ `util/ErrorLogger` (sección 11) |
| `PreparedStatement` en todas las consultas | ✅ Todo el SQL pasa por `dao/BaseDao`; ningún dato del usuario se concatena |
| `try-with-resources` | ✅ `BaseDao` (`Connection`, `PreparedStatement`, `ResultSet`), `ErrorLogger`, `TextInvoiceGenerator` |
| Principios SOLID | ✅ Sección 9.1 |
| Lambdas y Stream API | ✅ Sección 9 (`ReportController`, `AcademicController`, `FinanceController`, `TablePrinter`) |
| Generación de facturas | ✅ `invoices/factura_XXXXXX.txt` (sección 12) |
| Carpeta `docs/` con `database.sql` | ✅ |
| README con todas las secciones requeridas | ✅ Secciones 1 a 16 |
| Commits en inglés y atómicos | ✅ Ver el historial de commits del repositorio |

---

## Anexo B. Recorrido de prueba por módulo

Recorrido sugerido para revisar cada módulo con los datos de prueba. Las entradas se escriben en la consola seguidas de Enter. En cualquier momento, `X` cancela el formulario y `0` vuelve al menú.

**1. Gestión de Estudiantes** (menú `1`)

| Opción | Prueba | Resultado esperado |
|---|---|---|
| `2` Listar → `2` (Activo) | — | 38 estudiantes activos |
| `4` Ficha completa → `Jaimes` | Elegir un ID de la lista | Datos personales, acudientes y historial de matrículas |
| `12` Acudientes | — | Los acudientes con 2 hijos aparecen primero |
| `1` Registrar | Llenar el formulario; en el acudiente usar un documento nuevo | `✔ Estudiante registrado con ID 43 y matriculado en …` |

**2. Gestión Académica** (menú `2`)

| Opción | Prueba | Resultado esperado |
|---|---|---|
| `9` Boletín → estudiante `2` → Enter | — | Notas por período I–IV, promedio por materia y promedio general del año |
| `10` Historial → estudiante `10` | — | Notas de 2025 (Final) y de 2026 |
| `6` Nota individual → estudiante `43` (el recién creado) | — | `✖ El estudiante no está inscrito…` (regla de negocio) |
| `4` Inscribir → `43` → Enter → `4` | — | `✔ Se inscribió … en N materia(s)`; luego la opción `6` sí permite registrar la nota |
| `11` Tomar asistencia → grado `2` → materia `1` → Enter | `P`, `A`, `T`, `E`… | `✔ Asistencia guardada…` |

**3. Gestión de Profesores** (menú `3`)

| Opción | Prueba | Resultado esperado |
|---|---|---|
| `3` Ficha → profesor `1` → Enter | — | Carga académica y horario semanal |
| `7` Agregar franja → `1` → Enter → asignación `25` → día `2` (Martes) → `09:30` → `10:15` | — | `✖ Conflicto: el profesor ya dicta …` (cruce detectado) |
| `9` Horario de un grado → `2` (Primero) | — | Horario semanal del grado |

**4. Actividades Extracurriculares** (menú `4`)

| Opción | Prueba | Resultado esperado |
|---|---|---|
| `1` Catálogo → `s` | — | Clubes con `miembros/cupo`; Robótica aparece `LLENO` |
| `4` Inscribir → estudiante `1` → club `2` | — | `✖ El club alcanzó su cupo máximo (10 estudiantes).` |
| `12` Participantes → evento `1` (Feria de Ciencias) | — | Participantes con 1er, 2do y 3er lugar |

**5. Gestión Financiera** (menú `5`)

| Opción | Prueba | Resultado esperado |
|---|---|---|
| `5` Valores personalizados → Enter | — | 15 descuentos por hermanos y becas |
| `6` Registrar pago | Estudiante `4`, concepto `2`, `s`, Enter, `2`, `4`, detalle, `s` | Recibo `RC-2026-000605` y `factura_000001.txt` |
| `11` Morosos → Enter | — | 6 estudiantes con pensiones pendientes, deuda y teléfono del acudiente |
| `8` Historial por acudiente | Documento de un acudiente con 2 hijos | Pagos de ambos hermanos y total de la familia |

**6. Gestión de Recursos** (menú `6`)

| Opción | Prueba | Resultado esperado |
|---|---|---|
| `8` Préstamos vencidos | — | Lista con días de retraso |
| `4` Prestar a estudiante → `43` → `LIB-003` → Enter → Enter | — | `✔ Préstamo N° … registrado` y el recurso pasa a "Prestado" |
| `6` Devolución → `LIB-003` → Enter → Enter | — | `✔ Devolución registrada… vuelve a estar Disponible` |

**7. Reportes y Estadísticas** (menú `7`)

| Opción | Prueba | Resultado esperado |
|---|---|---|
| `1` Promedios del curso → grado `5` → Enter → `5` (año completo) | — | Promedio, mínima y máxima por materia |
| `3` Necesitan apoyo → Enter → `5` | — | Estudiantes con materias por debajo de 3.0 |
| `4` Inasistencia → Enter → Enter | — | 4 estudiantes con más de 3 ausencias en la semana del 2026-11-02 |
| `6` Panel general | — | Resumen de todo el colegio |

Para dejar los datos como al inicio, basta con volver a ejecutar `docs/database.sql`.

---

## Anexo C. Examen: buscador avanzado de estudiantes

Prototipo de búsqueda de estudiantes con filtros, implementado en el paquete independiente **`test`**. Se ejecuta por su cuenta, sin pasar por el menú principal del ERP, y reutiliza la conexión a la base de datos del proyecto (`DatabaseConnection`).

### Estructura

```
src/test/
├── modelo/        GradeOption, StudentSearchResult (DTO estudiante + matrícula + grado),
│                  StudentPerformance, SubjectSummary, PerformanceReport
├── controlador/   StudentSearchController  → consulta BD y filtra por grado con Stream API
│                  PerformanceReportController → promedios, ranking y desempeño con Stream API
├── reporte/       PerformanceReportWriter  → escribe el reporte .txt
└── vista/         BuscadorEstudiantesMain (main) y SearchMenuView (menú y resultados)
```

### Cómo ejecutarlo

1. Ejecutar `docs/database.sql` (si aún no se ha hecho) y luego **`docs/test_data_2025.sql`**, con **Alt + X** en DBeaver.
2. En IntelliJ elegir la configuración **`BuscadorEstudiantes`** (junto al botón ▶) y ejecutarla. Otra forma es abrir `src/test/vista/BuscadorEstudiantesMain.java` → clic derecho → **Run**.

### Datos de prueba 2025

Los datos principales del proyecto son de 2026, y sus matrículas de 2025 están en estado *Completada*. Por eso se agregó `docs/test_data_2025.sql`, que se puede ejecutar varias veces sin errores. Es una adaptación del script de datos de prueba del examen, con los mismos 3 estudiantes de Décimo y las mismas notas. Se hicieron dos ajustes: no usa IDs fijos, porque en esta base ya existen, y busca los grados por nombre, porque el `id_grado = 10` del script original corresponde a *Noveno*. Además incluye:

| Grado (2025) | Estudiantes con matrícula Activa | Para probar |
|---|---:|---|
| Décimo | 3 (+1 con matrícula *Retirada*) | Resultado del ejemplo del enunciado y caso especial 1 (la retirada no aparece) |
| Noveno | 4 | Reporte con promedios altos, medios y bajos; nombres sin segundo nombre o segundo apellido (caso especial 3) |
| Once | 0 | Caso especial 2: "No se encontraron estudiantes…" |

### Funcionamiento

- **Opción 1 – Filtrar por grado académico:** consulta todos los estudiantes con matrícula *Activa* en 2025 (tablas `estudiantes`, `matriculas` y `grados`) y aplica el filtro por grado con Stream API, en `StudentSearchController.searchByGrade`:

  ```java
  return loadActiveEnrollments().stream()
          .filter(student -> student.gradeId() == grade.id())
          .collect(Collectors.toList());
  ```

  Valida números fuera de rango, texto y `0` para cancelar, con los mensajes del enunciado. El nombre completo se arma solo con las partes que existen (`StudentSearchResult.fullName`).
- **Opciones 2 a 5:** muestran "Esta funcionalidad aún no ha sido implementada…" y regresan al menú.
- **Opción 6 – Reporte de rendimiento académico por grado (.txt):** usa el mismo filtro por grado y calcula con Stream API (`groupingBy`, `averagingDouble`, `sorted`, `counting`):
  1. ranking de estudiantes por promedio general, con su nivel de desempeño (Superior, Alto, Básico, Bajo);
  2. promedio del curso, nota máxima y mínima, y mejor estudiante de cada materia;
  3. promedio de cada estudiante en cada materia, marcando las que están por debajo de 3.0;
  4. resumen: promedio del curso, estudiantes por nivel, **candidatos a beca (> 4.5)** y **estudiantes que requieren refuerzo (< 3.0)**.

  El archivo se guarda en `reports/rendimiento_<grado>_2025_<fecha>.txt` y su ruta se muestra en pantalla. Si el grado no tiene estudiantes, no se crea el archivo.

