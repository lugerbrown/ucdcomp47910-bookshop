# PowerShell version of setup-env.sh
# Run with: powershell -ExecutionPolicy Bypass -File ./setup-env.ps1

function Ask($varName, $prompt, $silent = $false) {
    if ($silent) {
        $value = Read-Host -Prompt $prompt -AsSecureString | ConvertFrom-SecureString
    } else {
        $value = Read-Host -Prompt $prompt
    }
    return $value
}

Write-Host '--- Bookshop Environment Setup ---'
$MYSQL_ROOT_PASSWORD = Ask 'MYSQL_ROOT_PASSWORD' 'Enter MySQL root password' $true
$MYSQL_DATABASE = Ask 'MYSQL_DATABASE' 'Enter MySQL database name'
$MYSQL_USER = Ask 'MYSQL_USER' 'Enter MySQL user name'
$MYSQL_PASSWORD = Ask 'MYSQL_PASSWORD' 'Enter MySQL user password' $true
$SPRING_DATASOURCE_HIKARI_TRUSTSTORE_PASSWORD = Ask 'SPRING_DATASOURCE_HIKARI_TRUSTSTORE_PASSWORD' 'Enter password for Java truststore (used by Spring Boot)' $true

$ENV_FILE = 'docker/.env'
$SSL_DIR = 'docker/mysql-ssl'
$TRUSTSTORE_FILE = "$SSL_DIR/truststore.jks"
$SERVER_KEY = "$SSL_DIR/server-key.pem"
$SERVER_CERT = "$SSL_DIR/server-cert.pem"

# Convert secure strings to plain text for use in commands and file output
function Unsecure($secureString) {
    if ($null -eq $secureString) { return '' }
    try {
        return ([System.Runtime.InteropServices.Marshal]::PtrToStringAuto(
            [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR((ConvertTo-SecureString $secureString))
        ))
    } catch {
        return $secureString
    }
}

$MYSQL_ROOT_PASSWORD_PLAIN = Unsecure $MYSQL_ROOT_PASSWORD
$MYSQL_PASSWORD_PLAIN = Unsecure $MYSQL_PASSWORD
$TRUSTSTORE_PASSWORD_PLAIN = Unsecure $SPRING_DATASOURCE_HIKARI_TRUSTSTORE_PASSWORD

# --- Check for existing files ---
if (Test-Path $ENV_FILE) {
    $overwriteEnv = Read-Host "$ENV_FILE already exists. Overwrite? (y/N)"
    if ($overwriteEnv -notmatch '^[Yy]$') { Write-Host 'Aborting.'; exit 1 }
}
if ((Test-Path $SERVER_KEY) -or (Test-Path $SERVER_CERT)) {
    $overwriteSSL = Read-Host 'SSL key/cert already exist. Overwrite? (y/N)'
    if ($overwriteSSL -notmatch '^[Yy]$') { Write-Host 'Aborting.'; exit 1 }
}
if (Test-Path $TRUSTSTORE_FILE) {
    $overwriteTS = Read-Host "$TRUSTSTORE_FILE already exists. Overwrite? (y/N)"
    if ($overwriteTS -notmatch '^[Yy]$') { Write-Host 'Aborting.'; exit 1 }
}

# --- Create SSL directory if needed ---
if (-not (Test-Path $SSL_DIR)) { New-Item -ItemType Directory -Path $SSL_DIR | Out-Null }

# --- Check if OpenSSL is available ---
$opensslPath = Get-Command openssl -ErrorAction SilentlyContinue
if (-not $opensslPath) {
    Write-Host "ERROR: OpenSSL is not found in PATH." -ForegroundColor Red
    Write-Host "Please install OpenSSL and add it to your PATH, or use one of these options:" -ForegroundColor Yellow
    Write-Host "1. Install OpenSSL via Chocolatey: choco install openssl" -ForegroundColor Yellow
    Write-Host "2. Install via Scoop: scoop install openssl" -ForegroundColor Yellow
    Write-Host "3. Download from https://slproweb.com/products/Win32OpenSSL.html" -ForegroundColor Yellow
    Write-Host "4. Use Git Bash (if Git is installed): 'C:\Program Files\Git\usr\bin\openssl.exe'" -ForegroundColor Yellow
    exit 1
}

# --- Generate MySQL server key and certificate ---
Write-Host 'Generating MySQL server key and certificate...'
& openssl req -newkey rsa:2048 -nodes -keyout $SERVER_KEY -x509 -days 365 -out $SERVER_CERT -subj "/CN=localhost"
if ($LASTEXITCODE -ne 0) { Write-Host 'OpenSSL failed.'; exit 1 }

# --- Create Java truststore and import cert ---
Write-Host 'Creating Java truststore and importing certificate...'
if (Test-Path $TRUSTSTORE_FILE) {
    & keytool -delete -alias mysql-server -keystore $TRUSTSTORE_FILE -storepass $TRUSTSTORE_PASSWORD_PLAIN 2>$null
}
& keytool -importcert -noprompt -alias mysql-server -file $SERVER_CERT -keystore $TRUSTSTORE_FILE -storepass $TRUSTSTORE_PASSWORD_PLAIN
if ($LASTEXITCODE -ne 0) { Write-Host 'keytool failed.'; exit 1 }

# --- Compose connection string and truststore location ---
$BOOK_STORE_CONNECTION_STRING = "jdbc:mysql://localhost:3306/{0}?serverTimezone=UTC&useSSL=true&requireSSL=true" -f $MYSQL_DATABASE
$TRUSTSTORE_LOCATION = "file:" + (Resolve-Path $TRUSTSTORE_FILE).Path

# --- Write .env file ---
Write-Host "Writing $ENV_FILE..."
$envContent = @"
MYSQL_ROOT_PASSWORD=$MYSQL_ROOT_PASSWORD_PLAIN
MYSQL_DATABASE=$MYSQL_DATABASE
MYSQL_USER=$MYSQL_USER
MYSQL_PASSWORD=$MYSQL_PASSWORD_PLAIN
BOOK_STORE_CONNECTION_STRING=$BOOK_STORE_CONNECTION_STRING
SPRING_DATASOURCE_HIKARI_TRUSTSTORE_PASSWORD=$TRUSTSTORE_PASSWORD_PLAIN
SPRING_DATASOURCE_HIKARI_TRUSTSTORE_LOCATION=$TRUSTSTORE_LOCATION
"@
$envContent | Set-Content -Path $ENV_FILE -Encoding UTF8

# --- Success message ---
Write-Host "`nSetup complete!"
Write-Host "- .env file created at $ENV_FILE"
Write-Host "- SSL key/cert created at $SERVER_KEY and $SERVER_CERT"
Write-Host "- Java truststore created at $TRUSTSTORE_FILE"
Write-Host "`nYou can now start the MySQL server and the application as described in the README." 