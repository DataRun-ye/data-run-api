package org.nmcpye.datarun.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.nmcpye.datarun.domain.ActivityTestSamples.*;
import static org.nmcpye.datarun.domain.ChvSupplyTestSamples.*;
import static org.nmcpye.datarun.domain.TeamTestSamples.*;

import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.web.rest.TestUtil;

class ChvSupplyTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(ChvSupply.class);
        ChvSupply chvSupply1 = getChvSupplySample1();
        ChvSupply chvSupply2 = new ChvSupply();
        assertThat(chvSupply1).isNotEqualTo(chvSupply2);

        chvSupply2.setId(chvSupply1.getId());
        assertThat(chvSupply1).isEqualTo(chvSupply2);

        chvSupply2 = getChvSupplySample2();
        assertThat(chvSupply1).isNotEqualTo(chvSupply2);
    }

    @Test
    void activityTest() throws Exception {
        ChvSupply chvSupply = getChvSupplyRandomSampleGenerator();
        Activity activityBack = getActivityRandomSampleGenerator();

        chvSupply.setActivity(activityBack);
        assertThat(chvSupply.getActivity()).isEqualTo(activityBack);

        chvSupply.activity(null);
        assertThat(chvSupply.getActivity()).isNull();
    }

    @Test
    void teamTest() throws Exception {
        ChvSupply chvSupply = getChvSupplyRandomSampleGenerator();
        Team teamBack = getTeamRandomSampleGenerator();

        chvSupply.setTeam(teamBack);
        assertThat(chvSupply.getTeam()).isEqualTo(teamBack);

        chvSupply.team(null);
        assertThat(chvSupply.getTeam()).isNull();
    }
}
