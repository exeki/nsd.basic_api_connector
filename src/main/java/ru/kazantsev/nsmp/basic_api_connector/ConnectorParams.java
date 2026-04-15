package ru.kazantsev.nsmp.basic_api_connector;

import ru.kazantsev.nsmp.basic_api_connector.dto.InstallationDto;
import ru.kazantsev.nsmp.basic_api_connector.exception.ConfigurationException;
import ru.kazantsev.nsmp.basic_api_connector.exception.ConnectorParamsException;

import java.io.IOException;
import java.util.List;

/**
 * DTO, содержит параметры для связи с NSMP и методы по их упрощенному получению
 */
public class ConnectorParams {

    /**
     * Пользовательский идентификатор
     */
    private final String installationId;
    /**
     * Ключ доступа
     */
    private final String accessKey;
    /**
     * Хост
     */
    private final String host;
    /**
     * Схема (http/https)
     */
    private final String scheme;
    /**
     * Признак необходимости игнорировать ssl
     */
    private final Boolean ignoreSSL;

    /**
     * Конструктор для ручного сбора параметров
     *
     * @param installationId пользовательский идентификатор
     * @param scheme         Схема (http/https)
     * @param host           Хост
     * @param accessKey      Ключ доступа
     * @param ignoreSSL      Признак необходимости игнорировать ssl
     */
    public ConnectorParams(
            String installationId,
            String scheme,
            String host,
            String accessKey,
            Boolean ignoreSSL
    ) throws ConnectorParamsException {
        this.installationId = installationId;
        this.scheme = scheme;
        this.host = host;
        this.accessKey = accessKey;
        this.ignoreSSL = ignoreSSL;
        validateConnectorParams(this);
    }

    /**
     * Получить расположение конфигурационного файла по умолчанию
     */
    @SuppressWarnings("unused")
    public static String getDefaultParamsFilePath() {
        return ConfigService.getDefaultParamsFilePath();
    }

    /**
     * Собирает и наполняет экземпляр из параметров, описанных в конфигурационном файле,
     * расположенном по адресу {user.home}/.nsmp_sdk/conf/connector_params.json
     *
     * @param installationId ID инсталляции, указанный конфигурационном файле
     * @return заполненный экземпляр ConnectorParams
     * @throws ConfigurationException выбрасывается конфиг не заполнен/заполнен не полностью
     * @throws IOException            выбрасывается если не удается считать json
     */
    public static ConnectorParams byConfigFile(String installationId) throws ConfigurationException, IOException, ConnectorParamsException {
        return byConfigFileInPath(installationId, getDefaultParamsFilePath());
    }

    /**
     * Создать ConnectorParams из dto InstallationDto
     *
     * @param dto InstallationDto
     * @return новый экземпляр ConnectorParams
     */
    public static ConnectorParams fromDto(InstallationDto dto) throws ConnectorParamsException {
        return new ConnectorParams(
                dto.id,
                dto.scheme,
                dto.host,
                dto.accessKey,
                dto.ignoreSSL != null ? dto.ignoreSSL : false
        );
    }

    /**
     * Собирает и наполняет экземпляр из параметров, описанных в конфигурационном файле,
     * расположенном по адресу {user.home}/.nsmp_sdk/conf/connector_params.json
     *
     * @param installationId   ID инсталляции, указанный конфигурационном файле
     * @param pathToConfigFile путь до конфигурационного файла
     * @return заполненный экземпляр ConnectorParams
     * @throws ConfigurationException выбрасывается конфиг не заполнен/заполнен не полностью
     * @throws IOException            выбрасывается если не удается считать json
     */
    public static ConnectorParams byConfigFileInPath(String installationId, String pathToConfigFile) throws IOException, ConnectorParamsException, ConfigurationException {
        ConfigService service = new ConfigService(pathToConfigFile);
        InstallationDto installationDto = service.getInstallation(installationId);
        if (installationDto == null)
            throw new ConfigurationException("Installation configuration " + installationId + " could not be obtained in the configuration file at " + pathToConfigFile);
        return fromDto(installationDto);
    }

    private static void validateConnectorParams(ConnectorParams dto) throws ConnectorParamsException {
        List<String> schemes = List.of("https", "http");
        if (dto.scheme == null || dto.scheme.trim().isEmpty())
            throw new ConnectorParamsException("Scheme is not specified for installation " + dto.installationId);
        if (schemes.stream().noneMatch(it -> it.equals(dto.scheme))) {
            throw new ConnectorParamsException("Unknown scheme " + dto.scheme + " for installation " + dto.installationId);
        }
        if (dto.host == null || dto.host.trim().isEmpty()) {
            throw new ConnectorParamsException("Host for installation " + dto.installationId + " is not specified");
        }
        if (dto.accessKey == null || dto.accessKey.trim().isEmpty()) {
            throw new ConnectorParamsException("AccessKey for installation " + dto.installationId + " is not specified");
        }
    }

    public String getAccessKey() {
        return accessKey;
    }

    public String getHost() {
        return host;
    }

    public String getScheme() {
        return scheme;
    }

    public Boolean isIgnoringSSL() {
        return ignoreSSL;
    }

    @SuppressWarnings("unused")
    public String getInstallationId() {
        return installationId;
    }

}