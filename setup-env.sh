#!/bin/bash
set -e

# --- Helper Functions ---
ask() {
  local var="$1"
  local prompt="$2"
  local silent="$3"
  if [ "$silent" = "silent" ]; then
    read -rsp "$prompt: " $var
    echo
  else
    read -rp "$prompt: " $var
  fi
}

# --- Prompt for user input ---
echo "--- Bookshop Environment Setup ---"
ask MYSQL_ROOT_PASSWORD "Enter MySQL root password" silent
ask MYSQL_DATABASE "Enter MySQL database name"
ask MYSQL_USER "Enter MySQL user name"
ask MYSQL_PASSWORD "Enter MySQL user password" silent
ask SPRING_DATASOURCE_HIKARI_TRUSTSTORE_PASSWORD "Enter password for Java truststore (used by Spring Boot)" silent

# --- Paths ---
ENV_FILE="docker/.env"
SSL_DIR="docker/mysql-ssl"
TRUSTSTORE_FILE="$SSL_DIR/truststore.jks"
SERVER_KEY="$SSL_DIR/server-key.pem"
SERVER_CERT="$SSL_DIR/server-cert.pem"

# --- Check for existing files ---
if [ -f "$ENV_FILE" ]; then
  read -rp "$ENV_FILE already exists. Overwrite? (y/N): " OVERWRITE_ENV
  [[ "$OVERWRITE_ENV" =~ ^[Yy]$ ]] || { echo "Aborting."; exit 1; }
fi
if [ -f "$SERVER_KEY" ] || [ -f "$SERVER_CERT" ]; then
  read -rp "SSL key/cert already exist. Overwrite? (y/N): " OVERWRITE_SSL
  [[ "$OVERWRITE_SSL" =~ ^[Yy]$ ]] || { echo "Aborting."; exit 1; }
fi
if [ -f "$TRUSTSTORE_FILE" ]; then
  read -rp "$TRUSTSTORE_FILE already exists. Overwrite? (y/N): " OVERWRITE_TS
  [[ "$OVERWRITE_TS" =~ ^[Yy]$ ]] || { echo "Aborting."; exit 1; }
fi

# --- Create SSL directory if needed ---
mkdir -p "$SSL_DIR"

# --- Generate MySQL server key and certificate ---
echo "Generating MySQL server key and certificate..."
openssl req -newkey rsa:2048 -nodes -keyout "$SERVER_KEY" -x509 -days 365 -out "$SERVER_CERT" -subj "/CN=localhost"

# --- Create Java truststore and import cert ---
echo "Creating Java truststore and importing certificate..."
if [ -f "$TRUSTSTORE_FILE" ]; then
  keytool -delete -alias mysql-server -keystore "$TRUSTSTORE_FILE" -storepass "$SPRING_DATASOURCE_HIKARI_TRUSTSTORE_PASSWORD" 2>/dev/null || true
fi
keytool -importcert -noprompt -alias mysql-server -file "$SERVER_CERT" -keystore "$TRUSTSTORE_FILE" -storepass "$SPRING_DATASOURCE_HIKARI_TRUSTSTORE_PASSWORD"

# --- Compose connection string and truststore location ---
BOOK_STORE_CONNECTION_STRING="jdbc:mysql://localhost:3306/$MYSQL_DATABASE?serverTimezone=UTC&useSSL=true&requireSSL=true"
TRUSTSTORE_LOCATION="file:$(cd "$(dirname "$TRUSTSTORE_FILE")" && pwd)/$(basename "$TRUSTSTORE_FILE")"

# --- Write .env file ---
echo "Writing $ENV_FILE..."
cat > "$ENV_FILE" <<EOF
MYSQL_ROOT_PASSWORD=$MYSQL_ROOT_PASSWORD
MYSQL_DATABASE=$MYSQL_DATABASE
MYSQL_USER=$MYSQL_USER
MYSQL_PASSWORD=$MYSQL_PASSWORD
BOOK_STORE_CONNECTION_STRING="$BOOK_STORE_CONNECTION_STRING"
SPRING_DATASOURCE_HIKARI_TRUSTSTORE_PASSWORD=$SPRING_DATASOURCE_HIKARI_TRUSTSTORE_PASSWORD
SPRING_DATASOURCE_HIKARI_TRUSTSTORE_LOCATION=$TRUSTSTORE_LOCATION
EOF

# --- Success message ---
echo "\nSetup complete!"
echo "- .env file created at $ENV_FILE"
echo "- SSL key/cert created at $SERVER_KEY and $SERVER_CERT"
echo "- Java truststore created at $TRUSTSTORE_FILE"
echo "\nYou can now start the MySQL server and the application as described in the README." 