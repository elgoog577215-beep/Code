import { constants } from "node:fs";
import { access, mkdir } from "node:fs/promises";
import { createServer } from "node:net";
import { join, resolve } from "node:path";
import { spawn } from "node:child_process";
import { chromium } from "playwright";

const frontendDir = resolve(import.meta.dirname, "..");
const repoDir = resolve(frontendDir, "..");
const appDir = resolve(import.meta.dirname, process.env.BROWSER_SMOKE_APP_DIR || "../../src/main/resources/static/app");
const indexPath = join(appDir, "index.html");
const artifactDir = resolve(repoDir, "output/playwright");
const viteBin = join(frontendDir, "node_modules/vite/bin/vite.js");

const assignment = {
  id: 7,
  title: "课堂编程作业",
  description: "循环边界与调试练习",
  classGroupId: 3,
  className: "高一1班",
  hintPolicy: "L2",
  status: "ACTIVE",
  inviteCode: "WZAI01",
  tasks: [
    { problemId: 101, title: "求和边界", difficulty: "EASY", orderIndex: 1, required: true },
    { problemId: 102, title: "循环边界", difficulty: "MEDIUM", orderIndex: 2, required: true }
  ]
};

const studentAssignments = [
  assignment,
  {
    ...assignment,
    id: 8,
    title: "AI闭环复测作业",
    endsAt: "2026-07-15T23:59:00",
    tasks: [{ problemId: 101, title: "求和边界", difficulty: "EASY", orderIndex: 1, required: true }]
  },
  {
    ...assignment,
    id: 9,
    title: "AI闭环测试作业",
    endsAt: "2026-07-18T23:59:00",
    tasks: [{ problemId: 102, title: "循环边界", difficulty: "MEDIUM", orderIndex: 1, required: true }]
  },
  {
    ...assignment,
    id: 10,
    title: "课堂算法练习",
    status: "CLOSED",
    tasks: [
      { problemId: 101, title: "求和边界", difficulty: "EASY", orderIndex: 1, required: true },
      { problemId: 102, title: "循环边界", difficulty: "MEDIUM", orderIndex: 2, required: true },
      { problemId: 103, title: "数组统计", difficulty: "MEDIUM", orderIndex: 3, required: true }
    ]
  }
];

const student = {
  id: 41,
  classGroupId: 3,
  className: "高一1班",
  displayName: "学生甲",
  studentNo: "12"
};

const coachImpact = {
  coachedSubmissionId: 9001,
  followupSubmissionId: 9002,
  problemId: 101,
  status: "FOLLOWUP_ACCEPTED",
  statusLabel: "追问后通过",
  summary: "学生回答追问后的下一次同题提交已通过，说明这轮追问可能帮助其完成了关键修正。",
  previousVerdict: "WRONG_ANSWER",
  followupVerdict: "ACCEPTED",
  previousIssueTag: "BOUNDARY",
  previousFineGrainedTag: "OFF_BY_ONE",
  followupIssueTag: null,
  followupFineGrainedTag: null,
  answeredAt: "2026-05-19T09:05:00",
  followupSubmittedAt: "2026-05-19T09:16:00"
};

const trajectory = {
  assignment,
  student,
  totalTasks: 2,
  completedTasks: 1,
  totalAttempts: 4,
  stageTransition: "还需修正",
  repeatedIssueTag: "BOUNDARY",
  repeatedFineGrainedTag: "OFF_BY_ONE",
  repeatedIssueCount: 2,
  nextStep: "用最小输入复查循环边界。",
  attentionReason: "最近两次提交都卡在边界样例。",
  improvementSignal: "最新回答已经把问题缩小到循环边界。",
  primaryAbilityFocus: "循环与边界",
  crossProblemSummary: "错误集中在循环端点。",
  latestCoachInteraction: {
    submissionId: 9001,
    turnCount: 1,
    answeredTurnCount: 1,
    prompted: true,
    answered: true,
    status: "ANSWERED",
    statusLabel: "已回答追问",
    summary: "学生说明了边界判断。",
    latestQuestion: "当 n 为 1 时会发生什么？",
    latestAnswer: "循环仍然应该执行一次。",
    latestFeedback: "可以。接着检查最小输入分支。",
    impact: coachImpact
  },
  latestCoachImpact: coachImpact,
  recentIssueDistribution: [{ label: "BOUNDARY", count: 2 }],
  recentFineGrainedIssueDistribution: [{ label: "OFF_BY_ONE", count: 2 }],
  abilitySummary: [{ abilityPoint: "循环与边界", taskCount: 2, submissionCount: 4, evidenceTags: ["OFF_BY_ONE"] }],
  tasks: [
    {
      problemId: 101,
      title: "求和边界",
      difficulty: "EASY",
      attemptCount: 3,
      passed: false,
      latestVerdict: "WRONG_ANSWER",
      latestProgressSignal: "已定位边界问题",
      latestHint: "检查循环是否包含最后一个数。",
      latestImprovementSignal: "第二次提交已修正输入读取。",
      latestCoachInteraction: {
        submissionId: 9001,
        turnCount: 1,
        answeredTurnCount: 1,
        prompted: true,
        answered: true,
        status: "ANSWERED",
        statusLabel: "已回答追问",
        summary: "学生回答了边界问题。",
        impact: coachImpact
      },
      latestCoachImpact: coachImpact,
      submissions: [
        {
          submissionId: 9001,
          verdict: "WRONG_ANSWER",
          submittedAt: "2026-05-19T09:00:00",
          issueTags: ["BOUNDARY"],
          fineGrainedTags: ["OFF_BY_ONE"],
          progressSignal: "已定位边界问题",
          improvementSignal: "输入读取已修正",
          coachInteraction: {
            submissionId: 9001,
            turnCount: 1,
            answeredTurnCount: 1,
            prompted: true,
            answered: true,
            status: "ANSWERED",
            statusLabel: "已回答追问",
            summary: "学生回答了边界问题。",
            impact: coachImpact
          },
          coachImpact
        }
      ]
    },
    {
      problemId: 102,
      title: "循环边界",
      difficulty: "MEDIUM",
      attemptCount: 1,
      passed: true,
      latestVerdict: "ACCEPTED",
      latestProgressSignal: "已通过",
      latestHint: "保留这次通过的测试作为模板。",
      submissions: []
    }
  ]
};

const assignmentLeaderboard = {
  assignmentId: 7,
  totalStudents: 10,
  totalTasks: 2,
  myRank: 4,
  tiedStudentCount: 3,
  rankingRule: "PROGRESS_ONLY_V1",
  generatedAt: "2026-07-11T11:42:00",
  rows: [
    { rank: 1, displayName: "王同学", completedTasks: 2, totalTasks: 2, attemptCount: 3, lastSubmittedAt: "2026-07-11T10:28:00", currentStudent: false },
    { rank: 1, displayName: "李同学", completedTasks: 2, totalTasks: 2, attemptCount: 5, lastSubmittedAt: "2026-07-11T10:05:00", currentStudent: false },
    { rank: 1, displayName: "陈同学", completedTasks: 2, totalTasks: 2, attemptCount: 4, lastSubmittedAt: "2026-07-11T09:45:00", currentStudent: false },
    { rank: 4, studentProfileId: 41, displayName: "学生甲（我）", completedTasks: 1, totalTasks: 2, attemptCount: 4, lastSubmittedAt: "2026-07-11T09:16:00", currentStudent: true },
    { rank: 4, displayName: "林同学", completedTasks: 1, totalTasks: 2, attemptCount: 2, lastSubmittedAt: "2026-07-10T20:20:00", currentStudent: false },
    { rank: 4, displayName: "周同学", completedTasks: 1, totalTasks: 2, attemptCount: 6, lastSubmittedAt: "2026-07-10T19:15:00", currentStudent: false },
    { rank: 7, displayName: "吴同学", completedTasks: 0, totalTasks: 2, attemptCount: 1, lastSubmittedAt: "2026-07-10T18:40:00", currentStudent: false },
    { rank: 7, displayName: "郑同学", completedTasks: 0, totalTasks: 2, attemptCount: 0, lastSubmittedAt: null, currentStudent: false },
    { rank: 7, displayName: "孙同学", completedTasks: 0, totalTasks: 2, attemptCount: 0, lastSubmittedAt: null, currentStudent: false },
    { rank: 7, displayName: "赵同学", completedTasks: 0, totalTasks: 2, attemptCount: 0, lastSubmittedAt: null, currentStudent: false }
  ]
};

const assignmentSubmissionItems = Array.from({ length: 12 }, (_, index) => {
  const accepted = index % 3 === 0;
  const problemId = index % 2 === 0 ? 101 : 102;
  return {
    id: 9001 + index,
    problemId,
    problemTitle: problemId === 101 ? "求和边界" : "循环边界",
    verdict: accepted ? "ACCEPTED" : index % 3 === 1 ? "WRONG_ANSWER" : "RUNTIME_ERROR",
    languageName: index % 2 === 0 ? "Python 3" : "C++17",
    executionTime: accepted ? 0.032 : 0.047,
    memoryUsed: index === 0 ? 0 : 8192 + index * 128,
    submittedAt: `2026-07-${String(11 - Math.floor(index / 4)).padStart(2, "0")}T${String(11 - (index % 4)).padStart(2, "0")}:16:00`
  };
});

function assignmentSubmissionPage(url) {
  const page = Number(url.searchParams.get("page") || 0);
  const size = Number(url.searchParams.get("size") || 8);
  const accepted = url.searchParams.get("accepted");
  const problemId = url.searchParams.get("problemId");
  const languageName = url.searchParams.get("languageName");
  const submissionId = url.searchParams.get("submissionId");
  const filtered = assignmentSubmissionItems.filter(item =>
    (!accepted || String(item.verdict === "ACCEPTED") === accepted) &&
    (!problemId || String(item.problemId) === problemId) &&
    (!languageName || item.languageName === languageName) &&
    (!submissionId || String(item.id) === submissionId)
  );
  return {
    assignmentId: 7,
    totalSubmissionCount: 12,
    acceptedSubmissionCount: 4,
    distinctProblemCount: 2,
    latestSubmittedAt: assignmentSubmissionItems[0].submittedAt,
    totalElements: filtered.length,
    totalPages: Math.ceil(filtered.length / size),
    page,
    size,
    items: filtered.slice(page * size, (page + 1) * size)
  };
}

const abilityProfile = {
  student,
  mergedStudentProfileIds: [41],
  totalSubmissions: 7,
  problemCount: 3,
  assignmentCount: 2,
  failedSubmissionCount: 4,
  primaryAbilityFocus: "循环与边界",
  summary: "近期证据指向差一位错误，不是语法问题。",
  trendSignal: "明确检查边界后正确率提升。",
  recommendationEffectSummary: "后续练习已有一次通过。",
  coachImpactSummary: "AI 追问后已有 1 次同题后续提交通过，建议复盘学生回答中有效的证据意识。",
  latestCoachInteraction: trajectory.latestCoachInteraction,
  latestCoachImpact: coachImpact,
  abilityGaps: [{ abilityPoint: "循环与边界", taskCount: 2, submissionCount: 4, evidenceTags: ["OFF_BY_ONE"] }],
  knowledgeFocus: [{ label: "for 循环", count: 3, evidenceProblemIds: [101] }],
  commonMistakeFocus: [{ label: "差一位错误", count: 2, evidenceProblemIds: [101] }],
  boundaryFocus: [{ label: "n 等于 1", count: 2, evidenceProblemIds: [101] }]
};

const recommendation = {
  student,
  summary: "做一道同类题，并在写代码前说明边界。",
  recommendations: [
    {
      type: "NEXT_PROBLEM",
      title: "做一道同类边界题",
      reason: "它针对同一类差一位问题。",
      actionLabel: "开始练习",
      problemId: 101,
      problemTitle: "求和边界",
      focusAbility: "循环与边界",
      focusTags: ["OFF_BY_ONE", "EDGE_CASE"],
      evidenceProblemIds: [101],
      recommendationToken: "rec-next-101",
      priority: 1
    }
  ]
};

const problem = {
  id: 101,
  title: "求和边界",
  description: "# 求和边界\n读入 n 个整数，输出它们的和。注意最小有效输入。",
  difficulty: "EASY",
  timeLimit: 1000,
  memoryLimit: 65536,
  aiPromptDirection: "先引导学生手推边界，再给出修改方向。",
  knowledgePoints: ["循环", "输入读取"],
  algorithmStrategies: ["手推模拟"],
  commonMistakes: ["差一位错误"],
  boundaryTypes: ["n 等于 1"],
  sampleTestCases: [{ input: "3\n1 2 3\n", expectedOutput: "6\n" }]
};

const submissionResult = {
  id: 9001,
  problemId: 101,
  assignmentId: 7,
  studentProfileId: 41,
  problemTitle: "求和边界",
  languageId: 54,
  languageName: "C++17",
  sourceCode: "#include <bits/stdc++.h>\nusing namespace std;\n\nint main() {\n    ios::sync_with_stdio(false);\n    cin.tie(nullptr);\n    int n;\n    cin >> n;\n    cout << n << '\\n';\n    return 0;\n}\n",
  verdict: "WRONG_ANSWER",
  executionTime: 28,
  memoryUsed: 8192,
  output: "3\n",
  compileOutput: null,
  errorMessage: null,
  submittedAt: "2026-05-19T09:03:00",
  analysisStatus: "READY",
  analysis: {
    submissionId: 9001,
    sourceType: "AI",
    scenario: "WRONG_ANSWER",
    headline: "边界情况还未覆盖",
    summary: "代码读到了数量，但没有继续读取并求和后面的数。",
    issueTags: ["BOUNDARY"],
    abilityPoints: ["循环与边界"],
    focusPoints: ["n 等于 1", "循环体"],
    teacherNote: "适合用输入格式追问。",
    progressSignal: "问题集中在输入处理。",
    confidence: 0.68,
    fineGrainedTags: ["OFF_BY_ONE"],
    evidenceRefs: ["public-case-1"],
    uncertainty: "也可能是输入格式理解偏差。",
    answerLeakRisk: "LOW",
    firstFailedCase: {
      testCaseNumber: 1,
      hidden: false,
      input: "3\n1 2 3\n",
      expectedOutput: "6\n",
      actualOutput: "3\n"
    }
  },
  testCaseResults: [
    { testCaseNumber: 1, passed: false, actualOutput: "3\n", expectedOutput: "6\n", executionTime: 28, memoryUsed: 8192, hidden: false },
    { testCaseNumber: 2, passed: true, actualOutput: "1\n", expectedOutput: "1\n", executionTime: 19, memoryUsed: 8192, hidden: true }
  ]
};

const submissionPendingResult = {
  ...submissionResult,
  analysisStatus: "PROCESSING",
  analysis: null
};

const studentAiFeedbackGenerating = {
  status: "GENERATING",
  feedback: {
    submissionId: 9001,
    status: "GENERATING",
    source: "MODEL",
    repairItems: [],
    improvementItems: [],
    safety: { answerLeakRisk: "LOW", blockedReasons: [] },
    evidenceRefs: []
  }
};

const studentAiFeedbackReady = {
  status: "READY",
  feedback: {
    submissionId: 9001,
    status: "READY",
    source: "MODEL",
    generatedAt: "2026-05-19T09:03:02",
    latencyMs: 820,
    repairItems: [
      {
        title: "输入没有完整读取",
        body: "先把公开样例的每一行 input 手推清楚。",
        kind: "INPUT_FORMAT",
        mistakePointId: "MP_INPUT",
        normalizedPointKey: "mistake:point-key-v1:mp-input",
        pointKeySource: "FORMAL_ID",
        changeStatus: "PERSISTED",
        personalLabels: ["PERSISTENT_DIFFICULTY"],
        rawOccurrenceCount: 4,
        effectiveOccurrenceCount: 3,
        consecutiveEffectiveCount: 3,
        affectedProblemCount: 1,
        lifecycleEvidenceSubmissionIds: [8998, 8999, 9001],
        knowledgePath: ["信息学基础", "输入输出", "输入格式", "输入读取", "漏读变量"],
        knowledgePathStatus: "FORMAL",
        evidenceSnippets: [{ evidenceRef: "code:line:2", lineNumber: 2, lineEnd: 2, code: "numbers = list(map(int, input().split()))" }],
        evidenceRefs: ["code:line:2", "public-case-1"],
        qualitySignals: ["evidence_grounded", "actionable"]
      },
      {
        title: "累计结果输出时机不稳定",
        body: "先逐轮记录累计值，再确认输出发生在所有元素都处理完之后。",
        kind: "REPAIR",
        normalizedPointKey: "text:point-key-v1:output-timing",
        pointKeySource: "TEXT_FINGERPRINT",
        changeStatus: "NEW",
        personalLabels: ["SINGLE_OBSERVATION"],
        rawOccurrenceCount: 1,
        effectiveOccurrenceCount: 1,
        consecutiveEffectiveCount: 1,
        affectedProblemCount: 1,
        knowledgePath: ["信息学基础", "循环结构", "循环状态", "累计结果输出时机不稳定"],
        knowledgePathStatus: "PROVISIONAL",
        provisionalNodeCode: "MP_AI_OUTPUT_TIMING",
        evidenceRefs: ["judge:first_failed_case:1"],
        qualitySignals: ["evidence_grounded", "actionable"]
      }
    ],
    improvementItems: [
      {
        title: "测试习惯",
        body: "提交前先跑最小公开样例。能更早发现输入格式问题。",
        kind: "TESTING_HABIT",
        normalizedPointKey: "text:point-key-v1:testing-habit",
        pointKeySource: "TEXT_FINGERPRINT",
        changeStatus: "PERSISTED",
        personalLabels: ["OBSERVING"],
        rawOccurrenceCount: 2,
        effectiveOccurrenceCount: 2,
        consecutiveEffectiveCount: 2,
        affectedProblemCount: 2,
        knowledgePath: ["竞赛过程", "提交检查", "边界复测"],
        knowledgePathStatus: "INFERRED",
        evidenceRefs: ["public-case-1"],
        qualitySignals: ["transfer"]
      },
      {
        title: "迁移到多组输入",
        body: "基础问题修正后，再检查多组输入时每轮状态是否重新初始化。",
        kind: "IMPROVEMENT",
        normalizedPointKey: "text:point-key-v1:multi-case",
        pointKeySource: "TEXT_FINGERPRINT",
        changeStatus: "NEW",
        personalLabels: ["SINGLE_OBSERVATION"],
        rawOccurrenceCount: 1,
        effectiveOccurrenceCount: 1,
        consecutiveEffectiveCount: 1,
        affectedProblemCount: 1,
        knowledgePath: [],
        knowledgePathStatus: "UNCLASSIFIED",
        evidenceRefs: [],
        qualitySignals: ["transfer"]
      }
    ],
    issueChanges: [
      {
        normalizedPointKey: "mistake:point-key-v1:mp-input",
        pointKeySource: "FORMAL_ID",
        title: "输入没有完整读取",
        factType: "REPAIR",
        displayCategory: "REPAIR",
        changeStatus: "PERSISTED",
        personalLabels: ["PERSISTENT_DIFFICULTY"],
        rawOccurrenceCount: 4,
        effectiveOccurrenceCount: 3,
        consecutiveEffectiveCount: 3,
        affectedProblemCount: 1,
        effectiveAttempt: true,
        previousSubmissionId: 8999,
        currentSubmissionId: 9001,
        evidenceSubmissionIds: [8998, 8999, 9001]
      },
      {
        normalizedPointKey: "mistake:point-key-v1:mp-boundary",
        pointKeySource: "FORMAL_ID",
        title: "循环边界漏掉末项",
        factType: "REPAIR",
        displayCategory: "REPAIR",
        changeStatus: "RECOVERED",
        personalLabels: ["RECOVERED"],
        rawOccurrenceCount: 2,
        effectiveOccurrenceCount: 2,
        consecutiveEffectiveCount: 0,
        affectedProblemCount: 1,
        effectiveAttempt: true,
        previousSubmissionId: 8999,
        currentSubmissionId: 9001,
        evidenceSubmissionIds: [8997, 8999]
      }
    ],
    issueChangeSummary: {
      persistedCount: 2,
      newCount: 2,
      recurringCount: 0,
      notObservedCount: 0,
      recoveredCount: 1,
      uncomparableCount: 0,
      improvementCount: 2
    },
    nextQuestion: "第二次读取时应该拿到 1 2 3，还是已经没有读取？",
    safety: { answerLeakRisk: "LOW", blockedReasons: [] },
    evidenceRefs: ["public-case-1"]
  }
};

const coachPrompt = {
  id: 7001,
  assignmentId: 7,
  studentProfileId: 41,
  submissionId: 9001,
  parentPromptId: null,
  turnIndex: 1,
  hintPolicy: "L2",
  promptType: "NEXT_QUESTION",
  question: "如果 n 是 3，循环应该读取哪三个数？",
  rationale: "让学生重新确认输入格式。",
  contextSummary: "公开失败点显示程序输出了数量而不是和。",
  evidenceRefs: ["public-case-1"],
  turns: []
};

const history = [
  {
    id: 9001,
    problemId: 101,
    assignmentId: 7,
    studentProfileId: 41,
    problemTitle: "求和边界",
    languageId: 54,
    languageName: "C++17",
    verdict: "WRONG_ANSWER",
    executionTime: 28,
    memoryUsed: 8192,
    submittedAt: "2026-05-19T09:03:00",
    passedTestCases: 1,
    totalTestCases: 2,
    analysisStatus: "READY",
    analysisSourceType: "AI",
    analysisHeadline: "边界情况还未覆盖",
    analysisSummary: "代码只读取了数量，没有读取数值。",
    feedbackStatus: "READY",
    feedbackSource: "MODEL",
    feedbackRevisionId: 5001,
    dataCompletenessStatus: "COMPLETE"
  }
];

const classes = [{ id: 3, name: "高一1班", grade: "10", teacherName: "信息技术老师" }];

const problemCatalog = [
  { id: 101, title: "求和边界", summary: "练习输入循环。", difficulty: "EASY", timeLimit: 1000, memoryLimit: 65536 },
  { id: 102, title: "循环边界", summary: "练习循环端点。", difficulty: "MEDIUM", timeLimit: 1000, memoryLimit: 65536 }
];

function pathStat(granularity, label, labels, count) {
  const kinds = ["chapter", "knowledgePoint", "skillUnit", "mistakePoint"];
  return {
    id: `${granularity}:FORMAL:${labels.join("/")}`,
    label,
    granularity,
    normalizedIssueId: granularity === "mistakePoint" ? "OFF_BY_ONE" : null,
    path: labels.map((item, index) => ({ label: item, kind: kinds[index] })),
    pathStatus: "FORMAL",
    libraryFit: "HIT",
    source: "AI_PROJECTION",
    errorOccurrenceCount: count,
    rawOccurrenceCount: count,
    effectiveWeightedOccurrenceCount: Math.max(1, count - 2),
    affectedStudentCount: 4,
    repeatedStudentCount: 2,
    unresolvedStudentCount: 3,
    recurringStudentCount: 2,
    recoveredStudentCount: 1,
    recoveryNumerator: 1,
    recoveryDenominator: 4,
    recoveryRate: 0.25,
    difficultyClassification: "CLASS_DIFFICULTY",
    affectedProblemCount: 1,
    affectedStudentIds: [41, 42, 43, 44],
    repeatedStudentIds: [41, 42],
    affectedProblemIds: [101],
    evidenceSubmissionIds: [9001],
    evidenceSamples: [{ submissionId: 9001, studentProfileId: 41, problemId: 101, verdict: "WRONG_ANSWER", submittedAt: "2026-05-19T09:03:00" }]
  };
}

const assignmentOverview = {
  assignment,
  rosterStudentCount: 18,
  participantCount: 18,
  submittedStudentCount: 18,
  unsubmittedStudentCount: 0,
  attemptCount: 32,
  passedAttemptCount: 15,
  studentPassRate: 0.61,
  attemptPassRate: 0.4688,
  dataCompleteness: {
    totalSubmissionCount: 32,
    legalIdentityCount: 32,
    identityMissingCount: 0,
    invalidContextCount: 0,
    analysisReadyCount: 30,
    analysisMissingCount: 2,
    diagnosisFactCount: 24,
    diagnosedSubmissionCount: 24,
    unclassifiedFactCount: 2,
    feedbackEventSubmissionCount: 22,
    completeSubmissionCount: 20,
    completeRate: 0.625
  },
  knowledgePathStats: [
    pathStat("chapter", "基础语法", ["基础语法"], 8),
    pathStat("knowledgePoint", "循环结构", ["基础语法", "循环结构"], 8),
    pathStat("skillUnit", "边界处理", ["基础语法", "循环结构", "边界处理"], 8),
    pathStat("mistakePoint", "差一位错误", ["基础语法", "循环结构", "边界处理", "差一位错误"], 8)
  ],
  recoverySummary: {
    recoveryNumerator: 2,
    recoveryDenominator: 4,
    comparableSampleCount: 4,
    recoveredCount: 2,
    sameIssueCount: 1,
    shiftedCount: 0,
    regressedCount: 0,
    verdictChangedCount: 1,
    noClearChangeCount: 0,
    awaitingFollowupCount: 2,
    feedbackViewedComparableCount: 2,
    feedbackViewedRecoveredCount: 1,
    recoveryRate: 0.5,
    feedbackViewedRecoveryRate: 0.5,
    evidence: []
  },
  strugglingStudentCount: 4,
  topIssues: [
    {
      label: "BOUNDARY",
      count: 8,
      explanation: "学生容易混淆是否包含端点。",
      abilityPoint: "循环与边界",
      recommendedHintPolicy: "L2",
      interventionSuggestion: "先让学生手推最小输入，再修改代码。"
    }
  ],
  classAbilityWeaknesses: [{ abilityPoint: "循环与边界", taskCount: 2, submissionCount: 12, evidenceTags: ["OFF_BY_ONE"] }],
  coachAnswerQualitySummary: {
    promptedCount: 4,
    answeredCount: 3,
    verifiableCount: 2,
    transferReadyCount: 1,
    evidenceInsufficientCount: 1,
    safetyRiskCount: 1,
    teacherAttentionCount: 2,
    dominantGap: "SAFETY_RISK",
    summary: "有 1 个 Coach 回答疑似越过证据层或需要教师关注。",
    recommendedAction: "先示范如何描述最小样例、输出对比或变量轨迹，避免直接给改法。",
    evidenceRefs: ["coach-submission:9001", "coach-submission:9002"]
  },
  coachFollowupImpactSummary: {
    impactedCount: 4,
    acceptedCount: 1,
    shiftedCount: 1,
    sameIssueCount: 1,
    verdictChangedCount: 0,
    noClearChangeCount: 1,
    awaitingFollowupCount: 1,
    dominantOutcome: "SAME_ISSUE",
    summary: "有 1 个 Coach 追问后仍卡同类问题。",
    recommendedAction: "降低追问颗粒度，补一个最小失败样例或让教师检查学生证据。",
    evidenceRefs: ["coach-impact:SAME_ISSUE:submission:9001", "followup-submission:9002"]
  },
  classReviewSuggestions: [
    {
      title: "下次练习前复盘最小输入",
      suggestionKey: "review:7:ability:boundary-reasoning:101:off-by-one",
      targetAbility: "循环与边界",
      exampleProblemId: 101,
      exampleProblemTitle: "求和边界",
      evidenceTags: ["OFF_BY_ONE"],
      evidenceSubmissionIds: [9001],
      guidingQuestion: "当 n 为 1 时，循环应该处理哪些值？",
      action: "使用一个公开样例和一个教师补充边界样例。",
      evidenceSummary: "4 名学生重复出现端点错误。",
      latestFeedback: { actionType: "MODIFIED", teacherNote: "先使用更小的边界样例。", createdBy: "teacher", createdAt: "2026-05-19T10:00:00" }
    }
  ],
  problemSummaries: [
    {
      problemId: 101,
      title: "求和边界",
      difficulty: "EASY",
      orderIndex: 1,
      submittedStudentCount: 12,
      submissionCount: 20,
      passedStudentCount: 5,
      passedAttemptCount: 8,
      submissionRate: 66.7,
      passRate: 40,
      studentPassRate: 0.4167,
      attemptPassRate: 0.4,
      averageAttempts: 1.7,
      attentionStudentCount: 2,
      statusLabel: "需讲评",
      dataCompleteness: { totalSubmissionCount: 20, legalIdentityCount: 20, completeSubmissionCount: 14, completeRate: 0.7 },
      knowledgePathStats: [
        pathStat("chapter", "基础语法", ["基础语法"], 5),
        pathStat("knowledgePoint", "循环结构", ["基础语法", "循环结构"], 5),
        pathStat("skillUnit", "边界处理", ["基础语法", "循环结构", "边界处理"], 5),
        pathStat("mistakePoint", "差一位错误", ["基础语法", "循环结构", "边界处理", "差一位错误"], 5)
      ],
      recoverySummary: { recoveryNumerator: 1, recoveryDenominator: 2, comparableSampleCount: 2, recoveryRate: 0.5 },
      topIssues: [{ label: "BOUNDARY", count: 5, explanation: "端点处理错误。", abilityPoint: "循环与边界" }],
      abilityWeaknesses: [{ abilityPoint: "循环与边界", taskCount: 1, submissionCount: 20, evidenceTags: ["OFF_BY_ONE"] }],
      students: [
        {
          studentProfileId: 41,
          displayName: "学生甲",
          studentNo: "12",
          attemptCount: 3,
          passedCount: 1,
          latestSubmissionId: 9001,
          latestVerdict: "WRONG_ANSWER",
          latestSubmittedAt: "2026-05-19T09:03:00",
          latestIssueTag: "BOUNDARY",
          latestFineGrainedIssue: "OFF_BY_ONE",
          latestProgressSignal: "已定位边界问题",
          latestConfidence: 0.68,
          recentLearningState: {
            status: "RECENTLY_RECOVERED",
            evidenceStatus: "OBSERVED",
            independentSubmissionCount: 3,
            problemCount: 1,
            repeatedIssueId: "OFF_BY_ONE",
            repeatedIssueCount: 2,
            repeatedIssueProblemCount: 1,
            latestChangeStatus: "RECOVERED",
            evidenceSubmissionIds: [9001, 9002]
          },
          latestAiFeedbackImpact: {
            feedbackSubmissionId: 9001,
            followupSubmissionId: 9002,
            problemId: 101,
            status: "IMPROVED_AFTER_AI",
            statusLabel: "查看建议后改善",
            summary: "学生查看建议后，同题下一次提交已通过；这是观察到改善的相关证据，但不能单独证明由建议造成。",
            feedbackStatus: "READY",
            feedbackViewedAt: "2026-05-19T09:05:00",
            previousVerdict: "WRONG_ANSWER",
            followupVerdict: "ACCEPTED",
            needsTeacherAttention: false
          },
          needsAttention: true
        }
      ]
    },
    {
      problemId: 102,
      title: "循环边界",
      difficulty: "MEDIUM",
      orderIndex: 2,
      submittedStudentCount: 10,
      submissionCount: 12,
      passedStudentCount: 10,
      passedAttemptCount: 7,
      submissionRate: 55.6,
      passRate: 83.3,
      studentPassRate: 1,
      attemptPassRate: 0.5833,
      averageAttempts: 1.2,
      attentionStudentCount: 0,
      statusLabel: "已掌握",
      dataCompleteness: { totalSubmissionCount: 12, legalIdentityCount: 12, completeSubmissionCount: 6, completeRate: 0.5 },
      knowledgePathStats: [],
      recoverySummary: { recoveryNumerator: 0, recoveryDenominator: 0, comparableSampleCount: 0, recoveryRate: null },
      topIssues: [],
      abilityWeaknesses: [],
      students: [
        {
          studentProfileId: 41,
          displayName: "学生甲",
          studentNo: "12",
          attemptCount: 1,
          passedCount: 1,
          latestSubmissionId: 9002,
          latestVerdict: "ACCEPTED",
          latestSubmittedAt: "2026-05-19T09:16:00",
          latestProgressSignal: "已通过",
          needsAttention: false
        }
      ]
    }
  ],
  students: [
    {
      studentProfileId: 41,
      displayName: "学生甲",
      studentNo: "12",
      attemptCount: 3,
      passedCount: 1,
      latestSubmissionId: 9001,
      latestVerdict: "WRONG_ANSWER",
      latestIssue: "BOUNDARY",
      latestIssueTag: "BOUNDARY",
      latestFineGrainedIssue: "OFF_BY_ONE",
      latestProgressSignal: "已定位边界问题",
      latestConfidence: 0.68,
      latestUncertainty: "可能是输入格式理解偏差。",
      latestAnswerLeakRisk: "LOW",
      latestCorrection: null,
      latestCoachInteraction: trajectory.latestCoachInteraction,
      latestCoachImpact: coachImpact,
      primaryAbilityFocus: "循环与边界",
      crossProblemSummary: "两道题都出现端点错误。",
      abilitySummary: [{ abilityPoint: "循环与边界", taskCount: 2, submissionCount: 4, evidenceTags: ["OFF_BY_ONE"] }],
      repeatedIssueTag: "BOUNDARY",
      repeatedFineGrainedTag: "OFF_BY_ONE",
      repeatedIssueCount: 2,
      attentionReason: "公开和隐藏边界样例都出现错误。",
      attentionEvidence: [
        {
          submissionId: 9001,
          problemId: 101,
          verdict: "WRONG_ANSWER",
          submittedAt: "2026-05-19T09:03:00",
          issueTag: "BOUNDARY",
          fineGrainedTag: "OFF_BY_ONE",
          abilityPoint: "循环与边界",
          headline: "边界情况还未覆盖",
          reason: "公开样例未通过。"
        }
      ],
      recentLearningState: {
        status: "RECENTLY_RECOVERED",
        evidenceStatus: "OBSERVED",
        independentSubmissionCount: 4,
        problemCount: 2,
        repeatedIssueId: "OFF_BY_ONE",
        repeatedIssueCount: 2,
        repeatedIssueProblemCount: 1,
        latestChangeStatus: "RECOVERED",
        evidenceSubmissionIds: [9001, 9002]
      },
      needsAttention: true
    }
  ]
};

const aiQualityOverview = {
  assignmentId: 7,
  analyzedSubmissionCount: 24,
  correctionCount: 3,
  evalCandidateCount: 5,
  lowConfidenceCount: 4,
  highLeakRiskCount: 0,
  correctionRate: 12.5,
  lowConfidenceRate: 16.7,
  highLeakRiskRate: 0,
  summary: "多数反馈稳定，边界标签还需要补充评测样例。",
  correctedTags: [{ originalTag: "LOGIC", originalLabel: "逻辑问题", correctedTag: "BOUNDARY", correctedLabel: "边界条件", count: 2 }]
};

const studentAiFeedbackObservability = {
  assignmentId: 7,
  submissionCount: 32,
  failedSubmissionCount: 17,
  feedbackRecordCount: 12,
  modelReadyCount: 10,
  feedbackFailedCount: 2,
  timeoutCount: 1,
  safetyRejectedCount: 1,
  viewedCount: 7,
  modelReadyRate: 58.82,
  viewRate: 70,
  p50LatencyMs: 820,
  p95LatencyMs: 1450,
  latencySampleCount: 10,
  failureReasons: [{ reason: "TIMEOUT", count: 1 }],
  impactStats: [{ status: "IMPROVED_AFTER_AI", label: "查看后改善", count: 3 }],
  summary: "学生 AI 快反馈已有查看和后续改善证据。",
  recommendedAction: "持续比较查看后改善、仍卡住和等待后续提交的比例。"
};

const aiQualityTrend = {
  assignmentCount: 3,
  analyzedSubmissionCount: 72,
  correctionCount: 8,
  evalCandidateCount: 11,
  lowConfidenceCount: 9,
  highLeakRiskCount: 1,
  correctionRate: 11.1,
  lowConfidenceRate: 12.5,
  highLeakRiskRate: 1.4,
  summary: "跨作业趋势：边界诊断仍需补充评测覆盖。",
  assignments: [
    { assignmentId: 7, assignmentTitle: "课堂编程作业", analyzedSubmissionCount: 24, correctionCount: 3, evalCandidateCount: 5, lowConfidenceCount: 4, highLeakRiskCount: 0, correctionRate: 12.5, lowConfidenceRate: 16.7, highLeakRiskRate: 0, summary: "边界问题较集中" },
    { assignmentId: 6, assignmentTitle: "循环复习", analyzedSubmissionCount: 18, correctionCount: 2, evalCandidateCount: 3, lowConfidenceCount: 2, highLeakRiskCount: 1, correctionRate: 11.1, lowConfidenceRate: 11.1, highLeakRiskRate: 5.6, summary: "存在一个泄题风险样例" }
  ],
  correctedTags: [{ tag: "BOUNDARY", label: "边界条件", count: 5, evalCandidateCount: 4 }],
  evalNeededTags: [{ tag: "OFF_BY_ONE", label: "差一位错误", count: 6, evalCandidateCount: 5 }],
  sourceSegments: [
    { sourceType: "MODEL_SCOPE_EXTERNAL_MODEL", versionLabel: "diagnosis-v1 / agent-v1", analyzedSubmissionCount: 51, correctionCount: 6, lowConfidenceCount: 7, highLeakRiskCount: 1, correctionRate: 11.8, lowConfidenceRate: 13.7, highLeakRiskRate: 2 },
    { sourceType: "RULE_BASED_V1", versionLabel: "diagnosis-v1", analyzedSubmissionCount: 21, correctionCount: 2, lowConfidenceCount: 2, highLeakRiskCount: 0, correctionRate: 9.5, lowConfidenceRate: 9.5, highLeakRiskRate: 0 }
  ]
};

const recommendationEffectiveness = {
  recentEventCount: 9,
  uniqueRecommendationCount: 3,
  exposureCount: 3,
  clickCount: 3,
  enteredProblemCount: 1,
  followupSubmissionCount: 2,
  acceptedFollowupCount: 1,
  sameFocusIssueCount: 1,
  clickedWithoutSubmissionCount: 1,
  clickThroughRate: 100,
  followupSubmissionRate: 66.7,
  acceptedFollowupRate: 50,
  sameFocusIssueRate: 50,
  summary: "推荐后的提交已有记录，其中一次通过，一次仍需关注。",
  byType: [
    { key: "REDO", label: "重做", exposureCount: 1, clickCount: 1, enteredProblemCount: 1, followupSubmissionCount: 1, acceptedFollowupCount: 0, sameFocusIssueCount: 1, clickThroughRate: 100, followupSubmissionRate: 100, acceptedFollowupRate: 0, sameFocusIssueRate: 100 },
    { key: "NEXT_PROBLEM", label: "下一题", exposureCount: 1, clickCount: 1, enteredProblemCount: 0, followupSubmissionCount: 1, acceptedFollowupCount: 1, sameFocusIssueCount: 0, clickThroughRate: 100, followupSubmissionRate: 100, acceptedFollowupRate: 100, sameFocusIssueRate: 0 },
    { key: "REVIEW", label: "复盘", exposureCount: 1, clickCount: 1, enteredProblemCount: 0, followupSubmissionCount: 0, acceptedFollowupCount: 0, sameFocusIssueCount: 0, clickThroughRate: 100, followupSubmissionRate: 0, acceptedFollowupRate: 0, sameFocusIssueRate: 0 }
  ],
  focusTags: [
    { key: "OFF_BY_ONE", label: "差一位错误", exposureCount: 1, clickCount: 1, enteredProblemCount: 1, followupSubmissionCount: 1, acceptedFollowupCount: 0, sameFocusIssueCount: 1, clickThroughRate: 100, followupSubmissionRate: 100, acceptedFollowupRate: 0, sameFocusIssueRate: 100 },
    { key: "INPUT_PARSING", label: "输入读取", exposureCount: 1, clickCount: 1, enteredProblemCount: 0, followupSubmissionCount: 1, acceptedFollowupCount: 1, sameFocusIssueCount: 0, clickThroughRate: 100, followupSubmissionRate: 100, acceptedFollowupRate: 100, sameFocusIssueRate: 0 }
  ]
};

const identityAudit = {
  classGroupId: 3,
  className: "高一1班",
  totalProfiles: 4,
  stableIdentityCount: 3,
  manualIdentityCount: 1,
  legacyIdentityCount: 0,
  missingStudentNoCount: 1,
  duplicateGroupCount: 1,
  duplicateGroups: [
    {
      stableIdentityKey: "class:3:no:12",
      reason: "相同学号且姓名相近",
      studentProfileIds: [41, 42],
      displayNames: ["学生甲", "学生甲A"],
      studentNos: ["12"],
      identityKeys: ["stable:3:12", "legacy:42"]
    }
  ]
};

const diagnosisTags = [
  { id: "BOUNDARY", label: "边界条件", teacherExplanation: "端点或边界样例处理错误。", abilityPoint: "循环与边界", fineGrained: false },
  { id: "OFF_BY_ONE", label: "差一位错误", teacherExplanation: "循环多处理或少处理了一个位置。", abilityPoint: "循环与边界", fineGrained: true, parentTag: "BOUNDARY" }
];

const executorStatus = {
  mode: "LOCAL",
  executorType: "mock",
  dockerAvailable: false,
  pythonAvailable: true,
  cppAvailable: true,
  cpp17Available: true,
  message: "验证执行环境可用。"
};

const readiness = {
  status: "READY",
  updatedAt: "2026-05-19T10:00:00",
  checks: [
    {
      id: "executor",
      label: "执行环境",
      status: "PASS",
      blocking: true,
      message: "可用",
      action: ""
    }
  ]
};

const aiStandardLibraryItems = [
  {
    id: 1,
    layer: "MISTAKE_POINT",
    code: "OFF_BY_ONE",
    category: "循环与边界",
    name: "差一位错误",
    description: "循环端点多处理或少处理一个位置。",
    evidenceSignals: ["隐藏边界样例失败"],
    commonCodePatterns: ["i < n - 1"],
    judgeSignals: ["boundary case failed"],
    requiredEvidence: ["首个失败样例"],
    applicableLanguages: ["Python", "C++17"],
    relatedItems: [],
    knowledgeNodeCodes: [],
    prerequisiteKnowledgeCodes: [],
    enabled: true,
    libraryVersion: "smoke"
  }
];

const aiStandardLibraryGrowthCandidates = [
  {
    id: 1,
    layer: "MISTAKE_POINT",
    suggestedCode: "INPUT_PARSING",
    suggestedName: "输入读取理解",
    suggestedPath: ["循环与边界", "输入读取理解"],
    evidenceRefs: ["submission:9001"],
    similarExistingItems: [],
    changeReason: "学生没有读取完整输入。",
    status: "NEEDS_REVIEW",
    confidence: 0.8,
    occurrenceCount: 2
  }
];

const informaticsKnowledgeTree = [
  {
    id: 1,
    code: "LOOP_BOUNDARY",
    parentCode: null,
    type: "KNOWLEDGE_POINT",
    name: "循环与边界",
    description: "循环边界与输入读取",
    path: "循环与边界",
    stage: "高中",
    difficulty: "EASY",
    aliases: [],
    prerequisites: [],
    learningObjectives: [],
    typicalProblems: [],
    sortOrder: 1,
    enabled: true,
    libraryVersion: "smoke",
    children: [
      {
        id: 2,
        code: "INPUT_PARSING",
        parentCode: "LOOP_BOUNDARY",
        type: "MISTAKE_POINT",
        name: "输入读取理解",
        description: "读取完整输入并理解行列关系",
        path: "循环与边界 / 输入读取理解",
        stage: "高中",
        difficulty: "EASY",
        aliases: [],
        prerequisites: [],
        learningObjectives: [],
        typicalProblems: [],
        sortOrder: 1,
        enabled: true,
        libraryVersion: "smoke",
        children: []
      }
    ]
  }
];

const aiStandardLibraryGrowthGovernanceSummary = {
  totalCount: 1,
  proposedCount: 0,
  reviewPendingCount: 1,
  needsReviewCount: 1,
  blockedCount: 0,
  mergedSimilarCount: 0,
  teacherApprovedCount: 0,
  mergedCount: 0,
  rejectedCount: 0,
  ignoredCount: 0,
  duplicateAggregateCount: 0,
  statusStats: [{ status: "NEEDS_REVIEW", count: 1 }],
  highFrequencyPaths: [
    {
      path: ["循环与边界", "输入读取理解"],
      candidateCount: 1,
      pendingCount: 1,
      occurrenceCount: 2,
      recommendedAction: "保留人工审核"
    }
  ],
  weakPaths: []
};

const scenarios = [
  {
    name: "app-entry",
    path: "/app/",
    selectors: [
      [".route-hub-page", "route hub page"],
      [".route-hub-hero-copy", "route hub hero copy"],
      [".route-hub-preview-frame", "route hub screenshot preview"],
      [".route-hub-learning-loop", "route hub learning loop"]
    ],
    afterChecks: async (page, viewport) => {
      const navLabels = await page.locator(".top-nav__link span").allTextContents();
      const roleActions = page.locator('.route-hub-role-actions a[href="/app/student"], .route-hub-role-actions a[href="/app/teacher"]');
      const canonicalHrefs = await roleActions.evaluateAll(elements =>
        elements.map(element => element.getAttribute("href"))
      );
      const hubText = ((await page.locator(".route-hub-page").first().textContent()) || "").replace(/\s+/g, "");
      const previewText = ((await page.locator(".route-hub-preview-frame").first().textContent()) || "").replace(/\s+/g, "");
      const previewImage = await page.locator(".route-hub-preview-frame img").first().evaluate(image => ({
        alt: image.getAttribute("alt"),
        naturalWidth: image.naturalWidth,
        naturalHeight: image.naturalHeight
      }));
      const learningSteps = await page.locator(".route-hub-loop-step").count();
      const documentWidth = await page.evaluate(() => document.documentElement.scrollWidth);
      record("app root is explicit route hub", page.url().endsWith("/app") || page.url().endsWith("/app/"), page.url());
      record("app root leads with the selected concept message", hubText.includes("从一道题开始") && hubText.includes("看见真正的进步"), hubText.slice(0, 700));
      record("app root presents the code experience as a static screenshot", previewText === "界面截图" && previewImage.naturalWidth >= 700 && previewImage.naturalHeight >= 400, JSON.stringify({ previewText, previewImage }));
      record("app root screenshot has explicit alternative text", Boolean(previewImage.alt?.includes("编程与评测界面预览")), previewImage.alt || "");
      record("app root explains the learning loop", learningSteps === 3 && hubText.includes("练习") && hubText.includes("评测") && hubText.includes("复盘"), `steps ${learningSteps}; ${hubText.slice(0, 500)}`);
      record("app root exposes canonical paths", canonicalHrefs.includes("/app/student") && canonicalHrefs.includes("/app/teacher"), canonicalHrefs.join("|"));
      record("app root keeps one action per role", await roleActions.count() === 2, `role actions ${await roleActions.count()}`);
      record("app root keeps top nav simple", navLabels.join("|") === "学生端|教师端", navLabels.join("|"));
      record("app root fits the viewport", documentWidth <= viewport.width, `document ${documentWidth}; viewport ${viewport.width}`);
    }
  },
  {
    name: "student-default",
    path: "/app/student",
    beforeChecks: async page => {
      await page.evaluate(() => {
        Object.keys(window.sessionStorage)
          .filter(key => key.startsWith("wzai:student"))
          .forEach(key => window.sessionStorage.removeItem(key));
        window.dispatchEvent(new Event("wzai:student-change"));
      });
      await page.locator(".student-guest-assignment-preview").first().waitFor({ state: "visible", timeout: 10000 });
    },
    selectors: [
      [".student-home", "student default entry"],
      [".app-header", "application header"],
      [".student-guest-practice", "guest public practice"],
      [".student-guest-assignment-preview", "guest assignment preview"],
      [".student-guest-tools", "guest locked tools"]
    ],
    afterChecks: async (page, viewport) => {
      const navCount = await page.locator(".top-nav__link").count();
      const navLabels = await page.locator(".top-nav__link span").allTextContents();
      const guestRows = await page.locator(".student-guest-assignment-row").count();
      const loginActions = await page.locator(".student-guest-login-action").count();
      const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
      record("student direct URL opens entry", page.url().includes("/app/student"), page.url());
      record("student entry keeps role top nav only", navCount === 2 && navLabels.join("|") === "学生端|教师端", navLabels.join("|"));
      record("student guest page mirrors the assignment board", guestRows === 3, `guest rows ${guestRows}`);
      record("student guest page keeps one login action", loginActions === 1, `login actions ${loginActions}`);
      record("student guest page has no horizontal overflow", overflow <= 1, `overflow ${overflow}; viewport ${viewport.width}`);
      await page.locator(".student-guest-practice__row").click();
      await page.waitForURL(url => url.pathname === "/app/student/assignments/public", { timeout: 5000 });
      record("student guest public practice action opens the catalog", page.url().includes("/app/student/assignments/public"), page.url());
      await page.goBack({ waitUntil: "domcontentloaded" });
      await page.locator(".student-guest-assignment-preview").first().waitFor({ state: "visible", timeout: 5000 });
      await page.locator(".student-guest-login-action").click();
      await page.waitForURL(url => url.pathname === "/app/student/login", { timeout: 5000 });
      record("student guest assignment action opens login", page.url().includes("/app/student/login"), page.url());
      await page.goBack({ waitUntil: "domcontentloaded" });
      await page.locator(".student-guest-assignment-preview").first().waitFor({ state: "visible", timeout: 5000 });
    }
  },
  {
    name: "public-assignment",
    path: "/app/student/assignments/public",
    beforeChecks: async page => {
      let returnDocumentRequests = 0;
      const countReturnDocumentRequest = request => {
        const url = new URL(request.url());
        if (request.isNavigationRequest() && request.resourceType() === "document" && url.pathname === "/app/student") {
          returnDocumentRequests += 1;
        }
      };
      page.on("request", countReturnDocumentRequest);
      await page.locator('.student-home-command--entry a[href="/app/student"]').click();
      await page.waitForURL(url => url.pathname === "/app/student", { timeout: 5000 });
      record("public catalog back action performs a full navigation", returnDocumentRequests === 1, `document requests ${returnDocumentRequests}`);
      page.off("request", countReturnDocumentRequest);
      await page.goto(new URL("/app/student/assignments/public", page.url()).href, { waitUntil: "domcontentloaded" });
      await page.locator(".public-problem-link").filter({ hasText: "求和边界" }).first().click();
      await page.locator(".problem-workbench").first().waitFor({ state: "visible", timeout: 10000 });
    },
    afterChecks: async (page, viewport) => {
      const navCount = await page.locator(".top-nav__link").count();
      const navLabels = await page.locator(".top-nav__link span").allTextContents();
      const taskSidebarCount = await page.locator(".problem-task-sidebar").count();
      const navigationRailCount = await page.locator(".problem-workbench-rail").count();
      const shellColumns = await page.locator(".problem-workbench-shell").first().evaluate(element => window.getComputedStyle(element).gridTemplateColumns);
      record("public assignment keeps role top nav only", navCount === 2 && navLabels.join("|") === "学生端|教师端", navLabels.join("|"));
      record("public assignment opens workbench", page.url().includes("/app/student/assignments/public/problems/101"), page.url());
      record("public assignment keeps the problem list", taskSidebarCount === 1, `task sidebar count ${taskSidebarCount}`);
      record("public assignment omits the navigation rail", navigationRailCount === 0, `navigation rail count ${navigationRailCount}`);
      record("public assignment shell uses the full width", shellColumns.trim().split(/\s+/).length === 1, shellColumns);
      if (viewport.name === "mobile") {
        await checkVisible(page, ".problem-mobile-jump", "public mobile code jump");
      }
    },
    selectors: [
      [".problem-task-sidebar", "public task sidebar"],
      [".panel--statement", "public statement panel"],
      [".panel--editor", "public editor panel"]
    ]
  },
  {
    name: "student",
    path: "/app/student",
    afterChecks: async (page, viewport) => {
      await page.locator(".student-assignment-board").first().waitFor({ state: "visible", timeout: 10000 });
      const navCount = await page.locator(".top-nav__link").count();
      const inviteFormCount = await page.locator("text=输入邀请码").count();
      const homeText = ((await page.locator(".student-home").first().textContent()) || "").replace(/\s+/g, "");
      const assignmentRowCount = await page.locator(".student-assignment-row").count();
      const primaryActionCount = await page.locator(".student-assignment-row__action").count();
      const assignmentSelector = page.locator('input[name="student-assignment-selection"]');
      const assignmentSelectorCount = await assignmentSelector.count();
      const progressCount = await page.locator(".student-assignment-row__progress").count();
      const headerLoginCount = await page.locator(".header-login-link, .header-student-chip").count();
      const studentWidth = await page.locator(".student-home").first().evaluate(element => element.getBoundingClientRect().width);
      const commandHeight = await page.locator(".student-home-command").first().evaluate(element => element.getBoundingClientRect().height);
      const assignmentRowHeights = await page.locator(".student-assignment-row").evaluateAll(elements =>
        elements.map(element => element.getBoundingClientRect().height)
      );
      const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
      const assignmentBottom = await page.locator(".student-assignment-board").first().evaluate(element => element.getBoundingClientRect().bottom);
      const practiceTop = await page.locator(".student-self-practice").first().evaluate(element => element.getBoundingClientRect().top);
      const navLabels = await page.locator(".top-nav__link span").allTextContents();
      record("student home keeps role top nav only", navCount === 2 && navLabels.join("|") === "学生端|教师端", navLabels.join("|"));
      record("student identity lives in header", headerLoginCount >= 1, `header identity count ${headerLoginCount}`);
      record("student home shows public catalog entry", homeText.includes("公共题库"), homeText);
      record("student home shows assignment details", homeText.includes("课堂编程作业") && homeText.includes("高一1班") && homeText.includes("2题"), homeText);
      record("student home keeps entry copy direct", homeText.includes("今天先完成课堂任务") && !homeText.includes("输入邀请码"), homeText);
      record("student home uses a classroom assignment board", assignmentRowCount >= 1, `assignment rows ${assignmentRowCount}`);
      record("student home keeps one primary assignment action", primaryActionCount === 1, `primary actions ${primaryActionCount}`);
      record("student home exposes one native selector per assignment", assignmentSelectorCount === assignmentRowCount, `selectors ${assignmentSelectorCount}; rows ${assignmentRowCount}`);
      if (assignmentSelectorCount >= 3) {
        const urlBeforeSelection = page.url();
        await assignmentSelector.nth(1).check();
        const selectedSecondHref = await page.locator('.student-assignment-row[data-selected="true"] .student-assignment-row__action').getAttribute("href");
        record("student assignment selection moves highlight and action without navigation", page.url() === urlBeforeSelection && selectedSecondHref === "/app/student/assignments/8", `url ${page.url()}; href ${selectedSecondHref}`);
        await assignmentSelector.nth(1).press("ArrowDown");
        const thirdSelected = await assignmentSelector.nth(2).isChecked();
        const selectedThirdHref = await page.locator('.student-assignment-row[data-selected="true"] .student-assignment-row__action').getAttribute("href");
        record("student assignment selection supports arrow keys", thirdSelected && selectedThirdHref === "/app/student/assignments/9", `third selected ${thirdSelected}; href ${selectedThirdHref}`);
      }
      record("student home shows truthful progress for every assignment", progressCount === assignmentRowCount && homeText.includes("1/2") && homeText.includes("3/3"), `progress rows ${progressCount}; ${homeText}`);
      record("student home distinguishes learning states", homeText.includes("待开始") && homeText.includes("进行中") && homeText.includes("已完成") && !homeText.includes("信息技术老师"), homeText);
      record("student home separates public practice below assignments", practiceTop >= assignmentBottom, `assignment bottom ${assignmentBottom}; practice top ${practiceTop}`);
      if (viewport.name === "desktop") {
        record("student home uses concept-scale width", studentWidth >= 1280 && studentWidth <= 1360, `student width ${studentWidth}`);
      }
      if (viewport.name !== "mobile") {
        record("student home uses a compact command bar", commandHeight <= 82, `command height ${commandHeight}`);
        record("student home keeps concept-scale row rhythm", Math.min(...assignmentRowHeights) >= 104, `row heights ${assignmentRowHeights.join(",")}`);
      }
      record("student home has no horizontal overflow", overflow <= 1, `overflow ${overflow}`);
      record("student no longer starts with invite", inviteFormCount === 0, `invite form count ${inviteFormCount}`);
      await page.locator(".header-signout-button").first().click();
      await page.locator(".student-guest-assignment-preview").first().waitFor({ state: "visible", timeout: 5000 });
      const studentKeysAfterSignOut = await page.evaluate(() =>
        Array.from({ length: window.sessionStorage.length }, (_, index) => window.sessionStorage.key(index)).filter(key => key?.startsWith("wzai:student"))
      );
      const guestPreviewRowCount = await page.locator(".student-guest-assignment-row").count();
      const guestLoginActionCount = await page.locator(".student-guest-assignment-preview .student-guest-login-action").count();
      record("student sign out clears student session keys", studentKeysAfterSignOut.length === 0, studentKeysAfterSignOut.join("|"));
      record("student sign out keeps a classroom assignment preview", guestPreviewRowCount === 3, `guest preview rows ${guestPreviewRowCount}`);
      record("student guest view keeps one classroom login action", guestLoginActionCount === 1, `guest login actions ${guestLoginActionCount}`);
      record("student guest view separates public practice and locked tools", await page.locator(".student-guest-practice").count() === 1 && await page.locator(".student-guest-tools").count() === 1);
      await page.evaluate(studentJson => {
        window.sessionStorage.setItem("wzai:student", studentJson);
        window.sessionStorage.setItem("wzai:student:7", studentJson);
        window.dispatchEvent(new Event("wzai:student-change"));
      }, JSON.stringify(student));
      await page.locator(".student-entry-link").filter({ hasText: "课堂编程作业" }).first().waitFor({ state: "visible", timeout: 5000 });
    },
    selectors: [
      [".header-login-link, .header-student-chip", "student header identity"],
      [".student-assignment-board", "student assignment board"],
      [".student-self-practice", "student self practice"],
      [".student-review-strip", "student review strip"]
    ]
  },
  {
    name: "student-assignment",
    path: "/app/student/assignments/7",
    afterChecks: async page => {
      const pageText = ((await page.locator(".student-assignment-insights-page").textContent()) || "").replace(/\s+/g, "");
      const tabHrefs = await page.locator(".student-assignment-insights-tabs a").evaluateAll(elements => elements.map(element => element.getAttribute("href")));
      const rowCount = await page.locator(".student-assignment-progress-row").count();
      const continueHref = await page.locator(".student-assignment-continue").getAttribute("href");
      record("student assignment stays on assignment overview", page.url().endsWith("/app/student/assignments/7"), page.url());
      record("student assignment has canonical three page navigation", tabHrefs.join("|") === "/app/student/assignments/7|/app/student/assignments/7/ranking|/app/student/assignments/7/submissions", tabHrefs.join("|"));
      record("student assignment summary uses trajectory facts", pageText.includes("已通过1/2") && pageText.includes("总提交4次"), pageText);
      record("student assignment lists every task", rowCount === assignment.tasks.length, `rows ${rowCount}`);
      record("student assignment continue action opens coding page", continueHref?.startsWith("/app/student/assignments/7/problems/") === true, continueHref || "missing");
    },
    selectors: [
      [".student-assignment-insights-page", "student assignment overview"],
      [".student-assignment-insights-tabs", "student assignment page navigation"],
      [".student-assignment-progress-table", "student assignment progress table"]
    ]
  },
  {
    name: "student-assignment-ranking",
    path: "/app/student/assignments/7/ranking",
    afterChecks: async page => {
      const pageText = ((await page.locator(".student-assignment-insights-page").textContent()) || "").replace(/\s+/g, "");
      const currentRows = await page.locator(".student-ranking-row.is-current").count();
      const currentText = ((await page.locator(".student-ranking-row.is-current").textContent()) || "").replace(/\s+/g, "");
      const tabHrefs = await page.locator(".student-assignment-insights-tabs a").evaluateAll(elements => elements.map(element => element.getAttribute("href")));
      record("student ranking route is stable", page.url().endsWith("/app/student/assignments/7/ranking"), page.url());
      record("student ranking highlights current student", currentRows === 1 && currentText.includes("学生甲（我）") && currentText.includes("4"), currentText);
      record("student ranking masks classmates", pageText.includes("王同学") && !pageText.includes("王小明"), pageText);
      record("student ranking explains progress-only ties", pageText.includes("按已通过题数排名") && pageText.includes("同完成度并列") && pageText.includes("不按运行时间或提交速度"), pageText);
      record("student ranking reuses canonical three page navigation", tabHrefs.length === 3, tabHrefs.join("|"));
    },
    selectors: [
      [".student-ranking-table", "student assignment ranking table"],
      [".student-ranking-context", "student assignment ranking context"],
      [".student-assignment-insights-tabs a[aria-current='page']", "student assignment ranking active tab"]
    ]
  },
  {
    name: "student-assignment-submissions",
    path: "/app/student/assignments/7/submissions",
    afterChecks: async (page, viewport) => {
      const rowCount = await page.locator(".student-submission-row").count();
      const pageText = ((await page.locator(".student-assignment-insights-page").textContent()) || "").replace(/\s+/g, "");
      const filterHeight = await page.locator(".student-submission-filters").evaluate(element => element.getBoundingClientRect().height);
      const hiddenLabelMetrics = await page.locator(".student-submission-history .sr-only").evaluateAll(elements => elements.map(element => {
        const rect = element.getBoundingClientRect();
        return { width: rect.width, height: rect.height, position: getComputedStyle(element).position };
      }));
      const visibleFilterLabels = await page.locator(".student-submission-filter-label").allTextContents();
      const languageOptions = await page.locator(".student-submission-filter--language option").allTextContents();
      record("student submissions route is stable", page.url().endsWith("/app/student/assignments/7/submissions"), page.url());
      record("student submissions show truthful summary", pageText.includes("共提交12次") && pageText.includes("通过4次") && pageText.includes("涉及2道题"), pageText);
      record("student submissions paginate first page", rowCount === 8, `rows ${rowCount}`);
      record("student submissions hides assistive-only labels", hiddenLabelMetrics.length === 4 && hiddenLabelMetrics.every(item => item.width <= 1 && item.height <= 1 && item.position === "absolute"), JSON.stringify(hiddenLabelMetrics));
      record("student submissions keeps only compact visible filter labels", visibleFilterLabels.join("|") === "判题结果：|语言：", visibleFilterLabels.join("|"));
      record("student submissions omits Java from the language filter", !languageOptions.includes("Java 17"), languageOptions.join("|"));
      if (viewport.name === "desktop") {
        record("student submissions filter bar stays on one compact row", filterHeight <= 72, `height ${filterHeight}`);
      }
      record("student submissions shows descending time sort", await page.locator(".student-submission-sort svg").count() === 1);
      record("student submissions uses icon pagination and page numbers", await page.getByRole("button", { name: "提交记录上一页" }).count() === 1 && await page.getByRole("button", { name: "提交记录下一页" }).count() === 1 && await page.locator(".student-submission-page-number").count() === 2);
      const firstRowText = ((await page.locator(".student-submission-row").first().textContent()) || "").replace(/\s+/g, "");
      record("student submissions avoids false zero memory precision", firstRowText.includes("-") && !firstRowText.includes("0.0MB"), firstRowText);
      await page.getByRole("button", { name: "通过", exact: true }).click();
      await page.waitForFunction(() => document.querySelectorAll(".student-submission-row").length === 4);
      record("student submissions verdict filter works", await page.locator(".student-submission-row").count() === 4);
      await page.getByRole("button", { name: "全部", exact: true }).click();
      await page.waitForFunction(() => document.querySelectorAll(".student-submission-row").length === 8);
      await page.locator(".student-submission-row").first().click();
      await page.locator(".student-submission-detail").waitFor({ state: "visible", timeout: 5000 });
      await page.waitForFunction(() => document.querySelector(".student-submission-detail h2")?.textContent?.includes("#9001"), null, { timeout: 5000 });
      const detailText = ((await page.locator(".student-submission-detail").textContent()) || "").replace(/\s+/g, "");
      record("student submissions open personal submission detail", detailText.includes("#9001") && detailText.includes("求和边界"), detailText);
      await page.getByRole("button", { name: "关闭提交详情" }).click();
      await page.locator(".student-submission-detail").waitFor({ state: "detached", timeout: 5000 });
      await page.locator(".student-submission-history").click({ position: { x: 2, y: 2 } });
    },
    selectors: [
      [".student-submission-history", "student assignment submission history"],
      [".student-submission-filters", "student assignment submission filters"],
      [".student-submission-table", "student assignment submission table"]
    ]
  },
  {
    name: "problem",
    path: "/app/student/assignments/7/problems/101?studentProfileId=41&recommendationToken=rec-next-101",
    beforeChecks: async page => {
      await page.locator(".panel--editor button.ui-button--primary").first().click();
      await page.locator(".problem-result-modal").first().waitFor({ state: "visible", timeout: 10000 });
    },
    afterChecks: async (page, viewport) => {
      const navLabels = await page.locator(".top-nav__link span").allTextContents();
      const navCount = await page.locator(".top-nav__link").count();
      const modalText = ((await page.locator(".problem-result-modal").first().textContent()) || "").replace(/\s+/g, "");
      const modalHeaderText = ((await page.locator(".problem-result-modal__header").first().textContent()) || "").replace(/\s+/g, "");
      const modalFooterText = ((await page.locator(".problem-result-modal__footer").first().textContent()) || "").replace(/\s+/g, "");
      const repairText = ((await page.locator(".problem-result-section--repair").first().textContent()) || "").replace(/\s+/g, "");
      const growthText = ((await page.locator(".problem-result-section--growth").first().textContent()) || "").replace(/\s+/g, "");
      const commandCount = await page.locator(".practice-command--workbench").count();
      const statementTitle = ((await page.locator(".panel--statement .panel__header h2").first().textContent()) || "").trim();
      const taskItemHeight = await page.locator(".problem-task-item").first().evaluate(element => element.getBoundingClientRect().height);
      const taskSidebarWidth = await page.locator(".problem-task-sidebar").first().evaluate(element => element.getBoundingClientRect().width);
      const taskItemWidth = await page.locator(".problem-task-item").first().evaluate(element => element.getBoundingClientRect().width);
      record("problem keeps role top nav only", navCount === 2 && navLabels.join("|") === "学生端|教师端", navLabels.join("|"));
      record("problem removes duplicate command banner", commandCount === 0, `command banner count ${commandCount}`);
      record("problem statement title is the current problem", statementTitle.includes("求和边界"), statementTitle);
      record("problem task list is compact", taskItemHeight <= 48, `item height ${taskItemHeight}`);
      if (viewport.name === "mobile") {
        record("problem mobile task list is readable", taskSidebarWidth >= viewport.width - 24 && taskItemWidth >= 140, `sidebar ${taskSidebarWidth}; item ${taskItemWidth}; viewport ${viewport.width}`);
      }
      const feedbackWasPending = modalText.includes("AI分析中");
      record(
        "problem modal opens with a valid feedback state",
        feedbackWasPending || modalText.includes("修正建议"),
        modalText
      );
      record(
        "problem modal does not invent guidance while analysis pending",
        !feedbackWasPending || (
          !modalText.includes("先看测试点") &&
          !modalText.includes("先把当前错误修掉") &&
          !modalText.includes("先看左侧失败点") &&
          !modalText.includes("暂无提升建议")
        ),
        modalText
      );
      await page.waitForFunction(
        () => document.querySelectorAll(".student-feedback-item").length === 4,
        null,
        { timeout: 10000 }
      );
      const resultViewTabs = page.locator(".problem-result-view-switch button");
      const resultViewTabCount = await resultViewTabs.count();
      record("problem result separates repair and growth into two top views", resultViewTabCount === 2, `view tabs ${resultViewTabCount}`);
      if (resultViewTabCount === 2) {
        const resultTopLayout = await page.locator(".problem-result-modal").evaluate(element => {
          const header = element.querySelector(".problem-result-modal__header");
          const body = element.querySelector(".problem-result-modal__body");
          const switcher = element.querySelector(".problem-result-view-switch");
          if (!header || !body || !switcher) return null;
          const headerRect = header.getBoundingClientRect();
          const bodyRect = body.getBoundingClientRect();
          const switcherRect = switcher.getBoundingClientRect();
          return { headerBottom: headerRect.bottom, switcherBottom: switcherRect.bottom, bodyTop: bodyRect.top };
        });
        record(
          "problem result content starts below the top view switcher",
          Boolean(resultTopLayout && resultTopLayout.bodyTop >= Math.max(resultTopLayout.headerBottom, resultTopLayout.switcherBottom) - 1),
          JSON.stringify(resultTopLayout)
        );
        const repairTab = page.getByRole("tab", { name: "修正工作台" });
        const growthTab = page.getByRole("tab", { name: "成长仪表盘" });
        record(
          "problem result opens on the repair workspace",
          await repairTab.getAttribute("aria-selected") === "true" && await page.locator(".problem-result-view--repair").isVisible()
        );
        await growthTab.click();
        const dashboardText = ((await page.locator(".problem-result-view--growth").textContent()) || "").replace(/\s+/g, "");
        record(
          "problem growth view contains every required dashboard block",
          ["提交次数", "有效修改", "当前测试点", "未解决问题", "测试点通过率趋势", "高频知识点", "问题变化", "知识点×提交矩阵"]
            .every(label => dashboardText.includes(label)),
          dashboardText
        );
        record(
          "problem result displays only one page at a time",
          await page.locator(".problem-result-view--growth").isVisible() && !await page.locator(".problem-result-view--repair").isVisible()
        );
        const verticalScrollContainers = await page.locator(".problem-result-modal *").evaluateAll(elements => elements.filter(element => {
          const style = window.getComputedStyle(element);
          return ["auto", "scroll"].includes(style.overflowY) && element.scrollHeight > element.clientHeight + 1;
        }).map(element => element.className));
        record(
          "problem result avoids nested vertical scroll containers",
          verticalScrollContainers.length === 0,
          JSON.stringify(verticalScrollContainers)
        );
        await page.locator(".problem-result-modal").screenshot({
          path: join(artifactDir, `student-growth-dashboard-${viewport.name}-light.png`)
        });
        await repairTab.click();
      }
      const readyModalText = ((await page.locator(".problem-result-modal").first().textContent()) || "").replace(/\s+/g, "");
      const readyTestText = ((await page.locator(".problem-result-section--tests").first().textContent()) || "").replace(/\s+/g, "");
      const readyRepairText = ((await page.locator(".problem-result-section--repair").first().textContent()) || "").replace(/\s+/g, "");
      const readyGrowthText = ((await page.locator(".problem-result-section--growth").first().textContent()) || "").replace(/\s+/g, "");
      record(
        "problem modal has teaching columns",
        readyTestText.includes("评测") && readyRepairText.includes("修正建议") && readyGrowthText.includes("提升建议"),
        readyModalText
      );
      record(
        "problem modal keeps testcase column minimal",
        !readyTestText.includes("通过耗时内存") &&
          !readyTestText.includes("公开测试点") &&
          !readyTestText.includes("第一个公开失败点") &&
          readyTestText.includes("1公开28ms错"),
        readyTestText
      );
      record("problem modal removes empty count noise", !readyModalText.includes("0条"), readyModalText);
      record(
        "problem modal removes system-like labels",
        !readyModalText.includes("AI错误指导") &&
          !readyModalText.includes("AI提升指导") &&
          !readyModalText.includes("怎么改") &&
          !readyModalText.includes("验证一下") &&
          !readyModalText.includes("收益：") &&
          !readyModalText.includes("生成下一问") &&
          !readyModalText.includes("再生成一问"),
        readyModalText
      );
      record(
        "problem modal ignores noisy legacy feedback fields",
        !readyModalText.includes("本地可验证反馈") &&
          !readyModalText.includes("当前代码存在运行稳定性风险") &&
          !readyModalText.includes("先保证程序稳定运行") &&
          !readyModalText.includes("先看左侧失败点") &&
          !readyModalText.includes("先把当前错误修掉") &&
          !readyModalText.includes("暂无提升建议") &&
          !readyModalText.includes("当前最需要先处理的问题"),
        readyModalText
      );
      record("problem modal keeps header compact", !modalHeaderText.includes("先改这里") && !modalHeaderText.includes("先检查"), modalHeaderText);
      record("problem modal footer has no duplicate close copy", !modalFooterText.includes("关闭"), modalFooterText);
      record(
        "problem modal maps repair fields structurally",
        readyRepairText.includes("修正建议") &&
          readyRepairText.includes("输入没有完整读取") &&
          readyRepairText.includes("先把公开样例的每一行input手推清楚") &&
          readyRepairText.includes("第二次读取时应该拿到123"),
        readyRepairText
      );
      record("problem modal maps growth fields structurally", readyGrowthText.includes("提升建议") && readyGrowthText.includes("测试习惯") && readyGrowthText.includes("能更早发现输入格式问题"), readyGrowthText);
      record(
        "problem modal shows complete issue lifecycle",
        readyModalText.includes("本次问题变化") &&
          readyModalText.includes("仍存在") &&
          readyModalText.includes("新出现") &&
          readyModalText.includes("已有恢复证据") &&
          readyModalText.includes("完整展示4项"),
        readyModalText
      );
      const suggestionCardCount = await page.locator(".student-feedback-item").count();
      const flatMetaRows = page.locator(".student-feedback-meta-row");
      const flatMetaRowCount = await flatMetaRows.count();
      const nestedMetaCardCount = await page.locator(".student-feedback-knowledge, .student-feedback-evidence").count();
      const pathStatusText = (await page.locator(".student-feedback-path-status").allTextContents()).join("|");
      const metaRowTexts = (await flatMetaRows.allTextContents()).map(text => text.replace(/\s+/g, ""));
      record("problem modal preserves every suggestion", suggestionCardCount === 4, `suggestions ${suggestionCardCount}`);
      record(
        "problem modal flattens knowledge and evidence into one metadata row",
        flatMetaRowCount === 4 && nestedMetaCardCount === 0,
        `meta rows ${flatMetaRowCount}; nested cards ${nestedMetaCardCount}`
      );
      record(
        "problem modal preserves knowledge path and evidence for every suggestion",
        metaRowTexts.length === 4 && metaRowTexts.every(text => text.includes("知识路径") && (text.includes("代码证据") || text.includes("证据依据"))),
        metaRowTexts.join("|")
      );
      record(
        "problem modal distinguishes knowledge path provenance",
        ["正式标准库", "临时知识点", "历史推断", "暂未归类"].every(label => pathStatusText.includes(label)),
        pathStatusText
      );
      await page.locator(".problem-result-modal").screenshot({
        path: join(artifactDir, `student-feedback-cards-${viewport.name}-light.png`)
      });
      await page.evaluate(() => {
        document.documentElement.dataset.theme = "dark";
        window.localStorage.setItem("wzai:theme", "dark");
      });
      await page.waitForTimeout(120);
      await page.locator(".problem-result-modal").screenshot({
        path: join(artifactDir, `student-feedback-cards-${viewport.name}-dark.png`)
      });
      await page.evaluate(() => {
        document.documentElement.dataset.theme = "light";
        window.localStorage.setItem("wzai:theme", "light");
      });
      await page.waitForTimeout(80);
      await page.locator(".language-toggle").dispatchEvent("click");
      await page.waitForTimeout(80);
      const englishMetaText = ((await page.locator(".student-feedback-meta-row").allTextContents()) || []).join("|");
      record(
        "problem feedback metadata renders complete English copy",
        ["Knowledge path", "Formal library", "Provisional node", "Legacy inference", "Unclassified", "Code evidence"]
          .every(label => englishMetaText.includes(label)) && !englishMetaText.includes("feedbackMeta."),
        englishMetaText
      );
      await page.locator(".problem-result-modal").screenshot({
        path: join(artifactDir, `student-feedback-cards-${viewport.name}-english.png`)
      });
      await page.locator(".language-toggle").dispatchEvent("click");
      await page.waitForTimeout(80);
      record("problem modal uses student AI feedback endpoint", studentFeedbackLookupCount >= 2, `student feedback lookup count ${studentFeedbackLookupCount}`);
      record("problem modal records model feedback view", studentFeedbackViewCount === 1, `student feedback view count ${studentFeedbackViewCount}`);
      const flatEvidenceAction = page.locator(".student-feedback-meta-action").first();
      const flatEvidenceActionCount = await flatEvidenceAction.count();
      record("problem flat evidence keeps the code jump action", flatEvidenceActionCount === 1, `actions ${flatEvidenceActionCount}`);
      if (flatEvidenceActionCount === 1) {
        await flatEvidenceAction.click();
        await page.locator(".problem-result-modal").waitFor({ state: "hidden", timeout: 5000 });
        await page.locator(".cm-line-evidence-highlight").first().waitFor({ state: "visible", timeout: 5000 });
        record("problem evidence click highlights the matching editor line", await page.locator(".cm-line-evidence-highlight").count() === 1);
        await page.screenshot({
          path: join(artifactDir, `student-feedback-line-highlight-${viewport.name}.png`),
          fullPage: true
        });
      }
      await checkVisible(page, ".problem-last-result", "problem last result entry after modal close");
      await checkVisible(page, ".problem-history-panel", "problem submission history");
      if (viewport.name === "mobile") {
        await checkVisible(page, ".problem-mobile-jump", "problem mobile code jump");
      }
    },
    selectors: [
      [".problem-layout", "problem main layout"],
      [".problem-task-sidebar", "problem task sidebar"],
      [".panel--statement", "problem statement panel"],
      [".panel--editor", "problem editor panel"],
      [".problem-result-modal", "problem result modal"],
      [".problem-result-section--tests", "problem result testcase column"],
      [".problem-result-section--repair", "problem result repair column"],
      [".problem-result-section--growth", "problem result growth column"]
    ]
  },
  {
    name: "teacher",
    path: "/app/teacher",
    afterChecks: async (page, viewport) => {
      const shellText = ((await page.locator(".teacher-shell-nav").first().textContent()) || "").replace(/\s+/g, "");
      const teacherText = ((await page.locator(".teacher-analytics-page").first().textContent()) || "").replace(/\s+/g, "");
      record("teacher shell nav uses analytics entry", shellText.includes("教学分析") && !shellText.includes("作业中心班级学情"), shellText);
      record("teacher root redirects to class chooser", teacherText.includes("选择班级") && teacherText.includes("高一1班"), teacherText.slice(0, 800));
      record("teacher analytics avoids decision copy", !teacherText.includes("下一步") && !teacherText.includes("建议") && !teacherText.includes("讲评"), teacherText.slice(0, 800));
      record("teacher create action goes to dedicated page", await page.locator('a[href="/app/teacher/assignment/new"]').first().isVisible(), teacherText.slice(0, 800));
      record(
        "teacher main copy hides engineering tokens",
        !/BLOCKED|RECOVERED|NOT_COMPARABLE|fallback|smoke|profile/i.test(teacherText),
        teacherText.slice(0, 800)
      );
      if (viewport.name === "desktop") {
        await checkElementMaxWidth(page, ".teacher-analytics-page", 1440, "teacher desktop analytics width");
      }
      if (viewport.name === "mobile") {
        await checkMinControlHeight(page, ".teacher-analytics-hero .ui-button", 36, "teacher mobile primary action");
      }
    },
    selectors: [
      [".teacher-shell-nav", "teacher shell nav"],
      [".teacher-analytics-page", "teacher analytics page"],
      [".teacher-analytics-hero", "teacher analytics hero"],
      [".teacher-analytics-class-card", "teacher class card"]
    ]
  },
  {
    name: "assignment-analytics",
    path: "/app/teacher/classes/3/assignments/7",
    afterChecks: async (page, viewport) => {
      const assignmentText = ((await page.locator(".teacher-analytics-page").first().textContent()) || "").replace(/\s+/g, "");
      record("assignment analytics exposes objective metrics", assignmentText.includes("提交人数") && assignmentText.includes("正确率") && assignmentText.includes("平均提交"), assignmentText.slice(0, 900));
      record("assignment analytics lists problems", assignmentText.includes("求和边界") && assignmentText.includes("循环边界"), assignmentText.slice(0, 900));
      record("assignment analytics has AI attribution", assignmentText.includes("AI知识归因") && assignmentText.includes("当前知识路径"), assignmentText.slice(0, 900));
      record("assignment analytics avoids teacher decision language", !assignmentText.includes("下一步") && !assignmentText.includes("讲评"), assignmentText.slice(0, 900));
      record("assignment analytics normalizes percent fields", assignmentText.includes("40%") && assignmentText.includes("83%") && !assignmentText.includes("4000%") && !assignmentText.includes("8330%"), assignmentText.slice(0, 900));
      if (viewport.name === "desktop") {
        await checkElementMaxWidth(page, ".teacher-analytics-page", 1440, "assignment desktop analytics width");
      }
      if (viewport.name === "mobile") {
        await checkMinControlHeight(page, ".teacher-analytics-granularity button", 32, "assignment mobile granularity controls");
      }
    },
    selectors: [
      [".teacher-analytics-page", "assignment analytics page"],
      [".teacher-analytics-summary", "assignment analytics metrics"],
      [".teacher-analytics-board", "assignment analytics board"],
      [".teacher-analytics-ai-panel", "assignment AI attribution"],
      [".teacher-analytics-table-row", "assignment problem rows"]
    ]
  },
  {
    name: "assignment-create",
    path: "/app/teacher/assignment/new",
    afterChecks: async page => {
      const activeNav = await page.locator(".teacher-shell-nav a.is-active").allTextContents();
      const createText = ((await page.locator(".assignment-builder-page").first().textContent()) || "").replace(/\s+/g, "");
      record("assignment create belongs to analytics nav", activeNav.join("|").includes("教学分析"), activeNav.join("|"));
      record("assignment create uses three-step workflow", createText.includes("基本信息") && createText.includes("选择题目") && createText.includes("确认发布"), createText.slice(0, 900));
      record("assignment create returns to analytics wording", createText.includes("返回教学分析") && !createText.includes("返回作业中心"), createText.slice(0, 900));
      const hasProblemSearch = await page.getByPlaceholder("搜索题目").count() === 1;
      record("assignment create has problem bank controls", hasProblemSearch && createText.includes("全部难度") && createText.includes("已选题目"), createText.slice(0, 900));
      record("assignment create hides duplicate management links", !createText.includes("管理班级与题目") && !createText.includes("总体统计") && !createText.includes("编辑题目"), createText.slice(0, 900));
    },
    selectors: [
      [".assignment-builder-page", "assignment create page"],
      [".assignment-builder", "assignment builder workflow"],
      [".assignment-problem-bank", "assignment problem bank"],
      [".assignment-selected-panel", "assignment selected summary"],
      [".assignment-publish-review", "assignment publish review"]
    ]
  },
  {
    name: "teacher-management",
    path: "/app/teacher/manage",
    afterChecks: async (page, viewport) => {
      const activeNav = await page.locator(".teacher-shell-nav a.is-active").allTextContents();
      const managementText = ((await page.locator(".teacher-manage-page").first().textContent()) || "").replace(/\s+/g, "");
      record("teacher management redirects to class roster", page.url().includes("/app/teacher/manage/classes"), page.url());
      record("teacher management belongs to roster nav", activeNav.join("|").includes("班级名单"), activeNav.join("|"));
      record("teacher management does not also mark analytics", !activeNav.join("|").includes("教学分析"), activeNav.join("|"));
      record("teacher management shows class roster only", managementText.includes("班级名单") && managementText.includes("名单维护") && managementText.includes("创建班级") && managementText.includes("导入名单"), managementText.slice(0, 900));
      record("teacher management hides unrelated modules", !managementText.includes("AI标准库") && !managementText.includes("检测AI") && !managementText.includes("开课状态") && !managementText.includes("导入题目"), managementText.slice(0, 900));
      if (viewport.name === "desktop") {
        await checkVisible(page, ".management-object-main", "teacher management desktop roster workspace");
      }
      if (viewport.name === "tablet") {
        await checkVisible(page, ".management-object-main", "teacher management tablet roster workspace");
      }
      if (viewport.name === "mobile") {
        await checkStacked(page, ".management-object-list", ".management-object-main", "teacher management mobile roster before editor");
        await checkMinControlHeight(page, ".management-object-row", 44, "teacher management mobile class rows");
      }
    },
    selectors: [
      [".teacher-shell-nav", "teacher shell nav"],
      [".teacher-manage-page", "teacher manage page"],
      [".management-object-workbench--classes", "class roster workbench"],
      [".management-object-list", "class roster list"],
      [".management-object-main", "class roster import panel"]
    ]
  },
  {
    name: "teacher-management-system",
    path: "/app/teacher/manage/system",
    afterChecks: async (page, viewport) => {
      const activeNav = await page.locator(".teacher-shell-nav a.is-active").allTextContents();
      const systemText = ((await page.locator(".teacher-manage-page").first().textContent()) || "").replace(/\s+/g, "");
      record("teacher system belongs to system nav", activeNav.join("|").includes("系统状态"), activeNav.join("|"));
      record("teacher system does not also mark roster", !activeNav.join("|").includes("班级名单"), activeNav.join("|"));
      record("teacher system shows readiness only", systemText.includes("系统状态") && systemText.includes("开课状态") && systemText.includes("检测AI"), systemText.slice(0, 900));
      record("teacher system hides management workbenches", !systemText.includes("创建班级") && !systemText.includes("导入名单") && !systemText.includes("导入题目") && !systemText.includes("AI标准库知识树"), systemText.slice(0, 900));
      if (viewport.name === "mobile") {
        await checkMinControlHeight(page, ".management-readiness__actions .ui-button", 36, "teacher system mobile readiness actions");
      }
    },
    selectors: [
      [".teacher-shell-nav", "teacher shell nav"],
      [".teacher-manage-page", "teacher manage page"],
      [".management-readiness", "system readiness panel"],
      [".management-readiness__head", "system readiness head"],
      [".management-readiness__actions", "system readiness actions"]
    ]
  },
  {
    name: "class-analytics",
    path: "/app/teacher/classes/3",
    beforeChecks: async (page) => {
      await checkVisible(page, ".teacher-analytics-board", "class analytics board");
    },
    afterChecks: async page => {
      const classText = ((await page.locator(".teacher-analytics-page").first().textContent()) || "").replace(/\s+/g, "");
      record("class analytics keeps class assignment problem hierarchy", classText.includes("高一1班") && classText.includes("作业列表") && classText.includes("课堂编程作业"), classText.slice(0, 900));
      record("class analytics shows knowledge attribution", classText.includes("AI知识归因") && classText.includes("知识路径"), classText.slice(0, 900));
      record(
        "class analytics exposes distinct and weighted issue metrics",
        classText.includes("按涉及人数") && classText.includes("按有效次数") && classText.includes("原始8次") && classText.includes("有效6次") && classText.includes("班级重难点"),
        classText.slice(0, 1100)
      );
      record("class analytics normalizes percent fields", classText.includes("40%") && classText.includes("83%") && !classText.includes("4000%") && !classText.includes("8330%"), classText.slice(0, 900));
    },
    selectors: [
      [".teacher-shell-nav", "teacher shell nav"],
      [".teacher-analytics-page", "class analytics page"],
      [".teacher-analytics-summary", "class metrics"],
      [".teacher-analytics-board", "class analytics board"],
      [".teacher-analytics-ai-panel", "class AI attribution"]
    ]
  },
  {
    name: "problem-analytics",
    path: "/app/teacher/classes/3/assignments/7/problems/101",
    afterChecks: async page => {
      const problemText = ((await page.locator(".teacher-analytics-page").first().textContent()) || "").replace(/\s+/g, "");
      record("problem analytics shows problem objective results", problemText.includes("求和边界") && problemText.includes("未通过人数") && problemText.includes("证据样本"), problemText.slice(0, 900));
      record("problem analytics exposes correction in evidence layer", problemText.includes("校正归因") && !problemText.includes("教师动作"), problemText.slice(0, 900));
      record("problem analytics shows observational feedback impact", problemText.includes("查看反馈后的表现") && problemText.includes("查看反馈后改善") && problemText.includes("不能单独证明"), problemText.slice(0, 900));
      record("problem analytics exposes four correction types", problemText.includes("错因判断") && problemText.includes("知识路径") && problemText.includes("证据引用") && problemText.includes("反馈内容"), problemText.slice(0, 900));
      record("problem analytics avoids decision copy", !problemText.includes("下一步") && !problemText.includes("讲评"), problemText.slice(0, 900));
      record("problem analytics normalizes percent fields", problemText.includes("40%") && !problemText.includes("4000%"), problemText.slice(0, 900));
      await page.locator(".language-toggle").dispatchEvent("click");
      await page.waitForTimeout(80);
      const englishProblemText = ((await page.locator(".teacher-analytics-evidence").first().textContent()) || "").replace(/\s+/g, "");
      record(
        "problem analytics renders feedback loop and correction fields in English",
        englishProblemText.includes("Performanceafterviewingfeedback") &&
          englishProblemText.includes("Improvedafterviewingfeedback") &&
          englishProblemText.includes("Correctiontype") &&
          englishProblemText.includes("Issuediagnosis") &&
          englishProblemText.includes("Knowledgepath") &&
          englishProblemText.includes("Evidencereference") &&
          englishProblemText.includes("Feedbackcontent") &&
          !englishProblemText.includes("查看建议后") &&
          !englishProblemText.includes("学生查看建议后"),
        englishProblemText.slice(0, 900)
      );
      await page.screenshot({
        path: join(artifactDir, "problem-analytics-english.png"),
        fullPage: true
      });
      await page.locator(".language-toggle").dispatchEvent("click");
      await page.waitForTimeout(80);
    },
    selectors: [
      [".teacher-analytics-page", "problem analytics page"],
      [".teacher-analytics-summary", "problem metrics"],
      [".teacher-analytics-evidence", "problem evidence samples"],
      [".teacher-analytics-correction", "problem correction layer"]
    ]
  }
];

const viewports = [
  { name: "mobile", width: 390, height: 844 },
  { name: "tablet", width: 820, height: 900 },
  {
    name: "desktop",
    width: Number(process.env.BROWSER_SMOKE_DESKTOP_WIDTH || 1440),
    height: Number(process.env.BROWSER_SMOKE_DESKTOP_HEIGHT || 900)
  }
];

const checks = [];
const unmockedApis = [];
let analysisLookupCount = 0;
let studentFeedbackLookupCount = 0;
let studentFeedbackViewCount = 0;

function record(name, passed, detail = "") {
  checks.push({ name, passed, detail });
}

async function exists(path) {
  try {
    await access(path, constants.R_OK);
    return true;
  } catch {
    return false;
  }
}

async function getFreePort() {
  return new Promise((resolvePort, reject) => {
    const server = createServer();
    server.once("error", reject);
    server.listen(0, "127.0.0.1", () => {
      const address = server.address();
      server.close(() => resolvePort(address.port));
    });
  });
}

async function waitForServer(url, child, previewOutput) {
  const deadline = Date.now() + 30000;
  let lastError = null;
  while (Date.now() < deadline) {
    if (child.exitCode !== null) {
      throw new Error(`preview server exited with code ${child.exitCode}: ${previewOutput.join("").trim()}`);
    }
    try {
      const response = await fetch(url);
      if (response.ok) {
        return;
      }
      lastError = new Error(`HTTP ${response.status}`);
    } catch (error) {
      lastError = error;
    }
    await new Promise(resolveDelay => setTimeout(resolveDelay, 300));
  }
  throw new Error(`preview server did not become ready: ${lastError?.message || "timeout"}`);
}

async function json(route, payload, status = 200) {
  await route.fulfill({
    status,
    contentType: "application/json",
    body: JSON.stringify(payload)
  });
}

async function empty(route, status = 204) {
  await route.fulfill({ status, body: "" });
}

async function routeApi(route) {
  const request = route.request();
  const url = new URL(request.url());
  const path = url.pathname;
  const method = request.method();

  if (path === "/api/invites/resolve" && method === "POST") return json(route, assignment);
  if (path === "/api/teacher/auth/session") return json(route, { authenticated: true });
  if (path === "/api/teacher/auth/login" && method === "POST") return json(route, { authenticated: true });
  if (path === "/api/student/identity" && method === "POST") return json(route, student);
  if (path === "/api/student/login" && method === "POST") return json(route, student);
  if (path === "/api/student/classes") return json(route, classes);
  if (path === "/api/student/profile/41/assignments") return json(route, studentAssignments);
  if (path === "/api/student/assignments/7/profile/41/trajectory") return json(route, trajectory);
  if (path === "/api/student/assignments/8/profile/41/trajectory") return json(route, { ...trajectory, assignment: studentAssignments[1], totalTasks: 1, completedTasks: 0 });
  if (path === "/api/student/assignments/9/profile/41/trajectory") return json(route, { ...trajectory, assignment: studentAssignments[2], totalTasks: 1, completedTasks: 0 });
  if (path === "/api/student/assignments/10/profile/41/trajectory") return json(route, { ...trajectory, assignment: studentAssignments[3], totalTasks: 3, completedTasks: 3 });
  if (path === "/api/student/assignments/7/leaderboard") return json(route, assignmentLeaderboard);
  if (path === "/api/student/assignments/7/submissions") return json(route, assignmentSubmissionPage(url));
  if (path === "/api/student/profile/41/ability-profile") return json(route, abilityProfile);
  if (path === "/api/student/profile/41/recommendations") return json(route, recommendation);
  if (path === "/api/student/profile/41/recommendation-clicks" && method === "POST") return empty(route);

  if (path === "/api/problems/101") return json(route, problem);
  if (path === "/api/problems/101/manage") {
    return json(route, {
      ...problem,
      aiPromptDirection: problem.aiPromptDirection,
      knowledgePoints: problem.knowledgePoints,
      algorithmStrategies: problem.algorithmStrategies,
      commonMistakes: problem.commonMistakes,
      boundaryTypes: problem.boundaryTypes,
      testCases: problem.sampleTestCases.map(item => ({ ...item, hidden: false }))
    });
  }
  if (path === "/api/problems") return json(route, [problem]);
  if (path === "/api/problems/catalog") return json(route, problemCatalog);
  if (path === "/api/leaderboard/problems") {
    return json(route, [
      { problemId: 101, problemTitle: "求和边界", difficulty: "EASY", totalSubmissions: 24, acceptedSubmissions: 8, acceptanceRate: 33.3 },
      { problemId: 102, problemTitle: "循环边界", difficulty: "MEDIUM", totalSubmissions: 18, acceptedSubmissions: 15, acceptanceRate: 83.3 }
    ]);
  }

  if (path === "/api/submissions" && method === "POST") {
    analysisLookupCount = 0;
    studentFeedbackLookupCount = 0;
    studentFeedbackViewCount = 0;
    return json(route, submissionPendingResult);
  }
  if (/^\/api\/submissions\/\d+$/.test(path)) {
    const id = Number(path.split("/").at(-1));
    const item = assignmentSubmissionItems.find(candidate => candidate.id === id);
    return json(route, item ? { ...submissionResult, ...item, sourceCode: submissionResult.sourceCode } : submissionResult);
  }
  if (path === "/api/submissions/9001/student-ai-feedback/view" && method === "POST") {
    studentFeedbackViewCount += 1;
    return empty(route);
  }
  if (path === "/api/submissions/9001/student-ai-feedback") {
    if (method === "POST") {
      studentFeedbackLookupCount = 0;
      return json(route, studentAiFeedbackGenerating, 202);
    }
    studentFeedbackLookupCount += 1;
    if (studentFeedbackLookupCount === 1) {
      return json(route, studentAiFeedbackGenerating, 202);
    }
    return json(route, studentAiFeedbackReady);
  }
  if (path === "/api/submissions/9001/analysis") {
    analysisLookupCount += 1;
    if (analysisLookupCount === 1) {
      return json(route, { status: "PENDING", analysis: null }, 202);
    }
    return json(route, { status: "READY", analysis: submissionResult.analysis });
  }
  if (path === "/api/submissions/9001/coach-prompt") return json(route, coachPrompt);
  if (path === "/api/submissions/9001/coach-turns" && method === "POST") {
    return json(route, {
      ...coachPrompt,
      studentAnswer: "循环应该读取 n 后面的每个数。",
      coachFeedback: "可以。继续检查第一次和最后一次循环。",
      turns: [
        {
          ...coachPrompt,
          id: 7002,
          turnIndex: 2,
          question: "每一轮变化的是哪个变量？",
          studentAnswer: "累计和会变化。",
          coachFeedback: "这个跟踪方向是对的。"
        }
      ]
    });
  }
  if (path === "/api/submissions/problem/101/history-summary") return json(route, history);

  if (path === "/api/teacher/classes") return json(route, classes);
  if (path === "/api/teacher/classes/3/identity-audit") return json(route, identityAudit);
  if (path === "/api/teacher/classes/3/identity-merge" && method === "POST") return json(route, { ...identityAudit, manualIdentityCount: 2, duplicateGroupCount: 0, duplicateGroups: [] });
  if (path === "/api/teacher/classes/3/identity-split" && method === "POST") return json(route, { ...identityAudit, manualIdentityCount: 2, duplicateGroupCount: 0, duplicateGroups: [] });
  if (path === "/api/teacher/assignments") return json(route, [assignment]);
  if (path === "/api/teacher/assignments/7") return json(route, assignment);
  if (path === "/api/teacher/assignments/7/overview") return json(route, assignmentOverview);
  if (path === "/api/teacher/assignments/7/ai-quality") return json(route, aiQualityOverview);
  if (path === "/api/teacher/assignments/7/student-ai-feedback-observability") return json(route, studentAiFeedbackObservability);
  if (path === "/api/teacher/ai-quality/trend") return json(route, aiQualityTrend);
  if (path === "/api/teacher/recommendations/effectiveness") return json(route, recommendationEffectiveness);
  if (path === "/api/teacher/assignments/7/class-review-feedback" && method === "POST") return json(route, { ok: true });
  if (path === "/api/teacher/diagnosis-tags") return json(route, diagnosisTags);
  if (path === "/api/system/executor-status") return json(route, executorStatus);
  if (path === "/api/system/readiness") return json(route, readiness);
  if (path === "/api/system/ai-smoke" && method === "POST") return json(route, { status: "READY", message: "AI smoke ready", latencyMs: 12 });
  if (path === "/api/teacher/ai-standard-library/items") return json(route, aiStandardLibraryItems);
  if (path === "/api/teacher/ai-standard-library/growth-candidates") return json(route, aiStandardLibraryGrowthCandidates);
  if (path === "/api/teacher/ai-standard-library/growth-candidates/governance-summary") return json(route, aiStandardLibraryGrowthGovernanceSummary);
  if (path === "/api/teacher/informatics-knowledge/tree") return json(route, informaticsKnowledgeTree);

  unmockedApis.push(`${method} ${path}`);
  return json(route, { error: `Unmocked API: ${method} ${path}` }, 404);
}

async function checkVisible(page, selector, label) {
  try {
    const locator = page.locator(selector).first();
    await locator.waitFor({ state: "visible", timeout: 10000 });
    const text = (await locator.textContent({ timeout: 2000 }))?.trim() || "";
    record(`${label} visible`, true);
    record(`${label} non-empty`, text.length > 0, selector);
  } catch (error) {
    record(`${label} visible`, false, `${selector}: ${error.message}`);
    record(`${label} non-empty`, false, selector);
  }
}

async function checkNoHorizontalOverflow(page, label) {
  const metrics = await page.evaluate(() => ({
    innerWidth: window.innerWidth,
    docClientWidth: document.documentElement.clientWidth,
    docScrollWidth: document.documentElement.scrollWidth,
    bodyScrollWidth: document.body.scrollWidth
  }));
  const maxWidth = Math.max(metrics.docScrollWidth, metrics.bodyScrollWidth);
  const passed = maxWidth <= metrics.docClientWidth + 2;
  record(`${label} no horizontal overflow`, passed, JSON.stringify(metrics));
}

async function checkImportantControlsVisible(page, label) {
  const result = await page.evaluate(() => {
    const controls = [...document.querySelectorAll("button, a.ui-button, input, textarea, select")];
    const visibleControls = controls.filter(element => {
      const rect = element.getBoundingClientRect();
      const style = window.getComputedStyle(element);
      return rect.width > 0 && rect.height > 0 && style.visibility !== "hidden" && style.display !== "none";
    });
    const clipped = visibleControls.filter(element => {
      const rect = element.getBoundingClientRect();
      return rect.right > window.innerWidth + 2 || rect.left < -2;
    });
    return { visible: visibleControls.length, clipped: clipped.length };
  });
  record(`${label} controls visible`, result.visible > 0, JSON.stringify(result));
  record(`${label} controls not clipped`, result.clipped === 0, JSON.stringify(result));
}

async function rectFor(page, selector) {
  return page.locator(selector).first().evaluate(element => {
    const rect = element.getBoundingClientRect();
    return {
      top: rect.top,
      left: rect.left,
      right: rect.right,
      bottom: rect.bottom,
      width: rect.width,
      height: rect.height
    };
  });
}

async function checkSameRow(page, leftSelector, rightSelector, label) {
  const [left, right] = await Promise.all([rectFor(page, leftSelector), rectFor(page, rightSelector)]);
  const verticalOverlap = Math.min(left.bottom, right.bottom) - Math.max(left.top, right.top);
  const passed = verticalOverlap > Math.min(left.height, right.height) * 0.35 && Math.abs(left.left - right.left) > 12;
  record(label, passed, JSON.stringify({ left, right, verticalOverlap }));
}

async function checkStacked(page, firstSelector, secondSelector, label) {
  const [first, second] = await Promise.all([rectFor(page, firstSelector), rectFor(page, secondSelector)]);
  const passed = first.bottom <= second.top + 4;
  record(label, passed, JSON.stringify({ first, second }));
}

async function checkElementMaxWidth(page, selector, maxWidth, label) {
  const rect = await rectFor(page, selector);
  record(label, rect.width <= maxWidth, JSON.stringify(rect));
}

async function checkMinControlHeight(page, selector, minHeight, label) {
  const result = await page.locator(selector).evaluateAll((elements, expectedHeight) => {
    const visible = elements
      .map(element => element.getBoundingClientRect())
      .filter(rect => rect.width > 0 && rect.height > 0);
    return {
      visible: visible.length,
      tooSmall: visible.filter(rect => rect.height + 0.5 < expectedHeight).length,
      heights: visible.map(rect => Math.round(rect.height * 10) / 10)
    };
  }, minHeight);
  record(label, result.visible > 0 && result.tooSmall === 0, JSON.stringify(result));
}

async function checkDarkReadable(page, selector, label) {
  try {
    const locator = page.locator(selector).first();
    await locator.waitFor({ state: "visible", timeout: 10000 });
    const sample = await locator.evaluate(element => {
      function parseRgb(value) {
        const match = value.match(/rgba?\((\d+),\s*(\d+),\s*(\d+)(?:,\s*([0-9.]+))?\)/);
        if (!match) return null;
        return {
          r: Number(match[1]) / 255,
          g: Number(match[2]) / 255,
          b: Number(match[3]) / 255,
          a: match[4] === undefined ? 1 : Number(match[4])
        };
      }
      function channel(value) {
        return value <= 0.03928 ? value / 12.92 : ((value + 0.055) / 1.055) ** 2.4;
      }
      function luminance(color) {
        return 0.2126 * channel(color.r) + 0.7152 * channel(color.g) + 0.0722 * channel(color.b);
      }
      function contrast(a, b) {
        const l1 = luminance(a);
        const l2 = luminance(b);
        return (Math.max(l1, l2) + 0.05) / (Math.min(l1, l2) + 0.05);
      }
      const textElement = element.querySelector("h1,h2,h3,h4,strong,p,span,button,a") || element;
      const textStyle = window.getComputedStyle(textElement);
      let background = null;
      let current = textElement;
      while (current && !background) {
        const bg = parseRgb(window.getComputedStyle(current).backgroundColor);
        if (bg && bg.a > 0.05) {
          background = bg;
        }
        current = current.parentElement;
      }
      background ||= parseRgb(window.getComputedStyle(document.body).backgroundColor) || { r: 0, g: 0, b: 0, a: 1 };
      const foreground = parseRgb(textStyle.color);
      if (!foreground) {
        return { contrastRatio: 0 };
      }
      return { contrastRatio: contrast(foreground, background) };
    });
    record(`${label} dark readable sample`, sample.contrastRatio >= 2.8, JSON.stringify(sample));
  } catch (error) {
    const text = ((await page.locator("body").textContent().catch(() => "")) || "").replace(/\s+/g, "").slice(0, 500);
    record(`${label} dark readable sample`, false, `${selector}: ${error.message}; body=${text}`);
  }
}

async function runScenario(baseUrl, browser, viewport, scenario) {
  const context = await browser.newContext({ viewport });
  await context.addInitScript(({ studentJson }) => {
    window.localStorage.setItem("wzai:theme", "light");
    window.sessionStorage.setItem("wzai:student", studentJson);
    window.sessionStorage.setItem("wzai:student:7", studentJson);
  }, { studentJson: JSON.stringify(student) });
  await context.route("**/api/**", routeApi);

  const page = await context.newPage();
  const pageErrors = [];
  page.on("pageerror", error => pageErrors.push(error.message));

  const label = `${scenario.name} ${viewport.name}`;
  await page.goto(`${baseUrl}${scenario.path}`, { waitUntil: "domcontentloaded" });
  await page.locator(".app-shell").first().waitFor({ state: "visible", timeout: 10000 });
  if (scenario.beforeChecks) {
    await scenario.beforeChecks(page, viewport);
  }

  for (const [selector, selectorLabel] of scenario.selectors) {
    await checkVisible(page, selector, `${label} ${selectorLabel}`);
  }
  if (scenario.afterChecks) {
    await scenario.afterChecks(page, viewport);
  }
  await checkNoHorizontalOverflow(page, label);
  await checkImportantControlsVisible(page, label);
  record(`${label} no page errors`, pageErrors.length === 0, pageErrors.join(" | "));

  await page.screenshot({
    path: join(artifactDir, `${scenario.name}-${viewport.name}-light.png`),
    fullPage: true
  });

  await page.evaluate(() => {
    document.documentElement.dataset.theme = "dark";
    window.localStorage.setItem("wzai:theme", "dark");
  });
  await page.waitForTimeout(120);
  await checkNoHorizontalOverflow(page, `${label} dark`);
  await checkDarkReadable(page, scenario.selectors[0][0], label);
  await page.screenshot({
    path: join(artifactDir, `${scenario.name}-${viewport.name}-dark.png`),
    fullPage: true
  });

  await context.close();
}

async function main() {
  if (!(await exists(indexPath))) {
    throw new Error(`Built app not found at ${indexPath}. Run npm run build first.`);
  }
  await mkdir(artifactDir, { recursive: true });

  const port = await getFreePort();
  const baseUrl = `http://127.0.0.1:${port}`;
  const child = spawn(process.execPath, [
    viteBin,
    "preview",
    "--host",
    "127.0.0.1",
    "--port",
    String(port),
    "--config",
    "vite.config.mjs",
    "--configLoader",
    "native",
    "--outDir",
    appDir
  ], {
    cwd: frontendDir,
    stdio: ["ignore", "pipe", "pipe"]
  });
  const previewOutput = [];
  child.stdout.on("data", chunk => previewOutput.push(String(chunk)));
  child.stderr.on("data", chunk => previewOutput.push(String(chunk)));

  let browser;
  try {
    await waitForServer(`${baseUrl}/app/`, child, previewOutput);
    try {
      browser = await chromium.launch({
        headless: true,
        ...(process.env.PLAYWRIGHT_EXECUTABLE_PATH
          ? { executablePath: process.env.PLAYWRIGHT_EXECUTABLE_PATH }
          : {})
      });
    } catch (error) {
      throw new Error(`Chromium could not launch. Run "npx playwright install chromium" in frontend. ${error.message}`);
    }

    const requestedScenarios = new Set((process.env.BROWSER_SMOKE_SCENARIOS || "")
      .split(",")
      .map(value => value.trim())
      .filter(Boolean));
    const requestedViewports = new Set((process.env.BROWSER_SMOKE_VIEWPORTS || "")
      .split(",")
      .map(value => value.trim())
      .filter(Boolean));
    const activeScenarios = requestedScenarios.size
      ? scenarios.filter(scenario => requestedScenarios.has(scenario.name))
      : scenarios;
    const activeViewports = requestedViewports.size
      ? viewports.filter(viewport => requestedViewports.has(viewport.name))
      : viewports;
    if (!activeScenarios.length || !activeViewports.length) {
      throw new Error("No browser smoke scenario or viewport matched the requested filter.");
    }
    for (const viewport of activeViewports) {
      for (const scenario of activeScenarios) {
        await runScenario(baseUrl, browser, viewport, scenario);
      }
    }
  } finally {
    if (browser) {
      await browser.close();
    }
    child.kill();
  }

  if (unmockedApis.length) {
    record("all API calls are mocked", false, [...new Set(unmockedApis)].join(", "));
  } else {
    record("all API calls are mocked", true);
  }

  const failed = checks.filter(check => !check.passed);
  if (failed.length) {
    console.error("[browser-smoke] failed checks:");
    failed.forEach(check => console.error(`- ${check.name}${check.detail ? ` (${check.detail})` : ""}`));
    console.error("[browser-smoke] preview output:");
    console.error(previewOutput.join(""));
    process.exit(1);
  }

  console.log(`[browser-smoke] passed ${checks.length} checks; screenshots saved to ${artifactDir}`);
}

main().catch(error => {
  console.error(`[browser-smoke] ${error.message}`);
  process.exit(1);
});
