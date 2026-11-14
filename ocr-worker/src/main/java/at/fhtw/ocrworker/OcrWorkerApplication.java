package at.fhtw.ocrworker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class OcrWorkerApplication {

    private static final Logger log =
            LoggerFactory.getLogger(OcrWorkerApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(OcrWorkerApplication.class, args);
    }

    @Bean
    ApplicationRunner startupLog() {
        return args -> log.info(
                "Standalone OCR application started"
        );
    }
}