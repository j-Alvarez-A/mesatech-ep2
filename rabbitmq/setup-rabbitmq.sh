#!/usr/bin/env bash
# =====================================================================
#  Crea de forma reproducible los recursos de mensajería de MesaTech:
#  vhost, usuarios de aplicación, permisos, exchange, cola y binding.
#  Ejecutar en la EC2 C, en esta carpeta, con el contenedor ya "healthy":
#       chmod +x setup-rabbitmq.sh && ./setup-rabbitmq.sh
#  Es idempotente: se puede volver a ejecutar sin duplicar nada.
# =====================================================================
set -euo pipefail
cd "$(dirname "$0")"

# Carga credenciales desde .env (no se imprimen)
set -a; source ./.env; set +a

VHOST="mesatech"
EXCHANGE="catalogo.exchange"
QUEUE="solicitudes.catalogo.queue"
ROUTING_KEY="categoria.creada"

ctl()   { docker compose exec -T rabbitmq rabbitmqctl "$@"; }
# API HTTP de administración (puerto 15672 en la propia EC2). Funciona igual
# en cualquier versión 3.x/4.x de la imagen, sin depender de rabbitmqadmin.
api() {   # $1 método  $2 ruta  $3 json
  curl -fsS -u "$RABBITMQ_ADMIN_USER:$RABBITMQ_ADMIN_PASSWORD" \
       -H "content-type: application/json" -X "$1" "http://localhost:15672/api$2" -d "$3"
  echo "  OK $1 $2"
}

crear_usuario() {   # $1 usuario  $2 password
  if ctl list_users --quiet | awk '{print $1}' | grep -qx "$1"; then
    echo "  usuario $1 ya existe -> se actualiza su contraseña"
    ctl change_password "$1" "$2" >/dev/null
  else
    ctl add_user "$1" "$2" >/dev/null
  fi
  # Sin tags: NO es administrador, ni monitoring, ni puede entrar a la UI.
  ctl set_user_tags "$1" >/dev/null
}

echo "==> 1. Virtual host '$VHOST'"
ctl add_vhost "$VHOST" 2>/dev/null || echo "  (ya existía)"

echo "==> 2. Usuarios de aplicación (sin privilegios administrativos)"
crear_usuario "$RABBITMQ_PUBLICADOR_USER" "$RABBITMQ_PUBLICADOR_PASSWORD"
crear_usuario "$RABBITMQ_CONSUMIDOR_USER" "$RABBITMQ_CONSUMIDOR_PASSWORD"

echo "==> 3. Permisos (configure / write / read) mínimos sobre '$VHOST'"
# Publicador: solo puede ESCRIBIR (publicar) en catalogo.exchange.
ctl set_permissions -p "$VHOST" "$RABBITMQ_PUBLICADOR_USER" '^$' '^catalogo\.exchange$' '^$'
# Consumidor: solo puede LEER (consumir) de solicitudes.catalogo.queue.
ctl set_permissions -p "$VHOST" "$RABBITMQ_CONSUMIDOR_USER" '^$' '^$' '^solicitudes\.catalogo\.queue$'
# Administrador: acceso completo al vhost para gestionarlo desde la UI.
ctl set_permissions -p "$VHOST" "$RABBITMQ_ADMIN_USER" '.*' '.*' '.*'

echo "==> 4. Exchange direct durable '$EXCHANGE'"
api PUT "/exchanges/$VHOST/$EXCHANGE" \
    '{"type":"direct","durable":true,"auto_delete":false,"internal":false,"arguments":{}}'

echo "==> 5. Cola classic durable, sin autoeliminación '$QUEUE'"
api PUT "/queues/$VHOST/$QUEUE" \
    '{"durable":true,"auto_delete":false,"arguments":{}}'   # classic es el tipo por defecto

echo "==> 6. Binding $EXCHANGE --[$ROUTING_KEY]--> $QUEUE"
# (crear el mismo binding dos veces no lo duplica)
api POST "/bindings/$VHOST/e/$EXCHANGE/q/$QUEUE" \
    "{\"routing_key\":\"$ROUTING_KEY\",\"arguments\":{}}"

echo "==> Verificación"
ctl list_vhosts
ctl list_permissions -p "$VHOST"
ctl list_exchanges -p "$VHOST" name type durable auto_delete | grep -E "name|$EXCHANGE"
ctl list_queues    -p "$VHOST" name durable auto_delete messages_ready messages_unacknowledged
ctl list_bindings  -p "$VHOST" source_name destination_name routing_key | grep -E "source_name|$EXCHANGE"
echo "Listo."
