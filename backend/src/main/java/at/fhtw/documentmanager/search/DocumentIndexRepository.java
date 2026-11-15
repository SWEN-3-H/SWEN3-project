package at.fhtw.documentmanager.search;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

import java.util.List;

public interface DocumentIndexRepository extends ElasticsearchRepository<DocumentIndexEntry, String> {
    List<DocumentIndexEntry> findByContentOrFilename(String content, String filename);
}
