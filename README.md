# nsd_basic_api_connector

## Что это:

Библиотека предоставляет имплементацию базового REST API NSD. Писалась под версию 4.15.
Имплементированные методы описаны в оф. документации
naumen: https://www.naumen.ru/docs/sd/415/NSD_manual.htm#RESTful/REST_API_method.htm?Highlight=REST

## Как это работает:

Входным классом является Connector, он предоставляет методы для общения с NSD.
Для создания экземпляра Connector в него нужно поместить ConnectorParams при вызове конструктора.
ConnectorParams содержит в себе реквизиты доступа и адрес инсталляция NSD.

## Конфигурационный файл:

#### Описание:

Простейшим способом не создавать ConnectorParams вручную и не хардкодить в него авторизационные
данные является размещение конфигурационного файла nsd_connector_params.json по адресу
{user.home}/nsd_sdk/conf/nsd_connector_params.json,
в таком случае экземпляр ConnectorParams можно создавать статическим методом ConnectorParams.byConfigFile().
Так же можно создать параметры при помощи метода ConnectorParams.byConfigFileInPath(), если конфигурационный файл
размещен не в ранее упомянутой директории.

#### Структура конфигурационного файла:

| Ключ                      | Наименование ключа        | Тип данных JSON | Описание ключа                                                                                                                                                                                                 |
|---------------------------|---------------------------|-----------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| installations             | Перечень инсталляция      | object[]        | Здесь описывается ненормированные множество инсталляций, к которым возможно подключение.                                                                                                                       |
| installations[].id        | Идентификатор инсталляции | string          | Пользовательский идентификатор инсталляции, задается что бы идентифицировать разные инсталляции из файла по необходимости.                                                                                     |
| installations[].scheme    | Схема подключения         | string          | По данной схеме будет производится подключение.                                                                                                                                                                |
| installations[].host      | Хост                      | string          | Хост, на который будет обращаться коннектор                                                                                                                                                                    |
| installations[].accessKey | Ключ доступа              | string          | Авторизация будет производится под этим ключом. Нужно учитывать доступы данного пользователя, которому принадлежит ключ. Можно указать null и получить ключ по логину и паролю при помощи метода getAccessKey. |
| installations[].ignoreSLL | Игнорировать SSL          | boolean         | Признак необходимости игнорировать SSL.                                                                                                                                                                        |

#### Пример конфигурационного файла:

В примере, приведенном ниже, описано подключение к двум инсталляциям. Коннектор будет обращаться к той инсталляции,
идентификатор которой указан при вызове методе ConnectorParams.byConfigFile()

```json
{
  "installations": [
    {
      "id": "MY_ANOTHER_INST",
      "scheme": "https",
      "host": "naumen.service.desk.ru",
      "accessKey": "0f6128511d-d3c8c-21f29f9e9b13-4101-8",
      "ignoreSLL": false
    },
    {
      "id": "PUBLIC_TEST",
      "scheme": "http",
      "host": "itsm365.ru",
      "accessKey": null,
      "ignoreSLL": true
    }
  ]
}
```

## Как масштабировать:

Класс Connector может наследоваться для реализации ваших коннекторов к кастомным методам API NSD в модулях вашей
инсталляции, в таком случае
легче всего реализовывать имплементацию кастомных методов через методы Connector.execPost() и Connector.execGet().
Авторизация в таком случае будет так же будет подготовлена, как и в случае с оригинальной библиотекой. Это простой
способ подключить ваш java проект к nsd.

## Как использовать в NSD:

При необходимости классы из пакета ru.ekazantsev.nsd_basic_api_connector данной библиотеки можно собрать
в один .groovy файл и разместить его в nsd как модуль, добавив в него одно поле вне классов (тк nsd не скомпилирует
модуль иначе).
Библиотека писалась с учетом такого назначения (используемые зависимости есть и в NSD).

## Примеры:

Пример получения объекта из NSD (groovy):

```groovy
//Параметры получаются из конфигурационного файла
ConnectorParams params = ConnectorParams.byConfigFile('PUBLIC_TEST')
//Создается экземпляр коннектора, устанавливается логирование
Connector api = new Connector(params)
api.setInfoLogging(true)
//Обращение к NSD
Map<String, Object> myNsdObjectInfo = api.get('serviceCall$51856603')
//Распечатка данные объекта в консоли
myNsdObjectInfo.each { key, value ->
    println("$key - ${value.toString()}")
}
```

Пример редактирования объекта в NSD (groovy):

```groovy
ConnectorParams params = ConnectorParams.byConfigFile('PUBLIC_TEST')
Connector api = new Connector(params)
api.setInfoLogging(true)
Map<String, Object> myNsdObjectInfo = api.editM2M('serviceCall$51856603', ['@comment': 'myNewComment', 'title': 'MyNewTitle'])
myNsdObjectInfo.each { key, value ->
    println("$key - ${value.toString()}")
}
```
