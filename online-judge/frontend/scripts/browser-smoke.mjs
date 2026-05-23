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
  title: "AI Diagnosis Practice",
  description: "Boundary checks and debugging habits",
  classGroupId: 3,
  className: "Grade 10 Class 1",
  hintPolicy: "L2",
  status: "ACTIVE",
  inviteCode: "WZAI01",
  tasks: [
    { problemId: 101, title: "Sum Guard", difficulty: "EASY", orderIndex: 1, required: true },
    { problemId: 102, title: "Loop Boundary", difficulty: "MEDIUM", orderIndex: 2, required: true }
  ]
};

const student = {
  id: 41,
  classGroupId: 3,
  className: "Grade 10 Class 1",
  displayName: "Student Demo",
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
  stageTransition: "Needs one more correction",
  repeatedIssueTag: "BOUNDARY",
  repeatedFineGrainedTag: "OFF_BY_ONE",
  repeatedIssueCount: 2,
  nextStep: "Re-check loop boundary with the smallest input.",
  attentionReason: "Two recent submissions failed on edge cases.",
  improvementSignal: "The latest answer already narrows the bug to one loop.",
  primaryAbilityFocus: "Boundary reasoning",
  crossProblemSummary: "Errors cluster around inclusive and exclusive ranges.",
  latestCoachInteraction: {
    submissionId: 9001,
    turnCount: 1,
    answeredTurnCount: 1,
    prompted: true,
    answered: true,
    status: "ANSWERED",
    statusLabel: "Coach answered",
    summary: "Student explained the boundary guess.",
    latestQuestion: "What happens when n is 1?",
    latestAnswer: "The loop should still run once.",
    latestFeedback: "Good. Now test the zero-length branch.",
    impact: coachImpact
  },
  latestCoachImpact: coachImpact,
  recentIssueDistribution: [{ label: "BOUNDARY", count: 2 }],
  recentFineGrainedIssueDistribution: [{ label: "OFF_BY_ONE", count: 2 }],
  abilitySummary: [{ abilityPoint: "Boundary reasoning", taskCount: 2, submissionCount: 4, evidenceTags: ["OFF_BY_ONE"] }],
  tasks: [
    {
      problemId: 101,
      title: "Sum Guard",
      difficulty: "EASY",
      attemptCount: 3,
      passed: false,
      latestVerdict: "WRONG_ANSWER",
      latestProgressSignal: "Narrowed to boundary case",
      latestHint: "Check whether the loop includes the last item.",
      latestImprovementSignal: "The second submission fixed input parsing.",
      latestCoachInteraction: {
        submissionId: 9001,
        turnCount: 1,
        answeredTurnCount: 1,
        prompted: true,
        answered: true,
        status: "ANSWERED",
        statusLabel: "Coach answered",
        summary: "Student answered the boundary question.",
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
          progressSignal: "Narrowed to boundary case",
          improvementSignal: "Input parsing improved",
          coachInteraction: {
            submissionId: 9001,
            turnCount: 1,
            answeredTurnCount: 1,
            prompted: true,
            answered: true,
            status: "ANSWERED",
            statusLabel: "Coach answered",
            summary: "Student answered the boundary question.",
            impact: coachImpact
          },
          coachImpact
        }
      ]
    },
    {
      problemId: 102,
      title: "Loop Boundary",
      difficulty: "MEDIUM",
      attemptCount: 1,
      passed: true,
      latestVerdict: "ACCEPTED",
      latestProgressSignal: "Accepted",
      latestHint: "Keep the accepted test as a template.",
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
  primaryAbilityFocus: "Boundary reasoning",
  summary: "Recent evidence points to off-by-one mistakes, not syntax problems.",
  trendSignal: "Accuracy improves after explicit boundary checks.",
  recommendationEffectSummary: "Follow-up practice led to one accepted solution.",
  coachImpactSummary: "AI 追问后已有 1 次同题后续提交通过，建议复盘学生回答中有效的证据意识。",
  latestCoachInteraction: trajectory.latestCoachInteraction,
  latestCoachImpact: coachImpact,
  abilityGaps: [{ abilityPoint: "Boundary reasoning", taskCount: 2, submissionCount: 4, evidenceTags: ["OFF_BY_ONE"] }],
  knowledgeFocus: [{ label: "for loop", count: 3, evidenceProblemIds: [101] }],
  commonMistakeFocus: [{ label: "off by one", count: 2, evidenceProblemIds: [101] }],
  boundaryFocus: [{ label: "n equals 1", count: 2, evidenceProblemIds: [101] }]
};

const recommendation = {
  student,
  summary: "Practice one adjacent task and explain the boundary before coding.",
  recommendations: [
    {
      type: "NEXT_PROBLEM",
      title: "Try a nearby boundary task",
      reason: "It targets the same off-by-one pattern with lower noise.",
      actionLabel: "Practice now",
      problemId: 101,
      problemTitle: "Sum Guard",
      focusAbility: "Boundary reasoning",
      focusTags: ["OFF_BY_ONE", "EDGE_CASE"],
      evidenceProblemIds: [101],
      recommendationToken: "rec-next-101",
      priority: 1
    }
  ]
};

const problem = {
  id: 101,
  title: "Sum Guard",
  description: "# Sum Guard\nRead n numbers and print their sum. Pay attention to the smallest valid input.",
  difficulty: "EASY",
  timeLimit: 1000,
  memoryLimit: 65536,
  aiPromptDirection: "Focus on boundary reasoning before revealing a fix.",
  knowledgePoints: ["loop", "input"],
  algorithmStrategies: ["simulation"],
  commonMistakes: ["off by one"],
  boundaryTypes: ["n equals 1"],
  sampleTestCases: [{ input: "3\n1 2 3\n", expectedOutput: "6\n" }]
};

const submissionResult = {
  id: 9001,
  problemId: 101,
  assignmentId: 7,
  studentProfileId: 41,
  problemTitle: "Sum Guard",
  languageId: 71,
  languageName: "Python 3",
  sourceCode: "n = int(input())\nprint(n)\n",
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
    headline: "Boundary case is not covered yet",
    summary: "The code reads the count but does not consume or sum the following values.",
    issueTags: ["BOUNDARY"],
    abilityPoints: ["Boundary reasoning"],
    focusPoints: ["n equals 1", "loop body"],
    fixDirections: ["Trace the sample input by hand.", "Write a loop that visits every value exactly once."],
    studentHint: "Start with n = 1 and ask which input line your code reads next.",
    teacherNote: "Good candidate for a coach prompt about input contract.",
    progressSignal: "The bug is localized to input handling.",
    confidence: 0.68,
    fineGrainedTags: ["OFF_BY_ONE"],
    evidenceRefs: ["public-case-1"],
    uncertainty: "Could also be an input format misunderstanding.",
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

const coachPrompt = {
  id: 7001,
  assignmentId: 7,
  studentProfileId: 41,
  submissionId: 9001,
  parentPromptId: null,
  turnIndex: 1,
  hintPolicy: "L2",
  promptType: "NEXT_QUESTION",
  question: "If n is 3, which three values should your loop consume?",
  rationale: "This asks the student to reconstruct the input contract.",
  contextSummary: "The failed public case shows the program prints the count instead of the sum.",
  evidenceRefs: ["public-case-1"],
  turns: []
};

const history = [
  {
    id: 9001,
    problemId: 101,
    problemTitle: "Sum Guard",
    languageId: 71,
    languageName: "Python 3",
    verdict: "WRONG_ANSWER",
    executionTime: 28,
    memoryUsed: 8192,
    submittedAt: "2026-05-19T09:03:00",
    passedTestCases: 1,
    totalTestCases: 2,
    analysisStatus: "READY",
    analysisSourceType: "AI",
    analysisHeadline: "Boundary case is not covered yet",
    analysisSummary: "The code reads the count but not the values."
  }
];

const classes = [{ id: 3, name: "Grade 10 Class 1", grade: "10", teacherName: "Teacher Demo" }];

const problemCatalog = [
  { id: 101, title: "Sum Guard", summary: "Practice input loops.", difficulty: "EASY", timeLimit: 1000, memoryLimit: 65536 },
  { id: 102, title: "Loop Boundary", summary: "Practice interval endpoints.", difficulty: "MEDIUM", timeLimit: 1000, memoryLimit: 65536 }
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
      explanation: "Students confuse inclusive and exclusive endpoints.",
      abilityPoint: "Boundary reasoning",
      recommendedHintPolicy: "L2",
      interventionSuggestion: "Ask for the smallest input trace before code changes."
    }
  ],
  classAbilityWeaknesses: [{ abilityPoint: "Boundary reasoning", taskCount: 2, submissionCount: 12, evidenceTags: ["OFF_BY_ONE"] }],
  classReviewSuggestions: [
    {
      title: "Review the smallest input before the next exercise",
      suggestionKey: "review:7:ability:boundary-reasoning:101:off-by-one",
      targetAbility: "Boundary reasoning",
      exampleProblemId: 101,
      exampleProblemTitle: "Sum Guard",
      evidenceTags: ["OFF_BY_ONE"],
      evidenceSubmissionIds: [9001],
      guidingQuestion: "What exact values should the loop visit when n is 1?",
      action: "Use one public case and one teacher-written edge case.",
      evidenceSummary: "Four students repeated the same endpoint mistake.",
      latestFeedback: { actionType: "MODIFIED", teacherNote: "Use a smaller boundary case first.", createdBy: "teacher", createdAt: "2026-05-19T10:00:00" }
    }
  ],
  students: [
    {
      studentProfileId: 41,
      displayName: "Student Demo",
      studentNo: "12",
      attemptCount: 3,
      passedCount: 1,
      latestSubmissionId: 9001,
      latestVerdict: "WRONG_ANSWER",
      latestIssue: "BOUNDARY",
      latestIssueTag: "BOUNDARY",
      latestFineGrainedIssue: "OFF_BY_ONE",
      latestProgressSignal: "Narrowed to boundary case",
      latestConfidence: 0.68,
      latestUncertainty: "Could be input contract confusion.",
      latestAnswerLeakRisk: "LOW",
      latestCorrection: null,
      latestCoachInteraction: trajectory.latestCoachInteraction,
      latestCoachImpact: coachImpact,
      primaryAbilityFocus: "Boundary reasoning",
      crossProblemSummary: "Repeated endpoint mistakes across two problems.",
      abilitySummary: [{ abilityPoint: "Boundary reasoning", taskCount: 2, submissionCount: 4, evidenceTags: ["OFF_BY_ONE"] }],
      repeatedIssueTag: "BOUNDARY",
      repeatedFineGrainedTag: "OFF_BY_ONE",
      repeatedIssueCount: 2,
      attentionReason: "Repeated wrong answer on public and hidden edge cases.",
      attentionEvidence: [
        {
          submissionId: 9001,
          problemId: 101,
          verdict: "WRONG_ANSWER",
          submittedAt: "2026-05-19T09:03:00",
          issueTag: "BOUNDARY",
          fineGrainedTag: "OFF_BY_ONE",
          abilityPoint: "Boundary reasoning",
          headline: "Boundary case is not covered yet",
          reason: "Public sample failed."
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
  summary: "Most AI feedback is stable, but boundary tags need eval samples.",
  correctedTags: [{ originalTag: "LOGIC", originalLabel: "Logic", correctedTag: "BOUNDARY", correctedLabel: "Boundary", count: 2 }]
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
  summary: "Cross-assignment trend: boundary diagnosis still needs more eval coverage.",
  assignments: [
    { assignmentId: 7, assignmentTitle: "AI Diagnosis Practice", analyzedSubmissionCount: 24, correctionCount: 3, evalCandidateCount: 5, lowConfidenceCount: 4, highLeakRiskCount: 0, correctionRate: 12.5, lowConfidenceRate: 16.7, highLeakRiskRate: 0, summary: "Boundary-heavy class" },
    { assignmentId: 6, assignmentTitle: "Loop Review", analyzedSubmissionCount: 18, correctionCount: 2, evalCandidateCount: 3, lowConfidenceCount: 2, highLeakRiskCount: 1, correctionRate: 11.1, lowConfidenceRate: 11.1, highLeakRiskRate: 5.6, summary: "One leak-risk sample" }
  ],
  correctedTags: [{ tag: "BOUNDARY", label: "Boundary", count: 5, evalCandidateCount: 4 }],
  evalNeededTags: [{ tag: "OFF_BY_ONE", label: "Off by one", count: 6, evalCandidateCount: 5 }],
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
  summary: "Recommendation follow-ups are visible; one accepted submission and one repeated focus issue need review.",
  byType: [
    { key: "REDO", label: "Redo", exposureCount: 1, clickCount: 1, enteredProblemCount: 1, followupSubmissionCount: 1, acceptedFollowupCount: 0, sameFocusIssueCount: 1, clickThroughRate: 100, followupSubmissionRate: 100, acceptedFollowupRate: 0, sameFocusIssueRate: 100 },
    { key: "NEXT_PROBLEM", label: "Next problem", exposureCount: 1, clickCount: 1, enteredProblemCount: 0, followupSubmissionCount: 1, acceptedFollowupCount: 1, sameFocusIssueCount: 0, clickThroughRate: 100, followupSubmissionRate: 100, acceptedFollowupRate: 100, sameFocusIssueRate: 0 },
    { key: "REVIEW", label: "Review", exposureCount: 1, clickCount: 1, enteredProblemCount: 0, followupSubmissionCount: 0, acceptedFollowupCount: 0, sameFocusIssueCount: 0, clickThroughRate: 100, followupSubmissionRate: 0, acceptedFollowupRate: 0, sameFocusIssueRate: 0 }
  ],
  focusTags: [
    { key: "OFF_BY_ONE", label: "Off by one", exposureCount: 1, clickCount: 1, enteredProblemCount: 1, followupSubmissionCount: 1, acceptedFollowupCount: 0, sameFocusIssueCount: 1, clickThroughRate: 100, followupSubmissionRate: 100, acceptedFollowupRate: 0, sameFocusIssueRate: 100 },
    { key: "INPUT_PARSING", label: "Input parsing", exposureCount: 1, clickCount: 1, enteredProblemCount: 0, followupSubmissionCount: 1, acceptedFollowupCount: 1, sameFocusIssueCount: 0, clickThroughRate: 100, followupSubmissionRate: 100, acceptedFollowupRate: 100, sameFocusIssueRate: 0 }
  ]
};

const identityAudit = {
  classGroupId: 3,
  className: "Grade 10 Class 1",
  totalProfiles: 4,
  stableIdentityCount: 3,
  manualIdentityCount: 1,
  legacyIdentityCount: 0,
  missingStudentNoCount: 1,
  duplicateGroupCount: 1,
  duplicateGroups: [
    {
      stableIdentityKey: "class:3:no:12",
      reason: "Same student number and similar display name",
      studentProfileIds: [41, 42],
      displayNames: ["Student Demo", "Student Demo A"],
      studentNos: ["12"],
      identityKeys: ["stable:3:12", "legacy:42"]
    }
  ]
};

const diagnosisTags = [
  { id: "BOUNDARY", label: "Boundary", teacherExplanation: "Endpoint or edge-case mistake.", abilityPoint: "Boundary reasoning", fineGrained: false },
  { id: "OFF_BY_ONE", label: "Off by one", teacherExplanation: "Loop visits one too many or one too few items.", abilityPoint: "Boundary reasoning", fineGrained: true, parentTag: "BOUNDARY" }
];

const executorStatus = {
  mode: "LOCAL",
  executorType: "mock",
  dockerAvailable: false,
  pythonAvailable: true,
  cppAvailable: true,
  message: "Smoke executor is available."
};

const scenarios = [
  {
    name: "student",
    path: "/app/student?code=WZAI01",
    selectors: [
      [".student-assignment-grid.is-ready", "student assignment shell"],
      [".student-task-panel", "student task panel"],
      [".student-side-flow", "student status side flow"],
      [".student-task-card .ui-button", "student problem action"]
    ]
  },
  {
    name: "problem",
    path: "/app/problem/101?assignmentId=7&studentProfileId=41&recommendationToken=rec-next-101",
    beforeChecks: async page => {
      await page.locator(".panel--editor button.ui-button--primary").first().click();
      const details = page.locator("details.problem-compact-details", { hasText: "下一问" }).first();
      await details.waitFor({ state: "attached", timeout: 10000 });
      await details.evaluate(element => {
        element.open = true;
      });
      await page.locator(".coach-next-question").first().waitFor({ state: "visible", timeout: 10000 });
    },
    selectors: [
      [".practice-command", "problem command area"],
      [".practice-focus-strip", "problem focus strip"],
      [".panel--ai", "problem AI result panel"],
      [".coach-next-question", "coach next question"],
      [".testcase-compact-list", "testcase result list"]
    ]
  },
  {
    name: "teacher",
    path: "/app/teacher",
    selectors: [
      [".teacher-rail", "teacher assignment rail"],
      [".teacher-stage", "teacher classroom stage"],
      [".teacher-main-grid", "teacher classroom grid"],
      [".teacher-compact-details", "teacher compact details"],
      [".teacher-student-row", "teacher student row"]
    ]
  },
  {
    name: "teacher-management",
    path: "/app/teacher-management",
    beforeChecks: async page => {
      const identityDetails = page.locator("details.management-compact-details", { hasText: "身份审计" }).first();
      await identityDetails.waitFor({ state: "attached", timeout: 10000 });
      await identityDetails.evaluate(element => {
        element.open = true;
      });
    },
    selectors: [
      [".management-console", "management console"],
      [".management-identity-audit", "identity audit panel"],
      [".management-identity-audit__metrics", "identity audit metrics"],
      [".management-identity-groups", "identity duplicate groups"]
    ]
  }
];

const viewports = [
  { name: "desktop", width: 1366, height: 900 },
  { name: "mobile", width: 390, height: 844 }
];

const checks = [];
const unmockedApis = [];

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
  if (path === "/api/student/assignments/7/profile/41/trajectory") return json(route, trajectory);
  if (path === "/api/student/profile/41/ability-profile") return json(route, abilityProfile);
  if (path === "/api/student/profile/41/recommendations") return json(route, recommendation);
  if (path === "/api/student/profile/41/recommendation-clicks" && method === "POST") return empty(route);

  if (path === "/api/problems/101") return json(route, problem);
  if (path === "/api/problems") return json(route, [problem]);
  if (path === "/api/problems/catalog") return json(route, problemCatalog);

  if (path === "/api/submissions" && method === "POST") return json(route, submissionResult);
  if (path === "/api/submissions/9001") return json(route, submissionResult);
  if (path === "/api/submissions/9001/analysis") return json(route, { analysis: submissionResult.analysis });
  if (path === "/api/submissions/9001/coach-prompt") return json(route, coachPrompt);
  if (path === "/api/submissions/9001/coach-turns" && method === "POST") {
    return json(route, {
      ...coachPrompt,
      studentAnswer: "The loop should consume each value after n.",
      coachFeedback: "Good. Now verify the first and last iteration.",
      turns: [
        {
          ...coachPrompt,
          id: 7002,
          turnIndex: 2,
          question: "Which variable changes on each iteration?",
          studentAnswer: "The running sum changes.",
          coachFeedback: "That is the right trace target."
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
  if (path === "/api/teacher/assignments/7/overview") return json(route, assignmentOverview);
  if (path === "/api/teacher/assignments/7/ai-quality") return json(route, aiQualityOverview);
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

async function checkDarkReadable(page, selector, label) {
  const sample = await page.locator(selector).first().evaluate(element => {
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
