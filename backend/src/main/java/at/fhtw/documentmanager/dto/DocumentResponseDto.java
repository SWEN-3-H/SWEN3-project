package at.fhtw.documentmanager.dto;

import java.time.Instant;
import java.util.Set;

public record DocumentResponseDto(
        Long id,
        String filename,
        Instant createdAt,
        String summary,
        Set<LabelResponseDto> labels
) {}