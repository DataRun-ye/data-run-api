package org.nmcpye.datarun.drun.postgres.service.impl;

import jakarta.el.PropertyNotFoundException;
import org.nmcpye.datarun.domain.User;
import org.nmcpye.datarun.drun.postgres.domain.Team;
import org.nmcpye.datarun.drun.postgres.repository.TeamRelationalRepositoryCustom;
import org.nmcpye.datarun.drun.postgres.service.TeamServiceCustom;
import org.nmcpye.datarun.drun.postgres.service.indentifieble.IdentifiableRelationalServiceImpl;
import org.nmcpye.datarun.repository.UserRepository;
import org.nmcpye.datarun.security.AuthoritiesConstants;
import org.nmcpye.datarun.security.SecurityUtils;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Primary
@Transactional
public class TeamServiceCustomImpl
    extends IdentifiableRelationalServiceImpl<Team>
    implements TeamServiceCustom {

    TeamRelationalRepositoryCustom repositoryCustom;

    final UserRepository userRepository;

    public TeamServiceCustomImpl(TeamRelationalRepositoryCustom teamRepositoryCustom,
                                 UserRepository userRepository) {
        super(teamRepositoryCustom);
        this.repositoryCustom = teamRepositoryCustom;
        this.userRepository = userRepository;
    }

    @Override
    public Team saveWithRelations(Team object) {
        Team parent = object.getParent();
        if (parent != null) {
            parent = findParent(parent);
            object.setParent(parent);
        }

        Set<Long> usersUids = object.getUsers().stream().map(User::getId).collect(Collectors.toSet());
        Set<User> users = new HashSet<>(userRepository.findAllById(usersUids));
        object.setUsers(users);

        return repositoryCustom.save(object);
    }

    private Team findParent(Team parent) {
        return Optional.ofNullable(parent.getId())
            .flatMap(repositoryCustom::findById)
            .or(() -> Optional.ofNullable(parent.getUid())
                .flatMap(repositoryCustom::findByUid))
            .or(() -> Optional.ofNullable(parent.getCode())
                .flatMap(repositoryCustom::findByCode))
            .orElseThrow(() -> new PropertyNotFoundException("Parent not found: " + parent));
    }

    //    @Override
//    public Page<Team> findAllByUser(Pageable pageable) {
//        return repositoryCustom.findAllByUser(pageable);
//    }

    @Override
    public Page<Team> findAllByUser(Pageable pageable) {
        if (SecurityUtils.hasCurrentUserAnyOfAuthorities(AuthoritiesConstants.ADMIN)) {
            return repositoryCustom.findAll(pageable);
        }
//        if (!SecurityUtils.isAuthenticated()) {
//            return Page.empty(pageable);
//        }
//
//        String userLogin = SecurityUtils.getCurrentUserLogin().get();
//
//        Specification<Team> specification = Specification.where(TeamSpecifications.hasUser(userLogin))
//            .and(TeamSpecifications.isNotDisabled());

        return repositoryCustom.findAllByUser(/*specification, */pageable);
    }

    @Transactional
    @Override
    public void updatePaths() {
        repositoryCustom.updatePaths();
    }

    /**
     * This is scheduled to get fired everyday, at 03:19 (am).
     * - **`0`**: Second (`0` seconds)
     * - **`19`**: Minute (`15` minutes)
     * - **`3`**: Hour (`3` AM)
     * - **`* *`**: Day of month and Month (`* *` means every day of every month)
     * - **`?`**: Day of the week (`?` is used when you don't care about the specific day of the week)
     */
//    @Scheduled(cron = "0 0 3 * * ?")
    @Scheduled(cron = "0 19 3 * * ?")
    @Override
    @Transactional
    public void forceUpdatePaths() {
        repositoryCustom.forceUpdatePaths();
    }


}
