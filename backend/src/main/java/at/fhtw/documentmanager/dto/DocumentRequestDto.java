package at.fhtw.documentmanager.dto;
import jakarta.validation.constraints.NotBlank;

public record DocumentRequestDto(
        @NotBlank(message = "Filename must not be blank")
        String filename
) {}