package ru.ekazantsev.nsd_basic_api_connector;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.text.SimpleDateFormat;
import java.util.Date;

public class NsdDto {

    static abstract class AbstractNsdDto {
        @Override
        public String toString() {
            return ConnectorUtilities.writeValueAsString(
                    new ObjectMapper().setDateFormat(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")),
                    this
            );
        }
    }

    public static class ServiceTimeExclusionDto extends AbstractNsdDto {
        @JsonAlias("UUID")
        public String uuid;
        public Long startTime;
        public Long endTime;
        @JsonFormat(pattern = "yyyy-MM-dd")
        public Date exclusionDate;
    }

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