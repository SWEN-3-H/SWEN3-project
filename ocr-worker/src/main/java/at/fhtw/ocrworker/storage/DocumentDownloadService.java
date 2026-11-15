package at.fhtw.ocrworker.storage;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
public class DocumentDownloadService {

    private final MinioClient minioClient;
    private final String bucket;

    public DocumentDownloadService(
            MinioClient minioClient,
            @Value("${app.minio.bucket}") String bucket) {

        this.minioClient = minioClient;
        this.bucket = bucket;
    }

    public Path download(String objectKey) {
        Path temporaryFile = null;

        try {
            temporaryFile = Files.createTempFile("ocr-", ".pdf");

            try (InputStream input = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build()
            )) {
                Files.copy(
                        input,
                        temporaryFile,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }

            return temporaryFile;

        } catch (Exception failure) {
            if (temporaryFile != null) {
                try {
                    Files.deleteIfExists(temporaryFile);
                } catch (Exception cleanupFailure) {
                    failure.addSuppressed(cleanupFailure);
                }
            }

            throw new StorageException(
                    "Could not download PDF: objectKey=" + objectKey,
                    failure
            );
        }
    }
}