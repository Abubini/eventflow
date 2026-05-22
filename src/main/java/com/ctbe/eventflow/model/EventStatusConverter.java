package com.ctbe.eventflow.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converts EventStatus enum to String for database storage.
 * For PostgreSQL, enum values are stored as strings and cast to the enum type.
 */
@Converter(autoApply = true)
public class EventStatusConverter implements AttributeConverter<EventStatus, String> {
    
    @Override
    public String convertToDatabaseColumn(EventStatus attribute) {
        return attribute == null ? null : attribute.name();
    }
    
    @Override
    public EventStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : EventStatus.valueOf(dbData);
    }
}