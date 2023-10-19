package nsd_basic_api_connector

import ru.ekazantsev.nsd_basic_api_connector.Connector
import ru.ekazantsev.nsd_basic_api_connector.ConnectorParams

class ApiTestUtils {

    static String INSTALLATION_ID = "PUBLIC_TEST"
    static Boolean ENABLE_INFO_LOGGING = true

    static Connector getApi() {
        ConnectorParams params = ConnectorParams.byConfigFile(INSTALLATION_ID)
        Connector connector = new Connector(params)
        connector.setInfoLogging(ENABLE_INFO_LOGGING)
        return connector
    }
}

