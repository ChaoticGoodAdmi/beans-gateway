# Сервис BEANS-GATEWAY

## Описание

Этот микросервис представляет собой API Gateway, реализованный на Kotlin с использованием Spring Boot и Spring Cloud Gateway. Основная задача сервиса — маршрутизация запросов к микросервисам и управление доступом на основе JWT-токенов.
**Ключевые функции**

    Валидация JWT-токенов.
    Проверка ролей пользователей (GUEST или BARISTA) на основе маршрутов.
    Извлечение userId и coffeeShopId из токена и добавление их в заголовки запросов (X-UserId, X-CoffeeShopId).
    Гибкая маршрутизация запросов к разным микросервисам:
        profile-svc для работы с профилями.
        menu-svc для управления меню.
        order-svc для обработки заказов.
        loyalty-svc для работы с системой лояльности.
        insight-svc для аналитики.
        journal-svc для работы с журналами.
    Поддержка метрик через Spring Actuator для интеграции с Prometheus.

**Требования по инфраструктуре**

    Kubernetes: для развертывания сервисов.
    База данных:
        Gateway сам не требует базы данных, но должны быть развернуты соответствующие сервисы с подключением к своим базам данных (например, profile-svc, menu-svc и т.д.).
    Prometheus и Grafana: для мониторинга метрик.
    Java: версия 17 или выше.
    Kotlin: версия 1.8 или выше.

**Инструкция по сборке**

Клонирование репозитория

    git clone <URL_репозитория>
    cd <имя_репозитория>

**Сборка проекта** 
Убедитесь, что установлен Gradle. Затем выполните команду:

    ./gradlew build

Создание Docker-образа Сервис рассчитан на развертывание в Kubernetes, поэтому создайте Docker-образ:

    docker build -t my-gateway:latest .

Создание файла конфигурации В директории src/main/resources создайте файл application.yml, если его нет:

    server:
      port: 8080

    spring:
      cloud:
        gateway:
          routes:
            - id: profile
              uri: http://profile-svc
              predicates:
                - Path=/profile/**
            - id: menu
              uri: http://menu-svc
              predicates:
                - Path=/menu/**
      security:
        jwt:
          secret: "your-secret-key"

    management:
      endpoints:
        web:
          exposure:
            include: "*"

## Пример использования

**Тестовые маршруты**

Аутентификация:

    curl -X POST http://<gateway-url>/auth/login -d '{"username":"user", "password":"pass"}' -H "Content-Type: application/json"

Получение данных о меню (любая роль):

    curl -X GET http://<gateway-url>/menu -H "Authorization: Bearer <JWT_TOKEN>"

Удаление кофейни (только бариста):

    curl -X DELETE http://<gateway-url>/coffee-shops -H "Authorization: Bearer <JWT_TOKEN>"