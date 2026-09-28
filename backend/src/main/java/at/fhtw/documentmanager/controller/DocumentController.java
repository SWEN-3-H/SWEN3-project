package at.fhtw.documentmanager.controller;

import at.fhtw.documentmanager.dto.DocumentRequestDto;
import at.fhtw.documentmanager.dto.DocumentResponseDto;
import at.fhtw.documentmanager.mapper.DocumentMapper;
import at.fhtw.documentmanager.service.DocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;
    private final DocumentMapper documentMapper;

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<DocumentResponseDto> upload(@RequestParam("file") MultipartFile file) {
        var document = documentService.upload(file);
        return ResponseEntity.status(HttpStatus.CREATED).body(documentMapper.toDto(document));
    }

    @GetMapping
    public List<DocumentResponseDto> findAll() {
        return documentService.findAll().stream()
                .map(documentMapper::toDto)
                .toList();
    }

    @GetMapping("/{id}")
    public DocumentResponseDto findById(@PathVariable Long id) {
        return documentMapper.toDto(documentService.findById(id));
    }

    @PutMapping("/{id}")
    public DocumentResponseDto update(@PathVariable Long id, @Valid @RequestBody DocumentRequestDto request) {
        var updated = documentService.updateFilename(id, request.filename());
        return documentMapper.toDto(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        documentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}