# ShopApi
REST API сервис, который позволяет магазинам загружать и обновлять информацию о товарах, а пользователям - смотреть какие товары были обновлены за последние сутки. Сервис удовлетворяет требованиям, описанным в [задании](Task.md).

# Функционал
- Импортирует новые товары и/или категории. Товары/категории импортированные повторно обновляют текущие.
- Удаляет элемент по идентификатору. При удалении категории удаляются все дочерние элементы.
- Получает информацию об элементе по идентификатору. При получении информации о категории также предоставляется информация о её дочерних элементах.
- Получает список товаров, цена которых была обновлена за последние 24 часа включительно [now() - 24h, now()] от времени переданном в запросе.

# Стек технологий
- фреймворк для создания микросервиса на Java Spring Boot
- noSql база данных MongoDB
- ПО для автоматизации развертывания приложения Docker
- JUnit 5 для тестирования приложения
- Swagger 3 для документации

## Системные требования
- Docker Desktop 4.8.0+
- jdk 17
- Maven 3.8.0+

# Сборка
Для сборки сервиса необходимо прописать команду:
```
sudo docker-compose build
```

# Запуск
Для запуска сервиса необходимо прописать команду:
```
sudo docker-compose up
```
Для остановки сервиса необходимо прописать команду:
```
sudo docker kill $(sudo docker ps -q)
```


# Тестирование
Для тестирования сервиса написаны интеграционные тесты контроллера и unit-тесты сервиса.

# Документация
Обратитесь по адресу https://dennis-2009.usr.yandex-academy.ru/swagger-ui.html для открытия документации REST-api сервиса.