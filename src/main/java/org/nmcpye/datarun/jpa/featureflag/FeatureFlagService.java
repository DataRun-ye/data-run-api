package org.nmcpye.datarun.jpa.featureflag;

import lombok.RequiredArgsConstructor;
import org.nmcpye.datarun.jpa.featureflag.repository.FeatureFlagRepository;
import org.springframework.stereotype.Service;

/**
 * @author Hamza Assada
 * @since 23/09/2025
 */
@Service
@RequiredArgsConstructor
public class FeatureFlagService {
    private final FeatureFlagRepository repository;

    public boolean isEnabled(String flagName) {
        return repository.findByName(flagName)
            .map(FeatureFlag::isEnabled)
            .orElse(false);
    }
}

//@RestController
//@RequestMapping("/api/flags")
//@RequiredArgsConstructor
//public class FeatureFlagController {
//    private final FeatureFlagService service;
//
//    @GetMapping("/{flagName}")
//    public boolean getFlagStatus(@PathVariable String flagName) {
//        return service.isEnabled(flagName);
//    }
//}
