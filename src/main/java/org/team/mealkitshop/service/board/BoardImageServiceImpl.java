package org.team.mealkitshop.service.board;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.team.mealkitshop.domain.board.Board;
import org.team.mealkitshop.domain.board.BoardImage;
import org.team.mealkitshop.repository.board.BoardImageRepository;
import org.team.mealkitshop.repository.board.BoardRepository;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Log4j2
public class BoardImageServiceImpl implements BoardImageService {

    private final BoardImageRepository boardImageRepository;
    private final BoardRepository boardRepository;

    @Value("${uploadPath}")
    private String uploadDir;

    @Override
    public List<String> saveImages(Long bno, List<MultipartFile> files) {
        Board board = boardRepository.findById(bno)
                .orElseThrow(() -> new RuntimeException("게시글이 존재하지 않습니다."));

        List<String> savedFilePaths = new ArrayList<>();

        String folderPath = ""; // 날짜 경로 제거
        java.nio.file.Path uploadPath = java.nio.file.Paths.get(uploadDir); // 루트만 사용
        try {
            if (!java.nio.file.Files.exists(uploadPath)) {
                java.nio.file.Files.createDirectories(uploadPath);
            }
        } catch (Exception e) {
            log.error("폴더 생성 실패", e);
        }

        int ord = 1;
        for (MultipartFile file : files) {
            try {
                if (file.isEmpty()) continue;

                String uuid = java.util.UUID.randomUUID().toString();

                String originalName = file.getOriginalFilename();

                // 파일명 앞/뒤 불필요한 슬래시 제거
                originalName = originalName.replaceAll("^/+", "").replaceAll("/+$", "");

                // 파일 저장 후 DB에는 파일명만 저장
                String storedFileName = uuid + "_" + originalName;

                Path filePath = uploadPath.resolve(storedFileName);
                file.transferTo(filePath.toFile());

// DB 저장 (fileUrl 대신 fileName만 넣음)
                BoardImage image = BoardImage.builder()
                        .uuid(uuid)
                        .fileName(storedFileName)   // DB에는 파일명만 저장
                        .oriFileName(originalName)
                        .contentType(file.getContentType())
                        .folderPath("")             // 필요 시 날짜 폴더 관리 가능
                        .repImgYn(ord == 1 ? "Y" : "N")
                        .ord(ord++)
                        .size(file.getSize())
                        .board(board)
                        .build();

                boardImageRepository.save(image);

                savedFilePaths.add(storedFileName); // 파일명만 반환

            } catch (Exception e) {
                log.error("파일 저장 실패: " + file.getOriginalFilename(), e);
            }
        }

        return savedFilePaths;
    }

    @Override
    public void deleteFile(String fileName) {
        // DB 삭제
        BoardImage image = boardImageRepository.findByFileName(fileName)
                .orElseThrow(() -> new RuntimeException("이미지가 존재하지 않습니다."));
        boardImageRepository.delete(image);

        // 실제 파일 삭제 (upload 경로 기준)
        java.nio.file.Path path = java.nio.file.Paths.get(uploadDir, fileName); // 실제 경로로 변경
        try {
            java.nio.file.Files.deleteIfExists(path);
        } catch (Exception e) {
            log.error("파일 삭제 실패: " + fileName, e);
        }
    }

    @Override
    public void removeImagesByBoard(Long bno) {
        boardImageRepository.deleteByBoard_Bno(bno);
    }
}