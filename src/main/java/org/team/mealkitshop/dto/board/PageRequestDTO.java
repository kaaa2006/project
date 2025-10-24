package org.team.mealkitshop.dto.board;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Log4j2
public class PageRequestDTO {
    // 페이징 처리에 요청용(프론트에서 요청이 오면 동작)
    // 페이징에 관련된 정보(page, size), 검색 종류(types -> 제목t, 내용c, 작성자w), 키워드(검색단어) 처리용

    @Builder.Default // 빌더 패턴 시작시 초기값이 들어감
    private int page = 1; // 첫페이지

    @Builder.Default
    private int size = 10; // 게시물 수

    private String type; // t, c, w, t c, t w, t w c ... (다중 검색용)

    private String keyword ;  // 폼박스에 검색 내용

    private String link; // 프론트에 페이징번호 클릭시 처리되는 문자열
    // list?page=3&type=w&keyword=kkw

    public String getLink() {
        StringBuilder builder = new StringBuilder(); // String + 연산자로 사용하면 객체가 많이 생김
        // 이를 해결하기 위한 기법

        builder.append("page=" + this.page); // page=1
        builder.append("&size=" + this.size); // page=1&size=10

        if (type != null && type.length() > 0) {
            // 타입이 있을 때

            builder.append("&type=" + type); // page=1&size=10&type=???

        } // 타입이 있을 때 if문 종료

        if (keyword != null) {

            try {

                builder.append("&keyword=" + URLEncoder.encode(keyword, "UTF-8"));
                // page=1&size=10&type=???&keyword=???

            } catch (UnsupportedEncodingException e) {
                log.info(e.getStackTrace());
                log.info("UTF-8 처리중 오류 발생");
            } // try문 종료
        } // 키워드 if문 종료
        return builder.toString(); // 완성된 쿼리 스트링 반환

    }// 메서드 종료


    // 추가메서드
    public String[] getTypes(){
        // 프론트에서 문자열이 여러개가 넘어오면 배열로 변환
        if(type==null || type.isEmpty()){
            // 넘어온 값이 널이거나 비어 있으면
            return null;
        }
        return type.split(""); // 차후에 프론트에 폼박스 확인하고 조절!!!!
        // 문자열로 넘어온 값일 분할하여 배열에 꼽는다.
    }

    // 테스트용 코드를 dto로 만들어 메서드 처리함!!!
    public Pageable getPageable(String...props){ // String...props 배열이 몇개가 들어올지 모를때
        return PageRequest.of(this.page-1, this.size, Sort.by(props).descending());
        //                    페이지번호     게시물 수    정렬기법
    }

}
