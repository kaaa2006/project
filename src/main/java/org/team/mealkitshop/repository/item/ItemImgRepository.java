// src/main/java/org/team/mealkitshop/repository/item/ItemImgRepository.java
package org.team.mealkitshop.repository.item;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.team.mealkitshop.domain.item.ItemImage;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface ItemImgRepository extends JpaRepository<ItemImage, Long> {

    /* ========= 기본 조회 ========= */

    List<ItemImage> findByItemIdOrderByIdAsc(Long itemId);
    List<ItemImage> findByItemIdAndDetailTrueOrderByIdAsc(Long itemId);
    List<ItemImage> findByItemIdAndDetailFalseOrderByIdAsc(Long itemId);

    boolean existsByItemIdAndRepimgYnTrueAndDetailFalse(Long itemId);
    List<ItemImage> findByItem_IdInAndRepimgYnTrueAndDetailFalse(List<Long> itemIds);

    Optional<ItemImage> findTopByItemIdAndRepimgYnTrueOrderByIdAsc(Long itemId);
    boolean existsByItemIdAndRepimgYnTrue(Long itemId);

    /* ========= 배치 조회 ========= */

    List<ItemImage> findByItem_IdInAndRepimgYnTrue(Collection<Long> itemIds);
    List<ItemImage> findByItem_IdInOrderByIdAsc(Collection<Long> itemIds);

    /* ========= 대표 관리 ========= */

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ItemImage i set i.repimgYn = false where i.item.id = :itemId and i.repimgYn = true")
    int clearRep(@Param("itemId") Long itemId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update ItemImage i set i.repimgYn = true where i.id = :imageId")
    int setRep(@Param("imageId") Long imageId);

    /* ========= 삭제 ========= */

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("delete from ItemImage img where img.item.id = :itemId")
    int deleteByItem_Id(@Param("itemId") Long itemId);

    /* ========= 커스텀 JPQL ========= */

    /** 단건 대표 URL (정책: 갤러리 detail=false) */
    @Query("""
        select ii.imgUrl
          from ItemImage ii
         where ii.item.id = :itemId
           and ii.repimgYn = true
           and ii.detail = false
         order by ii.id asc
    """)
    Optional<String> findRepImgUrlByItemId(@Param("itemId") Long itemId);

    /** 배치: 목록용 대표 이미지 URL/ID (정책: 갤러리 detail=false) */
    interface RepOfItem {
        Long getItemId();
        Long getImageId();
        String getImgUrl();
    }

    @Query("""
       select img.item.id as itemId,
              img.id       as imageId,
              img.imgUrl   as imgUrl
         from ItemImage img
        where img.item.id in :itemIds
          and img.repimgYn = true
          and img.detail = false
    """)
    List<RepOfItem> findRepByItemIds(@Param("itemIds") Collection<Long> itemIds);

    /** 상세 페이지: 대표 우선 정렬 후 등록순 */
    @Query("""
       select img
         from ItemImage img
        where img.item.id = :itemId
        order by case when img.repimgYn = true then 0 else 1 end,
                 img.id asc
    """)
    List<ItemImage> findAllForDetail(@Param("itemId") Long itemId);

    /** 배치: 대표 URL만 (정책: 갤러리 detail=false) */
    interface ItemRepProjection {
        Long getItemId();
        String getImgUrl();
    }

    @Query("""
        select ii.item.id as itemId, ii.imgUrl as imgUrl
          from ItemImage ii
         where ii.repimgYn = true
           and ii.detail = false
           and ii.item.id in :itemIds
    """)
    List<ItemRepProjection> findRepUrlsByItemIdIn(@Param("itemIds") Collection<Long> itemIds);
}
