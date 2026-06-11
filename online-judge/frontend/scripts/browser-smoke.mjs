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
        evidenceRefs: ["public-case-1"],
        qualitySignals: ["evidence_grounded", "actionable"]
      }
    ],
    improvementItems: [
      {
        title: "测试习惯",
        body: "提交前先跑最小公开样例。能更早发现输入格式问题。",
        kind: "TESTING_HABIT",
        evidenceRefs: ["public-case-1"],
        qualitySignals: ["transfer"]
      }
    ],
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
    analysisSummary: "代码只读取了数量，没有读取数值。"
  }
];

const classes = [{ id: 3, name: "高一1班", grade: "10", teacherName: "信息技术老师" }];

const problemCatalog = [
  { id: 101, title: "求和边界", summary: "练习输入循环。", difficulty: "EASY", timeLimit: 1000, memoryLimit: 65536 },
  { id: 102, title: "循环边界", summary: "练习循环端点。", difficulty: "MEDIUM", timeLimit: 1000, memoryLimit: 65536 }
];

const assignmentOverview = {
  assignment,
  participantCount: 18,
  attemptCount: 32,
  passedAttemptCount: 15,
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

const scenarios = [
  {
    name: "app-entry",
    path: "/app/",
    selectors: [
      [".route-hub-page", "route hub page"],
      [".route-hub-card", "route hub cards"]
    ],
    afterChecks: async page => {
      const navLabels = await page.locator(".top-nav__link span").allTextContents();
      const hubText = ((await page.locator(".route-hub-page").first().textContent()) || "").replace(/\s+/g, "");
      record("app root is explicit route hub", page.url().endsWith("/app") || page.url().endsWith("/app/"), page.url());
      record("app root lists clear workspaces", hubText.includes("学生练习") && hubText.includes("作业中心") && hubText.includes("题库与班级"), hubText.slice(0, 700));
      record("app root exposes canonical paths", hubText.includes("/app/student") && hubText.includes("/app/teacher") && hubText.includes("/app/teacher-management"), hubText.slice(0, 700));
      record("app root keeps top nav simple", navLabels.join("|") === "学生端|教师端", navLabels.join("|"));
    }
  },
  {
    name: "student-default",
    path: "/app/student",
    selectors: [
      [".student-entry-list", "student default entry"],
      [".app-header", "application header"]
    ],
    afterChecks: async page => {
      const navCount = await page.locator(".top-nav__link").count();
      record("student direct URL opens entry", page.url().includes("/app/student"), page.url());
      record("student entry has no business top nav", navCount === 0, `nav count ${navCount}`);
    }
  },
  {
    name: "public-assignment",
    path: "/app/student/assignments/public",
    afterChecks: async (page, viewport) => {
      const navCount = await page.locator(".top-nav__link").count();
      const workbenchText = ((await page.locator(".problem-workbench").first().textContent()) || "").replace(/\s+/g, "");
      const taskItemHeight = await page.locator(".problem-task-item").first().evaluate(element => element.getBoundingClientRect().height);
      record("public assignment has no business top nav", navCount === 0, `nav count ${navCount}`);
      record("public assignment uses catalog task list", workbenchText.includes("求和边界") && workbenchText.includes("循环边界"), workbenchText);
      record("public assignment opens workbench", page.url().includes("/app/student/assignments/public/problems/101"), page.url());
      record("public assignment task list is compact", taskItemHeight <= 48, `item height ${taskItemHeight}`);
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
      await page.locator(".student-entry-list").first().waitFor({ state: "visible", timeout: 10000 });
      const navCount = await page.locator(".top-nav__link").count();
      const inviteFormCount = await page.locator("text=输入邀请码").count();
      const homeText = ((await page.locator(".student-home").first().textContent()) || "").replace(/\s+/g, "");
      const entryCount = await page.locator(".student-entry-link").count();
      const headerLoginCount = await page.locator(".header-login-link, .header-student-chip").count();
      const studentWidth = await page.locator(".student-home").first().evaluate(element => element.getBoundingClientRect().width);
      const overflow = await page.evaluate(() => document.documentElement.scrollWidth - document.documentElement.clientWidth);
      const noiseTextPattern = /公开|登录后查看|进入|班级作业|老师布置的作业/;
      record("student home has no business top nav", navCount === 0, `nav count ${navCount}`);
      record("student identity lives in header", headerLoginCount >= 1, `header identity count ${headerLoginCount}`);
      record("student home shows public catalog entry", homeText.includes("公共题库"), homeText);
      record("student home shows assignment details", homeText.includes("课堂编程作业") && homeText.includes("高一1班") && homeText.includes("2题"), homeText);
      record("student home keeps action copy minimal", !noiseTextPattern.test(homeText), homeText);
      record("student home uses clickable entry rows", entryCount >= 1, `entry count ${entryCount}`);
      if (viewport.name !== "mobile") {
        record("student home is centered on wider screens", studentWidth <= 860, `student width ${studentWidth}`);
      }
      record("student home has no horizontal overflow", overflow <= 1, `overflow ${overflow}`);
      record("student no longer starts with invite", inviteFormCount === 0, `invite form count ${inviteFormCount}`);
    },
    selectors: [
      [".header-login-link, .header-student-chip", "student header identity"],
      [".student-entry-list", "student entry list"],
      [".student-entry-link", "student entry row"]
    ]
  },
  {
    name: "student-assignment",
    path: "/app/student/assignments/7",
    afterChecks: async (page, viewport) => {
      const navCount = await page.locator(".top-nav__link").count();
      const workbenchText = ((await page.locator(".problem-workbench").first().textContent()) || "").replace(/\s+/g, "");
      const taskItemHeight = await page.locator(".problem-task-item").first().evaluate(element => element.getBoundingClientRect().height);
      record("student assignment has no business top nav", navCount === 0, `nav count ${navCount}`);
      record("student assignment redirects to workbench", page.url().includes("/app/student/assignments/7/problems/101"), page.url());
      record("student assignment workbench has task list", workbenchText.includes("求和边界") && workbenchText.includes("循环边界"), workbenchText);
      record("student assignment task list is compact", taskItemHeight <= 48, `item height ${taskItemHeight}`);
      if (viewport.name === "mobile") {
        await checkVisible(page, ".problem-mobile-jump", "student assignment mobile code jump");
      }
    },
    selectors: [
      [".problem-task-sidebar", "student assignment task sidebar"],
      [".panel--statement", "student assignment statement"],
      [".panel--editor", "student assignment editor"]
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
      record("problem has no business top nav", navCount === 0, navLabels.join("|"));
      record("problem removes duplicate command banner", commandCount === 0, `command banner count ${commandCount}`);
      record("problem statement title is the current problem", statementTitle.includes("求和边界"), statementTitle);
      record("problem task list is compact", taskItemHeight <= 48, `item height ${taskItemHeight}`);
      record("problem modal shows pending AI feedback state first", modalText.includes("AI分析中"), modalText);
      record(
        "problem modal does not invent guidance while analysis pending",
        !modalText.includes("先看测试点") &&
          !modalText.includes("先把当前错误修掉") &&
          !modalText.includes("先看左侧失败点") &&
          !modalText.includes("暂无提升建议"),
        modalText
      );
      await page.waitForFunction(() => !document.body.innerText.includes("AI 分析中"), null, { timeout: 10000 });
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
      record("problem modal uses student AI feedback endpoint", studentFeedbackLookupCount >= 2, `student feedback lookup count ${studentFeedbackLookupCount}`);
      record("problem modal records model feedback view", studentFeedbackViewCount === 1, `student feedback view count ${studentFeedbackViewCount}`);
      await page.locator(".problem-result-modal__footer button").filter({ hasText: "继续修改" }).click();
      await checkVisible(page, ".problem-last-result", "problem last result entry after modal close");
      if (viewport.name === "mobile") {
        await checkVisible(page, ".problem-mobile-jump", "problem mobile code jump");
      }
    },
    selectors: [
      [".problem-layout", "problem main layout"],
      [".problem-task-sidebar", "problem task sidebar"],
      [".problem-back-link", "problem back link"],
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
      const navLabels = await page.locator(".top-nav__link span").allTextContents();
      const activeNav = await page.locator(".top-nav__link.is-active span").allTextContents();
      const teacherText = ((await page.locator(".teacher-page").first().textContent()) || "").replace(/\s+/g, "");
      record("teacher global nav is classroom-first", navLabels.join("|") === "课堂|管理|题目", navLabels.join("|"));
      record("teacher nav is active", activeNav.includes("课堂"), activeNav.join("|"));
      record("teacher home is assignment workflow", teacherText.includes("作业中心") && teacherText.includes("管理作业") && teacherText.includes("进入作业"), teacherText.slice(0, 800));
      record("teacher home hides invite code", !teacherText.includes("邀请码") && !teacherText.includes("WZAI01"), teacherText.slice(0, 800));
      record("teacher home shows assignment row essentials", teacherText.includes("高一1班") && teacherText.includes("2题") && teacherText.includes("通过率") && teacherText.includes("需关注"), teacherText.slice(0, 800));
      record("teacher home keeps diagnostics out of main flow", !teacherText.includes("Coach") && !teacherText.includes("教师校正") && !teacherText.includes("错因证据"), teacherText.slice(0, 800));
      record("teacher home removes duplicate nav actions", !teacherText.includes("管理班级与题目") && !teacherText.includes("总体统计") && !teacherText.includes("刷新") && !teacherText.includes("编辑题目"), teacherText.slice(0, 800));
      record("teacher assignment rows keep one primary action", !teacherText.includes("编辑作业") && !teacherText.includes("编辑进入作业"), teacherText.slice(0, 800));
      record("teacher create action goes to dedicated page", await page.locator('a[href="/app/teacher/assignment/new"]').first().isVisible(), teacherText.slice(0, 800));
      record(
        "teacher main copy hides engineering tokens",
        !/BLOCKED|RECOVERED|NOT_COMPARABLE|fallback|smoke|profile/i.test(teacherText),
        teacherText.slice(0, 800)
      );
      if (viewport.name === "desktop") {
        await checkElementMaxWidth(page, ".teacher-workflow", 1370, "teacher desktop workbench width");
        await checkSameRow(page, ".teacher-workflow-panel__head", ".teacher-workflow-filters", "teacher desktop filters stay beside heading");
      }
      if (viewport.name === "mobile") {
        await checkMinControlHeight(page, ".teacher-workflow-header .ui-button", 44, "teacher mobile primary action");
      }
    },
    selectors: [
      [".teacher-workflow-header", "teacher assignment workflow header"],
      [".teacher-workflow-summary", "teacher assignment workflow summary"],
      [".teacher-workflow-panel", "teacher assignment workflow panel"],
      [".teacher-assignment-table", "teacher assignment table"]
    ]
  },
  {
    name: "assignment-detail",
    path: "/app/teacher/assignment/7",
    afterChecks: async (page, viewport) => {
      const activeNav = await page.locator(".top-nav__link.is-active span").allTextContents();
      const assignmentText = ((await page.locator(".assignment-detail-page").first().textContent()) || "").replace(/\s+/g, "");
      record("assignment detail belongs to classroom nav", activeNav.includes("课堂"), activeNav.join("|"));
      record("assignment detail hides invite code", !assignmentText.includes("邀请码") && !assignmentText.includes("WZAI01"), assignmentText.slice(0, 900));
      record("assignment detail exposes workflow tabs", assignmentText.includes("概览") && assignmentText.includes("学生") && assignmentText.includes("题目") && assignmentText.includes("诊断"), assignmentText.slice(0, 900));
      record("assignment detail defaults to overview", assignmentText.includes("完成情况") && assignmentText.includes("优先看谁") && assignmentText.includes("优先讲哪题"), assignmentText.slice(0, 900));
      record("assignment detail keeps diagnostics out of overview", !assignmentText.includes("AI/Coach/教师校正") && !assignmentText.includes("教师校正已保存"), assignmentText.slice(0, 900));
      await page.locator(".assignment-detail-tabs").getByRole("button", { name: "诊断", exact: true }).click();
      const diagnosisText = ((await page.locator(".assignment-diagnosis-tab").first().textContent()) || "").replace(/\s+/g, "");
      record("assignment diagnosis tab owns AI and correction", diagnosisText.includes("AI/Coach/教师校正") && diagnosisText.includes("校正错因"), diagnosisText.slice(0, 900));
      if (viewport.name === "desktop") {
        await checkSameRow(page, ".assignment-detail-header > div:first-child", ".assignment-detail-score", "assignment desktop summary score beside title");
      }
      if (viewport.name === "mobile") {
        await checkMinControlHeight(page, ".assignment-detail-tabs button", 42, "assignment mobile tabs stay tappable");
      }
    },
    selectors: [
      [".assignment-detail-header", "assignment detail header"],
      [".assignment-detail-tabs", "assignment detail tabs"],
      [".assignment-overview-tab", "assignment overview tab"],
      [".assignment-overview-grid", "assignment overview decision cards"]
    ]
  },
  {
    name: "assignment-create",
    path: "/app/teacher/assignment/new",
    afterChecks: async page => {
      const activeNav = await page.locator(".top-nav__link.is-active span").allTextContents();
      const createText = ((await page.locator(".assignment-builder-page").first().textContent()) || "").replace(/\s+/g, "");
      record("assignment create belongs to classroom nav", activeNav.includes("课堂"), activeNav.join("|"));
      record("assignment create uses three-step workflow", createText.includes("基本信息") && createText.includes("选择题目") && createText.includes("确认发布"), createText.slice(0, 900));
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
    path: "/app/teacher-management",
    afterChecks: async (page, viewport) => {
      const activeNav = await page.locator(".top-nav__link.is-active span").allTextContents();
      const managementText = ((await page.locator(".teacher-management-page").first().textContent()) || "").replace(/\s+/g, "");
      record("teacher management belongs to management nav", activeNav.includes("管理"), activeNav.join("|"));
      record("teacher management does not also mark classroom", !activeNav.includes("课堂"), activeNav.join("|"));
      record("teacher management removes internal classroom tab", !managementText.includes("课堂") && !managementText.includes("系统状态") && !managementText.includes("身份审计"), managementText.slice(0, 900));
      record("teacher management keeps basic actions", managementText.includes("创建班级") && managementText.includes("导入名单") && managementText.includes("导入题目"), managementText.slice(0, 900));
      if (viewport.name === "desktop") {
        await checkSameRow(page, ".management-task-card:nth-child(1)", ".management-task-card:nth-child(2)", "teacher management desktop first two tasks share row");
        await checkSameRow(page, ".management-task-card:nth-child(2)", ".management-task-card:nth-child(3)", "teacher management desktop import tasks share row");
      }
      if (viewport.name === "tablet") {
        await checkSameRow(page, ".management-task-card:nth-child(1)", ".management-task-card:nth-child(2)", "teacher management tablet first two tasks share row");
        await checkStacked(page, ".management-task-card:nth-child(2)", ".management-task-card:nth-child(3)", "teacher management tablet problem import wraps");
      }
      if (viewport.name === "mobile") {
        await checkStacked(page, ".management-task-card:nth-child(1)", ".management-task-card:nth-child(2)", "teacher management mobile class before roster");
        await checkStacked(page, ".management-task-card:nth-child(2)", ".management-task-card:nth-child(3)", "teacher management mobile roster before problems");
        await checkMinControlHeight(page, ".management-task-card .ui-button", 44, "teacher management mobile actions");
      }
    },
    selectors: [
      [".management-console", "management console"],
      [".management-task-card", "management task card"],
      [".management-file-picker", "file picker"]
    ]
  },
  {
    name: "task-editor",
    path: "/app/task-editor?id=101",
    selectors: [
      [".editor-command", "editor command"],
      [".editor-workbench--single", "single editor workbench"],
      [".editor-secondary-drawer", "editor secondary drawer"],
      [".editor-test-row", "editor testcase row"]
    ]
  },
  {
    name: "class-overview",
    path: "/app/class-overview",
    selectors: [
      [".overview-command", "overview command"],
      [".metric-grid", "overview metrics"],
      [".overview-insight-grid", "overview insight grid"],
      [".overview-coverage-row", "overview coverage row"],
      [".overview-compact-details", "overview details"]
    ]
  }
];

const viewports = [
  { name: "mobile", width: 390, height: 844 },
  { name: "tablet", width: 820, height: 900 },
  { name: "desktop", width: 1440, height: 900 }
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
  if (path === "/api/student/identity" && method === "POST") return json(route, student);
  if (path === "/api/student/login" && method === "POST") return json(route, student);
  if (path === "/api/student/profile/41/assignments") return json(route, [assignment]);
  if (path === "/api/student/assignments/7/profile/41/trajectory") return json(route, trajectory);
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
  if (path === "/api/submissions/9001") return json(route, submissionResult);
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
    await scenario.beforeChecks(page);
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
      browser = await chromium.launch({ headless: true });
    } catch (error) {
      throw new Error(`Chromium could not launch. Run "npx playwright install chromium" in frontend. ${error.message}`);
    }

    for (const viewport of viewports) {
      for (const scenario of scenarios) {
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
