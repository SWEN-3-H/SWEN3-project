package at.fhtw.ocrworker.storage;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
public class OcrResultStorageService {

    private static final Logger log =
            LoggerFactory.getLogger(OcrResultStorageService.class);

    private final MinioClient minioClient;
    private final String bucket;

    public OcrResultStorageService(
            MinioClient minioClient,
            @Value("${app.minio.bucket}") String bucket) {

        this.minioClient = minioClient;
        this.bucket = bucket;
    }

    public String store(Long documentId, String text) {
        String objectKey = "ocr-results/" + documentId + ".txt";
        byte[] data = text.getBytes(StandardCharsets.UTF_8);

        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .data(data, data.length)
                            .contentType("text/plain; charset=utf-8")
                            .build()
            );
        } catch (Exception e) {
            throw new StorageException(
                    "Could not store OCR result for document " + documentId,
                    e
            );
        }

        log.info(
                "OCR result stored: documentId={}, objectKey={}, bytes={}",
                documentId,
                objectKey,
                data.length
        );

        return objectKey;
    }

    public String load(String objectKey) {
        try (InputStream input = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectKey)
                        .build()
        )) {
            String text = new String(input.readAllBytes(), StandardCharsets.UTF_8);

            log.info(
                    "OCR result loaded: objectKey={}, characters={}",
                    objectKey,
                    text.length()
            );

            return text;
        } catch (Exception e) {
            throw new StorageException(
                    "Could not load OCR result: objectKey=" + objectKey,
                    e
            );
        }
    }
}