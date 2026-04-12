package ru.kazantsev.nsmp.basic_api_connector.dto.nsmp;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;


/**
 * Исключение в классе обслуживания
 */
@SuppressWarnings("unused")
public class ServiceTimeExclusionDto   {
    @JsonAlias("UUID")
    public String uuid;
    public Long startTime;
    public Long endTime;
    @JsonFormat(pattern = "yyyy-MM-dd")
    public Date exclusionDate;
}