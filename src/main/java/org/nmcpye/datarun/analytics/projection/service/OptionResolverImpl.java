package org.nmcpye.datarun.analytics.projection.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.nmcpye.datarun.jpa.option.Option;
import org.nmcpye.datarun.jpa.option.repository.OptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Hamza Assada
 * @since 18/09/2025
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OptionResolverImpl implements OptionResolver {

    private final OptionRepository optionRepository;
    private final ObjectMapper om;

    // Pilot cache: key = optionSetUid + '|' + normalizedInput
    private final ConcurrentHashMap<String, Optional<OptionResolution>> cache = new ConcurrentHashMap<>();

    // lock on external vs internal keys: optionSetUid here is the external uid (11-char)
    @Transactional(readOnly = true)
    @Override
    public OptionResolution resolve(String optionSetUid, String codeOrUidRaw) {
        if (codeOrUidRaw == null) return null;
        String raw = codeOrUidRaw.trim();
        if (raw.isEmpty()) return null;

        String normalized = raw; // keep original case for translations exact match, but use lower for cache key
        String cacheKey = (optionSetUid == null ? "__GLOBAL__" : optionSetUid) + "|" + normalized.toLowerCase(Locale.ROOT);
        Optional<OptionResolution> cached = cache.get(cacheKey);
        if (cached != null) return cached.orElse(null);

        try {
            // 1. If it looks like a uid (11 chars alnum) try uid lookup
            if (looksLikeUid(normalized)) {
                Optional<Option> byUid = optionRepository.findByUid(normalized);
                if (byUid.isPresent() && matchesSet(byUid.get(), optionSetUid)) {
                    OptionResolution res = toResolution(byUid.get());
                    cache.put(cacheKey, Optional.of(res));
                    return res;
                }
            }

            // 2. Try code (scoped)
            Optional<Option> byCode = optionRepository.findByCodeAndOptionSetUid(normalized, optionSetUid);
            if (byCode.isPresent()) {
                OptionResolution res = toResolution(byCode.get());
                cache.put(cacheKey, Optional.of(res));
                return res;
            }

            // 3. Try name exact (scoped)
            Optional<Option> byName = optionRepository.findByNameAndOptionSetUid(normalized, optionSetUid);
            if (byName.isPresent()) {
                OptionResolution res = toResolution(byName.get());
                cache.put(cacheKey, Optional.of(res));
                return res;
            }

            // 4. Try translations exact (scoped)
            Optional<Option> byTransExact = optionRepository.findByTranslationValueExact(optionSetUid, normalized);
            if (byTransExact.isPresent()) {
                OptionResolution res = toResolution(byTransExact.get());
                cache.put(cacheKey, Optional.of(res));
                return res;
            }

            // 5. Try translations ILIKE (scoped, partial)
            Optional<Option> byTransLike = optionRepository.findByTranslationValueILike(optionSetUid, "%" + normalized + "%");
            if (byTransLike.isPresent()) {
                OptionResolution res = toResolution(byTransLike.get());
                cache.put(cacheKey, Optional.of(res));
                return res;
            }

            // Not found -> cache negative
            cache.put(cacheKey, Optional.empty());
            return null;
        } catch (Exception ex) {
            throw new RuntimeException("Option resolution failed for input='" + raw + "' setUid='" + optionSetUid + "': " + ex.getMessage(), ex);
        }
    }

    private boolean looksLikeUid(String s) {
        if (s.length() != 11) return false;
        return s.chars().allMatch(ch -> Character.isLetterOrDigit(ch) || ch == '_' || ch == '-');
    }

    private boolean matchesSet(Option opt, String optionSetUid) {
        if (optionSetUid == null) return true;
        // opt.getOptionSetUid() or opt.getOptionSet() -> adjust depending on your entity mapping
        try {
            String osUid = opt.getOptionSet().getUid(); // assume you have this convenience column; otherwise join
            return optionSetUid.equals(osUid);
        } catch (Exception ex) {
            return true; // be permissive if mapping unknown in pilot
        }
    }

    private OptionResolution toResolution(Option opt) {
        String uid = opt.getUid(); // business uid (11 chars)
        String labelJson = buildLabelJsonFromTranslations(opt);
        return new OptionResolution(uid, labelJson);
    }

    private String buildLabelJsonFromTranslations(Option opt) {
        try {
            return om.writeValueAsString(opt.getLabel());
        } catch (Exception ex) {
            log.warn("could not parse translations for option {}: {}", opt.getUid(), ex.getMessage());
            return null;
        }
    }
}
