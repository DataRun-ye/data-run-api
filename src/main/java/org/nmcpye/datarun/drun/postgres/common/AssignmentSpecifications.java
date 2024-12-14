package org.nmcpye.datarun.drun.postgres.common;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.nmcpye.datarun.domain.User;
import org.nmcpye.datarun.drun.postgres.domain.Assignment;
import org.nmcpye.datarun.drun.postgres.domain.Team;
import org.nmcpye.datarun.drun.postgres.repository.IdentifiableRelationalRepository;
import org.springframework.data.jpa.domain.Specification;

public abstract class AssignmentSpecifications
    extends DefaultIdentifiableSpecifications<Assignment> {

    public AssignmentSpecifications(IdentifiableRelationalRepository<Assignment> repository) {
        super(repository);
    }

    public Specification<Assignment> canRead(String login) {
        return (root, query, criteriaBuilder) -> {
            // Join assignmentJoin -> team -> users
            Join<Assignment, Team> assignmentJoin = root.join("team", JoinType.INNER);
            Join<Team, User> userJoin = assignmentJoin.join("users", JoinType.INNER);
            // Create the predicate for the username
            return criteriaBuilder.equal(userJoin.get("login"), login);
        };
    }

    public Specification<Assignment> hasUserWithUsername(String login) {
        return (root, query, criteriaBuilder) -> {
            // Join assignmentJoin -> team -> users
            Join<Assignment, Team> assignmentJoin = root.join("team", JoinType.INNER);
            Join<Team, User> userJoin = assignmentJoin.join("users", JoinType.INNER);
            // Create the predicate for the username
            return criteriaBuilder.equal(userJoin.get("login"), login);
        };
    }

    public Specification<Assignment> isEager() {
        return (root, query, criteriaBuilder) -> {
            // Join Team with the users

            root.fetch("team");
            root.fetch("activity");
            root.fetch("orgUnit");
            // Check if the userId matches
//            return query.getRestriction();
            return criteriaBuilder.conjunction();
        };
    }
}
