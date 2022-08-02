package whu.edu.cs.transitnet;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
public class TransitnetApplication {
    public static void main(String[] args) {
        try {
            SpringApplication.run(TransitnetApplication.class, args);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.printf("Error while start:" + e);
        }
    }


}
