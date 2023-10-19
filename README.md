# nsd_basic_api_connector
Библиотека предоставляет имплементацию базового API NSD.
Входным классом является Connector, он предоставляет методы для общения с NSD. 
Для создания экземляра Connector в него нужно поместить ConnectorParams при выховыве конструктора. ConnectorParams содержит в себе реквизиты доступа и адрес инсталляция NSD. 
Простейшим способом не создавать ConnectorParams вручную и не хардкодить в него авторизационные данные является размезение конфигурационного файла nsd_connector_params.json по адресу {user.home}/nsd_sdk/conf, 
в таком случае экземпляр ConnectorParams можно создавать статическим методом ConnectorParams.byConfigFile() или ConnectorParams.byConfigFileInPath(), если конфигурационный файл размещен не в ранее упомянутой директории. 

Класс Connector может наследоваться для реализации коннекторов к кастомным методам API NSD, в таком случае легче всего реализовывать имплементацию кастомных методов через методы Connector.execPost() и Connector.execGet(). 
Авторизация так же будет подготовлена. 

Имплементированные методы описаны в оф. документации naumen: https://www.naumen.ru/docs/sd/415/NSD_manual.htm#RESTful/REST_API_method.htm?Highlight=REST
