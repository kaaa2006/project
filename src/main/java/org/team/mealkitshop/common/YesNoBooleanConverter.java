package org.team.mealkitshop.common;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * 전역 Boolean <-> 'Y'/'N' 컨버터 (autoApply)
 * - null -> 'N' 저장
 * - DB 값이 null/빈문자/알 수 없는 값이면 false 취급
 * - 'Y'/'y' 또는 'TRUE'/'1'도 true로 인식 (유연성)
 */
@Converter(autoApply = true)
public class YesNoBooleanConverter implements AttributeConverter<Boolean, String> {

    @Override
    public String convertToDatabaseColumn(Boolean attribute) {
        // null 이면 'N'으로 저장 (원치 않으면 null 반환으로 바꿔도 됨)
        return Boolean.TRUE.equals(attribute) ? "Y" : "N";
    }

    @Override
    public Boolean convertToEntityAttribute(String dbData) {
        if (dbData == null) return Boolean.FALSE;
        String v = dbData.trim();
        // 유연하게 true 인식
        return "Y".equalsIgnoreCase(v) || "TRUE".equalsIgnoreCase(v) || "1".equals(v);
    }
}