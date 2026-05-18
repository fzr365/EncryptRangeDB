$ErrorActionPreference = 'Stop'

$base = 'C:\Program Files\MySQL\MySQL Server 8.4'
$work = Split-Path -Parent $PSScriptRoot
$data = Join-Path $work 'mysql-data'
$cfg = Join-Path $work 'mysql-my.ini'

if (-not (Test-Path $base)) {
  throw "MySQL Server not found at $base. Install it first (e.g. winget install -e --id Oracle.MySQL --source winget)."
}

if (-not (Test-Path $data)) {
  New-Item -ItemType Directory -Force -Path $data | Out-Null
  @(
    '[mysqld]',
    "basedir=$base",
    "datadir=$data",
    'port=3307',
    'bind-address=127.0.0.1',
    'character-set-server=utf8mb4',
    'collation-server=utf8mb4_0900_ai_ci'
  ) | Set-Content -Encoding ASCII -Path $cfg

  & "$base\bin\mysqld.exe" --defaults-file="$cfg" --initialize-insecure
}

Start-Process -FilePath "$base\bin\mysqld.exe" -ArgumentList "--defaults-file=`"$cfg`" --console" -WorkingDirectory $work | Out-Null

Write-Host "MySQL starting at 127.0.0.1:3307 using datadir $data"
