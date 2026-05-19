export type HintPolicy = "L1" | "L2" | "L3" | "L4";
export type AssignmentStatus = "DRAFT" | "ACTIVE" | "CLOSED";
export type Difficulty = "EASY" | "MEDIUM" | "HARD";
export type Verdict =
  | "PENDING"
  | "ACCEPTED"
  | "WRONG_ANSWER"
  | "TIME_LIMIT_EXCEEDED"
  | "MEMORY_LIMIT_EXCEEDED"
  | "RUNTIME_ERROR"
  | "COMPILATION_ERROR"
  | "INTERNAL_ERROR";

export interface AssignmentTask {
  problemId: number;
  title: string;
  difficulty: string;
  orderIndex?: number;
  required?: boolean;
}

export interface Assignment {
  id: number;
  title: string;
  description?: string;
  classGroupId?: number | null;
  className?: string | null;
  hintPolicy: HintPolicy;
  status: AssignmentStatus;
  startsAt?: string | null;
  endsAt?: string | null;
  createdAt?: string;
  inviteCode?: string | null;
  tasks: AssignmentTask[];
}

export interface StudentProfile {
  id: number;
  classGroupId?: number | null;
  className?: string | null;
  displayName: string;
  studentNo?: string | null;
}

export interface StudentTrajectoryIssue {
  label: string;
  count: number;
}

export interface AbilityStat {
  abilityPoint: string;
  taskCount: number;
  submissionCount: number;
  evidenceTags?: string[];
}

export interface StudentTrajectoryPoint {
  submissionId: number;
  verdict: Verdict | string;
  submittedAt?: string;
  issueTags?: string[];
  fineGrainedTags?: string[];
  progressSignal?: string | null;
  improvementSignal?: string | null;
  coachInteraction?: CoachInteractionSummary | null;
  coachImpact?: CoachImpact | null;
}

export interface StudentTrajectoryTask {
  problemId: number;
  title: string;
  difficulty: string;
  attemptCount: number;
  passed: boolean;
  latestVerdict?: Verdict | string | null;
  latestProgressSignal?: string | null;
  latestHint?: string | null;
  latestImprovementSignal?: string | null;
  latestCoachInteraction?: CoachInteractionSummary | null;
  latestCoachImpact?: CoachImpact | null;
  submissions: StudentTrajectoryPoint[];
}

export interface CoachInteractionSummary {
  submissionId?: number | null;
  turnCount: number;
  answeredTurnCount: number;
  prompted: boolean;
  answered: boolean;
  status?: string | null;
  statusLabel?: string | null;
  summary?: string | null;
  latestQuestion?: string | null;
  latestAnswer?: string | null;
  latestFeedback?: string | null;
  latestAt?: string | null;
  impact?: CoachImpact | null;
}

export interface CoachImpact {
  coachedSubmissionId?: number | null;
  followupSubmissionId?: number | null;
  problemId?: number | null;
  status?: string | null;
  statusLabel?: string | null;
  summary?: string | null;
  previousVerdict?: string | null;
  followupVerdict?: string | null;
  previousIssueTag?: string | null;
  previousFineGrainedTag?: string | null;
  followupIssueTag?: string | null;
  followupFineGrainedTag?: string | null;
  answeredAt?: string | null;
  followupSubmittedAt?: string | null;
}

export interface StudentTrajectory {
  assignment: Assignment;
  student: StudentProfile;
  totalTasks: number;
  completedTasks: number;
  totalAttempts: number;
  stageTransition?: string | null;
  repeatedIssueTag?: string | null;
  repeatedFineGrainedTag?: string | null;
  repeatedIssueCount: number;
  nextStep?: string | null;
  attentionReason?: string | null;
  improvementSignal?: string | null;
  primaryAbilityFocus?: string | null;
  crossProblemSummary?: string | null;
  latestCoachInteraction?: CoachInteractionSummary | null;
  latestCoachImpact?: CoachImpact | null;
  recentIssueDistribution: StudentTrajectoryIssue[];
  recentFineGrainedIssueDistribution?: StudentTrajectoryIssue[];
  abilitySummary?: AbilityStat[];
  tasks: StudentTrajectoryTask[];
}

export interface ProfileStat {
  label: string;
  count: number;
  evidenceProblemIds?: number[];
}

export interface StudentAbilityProfile {
  student: StudentProfile;
  mergedStudentProfileIds: number[];
  totalSubmissions: number;
  problemCount: number;
  assignmentCount: number;
  failedSubmissionCount: number;
  primaryAbilityFocus?: string | null;
  summary?: string | null;
  trendSignal?: string | null;
  recommendationEffectSummary?: string | null;
  coachImpactSummary?: string | null;
  latestCoachInteraction?: CoachInteractionSummary | null;
  latestCoachImpact?: CoachImpact | null;
  abilityGaps?: AbilityStat[];
  knowledgeFocus?: ProfileStat[];
  commonMistakeFocus?: ProfileStat[];
  boundaryFocus?: ProfileStat[];
}

export interface StudentRecommendationItem {
  type: "REDO" | "NEXT_PROBLEM" | "REVIEW" | string;
  title: string;
  reason?: string | null;
  actionLabel?: string | null;
  problemId?: number | null;
  problemTitle?: string | null;
  focusAbility?: string | null;
  focusTags?: string[];
  evidenceProblemIds?: number[];
  recommendationToken?: string | null;
  priority: number;
}

export interface StudentRecommendation {
  student: StudentProfile;
  summary?: string | null;
  recommendations: StudentRecommendationItem[];
}

export interface SampleTestCase {
  input: string;
  expectedOutput: string;
}

export interface Problem {
  id: number;
  title: string;
  description: string;
  difficulty: Difficulty | string;
  timeLimit: number;
  memoryLimit: number;
  aiPromptDirection?: string | null;
  knowledgePoints?: string[];
  algorithmStrategies?: string[];
  commonMistakes?: string[];
  boundaryTypes?: string[];
  createdAt?: string;
  sampleTestCases: SampleTestCase[];
}

export interface ProblemCatalogItem {
  id: number;
  title: string;
  summary?: string | null;
  difficulty: Difficulty | string;
  timeLimit: number;
  memoryLimit: number;
  createdAt?: string;
}

export interface ProblemManage extends Problem {
  testCases: Array<{
    id?: number;
    input: string;
    expectedOutput: string;
    hidden: boolean;
    orderIndex?: number;
  }>;
}

export interface SubmissionAnalysis {
  submissionId: number;
  sourceType?: string;
  scenario?: string;
  headline?: string;
  summary?: string;
  issueTags?: string[];
  abilityPoints?: string[];
  focusPoints?: string[];
  fixDirections?: string[];
  studentHint?: string;
  teacherNote?: string;
  progressSignal?: string;
  confidence?: number;
  fineGrainedTags?: string[];
  evidenceRefs?: string[];
  uncertainty?: string;
  diagnosticTrace?: string;
  answerLeakRisk?: string;
  wrongSolution?: string;
  correctSolution?: string;
  lineIssues?: Array<{ lineNumber?: number; error?: string; suggestion?: string }>;
  firstFailedCase?: {
    testCaseNumber?: number;
    hidden?: boolean;
    input?: string;
    expectedOutput?: string;
    actualOutput?: string;
  } | null;
  reportMarkdown?: string;
  generatedAt?: string;
}

export interface CoachPrompt {
  id: number;
  assignmentId?: number | null;
  studentProfileId?: number | null;
  submissionId: number;
  parentPromptId?: number | null;
  turnIndex?: number | null;
  hintPolicy: HintPolicy | string;
  promptType: string;
  question: string;
  studentAnswer?: string | null;
  coachFeedback?: string | null;
  answeredAt?: string | null;
  rationale?: string | null;
  contextSummary?: string | null;
  evidenceRefs?: string[];
  turns?: CoachPrompt[];
  createdAt?: string;
}

export interface SubmissionResult {
  id: number;
  problemId: number;
  assignmentId?: number | null;
  studentProfileId?: number | null;
  problemTitle?: string;
  languageId: number;
  languageName?: string;
  sourceCode: string;
  verdict: Verdict | string;
  executionTime?: number | null;
  memoryUsed?: number | null;
  output?: string | null;
  compileOutput?: string | null;
  errorMessage?: string | null;
  submittedAt?: string;
  analysisStatus?: string;
  analysis?: SubmissionAnalysis | null;
  testCaseResults?: Array<{
    testCaseNumber: number;
    passed: boolean;
    actualOutput?: string | null;
    expectedOutput?: string | null;
    executionTime?: number | null;
    memoryUsed?: number | null;
    hidden: boolean;
  }>;
}

export interface SubmissionHistorySummary {
  id: number;
  problemId: number;
  problemTitle?: string;
  languageId?: number;
  languageName?: string;
  verdict: Verdict | string;
  executionTime?: number | null;
  memoryUsed?: number | null;
  submittedAt?: string;
  passedTestCases?: number | null;
  totalTestCases?: number | null;
  analysisStatus?: string | null;
  analysisSourceType?: string | null;
  analysisHeadline?: string | null;
  analysisSummary?: string | null;
}

export interface AssignmentOverview {
  assignment: Assignment;
  participantCount: number;
  attemptCount: number;
  passedAttemptCount: number;
  strugglingStudentCount: number;
  topIssues: Array<{
    label: string;
    count: number;
    explanation?: string | null;
    abilityPoint?: string | null;
    recommendedHintPolicy?: string | null;
    interventionSuggestion?: string | null;
  }>;
  classAbilityWeaknesses?: AbilityStat[];
  classReviewSuggestions?: Array<{
    suggestionKey?: string | null;
    title: string;
    targetAbility?: string | null;
    exampleProblemId?: number | null;
    exampleProblemTitle?: string | null;
    evidenceTags?: string[];
    evidenceSubmissionIds?: number[];
    guidingQuestion?: string | null;
    action?: string | null;
    evidenceSummary?: string | null;
    latestFeedback?: {
      actionType?: string | null;
      teacherNote?: string | null;
      createdBy?: string | null;
      createdAt?: string | null;
    } | null;
  }>;
  students: Array<{
    studentProfileId: number;
    displayName: string;
    studentNo?: string | null;
    attemptCount: number;
    passedCount: number;
    latestSubmissionId?: number | null;
    latestVerdict?: string | null;
    latestIssue?: string | null;
    latestIssueTag?: string | null;
    latestFineGrainedIssue?: string | null;
    latestProgressSignal?: string | null;
    latestConfidence?: number | null;
    latestUncertainty?: string | null;
    latestAnswerLeakRisk?: string | null;
    latestCorrection?: TeacherDiagnosisCorrection | null;
    latestCoachInteraction?: CoachInteractionSummary | null;
    latestCoachImpact?: CoachImpact | null;
    primaryAbilityFocus?: string | null;
    crossProblemSummary?: string | null;
    abilitySummary?: AbilityStat[];
    repeatedIssueTag?: string | null;
    repeatedFineGrainedTag?: string | null;
    repeatedIssueCount: number;
    attentionReason?: string | null;
    attentionEvidence?: Array<{
      submissionId: number;
      problemId?: number | null;
      verdict?: string | null;
      submittedAt?: string | null;
      issueTag?: string | null;
      fineGrainedTag?: string | null;
      abilityPoint?: string | null;
      headline?: string | null;
      reason?: string | null;
    }>;
    needsAttention: boolean;
  }>;
}

export interface AiQualityOverview {
  assignmentId: number;
  analyzedSubmissionCount: number;
  correctionCount: number;
  evalCandidateCount: number;
  lowConfidenceCount: number;
  highLeakRiskCount: number;
  correctionRate: number;
  lowConfidenceRate: number;
  highLeakRiskRate: number;
  summary?: string | null;
  correctedTags: Array<{
    originalTag: string;
    originalLabel?: string | null;
    correctedTag: string;
    correctedLabel?: string | null;
    count: number;
  }>;
}

export interface AiQualityTrend {
  assignmentCount: number;
  analyzedSubmissionCount: number;
  correctionCount: number;
  evalCandidateCount: number;
  lowConfidenceCount: number;
  highLeakRiskCount: number;
  correctionRate: number;
  lowConfidenceRate: number;
  highLeakRiskRate: number;
  summary?: string | null;
  assignments: AiQualityTrendPoint[];
  correctedTags: AiQualityTrendTag[];
  evalNeededTags: AiQualityTrendTag[];
  sourceSegments: AiQualitySourceSegment[];
}

export interface AiQualityTrendPoint {
  assignmentId?: number | null;
  assignmentTitle?: string | null;
  analyzedSubmissionCount: number;
  correctionCount: number;
  evalCandidateCount: number;
  lowConfidenceCount: number;
  highLeakRiskCount: number;
  correctionRate: number;
  lowConfidenceRate: number;
  highLeakRiskRate: number;
  summary?: string | null;
}

export interface AiQualityTrendTag {
  tag: string;
  label?: string | null;
  count: number;
  evalCandidateCount: number;
}

export interface AiQualitySourceSegment {
  sourceType: string;
  versionLabel?: string | null;
  provider?: string | null;
  model?: string | null;
  modelVersion?: string | null;
  promptVersion?: string | null;
  agentVersion?: string | null;
  status?: string | null;
  fallbackCount?: number;
  analyzedSubmissionCount: number;
  correctionCount: number;
  lowConfidenceCount: number;
  highLeakRiskCount: number;
  correctionRate: number;
  lowConfidenceRate: number;
  highLeakRiskRate: number;
}

export interface RecommendationEffectiveness {
  recentEventCount: number;
  uniqueRecommendationCount: number;
  exposureCount: number;
  clickCount: number;
  enteredProblemCount: number;
  followupSubmissionCount: number;
  acceptedFollowupCount: number;
  sameFocusIssueCount: number;
  clickedWithoutSubmissionCount: number;
  clickThroughRate: number;
  followupSubmissionRate: number;
  acceptedFollowupRate: number;
  sameFocusIssueRate: number;
  summary?: string | null;
  byType: RecommendationEffectivenessSegment[];
  focusTags: RecommendationEffectivenessSegment[];
}

export interface RecommendationEffectivenessSegment {
  key: string;
  label?: string | null;
  exposureCount: number;
  clickCount: number;
  enteredProblemCount: number;
  followupSubmissionCount: number;
  acceptedFollowupCount: number;
  sameFocusIssueCount: number;
  clickThroughRate: number;
  followupSubmissionRate: number;
  acceptedFollowupRate: number;
  sameFocusIssueRate: number;
}

export interface TeacherDiagnosisCorrection {
  id: number;
  assignmentId: number;
  submissionId: number;
  studentProfileId?: number | null;
  originalIssueTag?: string | null;
  originalFineGrainedTag?: string | null;
  correctedIssueTag: string;
  correctedFineGrainedTag?: string | null;
  teacherNote?: string | null;
  evalCandidate: boolean;
  correctedBy?: string | null;
  correctedAt?: string;
}

export interface DiagnosisTag {
  id: string;
  label: string;
  teacherExplanation?: string | null;
  abilityPoint?: string | null;
  fineGrained: boolean;
  parentTag?: string | null;
}

export interface ClassGroup {
  id: number;
  name: string;
  grade?: string | null;
  teacherName?: string | null;
  createdAt?: string;
}

export interface StudentIdentityAudit {
  classGroupId: number;
  className?: string | null;
  totalProfiles: number;
  stableIdentityCount: number;
  manualIdentityCount?: number;
  legacyIdentityCount: number;
  missingStudentNoCount: number;
  duplicateGroupCount: number;
  duplicateGroups: Array<{
    stableIdentityKey: string;
    reason?: string | null;
    studentProfileIds: number[];
    displayNames: string[];
    studentNos: string[];
    identityKeys: string[];
  }>;
}

export interface ExecutorStatus {
  mode: string;
  executorType: string;
  dockerAvailable: boolean;
  pythonAvailable: boolean;
  cppAvailable: boolean;
  message: string;
}

export interface ImportPreview {
  importType: string;
  totalRows: number;
  validRows: number;
  invalidRows: number;
  duplicateRows: number;
  message?: string;
  issues: Array<{ rowNumber: number; severity: string; message: string }>;
  students?: Array<{
    rowNumber: number;
    className?: string | null;
    classGroupId?: number | null;
    displayName?: string | null;
    studentNo?: string | null;
    note?: string | null;
    valid: boolean;
    duplicate: boolean;
    message?: string | null;
  }>;
  problems?: Array<{
    rowNumber: number;
    title?: string | null;
    description?: string | null;
    difficulty?: string | null;
    timeLimit?: number | null;
    memoryLimit?: number | null;
    aiPromptDirection?: string | null;
    testCaseCount: number;
    visibleTestCaseCount: number;
    valid: boolean;
    duplicate: boolean;
    message?: string | null;
  }>;
}

export interface ImportCommit {
  importType: string;
  createdCount: number;
  updatedCount: number;
  skippedCount: number;
  failedCount: number;
  message?: string;
  createdIds: number[];
  issues: Array<{ rowNumber: number; severity: string; message: string }>;
}

export interface LeaderboardEntry {
  rank: number;
  problemId: number;
  problemTitle: string;
  difficulty: string;
  totalSubmissions: number;
  acceptedSubmissions: number;
  acceptanceRate: number;
  bestAcceptedTime?: number | null;
  lastSubmittedAt?: string | null;
}
