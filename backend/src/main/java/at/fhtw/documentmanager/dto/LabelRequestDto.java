package at.fhtw.documentmanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LabelRequestDto(
        @NotBlank(message = "Label name must not be empty")
        @Size(max = 20, message = "Label name must not be longer than 20 characters")
        String name,

        @Pattern(regexp = "^#[0-9a-fA-F]{6}$", message = "Color must be a hex value")
        String color
) {}