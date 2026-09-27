package at.fhtw.documentManager.service;

import at.fhtw.documentmanager.model.Document;
import at.fhtw.documentmanager.repository.DocumentRepository;
import at.fhtw.documentManager.service.DocumentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
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

    private DocumentService documentService;

    @BeforeEach
    void setup(){
        documentService = new DocumentService(documentRepository);
    }

    @Test
    void uploadFile() {
        MultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", "content".getBytes());

        when(documentRepository.save(any(Document.class))).thenAnswer(i -> i.getArgument(0));
        Document result = documentService.upload(file);

        assertThat(result.getFilename()).isEqualTo("test.pdf");
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
                .isInstanceOf(at.fhtw.documentmanager.service.DocumentNotFoundException.class);

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
        when(documentRepository.existsById(1L)).thenReturn(true);

        documentService.delete(1L);

        verify(documentRepository).deleteById(1L);
    }

    @Test
    void deleteDocumentFail() {
        when(documentRepository.existsById(5L)).thenReturn(false);

        assertThatThrownBy(() -> documentService.delete(5L))
                .isInstanceOf(at.fhtw.documentmanager.service.DocumentNotFoundException.class);

        verify(documentRepository, never()).deleteById(5L);
    }



    private Document documentWithId(Long id, String filename) {
        Document document = new Document();
        document.setId(id);
        document.setFilename(filename);
        return document;
    }
}