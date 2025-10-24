package org.team.mealkitshop.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;
import org.team.mealkitshop.common.Category;

/**
 * QueryString → Category 변환기
 *
 * - 스프링 MVC가 요청 파라미터/쿼리스트링을 바인딩할 때 사용됨.
 * - 예: ?category=REFRIGERATED  → Category.REFRIGERATED
 *      ?category=냉장           → Category.REFRIGERATED
 * - enum의 name() 또는 label() 둘 다 지원.
 */
@Component
public class StringToCategoryConverter implements Converter<String, Category> {

    @Override
    public Category convert(String source) {
        if (source == null) return null;
        String s = source.trim();
        if (s.isEmpty()) return null;

        for (Category c : Category.values()) {
            if (c.name().equalsIgnoreCase(s) || c.getLabel().equals(s)) {
                return c;
            }
        }
        // 매칭 실패 시 null 반환 → 검색 조건 미적용
        return null;
    }
}
