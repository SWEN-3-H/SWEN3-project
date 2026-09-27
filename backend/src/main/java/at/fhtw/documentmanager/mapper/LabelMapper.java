package at.fhtw.documentmanager.mapper;

import at.fhtw.documentmanager.dto.LabelResponseDto;
import at.fhtw.documentmanager.model.Label;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LabelMapper {
    LabelResponseDto toDto(Label label);
    Label toEntity(LabelResponseDto dto);
}