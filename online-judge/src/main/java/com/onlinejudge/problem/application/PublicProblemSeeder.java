package com.onlinejudge.problem.application;

import com.onlinejudge.problem.domain.Problem;
import com.onlinejudge.problem.domain.TestCase;
import com.onlinejudge.problem.persistence.ProblemRepository;
import com.onlinejudge.problem.persistence.TestCaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
public class PublicProblemSeeder implements CommandLineRunner {

    private final ProblemRepository problemRepository;
    private final TestCaseRepository testCaseRepository;

    @Override
    public void run(String... args) {
        Set<String> existingTitles = problemRepository.findAllByOrderByIdAsc()
                .stream()
                .map(Problem::getTitle)
                .collect(Collectors.toSet());
        for (PublicProblemSeed seed : PublicProblemSeedCatalog.seeds()) {
            if (existingTitles.contains(seed.title())) {
                continue;
            }
            Problem problem = problemRepository.save(Problem.builder()
                    .title(seed.title())
                    .description(seed.description())
                    .difficulty(seed.difficulty())
                    .timeLimit(seed.timeLimit())
                    .memoryLimit(seed.memoryLimit())
                    .aiPromptDirection(seed.aiPromptDirection())
                    .starterCode(normalizeStarterCode(seed.starterCode()))
                    .knowledgePoints(seed.knowledgePoints())
                    .algorithmStrategies(seed.algorithmStrategies())
                    .commonMistakes(seed.commonMistakes())
                    .boundaryTypes(seed.boundaryTypes())
                    .build());
            saveTestCases(problem.getId(), seed.testCases());
            existingTitles.add(seed.title());
        }
    }

    private void saveTestCases(Long problemId, List<PublicProblemSeed.TestCaseSeed> testCases) {
        IntStream.range(0, testCases.size())
                .mapToObj(index -> {
                    PublicProblemSeed.TestCaseSeed testCase = testCases.get(index);
                    return TestCase.builder()
                            .problemId(problemId)
                            .input(testCase.input())
                            .expectedOutput(testCase.expectedOutput())
                            .isHidden(testCase.hidden())
                            .orderIndex(index)
                            .build();
                })
                .forEach(testCaseRepository::save);
    }

    private String normalizeStarterCode(String starterCode) {
        if (starterCode == null || starterCode.isBlank()) {
            return null;
        }
        return starterCode.replace("\r\n", "\n").replace('\r', '\n').stripTrailing() + "\n";
    }
}
