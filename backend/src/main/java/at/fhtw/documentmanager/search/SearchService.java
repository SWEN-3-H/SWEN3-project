package at.fhtw.documentmanager.search;

import at.fhtw.documentmanager.model.Document;
import at.fhtw.documentmanager.repository.DocumentRepository;
import at.fhtw.documentmanager.service.InvalidDocumentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {
    private final DocumentIndexRepository indexRepository;
    private final DocumentRepository documentRepository;

    public List<Document> search(String query) {
        if (query == null || query.isBlank()) {
            log.warn("Search rejected: query is blank");
            throw new InvalidDocumentException("Search term must not be blank");
        }

        List<DocumentIndexEntry> hits;
        try {
            hits = indexRepository.findByContentOrFilename(query, query);
        } catch (Exception e) {
            log.error("Elasticsearch search failed for query '{}'", query, e);
            throw new SearchException("Search is currently unavailable", e);
        }

        List<Long> ids = hits.stream()
                .map(hit -> Long.valueOf(hit.getId()))
                .toList();
        log.info("Search '{}' matched {} documents", query, ids.size());

        return documentRepository.findAllById(ids);
    }
}
