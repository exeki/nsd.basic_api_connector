package ru.kazantsev.nsd.basic_api_connector;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Перечень DTO, которые фиксированно возвращаются из штатных методов REST API NSD
 */
@SuppressWarnings("unused")
public class NsdDto {

    @SuppressWarnings("unused")
    static abstract class AbstractNsdDto {
        @Override
        public String toString() {
            try {
                return new ObjectMapper()
                        .setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"))
                        .writeValueAsString(this);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Исключение в классе обслуживания
     */
    @SuppressWarnings("unused")
    public static class ServiceTimeExclusionDto extends AbstractNsdDto {
        @JsonAlias("UUID")
        public String uuid;
        public Long startTime;
        public Long endTime;
        @JsonFormat(pattern = "yyyy-MM-dd")
        public Date exclusionDate;
    }

    /**
     * Файл
     */
    @SuppressWarnings("unused")
    public static class FileDto extends AbstractNsdDto {
        public byte[] bytes;
        public String title;
        public String contentType;

        public FileDto(byte[] bytes, String title, String contentType) {
            this.bytes = bytes;
            this.title = title;
            this.contentType = contentType;
        }

        @Override
        public String toString() {
            return this.title + " / " + this.contentType;
        }
    }

    /**
     * Ответ при пуше скриптов smpsync
     */
    @SuppressWarnings("unused")
    public static class ScriptChecksums extends AbstractNsdDto {
        public List<SrcChecksum> scripts;
        public List<SrcChecksum> modules;
        public List<ScriptCategory> scriptsCategories;
        public List<HashMap<String, Object>> advimports;
    }

    /**
     * Чексумма исходника
     */
    @SuppressWarnings("unused")
    public static class SrcChecksum {
        public String code;
        public String checksum;
    }

    /**
     * Категория скрипта
     */
    @SuppressWarnings("unused")
    public static class ScriptCategory {
        public List<String> list;
        public String code;
    }
}