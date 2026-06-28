package cmc.mody;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class ModyApplication {
    public static void main(String[] args) {
        SpringApplication.run(ModyApplication.class, args);
    }
}
