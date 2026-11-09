package com.outreach.planner.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/** Stores a weekday set as ISO day numbers, e.g. Mon/Wed/Fri -> "1,3,5". */
@Converter
public class WeekdaySetConverter implements AttributeConverter<Set<DayOfWeek>, String> {

    @Override
    public String convertToDatabaseColumn(Set<DayOfWeek> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        return attribute.stream().sorted().map(d -> String.valueOf(d.getValue())).collect(Collectors.joining(","));
    }

    @Override
    public Set<DayOfWeek> convertToEntityAttribute(String dbData) {
        Set<DayOfWeek> days = EnumSet.noneOf(DayOfWeek.class);
        if (dbData != null && !dbData.isBlank()) {
            Arrays.stream(dbData.split(",")).map(s -> DayOfWeek.of(Integer.parseInt(s))).forEach(days::add);
        }
        return days;
    }
}
