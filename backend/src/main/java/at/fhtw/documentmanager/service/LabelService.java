package at.fhtw.documentmanager.service;

import at.fhtw.documentmanager.model.Label;
import at.fhtw.documentmanager.repository.LabelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LabelService {

    private final LabelRepository labelRepository;

    public Label create(String name) {
        if (labelRepository.findByName(name).isPresent()) {
            throw new DuplicateLabelException(name);
        }
        Label label = new Label();
        label.setName(name);
        return labelRepository.save(label);
    }

    public List<Label> findAll() {
        return labelRepository.findAll();
    }

    public void delete(Long id) {
        if (!labelRepository.existsById(id)) {
            throw new ResourceNotFoundException("Label", id);
        }
        labelRepository.deleteById(id);
    }
}