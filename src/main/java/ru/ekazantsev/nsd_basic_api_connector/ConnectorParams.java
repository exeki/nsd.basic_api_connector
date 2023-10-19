package ru.ekazantsev.nsd_basic_api_connector;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.naming.ConfigurationException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ConnectorParams {
    private static final String DEFAULT_PARAMS_FILE_PATH = System.getProperty("user.home") + "\\nsd_sdk\\conf\\nsd_connector_params.json";
    private String accessKey;
    private String host;
    private String scheme;
    private Boolean ignoreSSL;

    public ConnectorParams(
            String scheme,
            String host,
            String accessKey,
            Boolean ignoreSSL
    ) {
        this.scheme = scheme;
        this.host = host;
        this.accessKey = accessKey;
        this.ignoreSSL = ignoreSSL;
    }

    protected ConnectorParams() {
    }

    public static ConnectorParams byConfigFile(String installationId) throws ConfigurationException, IOException {
        return byConfigFileInPath(installationId, DEFAULT_PARAMS_FILE_PATH);
    }

    public static ConnectorParams byConfigFileInPath(String installationId, String pathToConfigFile) throws IOException, ConfigurationException {
        File configFile = new File(pathToConfigFile);
        if (!configFile.exists())
            throw new FileNotFoundException("The configuration file was not found at " + pathToConfigFile);

        ConfigFileDto configFileDto;

        try {
            configFileDto = new ObjectMapper().readValue(configFile, ConfigFileDto.class);
        } catch (IOException e) {
            throw new IOException("Data could not be read from the configuration file at " + pathToConfigFile + ". Error text:" + e.getMessage());
        }

        List<ConfigFileDto.InstallationConfig> installationConfigs = configFileDto.installations
                .stream()
                .filter(it -> Objects.equals(it.id, installationId))
                .collect(Collectors.toList());
        if (installationConfigs.size() != 1) {
            throw new ConfigurationException("Installation configuration could not be obtained с ID "
                    + installationId + " in the configuration file at " + pathToConfigFile);
        }
        ConfigFileDto.InstallationConfig installationConfig = installationConfigs.get(0);
        if (installationConfig.host == null || installationConfig.host.trim().length() == 0) {
            throw new ConfigurationException("The host for installation is not specified" + installationId
                    + " in the configuration file at " + pathToConfigFile);
        }
        if (installationConfig.accessKey == null || installationConfig.accessKey.trim().length() == 0) {
            throw new ConfigurationException("accessKey for installation is not specified for " + installationId +
                    " in the configuration file at " + pathToConfigFile);
        }
        if (installationConfig.scheme == null || installationConfig.scheme.trim().length() == 0) {
            throw new ConfigurationException("Scheme is not specified for installation" + installationId +
                    " in the configuration file at " + pathToConfigFile);
        }
        return new ConnectorParams(
                installationConfig.scheme,
                installationConfig.host,
                installationConfig.accessKey,
                installationConfig.ignoreSLL != null ? installationConfig.ignoreSLL : false
        );
    }

    protected String getAccessKey() {
        return accessKey;
    }

    public String getHost() {
        return host;
    }

    public String getScheme() {
        return scheme;
    }

    public Boolean isIgnoringSSL(){
        return ignoreSSL;
    }

}