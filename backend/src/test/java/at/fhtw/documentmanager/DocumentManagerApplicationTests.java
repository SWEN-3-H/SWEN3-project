package at.fhtw.documentmanager;

import at.fhtw.documentmanager.messaging.OcrMessageProducer;
import at.fhtw.documentmanager.messaging.Recv;
import at.fhtw.documentmanager.messaging.Send;
import com.rabbitmq.client.amqp.Connection;
import com.rabbitmq.client.amqp.Environment;
import io.minio.MinioClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:context-test;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",

        "app.minio.endpoint=http://localhost:9000",
        "app.minio.access-key=test-user",
        "app.minio.secret-key=test-secret",
        "app.minio.bucket=test-documents"
})
@MockitoBean(
        types = {
                Environment.class,
                Connection.class,
                Send.class,
                Recv.class,
                OcrMessageProducer.class,
                MinioClient.class
        },
        enforceOverride = true
)
class DocumentManagerApplicationTests {

    @Test
    void contextLoads() {
    }
}