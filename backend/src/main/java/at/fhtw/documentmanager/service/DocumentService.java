package at.fhtw.documentmanager.service;

import at.fhtw.documentmanager.model.Document;
import at.fhtw.documentmanager.repository.DocumentRepository;
import at.fhtw.documentmanager.service.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.DocumentType;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;

    public Document upload(MultipartFile file) {
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
        log.info("Document renamed: id={}, old Name: '{}', new: '{}'", id, saved.getFilename(), newFilename );
        return documentRepository.save(document);
    }

    public void delete(Long id) {
        if (!documentRepository.existsById(id)) {
            log.warn("Document with id={} not found", id);
            throw new ResourceNotFoundException("Document", id);
        }
        documentRepository.deleteById(id);
        log.info("Document with id={} deleted successfully", id);
    }
}