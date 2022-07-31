package whu.edu.cs.transitnet;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@SpringBootApplication
@MapperScan("whu.edu.cs.transitnet.*")

public class TransitnetApplication {
    public static void main(String[] args) {
        try {
            System.setProperty("spring.devtools.restart.enabled", "false");
            SpringApplication.run(TransitnetApplication.class, args);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.printf("Error while start:" + e);
        }
    }


}
