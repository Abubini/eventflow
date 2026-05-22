package com.ctbe.eventflow.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Converts RegStatus enum to String for database storage.
 * For PostgreSQL, enum values are stored as strings and cast to the enum type.
 */
@Converter(autoApply = true)
public class RegStatusConverter implements AttributeConverter<RegStatus, String> {
    
    @Override
    public String convertToDatabaseColumn(RegStatus attribute) {
        return attribute == null ? null : attribute.name();
    }
    
    @Override
    public RegStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : RegStatus.valueOf(dbData);
    }
}