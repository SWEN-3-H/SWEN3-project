package at.fhtw.documentmanager.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class MinioConfig {

    @Bean
    public MinioClient minioClient(
            @Value("${app.minio.endpoint}") String endpoint,
            @Value("${app.minio.access-key}") String accessKey,
            @Value("${app.minio.secret-key}") String secretKey,
            @Value("${app.minio.bucket}") String bucket) {

        MinioClient client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();

        try {
            boolean exists = client.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucket)
                            .build()
            );

            if (!exists) {
                client.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucket)
                                .build()
                );

                log.info("MinIO bucket '{}' created", bucket);
            }

            log.info("MinIO ready: endpoint={}, bucket={}", endpoint, bucket);
            return client;

        } catch (Exception e) {
            throw new IllegalStateException(
                    "Could not initialize MinIO bucket '" + bucket + "'",
                    e
            );
        }
    }
}