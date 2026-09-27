package at.fhtw.documentmanager.service;

import at.fhtw.documentmanager.model.Document;
import at.fhtw.documentmanager.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final DocumentRepository documentRepository;

    public Document upload(MultipartFile file) {
        Document document = new Document();
        document.setFilename(file.getOriginalFilename());
        return documentRepository.save(document);
    }

    public List<Document> findAll() {
        return documentRepository.findAll();
    }

    public Document findById(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", id));
    }

    public Document updateFilename(Long id, String newFilename) {
        Document document = findById(id);
        document.setFilename(newFilename);
        return documentRepository.save(document);
    }

    public void delete(Long id) {
        if (!documentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Document", id);
        }
        documentRepository.deleteById(id);
    }
}