package at.fhtw.documentmanager.dto;

import jakarta.validation.constraints.NotBlank;

public record LabelRequestDto(
        @NotBlank(message = "Name must not be blank")
        String name
) {}