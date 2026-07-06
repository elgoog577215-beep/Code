package com.onlinejudge.learning.standardlibrary;

import com.onlinejudge.learning.standardlibrary.application.AiStandardLibraryFallbackArchiveValueCatalog;
import com.onlinejudge.learning.standardlibrary.application.AiStandardLibraryFallbackArchiveValueCatalog.FallbackArchiveTreatment;
import com.onlinejudge.learning.standardlibrary.application.AiStandardLibrarySeedCatalog;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class AiStandardLibraryFallbackArchiveValueCatalogTest {

    @Test
    void classifiesFallbackArchiveIntoDirectRewriteAndArchiveOnlyBuckets() {
        var signals = AiStandardLibraryFallbackArchiveValueCatalog.signals();
        Set<String> archivedKnowledgeCodes = AiStandardLibrarySeedCatalog.archivedGeneratedFallbackSeeds().stream()
                .flatMap(seed -> seed.knowledgeNodeCodes().stream())
                .collect(Collectors.toSet());
        Set<String> activeKnowledgeCodes = AiStandardLibrarySeedCatalog.seeds().stream()
                .flatMap(seed -> seed.knowledgeNodeCodes().stream())
                .collect(Collectors.toSet());

        assertThat(signals)
                .anySatisfy(signal -> {
                    assertThat(signal.knowledgeCode()).isEqualTo("BASIC.LOOP.CONTROL.break_使用");
                    assertThat(signal.treatment()).isEqualTo(FallbackArchiveTreatment.DIRECT_ABSORPTION);
                    assertThat(signal.extractedValues()).contains("主题本身可教学");
                })
                .anySatisfy(signal -> {
                    assertThat(signal.knowledgeCode()).isEqualTo("DS.LINEAR.STACK.括号匹配");
                    assertThat(signal.treatment()).isEqualTo(FallbackArchiveTreatment.TYPE_REWRITE);
                    assertThat(signal.extractedValues()).contains("保留错因类型和选题方向");
                })
                .anySatisfy(signal -> {
                    assertThat(signal.knowledgeCode()).isEqualTo("BASIC.IO.STDIN.单值读取");
                    assertThat(signal.treatment()).isEqualTo(FallbackArchiveTreatment.ARCHIVE_ONLY);
                    assertThat(activeKnowledgeCodes).doesNotContain(signal.knowledgeCode());
                });

        assertThat(AiStandardLibraryFallbackArchiveValueCatalog.directAbsorptionKnowledgeCodes())
                .contains("BASIC.ARRAY.PREFIX.前缀和定义", "BASIC.STRING.BUILD.删除替换",
                        "BASIC.BRANCH.IF.多分支链");
        assertThat(AiStandardLibraryFallbackArchiveValueCatalog.typeRewriteKnowledgeCodes())
                .contains("DS.SET_MAP.HASH.字符串键", "ENG.DEBUG.TRACE.DP_表变化",
                        "CONTEST.SUBMIT.CHECKLIST.初始化检查");
        assertThat(AiStandardLibraryFallbackArchiveValueCatalog.directAbsorptionKnowledgeCodes())
                .allMatch(archivedKnowledgeCodes::contains);
        assertThat(AiStandardLibraryFallbackArchiveValueCatalog.typeRewriteKnowledgeCodes())
                .allMatch(archivedKnowledgeCodes::contains);
        assertThat(signals.stream()
                .filter(signal -> signal.treatment() == FallbackArchiveTreatment.ARCHIVE_ONLY)
                .count()).isGreaterThan(150);
    }
}
