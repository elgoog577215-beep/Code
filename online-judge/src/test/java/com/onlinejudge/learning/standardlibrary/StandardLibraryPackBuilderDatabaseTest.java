package com.onlinejudge.learning.standardlibrary;

import com.onlinejudge.learning.standardlibrary.domain.AiStandardLibraryLayer;
import com.onlinejudge.learning.standardlibrary.persistence.AiStandardLibraryItemRepository;
import com.onlinejudge.submission.application.ModelDiagnosisBrief;
import com.onlinejudge.submission.application.StandardLibraryPack;
import com.onlinejudge.submission.application.StandardLibraryPackBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:ai-standard-library-builder;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "TEACHER_PASSWORD=test-teacher-password",
        "TEACHER_SESSION_SECRET=test-teacher-session-secret-1234567890",
        "STUDENT_TOKEN_SECRET=test-student-token-secret-1234567890",
        "AI_ENABLED=false"
})
class StandardLibraryPackBuilderDatabaseTest {

    @Autowired
    StandardLibraryPackBuilder builder;

    @Autowired
    AiStandardLibraryItemRepository repository;

    @Test
    void packBuilderReadsEnabledItemsFromDatabaseBeforeBuiltinFallback() {
        var item = repository.findByLayerAndCode(AiStandardLibraryLayer.MISTAKE_POINT, "IO_FORMAT").orElseThrow();
        item.setName("数据库输入输出易错点");
        item.setCommonMisconception("先确认数据库 v3 易错点是否被读取。");
        repository.saveAndFlush(item);

        StandardLibraryPack pack = builder.build(ModelDiagnosisBrief.builder()
                .allowedIssueTags(List.of("IO_FORMAT"))
                .build(), null);

        assertThat(pack.getBasicCauses())
                .filteredOn(cause -> "IO_FORMAT".equals(cause.getId()))
                .singleElement()
                .satisfies(cause -> {
                    assertThat(cause.getName()).isEqualTo("数据库输入输出易错点");
                    assertThat(cause.getStudentExplanation()).isEqualTo("先确认数据库 v3 易错点是否被读取。");
                });
    }

    @Test
    void directBuilderWithoutDatabaseServiceFallsBackToBuiltinLibrary() {
        StandardLibraryPack fallbackPack = new StandardLibraryPackBuilder(new com.onlinejudge.learning.diagnosis.DiagnosisTaxonomy())
                .build(ModelDiagnosisBrief.builder()
                        .allowedIssueTags(List.of("IO_FORMAT"))
                        .build(), null);

        assertThat(fallbackPack.getBasicCauses())
                .filteredOn(cause -> "IO_FORMAT".equals(cause.getId()))
                .singleElement()
                .satisfies(cause -> assertThat(cause.getName()).isEqualTo("输入输出格式"));
    }
}
