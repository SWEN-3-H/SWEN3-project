package at.fhtw.documentmanager.storage;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
public class DocumentStorageService {

    private final MinioClient minioClient;
    private final String bucket;

    public DocumentStorageService(
            MinioClient minioClient,
            @Value("${app.minio.bucket}") String bucket) {

        this.minioClient = minioClient;
        this.bucket = bucket;
    }

    public String store(MultipartFile file) {
        String objectKey = "documents/" + UUID.randomUUID() + ".pdf";

        try (InputStream stream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .stream(stream, file.getSize(), -1L)
                            .contentType("application/pdf")
                            .build()
            );
        } catch (Exception e) {
            throw new StorageException(
                    "Could not store document in MinIO",
                    e
            );
        }

        log.info("PDF stored: bucket={}, objectKey={}", bucket, objectKey);
        return objectKey;
    }

    public void delete(String objectKey) {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build()
            );
        } catch (Exception e) {
            throw new StorageException(
                    "Could not delete document from MinIO",
                    e
            );
        }

        log.info("PDF removed: bucket={}, objectKey={}", bucket, objectKey);
    }
}