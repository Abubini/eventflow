package com.ctbe.eventflow.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class RequestStatusConverter implements AttributeConverter<RequestStatus, String> {

    @Override
    public String convertToDatabaseColumn(RequestStatus attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public RequestStatus convertToEntityAttribute(String dbData) {
        return dbData == null ? null : RequestStatus.valueOf(dbData);
    }
}