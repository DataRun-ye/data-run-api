package org.nmcpye.datarun.web.rest.postgres.optionset;

import org.nmcpye.datarun.common.IdScheme;
import org.nmcpye.datarun.jpa.option.OptionSet;
import org.nmcpye.datarun.jpa.option.repository.OptionRepository;
import org.nmcpye.datarun.jpa.option.repository.OptionSetRepository;
import org.nmcpye.datarun.jpa.option.service.OptionSetService;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.web.rest.common.ApiVersion;
import org.nmcpye.datarun.web.rest.postgres.JpaBaseResource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

import static org.nmcpye.datarun.web.rest.postgres.optionset.OptionSetResource.CUSTOM;
import static org.nmcpye.datarun.web.rest.postgres.optionset.OptionSetResource.V1;

/**
 * REST controller for managing {@link OptionSet}.
 */
@RestController
@RequestMapping(value = {CUSTOM, V1})
@PreAuthorize("hasAnyAuthority(\"" + AuthoritiesConstants.ADMIN + "\", \"" + AuthoritiesConstants.USER + "\")")
public class OptionSetResource
        extends JpaBaseResource<OptionSet> {

    protected static final String NAME = "/optionSets";
    protected static final String CUSTOM = ApiVersion.API_CUSTOM + NAME;
    protected static final String V1 = ApiVersion.API_V1 + NAME;
    private final OptionRepository optionRepository;
    private final OptionSetRepository optionSetRepository;

    public OptionSetResource(OptionSetService optionSetService,
                             OptionRepository optionRepository,
                             OptionSetRepository optionSetRepository) {
        super(optionSetService, optionSetRepository);
        this.optionRepository = optionRepository;
        this.optionSetRepository = optionSetRepository;
    }

    /**
     * to maintain references indexing. if option set exists in db, looks up payload options by their
     * un-updatable codes and set their ids to existing id or null if new options
     *
     * @param payLoadEntity option set
     * @return processed option set
     */
    @Override
    protected OptionSet preProcess(OptionSet payLoadEntity) {
        Optional<OptionSet> optionalOptionSet = identifiableObjectService.findByIdOrUid(payLoadEntity);
        if (optionalOptionSet.isPresent()) {
            Map<String, String> codeIdMap = optionalOptionSet.get().getOptionCodePropertyMap(IdScheme.ID);
            final var options = payLoadEntity.getOptions().stream().peek((option) -> {
                option.setId(codeIdMap.get(option.getCode()));
            }).toList();
            payLoadEntity.setOptions(options);
        }
        return payLoadEntity;
    }

    @Override
    protected String getName() {
        return "optionSets";
    }
}
