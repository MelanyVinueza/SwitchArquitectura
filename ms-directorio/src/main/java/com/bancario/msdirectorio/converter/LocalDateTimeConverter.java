package com.bancario.msdirectorio.converter;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTypeConverter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LocalDateTimeConverter implements DynamoDBTypeConverter<String, LocalDateTime> {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    @Override
    public String convert(LocalDateTime object) {
        return object != null ? object.format(FORMATTER) : null;
    }

    @Override
    public LocalDateTime unconvert(String object) {
        return object != null && !object.isEmpty() ? LocalDateTime.parse(object, FORMATTER) : null;
    }
}
