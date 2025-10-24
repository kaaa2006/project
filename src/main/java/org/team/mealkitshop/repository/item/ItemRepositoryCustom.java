package org.team.mealkitshop.repository.item;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.team.mealkitshop.domain.item.Item;
import org.team.mealkitshop.dto.item.ItemSearchDTO;
import org.team.mealkitshop.dto.item.ListItemDTO;

@Repository
public interface ItemRepositoryCustom {

    /**
     * 관리자 페이지용 상품 조회
     * - 검색 조건(ItemSearchDTO)에 따라 상품 목록을 페이징 처리하여 반환
     * - 반환 타입: Page<Item> (엔티티 그대로)
     */
    Page<ListItemDTO> getAdminItemPage(ItemSearchDTO itemSearchDTO, Pageable pageable);

    /**
     * 메인 페이지용 상품 조회
     * - 검색 조건(ItemSearchDTO)에 따라 메인 화면에 표시할 상품 목록 조회
     * - DTO 프로젝션(MainItemDTO)으로 반환 (불필요한 엔티티 로딩 방지)
     * - 페이징 처리 지원
     */
    Page<ListItemDTO> getListItemPage(ItemSearchDTO itemSearchDTO, Pageable pageable);
}

