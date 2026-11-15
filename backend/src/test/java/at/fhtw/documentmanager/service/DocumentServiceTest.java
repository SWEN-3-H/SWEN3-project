package at.fhtw.documentmanager.service;

import at.fhtw.documentmanager.messaging.DocumentUploadedMessage;
import at.fhtw.documentmanager.messaging.OcrMessageProducer;
import at.fhtw.documentmanager.model.Document;
import at.fhtw.documentmanager.repository.DocumentRepository;
import at.fhtw.documentmanager.repository.LabelRepository;
import at.fhtw.documentmanager.storage.DocumentStorageService;
import at.fhtw.documentmanager.storage.StorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private LabelRepository labelRepository;

    @Mock
    private OcrMessageProducer ocrMessageProducer;

    @Mock
    private DocumentStorageService documentStorageService;

    private DocumentService documentService;

    @BeforeEach
    void setup() {
        documentService = new DocumentService(
                documentRepository,
                labelRepository,
                ocrMessageProducer,
                documentStorageService
        );
    }

    @Test
    void uploadFile() {
        MultipartFile file = new MockMultipartFile(
                "file",
                "test.pdf",
                "application/pdf",
                "content".getBytes()
        );

        String objectKey = "documents/test-key.pdf";

        when(documentStorageService.store(file)).thenReturn(objectKey);

        when(documentRepository.save(any(Document.class)))
                .thenAnswer(invocation -> {
                    Document document = invocation.getArgument(0);
                    document.setId(1L);
                    return document;
                });

        Document result = documentService.upload(file);

        assertThat(result.getFilename()).isEqualTo("test.pdf");
        assertThat(result.getObjectKey()).isEqualTo(objectKey);

        var order = inOrder(
                documentStorageService,
                documentRepository,
                ocrMessageProducer
        );

        order.verify(documentStorageService).store(file);
        order.verify(documentRepository).save(any(Document.class));
        order.verify(ocrMessageProducer).sendDocumentUploaded(
                new DocumentUploadedMessage(
                        1L,
                        "test.pdf",
                        objectKey
                )
        );
    }

    @Test
    void getDocumentById() {
        Document document = documentWithId(1L, "test.pdf");

        when(documentRepository.findById(1L)).thenReturn(Optional.of(document));

        Document result = documentService.findById(1L);
        assertThat(result).isEqualTo(document);
    }

    @Test
    void getDocumentByIdNotFound() {
        when(documentRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.findById(1L))
                .isInstanceOf(ResourceNotFoundException.class);

    }

    @Test
    void findAllDocuments() {
        Document documentA = documentWithId(1L, "testA.pdf");
        Document documentB = documentWithId(2L, "testB.pdf");
        Document documentC = documentWithId(3L, "testC.pdf");

        when(documentRepository.findAll()).thenReturn(List.of(documentA,documentB,documentC));

        assertThat(documentService.findAll()).containsExactly(documentA,documentB,documentC);
    }

    @Test
    void updateDocument() {
        Document existing = documentWithId(1L, "test.pdf");

        when(documentRepository.save(any(Document.class))).thenAnswer(i -> i.getArgument(0));
        when(documentRepository.findById(1L)).thenReturn(Optional.of(existing));

        Document result = documentService.updateFilename(1L, "new.pdf");

        assertThat(result.getFilename()).isEqualTo("new.pdf");
        verify(documentRepository).save(existing);
    }

    @Test
    void deleteDocument() {
        Document document = documentWithId(1L, "test.pdf");
        document.setObjectKey("documents/test-key.pdf");

        when(documentRepository.findById(1L))
                .thenReturn(Optional.of(document));

        documentService.delete(1L);

        var order = inOrder(documentStorageService, documentRepository);

        order.verify(documentStorageService)
                .delete("documents/test-key.pdf");

        order.verify(documentStorageService)
                .delete("ocr-results/1.txt");

        order.verify(documentRepository).delete(document);
    }

    @Test
    void uploadWithWrongExtensionFails() {
        MultipartFile file = new MockMultipartFile(
                "file", "test.txt", "application/pdf", "content".getBytes());

        assertThatThrownBy(() -> documentService.upload(file))
                .isInstanceOf(InvalidDocumentException.class);

        verifyNoInteractions(
                documentRepository,
                documentStorageService,
                ocrMessageProducer
        );
    }

    @Test
    void uploadWithTooLongFilenameFails() {
        String filename = "a".repeat(252) + ".pdf";

        MultipartFile file = new MockMultipartFile(
                "file", filename, "application/pdf", "content".getBytes());

        assertThatThrownBy(() -> documentService.upload(file))
                .isInstanceOf(InvalidDocumentException.class);

        verifyNoInteractions(
                documentRepository,
                documentStorageService,
                ocrMessageProducer
        );
    }

    @Test
    void updateMissingDocumentFails() {
        when(documentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.updateFilename(99L, "new.pdf"))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(documentRepository, never()).save(any(Document.class));
    }

    @Test
    void storageFailurePreventsDatabaseSaveAndOcrJob() {
        MultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "content".getBytes()
        );

        when(documentStorageService.store(file))
                .thenThrow(new StorageException(
                        "Storage unavailable",
                        new RuntimeException("Connection failed")
                ));

        assertThatThrownBy(() -> documentService.upload(file))
                .isInstanceOf(StorageException.class);

        verifyNoInteractions(documentRepository, ocrMessageProducer);
    }

    @Test
    void databaseFailureRemovesStoredPdfAndPreventsOcrJob() {
        MultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "content".getBytes()
        );

        String objectKey = "documents/test-key.pdf";

        when(documentStorageService.store(file)).thenReturn(objectKey);

        when(documentRepository.save(any(Document.class)))
                .thenThrow(new DataAccessResourceFailureException(
                        "Database unavailable"
                ));

        assertThatThrownBy(() -> documentService.upload(file))
                .isInstanceOf(DataAccessResourceFailureException.class);

        verify(documentStorageService).delete(objectKey);
        verifyNoInteractions(ocrMessageProducer);
    }

    @Test
    void deleteDocumentWithoutStoredPdf() {
        Document document = documentWithId(1L, "old.pdf");

        when(documentRepository.findById(1L))
                .thenReturn(Optional.of(document));

        documentService.delete(1L);

        verify(documentRepository).delete(document);
        verifyNoInteractions(documentStorageService);
    }

    @Test
    void storageDeleteFailureKeepsDatabaseEntry() {
        Document document = documentWithId(1L, "test.pdf");
        document.setObjectKey("documents/test-key.pdf");

        when(documentRepository.findById(1L))
                .thenReturn(Optional.of(document));

        doThrow(new StorageException(
                "Storage unavailable",
                new RuntimeException("Connection failed")
        )).when(documentStorageService).delete("documents/test-key.pdf");

        assertThatThrownBy(() -> documentService.delete(1L))
                .isInstanceOf(StorageException.class);

        verify(documentRepository, never()).delete(any(Document.class));
    }

    private Document documentWithId(Long id, String filename) {
        Document document = new Document();
        document.setId(id);
        document.setFilename(filename);
        return document;
    }
}