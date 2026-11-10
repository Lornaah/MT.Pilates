package fr.mtpilates.config;

import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConfigService {

    private final AppConfigRepository repository;

    public ConfigService(final AppConfigRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public Labels labels() {
        return new Labels(repository.findAll().stream()
                .collect(Collectors.toMap(AppConfigEntry::getKey, AppConfigEntry::getValue)));
    }
}
