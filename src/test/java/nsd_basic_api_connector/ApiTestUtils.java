package nsd_basic_api_connector;

import org.junit.jupiter.api.Assumptions;
import ru.kazantsev.nsmp.basic_api_connector.Connector;
import ru.kazantsev.nsmp.basic_api_connector.ConnectorParams;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ApiTestUtils {

    private static final String DEFAULT_INSTALLATION_ID = System.getProperty("nsd.installationId", "EXEKI1");
    private static final boolean ENABLE_DEBUG_LOGGING = true;

    private ApiTestUtils() {
    }

    public static Connector getApi() {
        Path configPath = Path.of(System.getProperty("user.home"), "nsd_sdk", "conf", "nsd_connector_params.json");
        Assumptions.assumeTrue(
                Files.exists(configPath),
                "NSD integration config not found at " + configPath
        );
        try {
            ConnectorParams params = ConnectorParams.byConfigFile(DEFAULT_INSTALLATION_ID);
            Connector connector = new Connector(params);
            connector.setDebugLogging(ENABLE_DEBUG_LOGGING);
            return connector;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unused")
    public static File getMetainfoFile() {
        File file = new File(System.getProperty("user.home"), "metainfo.xml");
        try {
            if (!file.exists()) {
                Files.createFile(file.toPath());
            }
            return file;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
