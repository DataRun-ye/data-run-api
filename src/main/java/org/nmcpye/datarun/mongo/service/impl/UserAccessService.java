package org.nmcpye.datarun.mongo.service.impl;

import org.nmcpye.datarun.drun.postgres.domain.DataElement;
import org.nmcpye.datarun.drun.postgres.domain.OptionSet;
import org.nmcpye.datarun.drun.postgres.domain.Team;
import org.nmcpye.datarun.drun.postgres.domain.enumeration.FormPermission;
import org.nmcpye.datarun.drun.postgres.repository.DataElementRepository;
import org.nmcpye.datarun.drun.postgres.repository.OptionSetRepository;
import org.nmcpye.datarun.drun.postgres.repository.TeamRelationalRepositoryCustom;
import org.nmcpye.datarun.mongo.domain.dataform.DataFormTemplate;
import org.nmcpye.datarun.mongo.repository.DataFormTemplateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class UserAccessService {
//    public static final String TEAMS_BY_LOGIN_CACHE = "teamsByLogin";
//    public static final String FORMS_BY_LOGIN_CACHE = "formsByLogin";
//    public static final String DATA_ELEMENTS_BY_LOGIN_CACHE = "dataElementsByLogin";
//    public static final String OPTION_SETS_BY_LOGIN_CACHE = "optionSetsByLogin";

    final private DataFormTemplateRepository templateRepository;
    final private TeamRelationalRepositoryCustom teamRepository;
    final private DataElementRepository dataElementRepository;
    final private OptionSetRepository optionSetRepository;

    public UserAccessService(DataFormTemplateRepository templateRepository,
                             TeamRelationalRepositoryCustom teamRepository,
                             DataElementRepository dataElementRepository,
                             OptionSetRepository optionSetRepository) {
        this.templateRepository = templateRepository;
        this.teamRepository = teamRepository;
        this.dataElementRepository = dataElementRepository;
        this.optionSetRepository = optionSetRepository;
    }

    public Set<Team> getUserTeams(String userLogin) {
        return new HashSet<>(teamRepository.findAllByUserLogin(userLogin, false));
    }

    public Set<DataFormTemplate> getUserFormsWithWritePermission(String userLogin) {
        return teamRepository.findAllByUserLogin(userLogin, false)
            .stream().flatMap((t) -> t.getFormWithAnyPermission(List.of(FormPermission.ADD_SUBMISSIONS)).stream())
            .flatMap(f -> templateRepository.findAllByUidInAndDisabledIsNot(List.of(f), true).stream())
            .collect(Collectors.toSet());
    }

    public Set<DataFormTemplate> getAllAccessibleUserForms(String userLogin) {
        return teamRepository.findAllByUserLogin(userLogin, false)
            .stream().flatMap((t) -> t.getFormPermissions().stream())
            .flatMap(f -> templateRepository.findAllByUidInAndDisabledIsNot(List.of(f.getForm()), true).stream())
            .collect(Collectors.toSet());
    }

    //    @Cacheable(cacheNames = DATA_ELEMENTS_BY_LOGIN_CACHE, key = "#userLogin")
    public Set<DataElement> getUserDataElements(String userLogin) {
        return getAllAccessibleUserForms(userLogin).stream().flatMap(f -> f.getFields().stream())
            .map(field -> dataElementRepository.findByUid(field.getId()).orElse(null))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }

    //    @Cacheable(cacheNames = OPTION_SETS_BY_LOGIN_CACHE, key = "#userLogin")
    public Set<OptionSet> getUserOptionSets(String userLogin) {
        return getAllAccessibleUserForms(userLogin)
            .stream().flatMap(f -> f.getFields().stream())
            .filter(f -> f.getType().isOptionsType())
            .map(field -> optionSetRepository.findByUid(field.getOptionSet()).orElse(null))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    }
}
