package nsd_basic_api_connector.examples

import ru.ekazantsev.nsd_basic_api_connector.Connector
import ru.ekazantsev.nsd_basic_api_connector.ConnectorParams

ConnectorParams params = ConnectorParams.byConfigFile('PUBLIC_TEST')
Connector api = new Connector(params)
api.setInfoLogging(true)
Map<String, Object> myNsdObjectInfo = api.editM2M('serviceCall$51856603', ['@comment' : 'myNewComment', 'title' : 'MyNewTitle'])
myNsdObjectInfo.each{ key, value ->
    println("$key - ${value.toString()}")
}