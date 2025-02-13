package nsd_basic_api_connector

import ru.kazantsev.nsd.basic_api_connector.Connector
import ru.kazantsev.nsd.basic_api_connector.ConnectorParams

class ApiTestUtils {

    static String INSTALLATION_ID = "EXEKI1"
    static Boolean ENABLE_INFO_LOGGING = true

    static Connector getApi() {
        ConnectorParams params = ConnectorParams.byConfigFile(INSTALLATION_ID)
        Connector connector = new Connector(params)
        connector.setDebugLogging(ENABLE_INFO_LOGGING)
        return connector
    }

    static File getMetainfoFile() {
        def file = new File(System.getProperty("user.home") + "/metainfo.xml")
        if(!file.exists()) file.createNewFile()
        return file
    }
}

