package com.onlinejudge.learning.standardlibrary;

import com.onlinejudge.learning.knowledge.application.InformaticsKnowledgeSeed;
import com.onlinejudge.learning.knowledge.application.InformaticsKnowledgeSeedCatalog;
import com.onlinejudge.learning.standardlibrary.application.AiStandardLibrarySeeder;
import com.onlinejudge.learning.standardlibrary.application.AiStandardLibrarySeed;
import com.onlinejudge.learning.standardlibrary.application.AiStandardLibrarySeedCatalog;
import com.onlinejudge.learning.standardlibrary.domain.AiStandardLibraryLayer;
import com.onlinejudge.learning.standardlibrary.domain.AiStandardLibraryItem;
import com.onlinejudge.learning.standardlibrary.persistence.AiStandardLibraryItemRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:ai-standard-library-seeder;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "TEACHER_PASSWORD=test-teacher-password",
        "TEACHER_SESSION_SECRET=test-teacher-session-secret-1234567890",
        "STUDENT_TOKEN_SECRET=test-student-token-secret-1234567890",
        "AI_ENABLED=false"
})
class AiStandardLibrarySeederTest {

    @Autowired
    AiStandardLibraryItemRepository repository;

    @Autowired
    AiStandardLibrarySeeder seeder;

    @Test
    void seedsSkillUnitsAndMistakePointsIdempotently() {
        long initialCount = repository.count();

        assertThat(initialCount).isGreaterThanOrEqualTo(1320);
        assertThat(repository.findAll().stream()
                .filter(item -> item.getLayer() == AiStandardLibraryLayer.SKILL_UNIT)
                .count()).isGreaterThanOrEqualTo(615);
        assertThat(repository.findAll().stream()
                .filter(item -> item.getLayer() == AiStandardLibraryLayer.MISTAKE_POINT)
                .count()).isGreaterThanOrEqualTo(690);
        assertThat(repository.findAll().stream()
                .filter(item -> item.getLayer() == AiStandardLibraryLayer.IMPROVEMENT_POINT)
                .count()).isGreaterThanOrEqualTo(6);
        assertThat(repository.findAll().stream()
                .filter(item -> item.getLayer() == AiStandardLibraryLayer.MISTAKE_POINT)
                .allMatch(item -> item.getSkillUnitCode() != null && !item.getSkillUnitCode().isBlank()))
                .isTrue();
        assertThat(repository.findAll().stream()
                .filter(item -> item.getKnowledgeNodeCodes() != null
                        && item.getKnowledgeNodeCodes().contains("BASIC.LOOP.BOUNDARY.左闭右开"))
                .count()).isGreaterThanOrEqualTo(1);
        assertThat(repository.findAll().stream()
                .filter(item -> item.getKnowledgeNodeCodes() != null
                        && item.getKnowledgeNodeCodes().contains("ALGO.DP.STATE.状态含义"))
                .count()).isGreaterThanOrEqualTo(1);
        assertThat(repository.findAll().stream()
                .filter(item -> item.getKnowledgeNodeCodes() != null
                        && item.getKnowledgeNodeCodes().contains("CONTEST.READING.CONSTRAINT.数据范围"))
                .count()).isGreaterThanOrEqualTo(1);
        assertThat(repository.findAll().stream()
                .filter(item -> item.getLayer() == AiStandardLibraryLayer.MISTAKE_POINT)
                .filter(item -> item.getCode().equals("MP_LOOP_STRICT_INEQUALITY_WHEN_EQUAL_ALLOWED"))
                .count()).isEqualTo(1);
        assertThat(repository.findAll().stream()
                .filter(item -> item.getLayer() == AiStandardLibraryLayer.MISTAKE_POINT)
                .filter(item -> item.getCode().equals("MP_DP_STATE_MISSING_DIMENSION"))
                .count()).isEqualTo(1);
        assertThat(repository.findAll().stream()
                .filter(item -> item.getLayer() == AiStandardLibraryLayer.MISTAKE_POINT)
                .filter(item -> item.getCode().equals("MP_GRAPH_UNDIRECTED_EDGE_ADDED_ONCE"))
                .count()).isEqualTo(1);
        assertThat(repository.findAll().stream()
                .filter(item -> item.getLayer() == AiStandardLibraryLayer.MISTAKE_POINT)
                .filter(item -> item.getCode().equals("MP_FUNCTION_OUTPUT_RETURN_MIXED"))
                .count()).isEqualTo(1);
        assertThat(repository.findAll().stream()
                .filter(item -> item.getLayer() == AiStandardLibraryLayer.MISTAKE_POINT)
                .filter(item -> item.getCode().equals("MP_TOPO_EDGE_DIRECTION_REVERSED"))
                .count()).isEqualTo(1);
        assertThat(repository.findAll().stream()
                .filter(item -> item.getLayer() == AiStandardLibraryLayer.MISTAKE_POINT)
                .filter(item -> item.getCode().equals("MP_BITMASK_INDEX_OFF_BY_ONE"))
                .count()).isEqualTo(1);
        assertThat(repository.findAll().stream()
                .filter(item -> item.getLayer() == AiStandardLibraryLayer.MISTAKE_POINT)
                .filter(item -> item.getCode().equals("MP_SIM_UPDATE_ORDER_USES_STALE_STATE"))
                .count()).isEqualTo(1);
        assertThat(repository.findAll().stream()
                .filter(item -> item.getLayer() == AiStandardLibraryLayer.MISTAKE_POINT)
                .filter(item -> item.getCode().equals("MP_STRING_FIND_NOT_FOUND_USED_AS_INDEX"))
                .count()).isEqualTo(1);
        assertThat(repository.findAll().stream()
                .filter(item -> item.getLayer() == AiStandardLibraryLayer.MISTAKE_POINT)
                .filter(item -> item.getCode().equals("MP_ARRAY_IN_PLACE_UPDATE_OVERWRITES_SOURCE"))
                .count()).isEqualTo(1);
        assertThat(repository.findAll().stream()
                .filter(item -> item.getLayer() == AiStandardLibraryLayer.MISTAKE_POINT)
                .filter(item -> item.getCode().equals("MP_READING_SAMPLE_SHAPE_OVERFITS_FORMAT"))
                .count()).isEqualTo(1);
        assertThat(repository.findAll().stream()
                .filter(item -> item.getLayer() == AiStandardLibraryLayer.MISTAKE_POINT)
                .filter(item -> item.getCode().equals("MP_V6_DP_01_KNAPSACK_FORWARD_REUSES_ITEM"))
                .count()).isEqualTo(1);
        assertThat(repository.findAll().stream()
                .filter(item -> item.getLayer() == AiStandardLibraryLayer.MISTAKE_POINT)
                .filter(item -> item.getCode().equals("MP_V6_DIJKSTRA_NEGATIVE_EDGE_USED"))
                .count()).isEqualTo(1);
        assertThat(repository.findAll().stream()
                .filter(item -> item.getLayer() == AiStandardLibraryLayer.MISTAKE_POINT)
                .filter(item -> item.getCode().equals("MP_V6_IO_TREATED_T_AS_DATA_VALUE"))
                .count()).isEqualTo(1);
        assertThat(repository.findAll().stream()
                .filter(item -> item.getLayer() == AiStandardLibraryLayer.MISTAKE_POINT)
                .filter(item -> item.getCode().equals("MP_V7_DP_COUNT_EMPTY_SCHEME_OMITTED"))
                .count()).isEqualTo(1);
        assertThat(repository.findAll().stream()
                .filter(item -> item.getLayer() == AiStandardLibraryLayer.MISTAKE_POINT)
                .filter(item -> item.getCode().equals("MP_V7_BFS_MULTI_SOURCE_ENQUEUES_ONLY_ONE_START"))
                .count()).isEqualTo(1);
        assertThat(repository.findAll().stream()
                .filter(item -> item.getLayer() == AiStandardLibraryLayer.MISTAKE_POINT)
                .filter(item -> item.getCode().equals("MP_V7_MULTICASE_GRAPH_NOT_CLEARED_BETWEEN_CASES"))
                .count()).isEqualTo(1);

        seeder.run();

        assertThat(repository.count()).isEqualTo(initialCount);
    }

    @Test
    void generatedEntriesUseDomainSpecificEducationalLanguage() {
        AiStandardLibraryItem prefixSkill = findGeneratedByKnowledge(AiStandardLibraryLayer.SKILL_UNIT,
                "SK_ALGO_PREFIX_SUM",
                "ALGO.PREFIX.SUM.区间查询");
        assertThat(prefixSkill.getName()).contains("区间查询");
        assertThat(prefixSkill.getDescription()).contains("前缀").contains("区间");
        assertThat(prefixSkill.getDescription()).contains("下标");
        assertThat(prefixSkill.getPrimaryKnowledgeNodeCode()).isEqualTo("ALGO.PREFIX.SUM.区间查询");

        AiStandardLibraryItem graphMistake = repository.findByLayerAndCode(AiStandardLibraryLayer.MISTAKE_POINT,
                "MP_GRAPH_UNDIRECTED_EDGE_ADDED_ONCE").orElseThrow();
        assertThat(graphMistake.getDescription()).contains("边");
        assertThat(graphMistake.getCommonMisconception()).contains("无向");
        assertThat(graphMistake.getSkillUnitCode()).isEqualTo("SK_GRAPH_EDGE_MODELING");
        assertThat(graphMistake.getPrimaryKnowledgeNodeCode()).isNotBlank();

        AiStandardLibraryItem windowMistake = findGeneratedByKnowledge(AiStandardLibraryLayer.MISTAKE_POINT,
                "MP_ALGO_TWO_POINTERS_WINDOW",
                "ALGO.TWO_POINTERS.WINDOW.合法性判断");
        assertThat(windowMistake.getMistakeType()).isEqualTo("STATE");
        assertThat(windowMistake.getCommonMisconception()).contains("窗口").contains("答案更新");
        assertThat(windowMistake.getPrimaryKnowledgeNodeCode()).isEqualTo("ALGO.TWO_POINTERS.WINDOW.合法性判断");
        assertThat(windowMistake.getRelatedKnowledgeNodeCodes()).doesNotContain("ALGO.TWO_POINTERS.WINDOW.合法性判断");

        AiStandardLibraryItem integerMistake = findGeneratedByKnowledge(AiStandardLibraryLayer.MISTAKE_POINT,
                "MP_BASIC_TYPE_INTEGER",
                "BASIC.TYPE.INTEGER.整型溢出");
        assertThat(integerMistake.getMistakeType()).isEqualTo("VALUE_RANGE");
        assertThat(integerMistake.getCommonMisconception()).contains("中间结果");
    }

    @Test
    void seedsHaveValidKnowledgeAndSkillReferences() {
        Set<String> knowledgeCodes = InformaticsKnowledgeSeedCatalog.seeds().stream()
                .map(InformaticsKnowledgeSeed::code)
                .collect(Collectors.toSet());
        Set<String> skillCodes = AiStandardLibrarySeedCatalog.seeds().stream()
                .filter(seed -> seed.layer() == AiStandardLibraryLayer.SKILL_UNIT)
                .map(AiStandardLibrarySeed::code)
                .collect(Collectors.toSet());
        Set<String> allowedMistakeTypes = Set.of(
                "BOUNDARY",
                "CE",
                "COMPLEXITY",
                "CONCEPT",
                "DEBUGGING",
                "INITIALIZATION",
                "IO_FORMAT",
                "LOGIC",
                "MODELING",
                "RUNTIME",
                "STATE",
                "SYNTAX",
                "TRANSITION",
                "VALUE_RANGE");

        List<String> errors = new ArrayList<>();
        for (AiStandardLibrarySeed seed : AiStandardLibrarySeedCatalog.seeds()) {
            seed.knowledgeNodeCodes().stream()
                    .filter(code -> !knowledgeCodes.contains(code))
                    .forEach(code -> errors.add(seed.code() + " unknown knowledge node: " + code));
            seed.prerequisiteKnowledgeCodes().stream()
                    .filter(code -> !knowledgeCodes.contains(code))
                    .forEach(code -> errors.add(seed.code() + " unknown prerequisite node: " + code));
            if (seed.layer() == AiStandardLibraryLayer.SKILL_UNIT) {
                if (seed.description() == null || seed.description().isBlank()) {
                    errors.add(seed.code() + " blank skill definition");
                }
                if (seed.studentExplanation() == null || seed.studentExplanation().isBlank()) {
                    errors.add(seed.code() + " blank learning goal");
                }
                if (seed.knowledgeNodeCodes().isEmpty()) {
                    errors.add(seed.code() + " missing skill knowledge nodes");
                }
            }
            if (seed.layer() == AiStandardLibraryLayer.MISTAKE_POINT) {
                if (!skillCodes.contains(seed.skillUnitCode())) {
                    errors.add(seed.code() + " unknown linked skill: " + seed.skillUnitCode());
                }
                if (!allowedMistakeTypes.contains(seed.mistakeType())) {
                    errors.add(seed.code() + " invalid mistake type: " + seed.mistakeType());
                }
                if (seed.commonMisconception() == null || seed.commonMisconception().isBlank()) {
                    errors.add(seed.code() + " blank misconception");
                }
                if (seed.knowledgeNodeCodes().isEmpty()) {
                    errors.add(seed.code() + " missing mistake knowledge nodes");
                }
            }
            if (seed.layer() == AiStandardLibraryLayer.IMPROVEMENT_POINT) {
                if (!skillCodes.contains(seed.skillUnitCode())) {
                    errors.add(seed.code() + " unknown linked improvement skill: " + seed.skillUnitCode());
                }
                if (seed.whenToUse() == null || seed.whenToUse().isBlank()) {
                    errors.add(seed.code() + " blank improvement usage");
                }
                if (seed.studentBenefit() == null || seed.studentBenefit().isBlank()) {
                    errors.add(seed.code() + " blank improvement benefit");
                }
                if (seed.teacherExplanation() == null || seed.teacherExplanation().isBlank()) {
                    errors.add(seed.code() + " blank improvement teacher explanation");
                }
                if (seed.knowledgeNodeCodes().isEmpty()) {
                    errors.add(seed.code() + " missing improvement knowledge nodes");
                }
            }
        }

        assertThat(errors)
                .as("standard library relation errors: " + errors.stream().limit(20).toList())
                .isEmpty();
    }

    @Test
    void seedContentQualityDoesNotRegressToGenericTemplates() {
        long generatedSkillCount = AiStandardLibrarySeedCatalog.seeds().stream()
                .filter(seed -> seed.layer() == AiStandardLibraryLayer.SKILL_UNIT)
                .count();
        long genericSkillNameCount = AiStandardLibrarySeedCatalog.seeds().stream()
                .filter(seed -> seed.layer() == AiStandardLibraryLayer.SKILL_UNIT)
                .filter(seed -> seed.name().startsWith("掌握"))
                .count();
        long genericMistakeNameCount = AiStandardLibrarySeedCatalog.seeds().stream()
                .filter(seed -> seed.layer() == AiStandardLibraryLayer.MISTAKE_POINT)
                .filter(seed -> seed.name().contains("理解或应用偏差"))
                .count();
        long strongHandwrittenSamples = AiStandardLibrarySeedCatalog.seeds().stream()
                .filter(seed -> seed.layer() == AiStandardLibraryLayer.MISTAKE_POINT)
                .filter(seed -> seed.code().startsWith("MP_FUNCTION_")
                        || seed.code().startsWith("MP_BACKTRACK_")
                        || seed.code().startsWith("MP_MATRIX_")
                        || seed.code().startsWith("MP_SORT_")
                        || seed.code().startsWith("MP_UNION_FIND_")
                        || seed.code().startsWith("MP_TOPO_")
                        || seed.code().startsWith("MP_BIT")
                        || seed.code().startsWith("MP_READING_")
                        || seed.code().startsWith("MP_SIM_")
                        || seed.code().startsWith("MP_STRING_")
                        || seed.code().startsWith("MP_ARRAY_")
                        || seed.code().startsWith("MP_V6_")
                        || seed.code().startsWith("MP_V7_")
                        || seed.code().startsWith("MP_V8_"))
                .count();

        assertThat(generatedSkillCount).isGreaterThanOrEqualTo(615);
        assertThat(genericSkillNameCount).isZero();
        assertThat(genericMistakeNameCount).isZero();
        assertThat(strongHandwrittenSamples).isGreaterThanOrEqualTo(116);
    }

    @Test
    void representativeV4SamplesArePreciseAndTeacherReadable() {
        Map<String, AiStandardLibrarySeed> seedsByCode = AiStandardLibrarySeedCatalog.seeds().stream()
                .collect(Collectors.toMap(AiStandardLibrarySeed::code, Function.identity(), (left, right) -> left));

        assertThat(seedsByCode.get("MP_FUNCTION_OUTPUT_RETURN_MIXED").description())
                .contains("print").contains("返回值");
        assertThat(seedsByCode.get("MP_MATRIX_ROW_COL_SWAPPED").commonMisconception())
                .contains("x/y").contains("row/col");
        assertThat(seedsByCode.get("MP_UNION_FIND_PARENT_WITHOUT_FIND").description())
                .contains("parent").contains("find");
        assertThat(seedsByCode.get("MP_TOPO_CYCLE_NOT_CHECKED").description())
                .contains("处理点数").contains("总点数");
        assertThat(seedsByCode.get("MP_READING_OBJECTIVE_MISIDENTIFIED").description())
                .contains("最值").contains("路径");
        assertThat(seedsByCode.get("MP_SIM_MULTI_OBJECT_STATE_DESYNC").description())
                .contains("多个对象").contains("只更新其中一部分");
        assertThat(seedsByCode.get("MP_STRING_FIND_NOT_FOUND_USED_AS_INDEX").description())
                .contains("没有命中").contains("有效位置");
        assertThat(seedsByCode.get("MP_ARRAY_IN_PLACE_UPDATE_OVERWRITES_SOURCE").description())
                .contains("原数组旧值").contains("本轮新状态");
        assertThat(seedsByCode.get("MP_READING_SAMPLE_SHAPE_OVERFITS_FORMAT").description())
                .contains("样例").contains("隐藏数据");
        assertThat(seedsByCode.get("MP_READING_TIE_RULE_IGNORED").description())
                .contains("并列").contains("输出顺序");
        assertThat(seedsByCode.get("MP_V6_BINARY_LOWER_UPPER_BOUND_MIXED").description())
                .contains("大于等于").contains("大于");
        assertThat(seedsByCode.get("MP_V6_DIFF_RIGHT_PLUS_ONE_MISSING").description())
                .contains("r+1").contains("后续所有位置");
        assertThat(seedsByCode.get("MP_V6_FUNCTION_MISSING_RETURN_ON_BRANCH").description())
                .contains("部分条件").contains("其他合法分支");
        assertThat(seedsByCode.get("MP_V7_DP_COUNT_EMPTY_SCHEME_OMITTED").description())
                .contains("空方案").contains("初始化为 1");
        assertThat(seedsByCode.get("MP_V7_LONG_LONG_CAST_AFTER_MULTIPLICATION").description())
                .contains("int").contains("long long");
        assertThat(seedsByCode.get("MP_V7_MULTICASE_GRAPH_NOT_CLEARED_BETWEEN_CASES").description())
                .contains("邻接表").contains("旧边");
        assertThat(seedsByCode.get("MP_V8_GRAPH_MULTICASE_ADJACENCY_NOT_CLEARED").description())
                .contains("邻接表").contains("上一组");
        assertThat(seedsByCode.get("MP_V8_SUBMIT_OVERFLOW_CHECK_ONLY_ON_FINAL_ANSWER").description())
                .contains("long long").contains("中间表达式");
        assertThat(seedsByCode.get("IP_V8_ARRAY_UPDATE_INVARIANT_TESTING").studentBenefit())
                .contains("旧值").contains("累计量");
    }

    private AiStandardLibraryItem findGeneratedByKnowledge(AiStandardLibraryLayer layer, String codePrefix, String knowledgeCode) {
        return repository.findAll().stream()
                .filter(item -> item.getLayer() == layer)
                .filter(item -> item.getCode().startsWith(codePrefix))
                .filter(item -> item.getKnowledgeNodeCodes() != null && item.getKnowledgeNodeCodes().contains(knowledgeCode))
                .findFirst()
                .orElseThrow();
    }
}
