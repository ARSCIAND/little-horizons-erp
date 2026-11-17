# lib/

Coloque aquí el driver JDBC **MySQL Connector/J** (`mysql-connector-j-9.x.x.jar`).

- Linux / macOS: `./scripts/download-driver.sh`
- Windows (PowerShell): `.\scripts\download-driver.ps1`
- Manual: <https://dev.mysql.com/downloads/connector/j/> → *Platform Independent* → copie el `.jar` en esta carpeta.

El módulo de IntelliJ (`little-horizons-erp.iml`) y los scripts de `scripts/` cargan automáticamente cualquier `.jar` de esta carpeta.
