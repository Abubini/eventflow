#!/usr/bin/env bash
# Run once on your production server as root or sudo user.
# Sets up Docker, the app directory, and the .env file.
set -e

echo "── Installing Docker ──────────────────────────────────"
apt-get update -q
apt-get install -y ca-certificates curl gnupg
install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg \
  | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
chmod a+r /etc/apt/keyrings/docker.gpg

echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] \
  https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" \
  | tee /etc/apt/sources.list.d/docker.list > /dev/null

apt-get update -q
apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

echo "── Creating app directory ─────────────────────────────"
mkdir -p /opt/eventflow
cd /opt/eventflow

echo "── Copying docker-compose.yml ─────────────────────────"
# Copy your docker-compose.yml here (scp or paste it)
# scp docker-compose.yml user@your-server:/opt/eventflow/

echo "── Creating .env file ─────────────────────────────────"
cat > /opt/eventflow/.env << 'EOF'
POSTGRES_DB=eventflow
POSTGRES_USER=eventflow
POSTGRES_PASSWORD=CHANGE_ME_STRONG_PASSWORD

JWT_SECRET=CHANGE_ME_64_CHAR_HEX_STRING
JWT_EXPIRATION_MS=86400000

MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=CHANGE_ME@gmail.com
MAIL_PASSWORD=CHANGE_ME_APP_PASSWORD

BASE_URL=https://your-domain.com
APP_PORT=8080
EOF

echo ""
echo "✅ Server setup complete."
echo "   Edit /opt/eventflow/.env with your real values, then run:"
echo "   cd /opt/eventflow && docker compose up -d"