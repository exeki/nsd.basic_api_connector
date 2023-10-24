package nsd_basic_api_connector.examples

import ru.kazantsev.nsd.basic_api_connector.Connector
import ru.kazantsev.nsd.basic_api_connector.ConnectorParams

ConnectorParams params = ConnectorParams.byConfigFile('PUBLIC_TEST')
Connector api = new Connector(params)
api.setInfoLogging(true)
Map<String, Object> myNsdObjectInfo = api.get('serviceCall$51856603')
myNsdObjectInfo.each{ key, value ->
    println("$key - ${value.toString()}")
}