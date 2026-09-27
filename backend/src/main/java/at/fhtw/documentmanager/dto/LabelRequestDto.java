package at.fhtw.documentmanager.dto;

import jakarta.validation.constraints.NotBlank;

public record LabelRequestDto(
        @NotBlank(message = "Label name must not be blank")
        String name
) {}