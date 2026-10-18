package at.fhtw.documentmanager.service;

import at.fhtw.documentmanager.model.Document;
import at.fhtw.documentmanager.model.Label;
import at.fhtw.documentmanager.repository.LabelRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LabelService {

    private final LabelRepository labelRepository;

    public Label create(String name, String color) {
        if (labelRepository.findByName(name).isPresent()) {
            log.warn("Label creation failed, name already exists: '{}", name);
            throw new DuplicateLabelException(name);
        }
        Label label = new Label();
        label.setName(name);
        label.setColor(color);
        Label saved = labelRepository.save(label);
        log.info("Label created succesfully: id:{}, name = '{}'",saved.getId(), name);
        return saved;
    }

    public List<Label> findAll() {
        List<Label> labels = labelRepository.findAll();
        log.debug("Loaded {} lables", labels.size());
        return labels;
    }

    @Transactional
    public Label update(Long id, String name, String color) {
        Label label = labelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Label", id));

        if (labelRepository.existsByNameAndIdNot(name, id)) {
            throw new DuplicateLabelException(name);
        }

        label.setName(name);
        label.setColor(color);
        log.info("Label updated: id={}", id);
        return label;
    }

    @Transactional
    public void delete(Long id) {
        if (!labelRepository.existsById(id)) {
            log.warn("Label delete failed, no Label with id: {}", id);
            throw new ResourceNotFoundException("Label", id);
        }
        Label label = labelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Label", id));
        for (Document document : label.getDocuments()) {
            document.getLabels().remove(label);
        }
        labelRepository.delete(label);
        log.info("Label deleted: id={}", id);
    }
}