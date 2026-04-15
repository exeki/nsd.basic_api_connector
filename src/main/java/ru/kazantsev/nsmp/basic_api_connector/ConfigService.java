package ru.kazantsev.nsmp.basic_api_connector;

import com.fasterxml.jackson.databind.ObjectMapper;
import ru.kazantsev.nsmp.basic_api_connector.dto.ConfigDto;
import ru.kazantsev.nsmp.basic_api_connector.dto.InstallationDto;
import ru.kazantsev.nsmp.basic_api_connector.exception.ConfigurationException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * Служба для управления конфигурационным файлом
 */
public class ConfigService {

    private static final String DEFAULT_PARAMS_FILE_PATH = System.getProperty("user.home") + "\\.nsmp_sdk\\conf\\connector_params.json";

    private final String pathToConfigFile;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Получить расположение конфигурационного файла по умолчанию
     */
    public static String getDefaultParamsFilePath() {
        return DEFAULT_PARAMS_FILE_PATH;
    }

    /**
     * Создать службу с кастомным путем до конфигурационного файла
     * @param pathToConfigFile путь до конфигурационного файла
     */
    public ConfigService(String pathToConfigFile) {
        this.pathToConfigFile = pathToConfigFile;
    }

    /**
     * Создать службу со стандартным путем для конфигурационного файла
     */
    @SuppressWarnings("unused")
    public ConfigService() {
        this.pathToConfigFile = DEFAULT_PARAMS_FILE_PATH;
    }

    /**
     * Сохранить конфигурацию
     * @param dto dto конфигурации в полном составе
     * @throws IOException если не удалось записать
     */
    public void saveConfig(ConfigDto dto) throws IOException {
        Objects.requireNonNull(dto, "Configuration data must not be null");
        File file = new File(pathToConfigFile);
        File parentDir = file.getParentFile();
        if (parentDir != null && !parentDir.exists() && !parentDir.mkdirs()) {
            throw new IOException("Could not create directory for configuration file at " + parentDir.getAbsolutePath());
        }
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(file, dto);
        } catch (IOException e) {
            throw new IOException("The data could not be saved in the configuration file at " +
                    pathToConfigFile + ". Error text:" + e.getMessage(), e);
        }
    }

    /**
     * Получить конфигурацию в полном составе
     * @return dto конфигурацию, null если не существует
     * @throws IOException если не удалось прочитать файл
     */
    public ConfigDto getConfig() throws IOException {
        File configFile = new File(pathToConfigFile);
        if (!configFile.exists()) return null;
        try {
            return objectMapper.readValue(configFile, ConfigDto.class);
        } catch (IOException e) {
            throw new IOException("Couldn't read data from the configuration file at " +
                    pathToConfigFile + ". Error text:" + e.getMessage(), e);
        }
    }

    /**
     * Получить конфигурацию конкретной инсталляции
     * @param installationId идентификатор инсталляции
     * @param configDto дто инсталляции
     * @return конфигурация инсталляции, null если ее не существует
     * @throws ConfigurationException если в конфигурации ошибка
     */
    private InstallationDto getInstallation(String installationId, ConfigDto configDto) throws ConfigurationException {
        Objects.requireNonNull(installationId, "InstallationId must not be null");
        Objects.requireNonNull(configDto, "configDto must not be null");
        List<InstallationDto> installations = configDto.installations
                .stream()
                .filter(it -> it.id.equals(installationId))
                .toList();
        if (installations.size() == 1) return installations.getFirst();
        else if (installations.isEmpty()) return null;
        else throw new ConfigurationException("Several installations with ID " + installationId +
                    " were found. Please manually fix config file in path: " + pathToConfigFile);
    }

    /**
     * Прочитать конфигурацию конкретной инсталляции
     * @param installationId идентификатор инсталляции
     * @return конфигурация инсталляции, null если конфигурации не существует
     * @throws IOException если ошибка при чтении
     * @throws ConfigurationException если в конфигурации ошибка
     */
    public InstallationDto getInstallation(String installationId) throws IOException, ConfigurationException {
        Objects.requireNonNull(installationId, "InstallationId must not be null");
        ConfigDto configDto = getConfig();
        if(configDto == null) return null;
        return getInstallation(installationId, configDto);
    }

    /**
     * Сохранить конфигурацию инсталляции с перезаписью, если существует
     * @param dto конфигурация инсталляции для сохранения
     * @throws IOException если ошибка при записи/чтении
     * @throws ConfigurationException если ошибка в составе конфигурации
     */
    @SuppressWarnings("unused")
    public void saveInstallation(InstallationDto dto) throws IOException, ConfigurationException {
        Objects.requireNonNull(dto, "InstallationDto must not be null");
        ConfigDto configDto = getConfig();
        if (configDto == null) configDto = new ConfigDto();
        InstallationDto existed = getInstallation(dto.id, configDto);
        if (existed != null) {
            existed.id = dto.id;
            existed.scheme = dto.scheme;
            existed.host = dto.host;
            existed.accessKey = dto.accessKey;
            existed.ignoreSSL = dto.ignoreSSL;
        } else configDto.installations.add(dto);
        saveConfig(configDto);
    }

    /**
     * Удалить конфигурацию инсталляции
     * @param installationId идентификатор инсталляции
     * @throws IOException если ошибка при чтении/записи
     */
    @SuppressWarnings("unused")
    public void removeInstallation(String installationId) throws IOException {
        Objects.requireNonNull(installationId, "InstallationId must not be null");
        ConfigDto configDto = getConfig();
        if (configDto == null) return;
        else configDto.installations.removeIf(it -> installationId.equals(it.id));
        saveConfig(configDto);
    }
}
