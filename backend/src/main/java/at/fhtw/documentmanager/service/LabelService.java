package at.fhtw.documentmanager.service;

import at.fhtw.documentmanager.model.Label;
import at.fhtw.documentmanager.repository.LabelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LabelService {

    private final LabelRepository labelRepository;

    public Label create(String name) {
        if (labelRepository.findByName(name).isPresent()) {
            log.warn("Label creation failed, name already exists: '{}", name);
            throw new DuplicateLabelException(name);
        }
        Label label = new Label();
        label.setName(name);
        Label saved = labelRepository.save(label);
        log.info("Label created succesfully: id:{}, name = '{}'",saved.getId(), name);
        return saved;
    }

    public List<Label> findAll() {
        List<Label> labels = labelRepository.findAll();
        log.debug("Loaded {} lables", labels.size());
        return labels;
    }

    public void delete(Long id) {
        if (!labelRepository.existsById(id)) {
            log.warn("Label delete failed, no Label with id: {}", id);
            throw new ResourceNotFoundException("Label", id);
        }
        labelRepository.deleteById(id);
        log.info("Label deleted: id={}", id);
    }
}