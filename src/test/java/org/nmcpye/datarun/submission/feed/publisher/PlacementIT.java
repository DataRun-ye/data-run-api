package org.nmcpye.datarun.submission.feed.publisher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.ACTIVITY_ID;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.ASSIGNMENT_ID;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.ORG_UNIT_ID;
import static org.nmcpye.datarun.submission.feed.publisher.FeedFixture.TEAM_ID;

/** The placement a head names, resolved from its stored uids to ids, in the one place the publisher does it. */
@FeedPublisherIntegrationTest
class PlacementIT {

    @Autowired
    private NamedParameterJdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @BeforeEach
    void setUp() {
        new FeedFixture(jdbc, transactionManager, url, username, password).ensureConfiguration();
    }

    private Placement placement(String metadata) {
        return new TransactionTemplate(transactionManager).execute(status -> Placement.of(jdbc, metadata));
    }

    @Test
    void theStoredUidsResolveToTheirRowsIds() {
        Placement placement = placement("""
            {"assignment_uid": "fdAssign001", "team_uid": "fdTeam00001", "org_unit_uid": "fdOrgUnit01",
             "activity_uid": "fdActivity1", "team_code": "FDT", "status": null}""");

        assertThat(placement).isEqualTo(new Placement(ASSIGNMENT_ID, TEAM_ID, ORG_UNIT_ID, ACTIVITY_ID));
        assertThat(placement.complete()).isTrue();
    }

    @Test
    void aUidThatNamesNothingLeavesItsIdMissing() {
        Placement placement = placement("""
            {"assignment_uid": "fdAssignNo1", "team_uid": "fdTeam00001", "org_unit_uid": "fdOrgUnit01",
             "activity_uid": "fdActivity1"}""");

        assertThat(placement.assignmentId()).isNull();
        assertThat(placement.teamId()).isEqualTo(TEAM_ID);
        assertThat(placement.complete()).isFalse();
    }

    @Test
    void metadataWithoutAPlacementResolvesNothing() {
        assertThat(placement("{}").complete()).isFalse();
        assertThat(placement(null)).isEqualTo(new Placement(null, null, null, null));
    }
}
