package at.fhtw.documentmanager.service;

import at.fhtw.documentmanager.model.Document;
import at.fhtw.documentmanager.model.Label;
import at.fhtw.documentmanager.repository.DocumentRepository;
import at.fhtw.documentmanager.repository.LabelRepository;
import at.fhtw.documentmanager.service.ResourceNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.DocumentType;

import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final LabelRepository labelRepository;

    public Document upload(MultipartFile file) {
        validateUpload(file);
        log.info("Uploading document: filename={}, size={} bytes",
                file.getOriginalFilename(), file.getSize());
        Document document = new Document();
        document.setFilename(file.getOriginalFilename());
        Document saved = documentRepository.save(document);
        log.info("Document with id={} saved successfully", saved.getId());
        return saved;
    }

    public List<Document> findAll() {
        List<Document> documents = documentRepository.findAll();
        log.debug("Found {} documents", documents.size());
        return documents;
    }

    public Document findById(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Document with id={} not found", id);
                    return new ResourceNotFoundException("Document", id);
                });
    }

    public Document updateFilename(Long id, String newFilename) {
        Document document = findById(id);
        String oldFilename =  document.getFilename();
        document.setFilename(newFilename);
        Document saved = documentRepository.save(document);
        log.info("Document renamed: id={}, old Name: '{}', new: '{}'", id, oldFilename, newFilename );
        return saved;
    }

    public void delete(Long id) {
        if (!documentRepository.existsById(id)) {
            log.warn("Document with id={} not found", id);
            throw new ResourceNotFoundException("Document", id);
        }
        documentRepository.deleteById(id);
        log.info("Document with id={} deleted successfully", id);
    }

    private void validateUpload(MultipartFile file) {
        String filename = file.getOriginalFilename();

        if (filename == null || filename.isBlank()) {
            throw new InvalidDocumentException("Filename must not be blank");
        }

        if (filename.length() > 255) {
            throw new InvalidDocumentException(
                    "Filename must not exceed 255 characters"
            );
        }

        if (!filename.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new InvalidDocumentException("Only PDF files are allowed");
        }

        String contentType = file.getContentType();

        if (contentType != null
                && !contentType.isBlank()
                && !"application/pdf".equalsIgnoreCase(contentType)
                && !"application/octet-stream".equalsIgnoreCase(contentType)) {
            throw new InvalidDocumentException("Only PDF files are allowed");
        }
    }

    @Transactional
    public Document addLabel(Long documentId, Long labelId) {
        Document document = findById(documentId);
        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> new ResourceNotFoundException("Label", labelId));
        document.getLabels().add(label);
        return document;
    }

    @Transactional
    public Document removeLabel(Long documentId, Long labelId) {
        Document document = findById(documentId);
        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> new ResourceNotFoundException("Label", labelId));
        document.getLabels().remove(label);
        return document;
    }
}