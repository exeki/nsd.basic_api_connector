package ru.kazantsev.nsd.basic_api_connector;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Перечень DTO, которые фиксированно возвращаются из штатных методов REST API NSD
 */
public class NsdDto {

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
}