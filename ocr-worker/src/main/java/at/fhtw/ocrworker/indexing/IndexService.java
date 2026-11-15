package at.fhtw.ocrworker.indexing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class IndexService {
    private static final Logger log = LoggerFactory.getLogger(IndexService.class);

    private final DocumentIndexRepository repository;

    public IndexService(DocumentIndexRepository repository){
        this.repository = repository;
    }

    public void index(Long documentId, String filename, String text) {
        repository.save(new DocumentIndexEntry(String.valueOf(documentId), filename, text));
        log.info("Indexed document {} ({} characters)", documentId, text == null ? 0 : text.length());
    }
}