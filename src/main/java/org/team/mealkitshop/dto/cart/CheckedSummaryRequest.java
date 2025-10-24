package org.team.mealkitshop.dto.cart;

import lombok.Getter;

import java.util.List;

@Getter
public class CheckedSummaryRequest {
    private List<Long> cartItemIds;
    private String zipcode;
}
