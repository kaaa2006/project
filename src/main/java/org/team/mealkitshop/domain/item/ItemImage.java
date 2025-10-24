package org.team.mealkitshop.domain.item;

import jakarta.persistence.*;
import lombok.*;
import org.team.mealkitshop.common.BaseEntity;
import org.team.mealkitshop.common.YesNoBooleanConverter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "item")
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Entity
@Table(name = "item_img", indexes = { @Index(name = "idx_itemimg_item", columnList = "item_id") })
public class ItemImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_img_id")
    @EqualsAndHashCode.Include
    private Long id;

    /** 저장 파일명 (서버에 저장되는 이름) */
    @Column(nullable = false)
    private String imgName;

    /** 원본 파일명 (업로드 당시 이름) */
    @Column(nullable = false)
    private String oriImgName;

    /** 공개 경로 (/images/...) */
    @Column(nullable = false, length = 500)
    private String imgUrl;

    /**
     * 대표 이미지 여부 (Y/N 컨버터)
     * - DB: CHAR(1) 'Y'/'N'
     * - Java: Boolean (null 허용) → 저장 직전에 false로 보정
     */
    @Column(name = "repimg_yn", length = 1)
    @Convert(converter = YesNoBooleanConverter.class)
    @Setter(AccessLevel.NONE)     // ✅ 외부에서 직접 set 금지 (무결성 보호)
    private Boolean repimgYn;

    /*본문영역 이미지 여부*/
    @Column(nullable = false)
    private boolean detail = false; // false=PRODUCT(기본), true=DETAIL

    public void markDetail() { this.detail = true; }
    public boolean isDetail() { return detail; }


    /** 소속 상품 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "item_id", nullable = false)
    private Item item;

    /** 파일명/경로 갱신 편의 메서드 */
    public void updateItemImg(String oriImgName, String imgName, String imgUrl) {
        this.oriImgName = oriImgName;
        this.imgName = imgName;
        this.imgUrl = imgUrl;
    }

    /** 양방향 연결용 (필요 시 public 유지) */
    void setItem(Item item) { this.item = item; }

    /** ✅ Item에서만 사용하는 내부 전용 토글 */
    void markRep()  { this.repimgYn = Boolean.TRUE;  }
    void clearRep() { this.repimgYn = Boolean.FALSE; }

    /** ✅ boolean 형태의 읽기 전용 헬퍼 */
    public boolean isRep() { return Boolean.TRUE.equals(this.repimgYn); }

    /** 저장 직전 null 방지 보정(기본 false) */
    @PrePersist
    private void prePersist() {
        if (this.repimgYn == null) {
            this.repimgYn = Boolean.FALSE;
        }
    }
}
