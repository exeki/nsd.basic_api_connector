package ru.kazantsev.nsmp.basic_api_connector.dto;

import java.util.List;

/** DTO со структурой файла конфига коннектора */
public class ConfigFileDto {
    /** Перечень конфигов инталляций */
    public List<InstallationConfigDto> installations;

    @SuppressWarnings("unused")
    public ConfigFileDto() {}

    @SuppressWarnings("unused")
    public ConfigFileDto(List<InstallationConfigDto> installations) {
        this.installations = installations;
    }
}
