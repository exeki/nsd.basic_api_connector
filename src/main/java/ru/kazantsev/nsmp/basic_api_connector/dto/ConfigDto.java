package ru.kazantsev.nsmp.basic_api_connector.dto;

import java.util.ArrayList;
import java.util.List;

/** DTO со структурой файла конфига коннектора */
public class ConfigDto {
    /** Перечень конфигов инсталляций */
    public List<InstallationDto> installations = new ArrayList<>();

    @SuppressWarnings("unused")
    public ConfigDto() {}

    @SuppressWarnings("unused")
    public ConfigDto(List<InstallationDto> installations) {
        this.installations = installations;
    }
}
