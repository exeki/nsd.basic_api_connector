# nsd_basic_api_connector
## Что это
Библиотека предоставляет имплементацию базового REST API NSD. Писалась под версию 4.15.
Имплементированные методы описаны в оф. документации naumen: https://www.naumen.ru/docs/sd/415/NSD_manual.htm#RESTful/REST_API_method.htm?Highlight=REST

## Как это работает
Входным классом является Connector, он предоставляет методы для общения с NSD. 
Для создания экземпляра Connector в него нужно поместить ConnectorParams при вызове конструктора. 
ConnectorParams содержит в себе реквизиты доступа и адрес инсталляция NSD. 
Простейшим способом не создавать ConnectorParams вручную и не хардкодить 
в него авторизационные данные является размещение конфигурационного файла nsd_connector_params.json по адресу {user.home}/nsd_sdk/conf, 
в таком случае экземпляр ConnectorParams можно создавать статическим 
методом ConnectorParams.byConfigFile() или ConnectorParams.byConfigFileInPath(), если конфигурационный файл размещен не в ранее упомянутой директории. 

## Как масштабировать
Класс Connector может наследоваться для реализации ваших коннекторов к кастомным методам API NSD в модулях вашей инсталляции, в таком случае
легче всего реализовывать имплементацию кастомных методов через методы Connector.execPost() и Connector.execGet().
Авторизация в таком случае будет так же будет подготовлена, как и в случае с оригинальной библиотекой. Это простой способ подключить ваш java проект к nsd.

## Как использовать в NSD
При необходимости классы из пакета ru.ekazantsev.nsd_basic_api_connector данной библиотеки можно собрать
в один .groovy файл и разместить его в nsd как модуль, добавив в него одно поле вне классов (тк nsd не скомпилирует модуль иначе).
Библиотека писалась с учетом такого назначения (используемые зависимости есть и в NSD).

## Примеры
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
myNsdObjectInfo.each{ key, value ->
    println("$key - ${value.toString()}")
}
```
Пример редактирования объекта в NSD (groovy):
```groovy
ConnectorParams params = ConnectorParams.byConfigFile('PUBLIC_TEST')
Connector api = new Connector(params)
api.setInfoLogging(true)
Map<String, Object> myNsdObjectInfo = api.editM2M('serviceCall$51856603', ['@comment' : 'myNewComment', 'title' : 'MyNewTitle'])
myNsdObjectInfo.each{ key, value ->
    println("$key - ${value.toString()}")
}
```
