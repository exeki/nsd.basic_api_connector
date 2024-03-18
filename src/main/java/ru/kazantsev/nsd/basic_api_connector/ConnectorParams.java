package ru.kazantsev.nsd.basic_api_connector;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.naming.ConfigurationException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * DTO, содержит параметры для связи с NSD и методы по их упрощенному получению
 */
public class ConnectorParams {
    /**
     * Расположение конфигурационного файла по умолчанию
     */
    private static final String DEFAULT_PARAMS_FILE_PATH = System.getProperty("user.home") + "\\nsd_sdk\\conf\\nsd_connector_params.json";

    /**
     * Пользовательский идентификатор
     */

    private String userId;
    /**
     * Ключ доступа
     */
    private String accessKey;
    /**
     * Хост
     */
    private String host;
    /**
     * Схема (http/https)
     */
    private String scheme;
    /**
     * Признак необходимости игнорировать ssl
     */
    private Boolean ignoreSSL;

    /**
     * Конструктор для ручного сбора параметров
     * @param userId пользовательский идентификатор
     * @param scheme Схема (http/https)
     * @param host Хост
     * @param accessKey Ключ доступа
     * @param ignoreSSL Признак необходимости игнорировать ssl
     */

    public ConnectorParams(
            String userId,
            String scheme,
            String host,
            String accessKey,
            Boolean ignoreSSL
    ) {
        this.userId = userId;
        this.scheme = scheme;
        this.host = host;
        this.accessKey = accessKey;
        this.ignoreSSL = ignoreSSL;
    }

    protected ConnectorParams() {}

    public static String getDefaultParamsFilePath(){
        return DEFAULT_PARAMS_FILE_PATH;
    }

    /**
     * Собирает и наполняет экземпляр из параметров, описанных в конфигурационном файле,
     * расположенном по адресу {user.home}/nsd_sdk/conf/nsd_connector_params.json
     * @param installationId ID инсталляции, указанный конфигурационном файле
     * @return заполненный экземпляр ConnectorParams
     * @throws ConfigurationException выбрасывается конфиг не заполнен/заполнен не полностью
     * @throws IOException выбрасывается если не удается считать json
     */
    public static ConnectorParams byConfigFile(String installationId) throws ConfigurationException, IOException {
        return byConfigFileInPath(installationId, DEFAULT_PARAMS_FILE_PATH);
    }

    /**
     * Собирает и наполняет экземпляр из параметров, описанных в конфигурационном файле,
     * расположенном по адресу {user.home}/nsd_sdk/conf/nsd_connector_params.json
     * @param installationId ID инсталляции, указанный конфигурационном файле
     * @param pathToConfigFile путь до конфигурационного файла
     * @return заполненный экземпляр ConnectorParams
     * @throws ConfigurationException выбрасывается конфиг не заполнен/заполнен не полностью
     * @throws IOException выбрасывается если не удается считать json
     */
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

        List<ConfigFileDto.InstallationConfig> installationConfigs = new ArrayList<>();

        for(ConfigFileDto.InstallationConfig config : configFileDto.installations) {
            if(Objects.equals(config.id, installationId)) installationConfigs.add(config);
        }

        if (installationConfigs.size() != 1) {
            throw new ConfigurationException("Installation configuration "+  installationId + " could not be obtained " +
                    "in the configuration file at " + pathToConfigFile);
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
                installationId,
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

    public String getUserId() {return userId; }

}