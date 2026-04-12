package ru.kazantsev.nsmp.basic_api_connector.dto;

/** Конфиг конкретной инсталляции */
public class InstallationConfigDto {
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

    public InstallationConfigDto(){}

    public InstallationConfigDto(String id, String scheme, String host, String accessKey, Boolean ignoreSLL) {
        this.id = id;
        this.scheme = scheme;
        this.host = host;
        this.accessKey = accessKey;
        this.ignoreSLL = ignoreSLL;
    }
}
