package at.fhtw.documentmanager.search;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(indexName = "documents")
public class DocumentIndexEntry {
    @Id
    private String id;

    @Field(type = FieldType.Text)
    private String filename;

    @Field(type = FieldType.Text)
    private String content;
}
