package nsd_basic_api_connector

import ru.ekazantsev.nsd_basic_api_connector.Connector
import ru.ekazantsev.nsd_basic_api_connector.ConnectorParams

ConnectorParams params = ConnectorParams.byConfigFile("DSO_TEST")
Connector.newInstance(params)
