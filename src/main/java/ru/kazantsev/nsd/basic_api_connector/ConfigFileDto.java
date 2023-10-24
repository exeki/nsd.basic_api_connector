package ru.kazantsev.nsd.basic_api_connector;

import java.util.List;

/** DTO со структурой файла конфига коннектора */
public class ConfigFileDto {
    /** Перечень конфигов инталляций */
    public List<InstallationConfig> installations;
    /** Конфиг конкретной инсталляции */
    public static class InstallationConfig {
        /** Идентификатор инсталляции */
        public String id;
        /** Схема, по которой система будет обращаться */
        public String scheme;
        /** Хост инсталляции */
        public String host;
        /** Ключ, по которому будет происходить обращение */
        public String accessKey;
        /** Признак необходимости игнорировать ssl */
        public Boolean ignoreSLL;
    }
}
