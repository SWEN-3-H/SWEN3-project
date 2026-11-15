package at.fhtw.ocrworker.indexing;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface DocumentIndexRepository extends ElasticsearchRepository<DocumentIndexEntry, String> {
}