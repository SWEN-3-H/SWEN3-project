package at.fhtw.documentmanager.mapper;

import at.fhtw.documentmanager.dto.DocumentResponseDto;
import at.fhtw.documentmanager.model.Document;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring", uses = LabelMapper.class)
public interface DocumentMapper {
    DocumentResponseDto toDto(Document document);
}