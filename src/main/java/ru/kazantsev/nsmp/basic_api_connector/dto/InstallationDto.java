package ru.kazantsev.nsmp.basic_api_connector.dto;

/** Конфиг конкретной инсталляции */
public class InstallationDto {
    /** Идентификатор инсталляции */
    public String id;
    /** Схема, по которой система будет обращаться */
    public String scheme;
    /** Хост инсталляции */
    public String host;
    /** Ключ, по которому будет происходить обращение */
    public String accessKey;
    /** Признак необходимости игнорировать ssl */
    public Boolean ignoreSSL;

    @SuppressWarnings("unused")
    public InstallationDto(){}

    @SuppressWarnings("unused")
    public InstallationDto(String id, String scheme, String host, String accessKey, Boolean ignoreSSL) {
        this.id = id;
        this.scheme = scheme;
        this.host = host;
        this.accessKey = accessKey;
        this.ignoreSSL = ignoreSSL;
    }
}
