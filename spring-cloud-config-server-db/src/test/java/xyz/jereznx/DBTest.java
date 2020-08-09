package xyz.jereznx;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@SpringBootTest
public class DBTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void test() throws SQLException {
        final Connection connection = dataSource.getConnection();
        final Statement statement = connection.createStatement();
        final ResultSet resultSet = statement.executeQuery("select * from SYS_CONFIG");
        while (resultSet.next()) {
            System.out.println(resultSet.getString(1) + resultSet.getString(2));
        }
    }

}
