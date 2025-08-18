package org.nmcpye.datarun.jpa.activity;

import org.junit.jupiter.api.Test;
import org.nmcpye.datarun.web.rest.TestUtil;

import static org.assertj.core.api.Assertions.assertThat;
import static org.nmcpye.datarun.jpa.activity.ActivityTestSamples.getActivitySample1;
import static org.nmcpye.datarun.jpa.activity.ActivityTestSamples.getActivitySample2;

/**
 * @author Hamza Assada 14/08/2025 (7amza.it@gmail.com)
 */
class ActivityTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Activity.class);
        Activity activity1 = getActivitySample1();
        Activity activity2 = new Activity();
        assertThat(activity1).isNotEqualTo(activity2);

        activity2.setId(activity1.getId());
        assertThat(activity1).isEqualTo(activity2);

        activity2 = getActivitySample2();
        assertThat(activity1).isNotEqualTo(activity2);
    }

//    @Test
//    void projectTest() {
//        Activity activity = getActivityRandomSampleGenerator();
//        Project projectBack = getProjectRandomSampleGenerator();
//
//        activity.setProject(projectBack);
//        assertThat(activity.getProject()).isEqualTo(projectBack);
//
//        activity.setProject(null);
//        assertThat(activity.getProject()).isNull();
//    }
}
