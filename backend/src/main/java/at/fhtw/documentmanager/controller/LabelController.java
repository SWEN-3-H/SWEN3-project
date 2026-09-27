package at.fhtw.documentmanager.controller;

import at.fhtw.documentmanager.dto.LabelResponseDto;
import at.fhtw.documentmanager.dto.LabelRequestDto;
import at.fhtw.documentmanager.mapper.LabelMapper;
import at.fhtw.documentmanager.service.LabelService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/labels")
@RequiredArgsConstructor
public class LabelController {

    private final LabelService labelService;
    private final LabelMapper labelMapper;

    @PostMapping
    public ResponseEntity<LabelResponseDto> create(@Valid @RequestBody LabelRequestDto request) {
        var label = labelService.create(request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(labelMapper.toDto(label));
    }

    @GetMapping
    public List<LabelResponseDto> findAll() {
        return labelService.findAll().stream()
                .map(labelMapper::toDto)
                .toList();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        labelService.delete(id);
        return ResponseEntity.noContent().build();
    }
}