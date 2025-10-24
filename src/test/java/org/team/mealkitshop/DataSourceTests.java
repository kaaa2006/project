package org.team.mealkitshop;

import lombok.Cleanup;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@SpringBootTest  // 부트용 테스트임을 선언
@Log4j2         // 콘솔에 log.info("")
public class DataSourceTests {
    // 데이터베이스 연결 테스트용

    @Autowired // 생성자 자동 주입
    private DataSource dataSource;  // new DataSource();
    // DataSource 객체는 application.properties에 있는 데이터베이스 정보를 활용한다.

    @Test //import org.junit.jupiter.api.Test;
    public void testConnection() throws SQLException {

        @Cleanup // 시작시 청소
        Connection con = dataSource.getConnection();

        log.info("데이터베이스 연결 테스트용 객체 : " + con);
        Assertions.assertNotNull(con);  //import org.junit.jupiter.api.Assertions;
    }
}