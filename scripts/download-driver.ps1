# Downloads MySQL Connector/J into lib\ (run from the project root in PowerShell).
param([string]$Version = "9.1.0")
New-Item -ItemType Directory -Force -Path lib | Out-Null
$url = "https://repo1.maven.org/maven2/com/mysql/mysql-connector-j/$Version/mysql-connector-j-$Version.jar"
Invoke-WebRequest -Uri $url -OutFile "lib\mysql-connector-j-$Version.jar"
Write-Host "Driver saved to lib\mysql-connector-j-$Version.jar"
