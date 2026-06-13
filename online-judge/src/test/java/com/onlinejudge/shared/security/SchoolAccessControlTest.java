package com.onlinejudge.shared.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlinejudge.classroom.domain.ClassGroup;
import com.onlinejudge.classroom.persistence.ClassGroupRepository;
import com.onlinejudge.submission.domain.Submission;
import com.onlinejudge.submission.persistence.SubmissionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:school-access-control;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "TEACHER_PASSWORD=test-teacher-password",
        "TEACHER_SESSION_SECRET=test-teacher-session-secret-1234567890",
        "STUDENT_TOKEN_SECRET=test-student-token-secret-1234567890",
        "AI_ENABLED=false"
})
class SchoolAccessControlTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    ClassGroupRepository classGroupRepository;

    @Autowired
    SubmissionRepository submissionRepository;

    @Test
    void teacherApisRequireSessionAndLoginSetsCookie() throws Exception {
        mockMvc.perform(get("/api/teacher/assignments"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/teacher/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"test-teacher-password\"}"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists(TeacherSessionService.COOKIE_NAME));
    }

    @Test
    void studentTokenGuardsStudentScopedApis() throws Exception {
        ClassGroup group = classGroupRepository.save(ClassGroup.builder()
                .name("高一试点班")
                .grade("高一")
                .teacherName("老师")
                .build());

        MvcResult login = mockMvc.perform(post("/api/student/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"classGroupId\":" + group.getId() + ",\"displayName\":\"张三\",\"studentNo\":\"01\"}"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode student = objectMapper.readTree(login.getResponse().getContentAsString());
        long studentId = student.path("id").asLong();
        String token = student.path("studentAccessToken").asText();
        assertThat(token).isNotBlank();

        mockMvc.perform(get("/api/student/profile/" + studentId + "/assignments"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/student/profile/" + studentId + "/assignments")
                        .header(StudentAccessTokenService.HEADER_NAME, token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/student/profile/" + (studentId + 1) + "/assignments")
                        .header(StudentAccessTokenService.HEADER_NAME, token))
                .andExpect(status().isForbidden());
    }

    @Test
    void submissionOwnedByStudentRequiresMatchingToken() throws Exception {
        Submission submission = submissionRepository.save(Submission.builder()
                .problemId(1L)
                .studentProfileId(99L)
                .languageId(71)
                .languageName("Python 3")
                .sourceCode("print(1)")
                .verdict(Submission.Verdict.ACCEPTED)
                .memoryUsed(0)
                .executionTime(0.0)
                .build());

        mockMvc.perform(get("/api/submissions/" + submission.getId()))
                .andExpect(status().isUnauthorized());
    }
}
