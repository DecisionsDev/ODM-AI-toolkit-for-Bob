#!/usr/bin/env bash
# odm.sh — manage the ODM Docker container
# Usage: ./odm.sh [start|stop|restart|status]

set -euo pipefail

CONTAINER_NAME="odm"
IMAGE="icr.io/cpopen/odm-k8s/odm:latest"
HTTP_PORT=9060
HTTPS_PORT=9443
RUN_USER=501

usage() {
  echo "Usage: $0 {start|stop|restart|status}"
  exit 1
}

is_running() {
  docker inspect -f '{{.State.Running}}' "$CONTAINER_NAME" 2>/dev/null | grep -q "^true$"
}

container_exists() {
  docker inspect -f '{{.Name}}' "$CONTAINER_NAME" 2>/dev/null | grep -q "/$CONTAINER_NAME"
}

start() {
  if is_running; then
    echo "ODM is already running (container: $CONTAINER_NAME)."
    return 0
  fi

  if container_exists; then
    echo "Starting existing ODM container..."
    docker start "$CONTAINER_NAME"
  else
    echo "Creating and starting ODM container..."
    docker run -d \
      -e LICENSE=accept \
      -p "${HTTP_PORT}:9060" \
      -p "${HTTPS_PORT}:9443" \
      -u "$RUN_USER" \
      --name "$CONTAINER_NAME" \
      "$IMAGE"
  fi

  echo "ODM started."
  echo "  HTTP  → http://localhost:${HTTP_PORT}/odm"
  echo "  HTTPS → https://localhost:${HTTPS_PORT}/odm"
}

stop() {
  if ! container_exists; then
    echo "ODM container '$CONTAINER_NAME' does not exist."
    return 0
  fi

  if is_running; then
    echo "Stopping ODM container..."
    docker stop "$CONTAINER_NAME"
    echo "ODM stopped."
  else
    echo "ODM container is already stopped."
  fi
}

restart() {
  echo "Restarting ODM..."
  stop
  start
}

status() {
  if ! container_exists; then
    echo "ODM container '$CONTAINER_NAME' does not exist."
    return 0
  fi

  if is_running; then
    echo "ODM is running."
    docker ps --filter "name=^/${CONTAINER_NAME}$" --format "  ID: {{.ID}}  Ports: {{.Ports}}  Status: {{.Status}}"
  else
    echo "ODM container exists but is stopped."
  fi
}

case "${1:-}" in
  start)   start   ;;
  stop)    stop    ;;
  restart) restart ;;
  status)  status  ;;
  *)       usage   ;;
esac
