package org.team.mealkitshop.service.board;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BoardImageService {

    // 이미지 저장 후 저장된 파일명 리스트 반환
    List<String> saveImages(Long bno, List<MultipartFile> files);

    // 게시글 이미지 삭제
    void deleteFile(String fileName);

    // 게시글 전체 이미지 삭제
    void removeImagesByBoard(Long bno);
}