//package org.nmcpye.datarun.domain;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.nmcpye.datarun.domain.ActivityTestSamples.*;
//import static org.nmcpye.datarun.domain.ProjectTestSamples.*;
//
//import org.junit.jupiter.api.Test;
//import org.nmcpye.datarun.web.rest.TestUtil;
//
//class ActivityTest {
//
//    @Test
//    void equalsVerifier() throws Exception {
//        TestUtil.equalsVerifier(Activity.class);
//        Activity activity1 = getActivitySample1();
//        Activity activity2 = new Activity();
//        assertThat(activity1).isNotEqualTo(activity2);
//
//        activity2.setId(activity1.getId());
//        assertThat(activity1).isEqualTo(activity2);
//
//        activity2 = getActivitySample2();
//        assertThat(activity1).isNotEqualTo(activity2);
//    }
//
//    @Test
//    void projectTest() {
//        Activity activity = getActivityRandomSampleGenerator();
//        Project projectBack = getProjectRandomSampleGenerator();
//
//        activity.setProject(projectBack);
//        assertThat(activity.getProject()).isEqualTo(projectBack);
//
//        activity.project(null);
//        assertThat(activity.getProject()).isNull();
//    }
//}
