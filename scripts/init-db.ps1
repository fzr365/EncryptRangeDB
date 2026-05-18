$ErrorActionPreference = 'Stop'

$mysql = 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe'
$schema = Join-Path (Split-Path -Parent $PSScriptRoot) 'docs\schema.sql'

if (-not (Test-Path $mysql)) {
  throw "mysql.exe not found at $mysql"
}

& $mysql --protocol=TCP -h 127.0.0.1 -P 3307 -u root -e "CREATE DATABASE IF NOT EXISTS encryprangedb CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci; CREATE USER IF NOT EXISTS 'encryprangedb'@'localhost' IDENTIFIED BY 'change_me'; GRANT ALL PRIVILEGES ON encryprangedb.* TO 'encryprangedb'@'localhost'; FLUSH PRIVILEGES;"
Get-Content -Raw $schema | & $mysql --protocol=TCP -h 127.0.0.1 -P 3307 -u root encryprangedb
& $mysql --protocol=TCP -h 127.0.0.1 -P 3307 -u root -e "SHOW TABLES FROM encryprangedb;"

Write-Host "Database initialized."
