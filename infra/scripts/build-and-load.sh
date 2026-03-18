#!/bin/bash

GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m'

PROJECT_ROOT="/Users/mordvinovila/Downloads/sub-engine-project"
SERVICES_DIR="$PROJECT_ROOT/services"

echo -e "${YELLOW}========================================${NC}"
echo -e "${YELLOW}    Полная сборка и загрузка в minikube${NC}"
echo -e "${YELLOW}========================================${NC}"

echo -e "\n${GREEN}1. Проверяю minikube...${NC}"
if ! minikube status | grep -q "host: Running"; then
  echo "Minikube не запущен. Запускаю..."
  minikube start --cpus=4 --memory=8192 --driver=docker
fi

echo -e "\n${GREEN}2. Настраиваю Docker окружение для minikube...${NC}"
eval $(minikube docker-env)

echo -e "\n${GREEN}3. Сборка микросервисов...${NC}"

SERVICES=(
  "eureka-server"
  "api-gateway"
  "subscription-service"
  "billing-service"
  "worker-service"
)

for SERVICE in "${SERVICES[@]}"; do
  echo -e "\n${YELLOW}Собираю $SERVICE...${NC}"

  if [ ! -d "$SERVICES_DIR/$SERVICE" ]; then
    echo -e "${RED}Папка $SERVICES_DIR/$SERVICE не найдена!${NC}"
    continue
  fi

  cd "$SERVICES_DIR/$SERVICE"

  if [ -f "pom.xml" ]; then
    echo "Maven проект: собираю jar..."
    ./mvnw clean package -DskipTests
  elif [ -f "build.gradle" ]; then
    echo "Gradle проект: собираю jar..."
    ./gradlew clean build -x test
  else
    echo "Не найден файл сборки, пропускаю..."
    continue
  fi

  if [ $? -ne 0 ]; then
    echo -e "${RED}Ошибка при сборке jar!${NC}"
    continue
  fi

  echo "Собираю Docker образ..."
  docker build -t "$SERVICE:latest" .

  if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ $SERVICE успешно собран${NC}"
  else
    echo -e "${RED}✗ Ошибка при сборке Docker образа $SERVICE${NC}"
  fi
done

cd "$PROJECT_ROOT"

echo -e "\n${GREEN}4. Загружаю инфраструктурные образы...${NC}"

INFRA_IMAGES=(
  "quay.io/keycloak/keycloak:26.2"
  "postgres:17-alpine"
  "redis:7-alpine"
  "mongo:7.0"
  "apache/kafka:latest"
  "prom/prometheus:latest"
  "grafana/grafana:10.3.1"
  "grafana/loki:2.9.2"
  "grafana/tempo:2.4.1"
  "grafana/alloy:latest"
  "prometheuscommunity/postgres-exporter:latest"
  "maildev/maildev:latest"
)

for IMAGE in "${INFRA_IMAGES[@]}"; do
  echo "Загружаю $IMAGE..."

  if ! docker images | grep -q "$(echo "$IMAGE" | cut -d':' -f1)"; then
    echo "  Скачиваю $IMAGE..."
    docker pull "$IMAGE"
  fi

  minikube image load "$IMAGE" --overwrite=true
done

echo -e "\n${GREEN}5. Проверяю загруженные образы...${NC}"
echo -e "${YELLOW}Микросервисы в minikube:${NC}"
minikube ssh -- docker images | grep -E "eureka|gateway|subscription|billing|worker"

echo -e "\n${YELLOW}Инфраструктура в minikube:${NC}"
minikube ssh -- docker images | grep -E "keycloak|postgres|redis|mongo|kafka|prometheus|grafana"

echo -e "\n${GREEN}========================================${NC}"
echo -e "${GREEN}Готово! Все образы собраны и загружены в minikube.${NC}"
echo -e "${GREEN}Теперь можно запускать: ./scripts/deploy-full.sh${NC}"