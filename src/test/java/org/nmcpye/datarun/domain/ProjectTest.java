//package org.nmcpye.datarun.domain;
//
//import static org.assertj.core.api.Assertions.assertThat;
//import static org.nmcpye.datarun.domain.ActivityTestSamples.*;
//import static org.nmcpye.datarun.domain.ProjectTestSamples.*;
//
//import java.util.HashSet;
//import java.util.Set;
//import org.junit.jupiter.api.Test;
//import org.nmcpye.datarun.web.rest.TestUtil;
//
//class ProjectTest {
//
//    @Test
//    void equalsVerifier() throws Exception {
//        TestUtil.equalsVerifier(Project.class);
//        Project project1 = getProjectSample1();
//        Project project2 = new Project();
//        assertThat(project1).isNotEqualTo(project2);
//
//        project2.setId(project1.getId());
//        assertThat(project1).isEqualTo(project2);
//
//        project2 = getProjectSample2();
//        assertThat(project1).isNotEqualTo(project2);
//    }
//
//    @Test
//    void activityTest() {
//        Project project = getProjectRandomSampleGenerator();
//        Activity activityBack = getActivityRandomSampleGenerator();
//
//        project.addActivity(activityBack);
//        assertThat(project.getActivities()).containsOnly(activityBack);
//        assertThat(activityBack.getProject()).isEqualTo(project);
//
//        project.removeActivity(activityBack);
//        assertThat(project.getActivities()).doesNotContain(activityBack);
//        assertThat(activityBack.getProject()).isNull();
//
//        project.activities(new HashSet<>(Set.of(activityBack)));
//        assertThat(project.getActivities()).containsOnly(activityBack);
//        assertThat(activityBack.getProject()).isEqualTo(project);
//
//        project.setActivities(new HashSet<>());
//        assertThat(project.getActivities()).doesNotContain(activityBack);
//        assertThat(activityBack.getProject()).isNull();
//    }
//}
