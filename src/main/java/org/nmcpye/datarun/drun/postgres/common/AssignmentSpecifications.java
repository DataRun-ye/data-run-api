package org.nmcpye.datarun.drun.postgres.common;

import jakarta.persistence.criteria.*;
import org.nmcpye.datarun.domain.Activity;
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

    //    @Override
    public Specification<Assignment> canRead(String login) {
        return (root, query, criteriaBuilder) -> {
            // Join assignmentJoin -> team -> users
            Join<Assignment, Team> assignmentJoin = root.join("team", JoinType.INNER);
            Join<Team, User> userJoin = assignmentJoin.join("users", JoinType.INNER);
            // Create the predicate for the username
            return criteriaBuilder.equal(userJoin.get("login"), login);
        };
    }

    public Specification<Assignment> canReadWithChildren(String login) {
        return (root, query, criteriaBuilder) -> {
            // Existing code for direct assignments
            // Existing code for direct assignments
            Join<Assignment, Team> teamJoin = root.join("team", JoinType.INNER);
            Join<Team, User> userJoin = teamJoin.join("users", JoinType.INNER);
            Join<Assignment, Activity> activityJoin = root.join("activity", JoinType.INNER);

//            Join<Assignment, Team> assignmentJoin = root.join("team", JoinType.INNER);
//            Join<Team, User> userJoin = assignmentJoin.join("users", JoinType.INNER);
//            Predicate directAssignments = criteriaBuilder.equal(userJoin.get("login"), login);
            Predicate directAssignments = criteriaBuilder.and(
                criteriaBuilder.equal(userJoin.get("login"), login),
                criteriaBuilder.equal(teamJoin.get("disabled"), false),
                criteriaBuilder.equal(activityJoin.get("disabled"), false)
            );


            // Add a subquery for descendant assignments
            Subquery<Assignment> descendantSubquery = query.subquery(Assignment.class);
            Root<Assignment> descendantRoot = descendantSubquery.from(Assignment.class);
            Join<Assignment, Team> descendantTeamJoin = descendantRoot.join("team", JoinType.INNER);
            Join<Assignment, Activity> descendantActivityJoin = descendantRoot.join("activity", JoinType.INNER);

            descendantSubquery.select(descendantRoot)
                .where(criteriaBuilder.and(
                    criteriaBuilder.like(descendantRoot.get("path"),
                        criteriaBuilder.concat(criteriaBuilder.concat("%,", root.get("uid")), ",%")),
                    criteriaBuilder.equal(descendantTeamJoin.get("disabled"), false),
                    criteriaBuilder.equal(descendantActivityJoin.get("disabled"), false)
                ));

            return criteriaBuilder.or(
                directAssignments,
                criteriaBuilder.exists(descendantSubquery)
            );

//            // Add a subquery for descendant assignments
//            Subquery<Assignment> descendantSubquery = query.subquery(Assignment.class);
//            Root<Assignment> descendantRoot = descendantSubquery.from(Assignment.class);

//            descendantSubquery.select(descendantRoot)
//                .where(criteriaBuilder.like(descendantRoot.get("path"),
//                    criteriaBuilder.concat(criteriaBuilder.concat("%,", root.get("uid")), ",%")));
//
//            return criteriaBuilder.or(
//                directAssignments,
//                criteriaBuilder.exists(descendantSubquery)
//            );
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
