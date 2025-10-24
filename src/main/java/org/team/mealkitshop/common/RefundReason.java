package org.team.mealkitshop.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RefundReason {

    CHANGE_OF_MIND("단순 변심"),
    DEFECT("상품 불량"),
    WRONG_ITEM("오배송"),
    DELAYED("배송 지연"),
    ETC("기타");

    private final String description;
}
