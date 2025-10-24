package org.team.mealkitshop.service.item;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.team.mealkitshop.domain.item.Review;
import org.team.mealkitshop.domain.item.ReviewImage;
import org.team.mealkitshop.dto.item.ReviewImageDTO;
import org.team.mealkitshop.repository.item.ReviewImageRepository;
import org.team.mealkitshop.repository.item.ReviewRepository;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
public class ReviewImageService {

    @Value("${imageBasePath:/images/}")
    private String imageBasePath; // 업로드된 파일 접근 URL prefix

    private final FileService fileService;               // 실제 파일 입출력 담당
    private final ReviewImageRepository reviewImageRepo; // 리뷰 이미지 DB 저장소
    private final ReviewRepository reviewRepo;           // 리뷰 참조용

    private static final int MAX_IMAGES_PER_REVIEW = 3;
    private static final long MAX_BYTES_PER_IMAGE = 3L * 1024 * 1024;

    private static final String SUBDIR = "review";       //리뷰 전용 하위 폴더

    /* ==================== CREATE ==================== */

    public void addImages(Long reviewId, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) return;

        // 기존 개수 + 이번 업로드 유효 파일 수 합산 제한
        long existing = reviewImageRepo.countByReview_Id(reviewId);
        long incoming = files.stream().filter(f -> f != null && !f.isEmpty()).count();
        if (existing + incoming > MAX_IMAGES_PER_REVIEW) {
            throw new IllegalArgumentException("이미지는 리뷰당 최대 " + MAX_IMAGES_PER_REVIEW + "장입니다.");
        }

        // 용량/타입/디코딩 등 단일 파일 유효성
        validateImages(files); // ★ 내부에서 strictDecodeCheck 수행

        // 리뷰 존재 검증
        if (!reviewRepo.existsById(reviewId)) {
            throw new IllegalArgumentException("리뷰를 찾을 수 없습니다. id=" + reviewId);
        }
        Review reviewRef = reviewRepo.getReferenceById(reviewId);

        List<ReviewImage> batch = new ArrayList<>();
        List<String> uploadedNames = new ArrayList<>();
        registerRollbackCleanup(uploadedNames); // 롤백 시 실제 파일 삭제

        for (MultipartFile f : files) {
            if (f == null || f.isEmpty()) continue;

            String ori = Objects.requireNonNull(f.getOriginalFilename(), "original filename is null").trim();
            try (InputStream in = f.getInputStream()) {
                // ★ 서브디렉터리에 저장
                String saved = fileService.uploadFileIn(SUBDIR, ori, in);

                // 롤백/커밋 삭제 대비: 서브디렉터리 포함 경로로 기록
                uploadedNames.add(SUBDIR + "/" + saved); // ★ 변경

                ReviewImage e = new ReviewImage();
                e.setReview(reviewRef);
                e.setOriImgName(ori);
                e.setImgName(saved);                      // DB에는 순수 파일명
                e.setImgUrl(buildPublicUrl(saved));       // /images/review/{saved}
                batch.add(e);
            } catch (Exception ex) {
                throw new RuntimeException(ex);
            }
        }
        if (!batch.isEmpty()) reviewImageRepo.saveAll(batch);

        // ★ 경쟁 상황 재검증: 최종 개수 초과 시 롤백 유도
        long total = reviewImageRepo.countByReview_Id(reviewId);
        if (total > MAX_IMAGES_PER_REVIEW) {
            throw new IllegalStateException("이미지는 리뷰당 최대 " + MAX_IMAGES_PER_REVIEW + "장입니다.");
        }
    }

    public void replaceImages(Long reviewId, List<MultipartFile> files) {
        deleteByReview(reviewId);
        addImages(reviewId, files);
    }

    /* ==================== READ ==================== */

    @Transactional(readOnly = true)
    public List<ReviewImageDTO> listByReview(Long reviewId) {
        return reviewImageRepo.findByReview_IdOrderByIdAsc(reviewId).stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReviewImageDTO> listByItem(Long itemId) {
        return reviewImageRepo.findByReview_Item_Id(itemId).stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<Long, List<ReviewImageDTO>> listByReviewIdsGrouped(List<Long> reviewIds) {
        if (reviewIds == null || reviewIds.isEmpty()) return Collections.emptyMap();

        var all = reviewImageRepo.findByReview_IdInOrderByIdAsc(reviewIds);
        Map<Long, List<ReviewImageDTO>> map = new HashMap<>();
        for (ReviewImage ri : all) {
            Long rid = ri.getReview().getId();
            map.computeIfAbsent(rid, k -> new ArrayList<>()).add(toDTO(ri));
        }
        return map;
    }

    /* ==================== DELETE ==================== */

    public void deleteImage(Long imageId, Long reviewId) {
        var img = reviewImageRepo.findById(imageId)
                .orElseThrow(() -> new NoSuchElementException("ReviewImage not found: " + imageId));
        if (!img.getReview().getId().equals(reviewId)) {
            throw new IllegalArgumentException("이미지와 리뷰가 일치하지 않습니다.");
        }
        reviewImageRepo.delete(img);
        // ★ 서브디렉터리 포함하여 커밋 후 물리파일 삭제
        registerAfterCommitDelete(List.of(SUBDIR + "/" + img.getImgName()));
    }

    public void deleteByReview(Long reviewId) {
        var imgs = reviewImageRepo.findByReview_Id(reviewId);
        if (imgs.isEmpty()) {
            reviewImageRepo.deleteByReview_Id(reviewId);
            return;
        }
        List<String> names = imgs.stream().map(ReviewImage::getImgName).toList();

        // 메타 먼저 제거 (롤백되면 파일 삭제도 하지 않음)
        reviewImageRepo.deleteByReview_Id(reviewId);

        // 커밋 후 파일 삭제 (서브디렉터리 포함)
        registerAfterCommitDelete(names.stream().map(n -> SUBDIR + "/" + n).toList());
    }

    /* ==================== HELPER ==================== */

    // ★ 실제 디코딩으로 “진짜 이미지” 검증
    private void strictDecodeCheck(MultipartFile f) {
        try (InputStream in = f.getInputStream()) {
            BufferedImage img = ImageIO.read(in);
            if (img == null) throw new IllegalArgumentException("정상적인 이미지가 아닙니다.");
            int w = img.getWidth(), h = img.getHeight();
            if (w <= 0 || h <= 0) throw new IllegalArgumentException("잘못된 이미지 치수입니다.");
            if (w > 10000 || h > 10000) throw new IllegalArgumentException("이미지 크기가 과도합니다.");
        } catch (IOException e) {
            throw new IllegalArgumentException("이미지 판독 실패", e);
        }
    }

    private String buildPublicUrl(String savedName) {   // ★ SUBDIR 적용
        final String base = imageBasePath.endsWith("/") ? imageBasePath : imageBasePath + "/";
        return base + SUBDIR + "/" + savedName;
    }

    private void validateImages(List<MultipartFile> files) {
        if (files == null) return;

        if (files.size() > MAX_IMAGES_PER_REVIEW) {
            // 업로드 묶음 자체가 과도한 경우 조기 차단(총합 제한은 addImages에서 별도 수행)
            throw new IllegalArgumentException("이미지는 최대 " + MAX_IMAGES_PER_REVIEW + "장까지 업로드 가능합니다.");
        }

        for (MultipartFile f : files) {
            if (f == null || f.isEmpty()) continue;

            if (f.getSize() > MAX_BYTES_PER_IMAGE) {
                throw new IllegalArgumentException("이미지 용량 초과(" + (MAX_BYTES_PER_IMAGE / (1024 * 1024)) + "MB)");
            }

            String ct = Optional.ofNullable(f.getContentType()).orElse("");
            if (!ct.startsWith("image/")) {
                throw new IllegalArgumentException("이미지 파일만 업로드 가능합니다.");
            }

            // 확장자 화이트리스트
            String name = Optional.ofNullable(f.getOriginalFilename()).orElse("").toLowerCase();
            if (!(name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png")
                    || name.endsWith(".gif"))) {
                throw new IllegalArgumentException("허용되지 않는 이미지 형식입니다. (jpg, jpeg, png, gif)");
            }

            strictDecodeCheck(f); // ★ 추가: 실제 디코딩 검증
        }
    }

    /** 트랜잭션 롤백 시 업로드했던 파일 정리 */
    private void registerRollbackCleanup(List<String> uploadedNames) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) return;
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    for (String fn : uploadedNames) {
                        try { fileService.deleteBySavedName(fn); } catch (Exception ignore) {}
                    }
                }
            }
        });
    }

    /** 커밋 이후 실제 파일 삭제 */
    private void registerAfterCommitDelete(List<String> names) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // 트랜잭션 외부라면 즉시 시도
            for (String fn : names) { try { fileService.deleteBySavedName(fn); } catch (Exception ignore) {} }
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                for (String fn : names) {
                    try { fileService.deleteBySavedName(fn); } catch (Exception ignore) {}
                }
            }
        });
    }

    private ReviewImageDTO toDTO(ReviewImage e) {
        return ReviewImageDTO.builder()
                .id(e.getId())
                .imgName(e.getImgName())
                .oriImgName(e.getOriImgName())
                .imgUrl(e.getImgUrl())
                .build();
    }
}
