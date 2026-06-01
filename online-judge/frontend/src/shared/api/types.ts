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
  learningTrajectorySignal?: LearningTrajectorySignal | null;
  learningInterventionPlan?: LearningInterventionPlan | null;
  learningInterventionImpact?: LearningInterventionImpact | null;
  learningActionEvidence?: LearningActionEvidence | null;
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
  latestLearningTrajectorySignal?: LearningTrajectorySignal | null;
  latestLearningInterventionPlan?: LearningInterventionPlan | null;
  latestLearningInterventionImpact?: LearningInterventionImpact | null;
  latestLearningActionEvidence?: LearningActionEvidence | null;
  postAcTransferSignal?: PostAcTransferSignal | null;
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
  answerQualitySignal?: CoachAnswerQualitySignal | null;
  coachSafetyRejectionSignal?: CoachSafetyRejectionSignal | null;
}

export interface CoachSafetyRejectionSignal {
  status?: string | null;
  rejectionCount: number;
  latestReason?: string | null;
  latestAnswerLeakRisk?: string | null;
  summary?: string | null;
  recommendedAction?: string | null;
  needsTeacherAttention?: boolean;
  evidenceRefs?: string[];
}

export interface CoachAnswerQualitySignal {
  qualityLevel?: string | null;
  qualityLabel?: string | null;
  understandingLevel?: string | null;
  evidenceCompleteness?: number | null;
  verifiable?: boolean | null;
  actionStatus?: string | null;
  recommendedTeachingAction?: string | null;
  evidenceTypes?: string[];
  missingEvidence?: string[];
  summary?: string | null;
  nextCoachMove?: string | null;
  needsTeacherAttention?: boolean;
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
  latestLearningTrajectorySignal?: LearningTrajectorySignal | null;
  latestLearningInterventionPlan?: LearningInterventionPlan | null;
  latestLearningInterventionImpact?: LearningInterventionImpact | null;
  latestLearningActionEvidence?: LearningActionEvidence | null;
  postAcTransferSignal?: PostAcTransferSignal | null;
  selfExplanationMasterySignal?: SelfExplanationMasterySignal | null;
  aiDependencySignal?: AiDependencySignal | null;
  masteryGrowthSignal?: MasteryGrowthSignal | null;
  teachingActionDecision?: TeachingActionDecision | null;
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
  recurringMisconceptionSignal?: RecurringMisconceptionSignal | null;
  selfExplanationMasterySignal?: SelfExplanationMasterySignal | null;
  aiDependencySignal?: AiDependencySignal | null;
  masteryGrowthSignal?: MasteryGrowthSignal | null;
  teachingActionDecision?: TeachingActionDecision | null;
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
  assignmentId?: number | null;
  problemId?: number | null;
  problemTitle?: string | null;
  focusAbility?: string | null;
  focusTags?: string[];
  evidenceProblemIds?: number[];
  recommendationToken?: string | null;
  learningHypothesis?: string | null;
  expectedCompletionSignal?: string | null;
  strategy?: string | null;
  riskLevel?: string | null;
  fallbackAction?: string | null;
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

export interface StudentHintPlan {
  hintLevel?: string;
  problemType?: string;
  evidenceAnchor?: string;
  nextAction?: string;
  coachQuestion?: string;
  teachingAction?: string;
  evidenceRefs?: string[];
  answerLeakRisk?: string;
}

export interface LearningTrajectorySignal {
  phase?: string;
  label?: string;
  evidenceRef?: string;
  summary?: string;
  nextFocus?: string;
  needsTeacherAttention?: boolean;
}

export interface LearningInterventionPlan {
  interventionType?: string;
  goal?: string;
  studentTask?: string;
  checkQuestion?: string;
  completionSignal?: string;
  evidenceRefs?: string[];
  estimatedMinutes?: number;
  answerLeakRisk?: string;
}

export interface LearningInterventionImpact {
  interventionSubmissionId?: number | null;
  followupSubmissionId?: number | null;
  problemId?: number | null;
  interventionType?: string | null;
  status?: string | null;
  statusLabel?: string | null;
  summary?: string | null;
  previousVerdict?: string | null;
  followupVerdict?: string | null;
  previousIssueTag?: string | null;
  previousFineGrainedTag?: string | null;
  followupIssueTag?: string | null;
  followupFineGrainedTag?: string | null;
  plannedAt?: string | null;
  followupSubmittedAt?: string | null;
}

export interface LearningActionEvidence {
  expectedActionType?: string | null;
  executionStatus?: string | null;
  statusLabel?: string | null;
  observedEvidence?: string | null;
  confidence?: number | null;
  evidenceRefs?: string[];
  nextAdjustment?: string | null;
}

export interface PostAcTransferSignal {
  phase?: string | null;
  label?: string | null;
  summary?: string | null;
  evidenceRefs?: string[];
  recommendedAction?: string | null;
  targetAbility?: string | null;
  targetTags?: string[];
  problemId?: number | null;
  problemTitle?: string | null;
  needsTeacherAttention?: boolean;
}

export interface RecurringMisconceptionSignal {
  status?: string | null;
  label?: string | null;
  summary?: string | null;
  misconceptionTag?: string | null;
  fineGrainedTag?: string | null;
  abilityPoint?: string | null;
  problemCount?: number;
  assignmentCount?: number;
  submissionCount?: number;
  evidenceRefs?: string[];
  evidenceProblemIds?: number[];
  recommendedAction?: string | null;
  needsTeacherAttention?: boolean;
}

export interface SelfExplanationMasterySignal {
  status?: string | null;
  label?: string | null;
  summary?: string | null;
  evidenceCompleteness?: number | null;
  answeredTurnCount?: number;
  verifiableAnswerCount?: number;
  transferReadyCount?: number;
  vagueAnswerCount?: number;
  safetyRiskCount?: number;
  evidenceTypes?: string[];
  evidenceRefs?: string[];
  recommendedAction?: string | null;
  needsTeacherAttention?: boolean;
}

export interface AiDependencySignal {
  status?: string | null;
  label?: string | null;
  summary?: string | null;
  independenceScore?: number | null;
  coachPromptCount?: number;
  answeredCoachCount?: number;
  recommendationClickCount?: number;
  recommendationSubmissionCount?: number;
  independentSubmissionCount?: number;
  independentAcceptedCount?: number;
  scaffoldedAcceptedCount?: number;
  dependencyEvidenceRefs?: string[];
  recommendedAction?: string | null;
  needsTeacherAttention?: boolean;
}

export interface MasteryGrowthSignal {
  status?: string | null;
  label?: string | null;
  summary?: string | null;
  growthScore?: number | null;
  focusAbility?: string | null;
  focusTag?: string | null;
  fineGrainedTag?: string | null;
  recentSubmissionCount?: number;
  recentAcceptedCount?: number;
  recentFailedCount?: number;
  crossProblemEvidenceCount?: number;
  regressionCount?: number;
  plateauCount?: number;
  evidenceRefs?: string[];
  recommendedAction?: string | null;
  needsTeacherAttention?: boolean;
}

export interface TeachingActionDecision {
  actionType?: string | null;
  actor?: string | null;
  priority?: number | null;
  riskLevel?: string | null;
  title?: string | null;
  summary?: string | null;
  primaryReason?: string | null;
  recommendedAction?: string | null;
  fallbackAction?: string | null;
  evidenceRefs?: string[];
  sourceSignals?: string[];
  candidateCount?: number | null;
  needsTeacherAttention?: boolean;
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
  studentHintPlan?: StudentHintPlan | null;
  learningInterventionPlan?: LearningInterventionPlan | null;
  teacherNote?: string;
  progressSignal?: string;
  learningTrajectorySignal?: LearningTrajectorySignal | null;
  learningActionEvidence?: LearningActionEvidence | null;
  confidence?: number;
  fineGrainedTags?: string[];
  evidenceRefs?: string[];
  uncertainty?: string;
  diagnosticTrace?: string;
  aiInvocation?: {
    provider?: string | null;
    model?: string | null;
    modelVersion?: string | null;
    promptVersion?: string | null;
    agentVersion?: string | null;
    analysisSchemaVersion?: string | null;
    evidenceSchemaVersion?: string | null;
    taxonomyVersion?: string | null;
    status?: string | null;
    fallbackUsed?: boolean;
    runtimeMode?: string | null;
    failureStage?: string | null;
    failureReason?: string | null;
    transportMode?: string | null;
    streamChunkCount?: number | null;
    streamContentChunkCount?: number | null;
    streamReasoningChunkCount?: number | null;
    streamInvalidChunkCount?: number | null;
    streamFinishReason?: string | null;
    streamFallbackRetryUsed?: boolean | null;
  } | null;
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
  modelFailureReason?: string | null;
  modelAnswerLeakRisk?: string | null;
  question: string;
  studentAnswer?: string | null;
  coachFeedback?: string | null;
  answeredAt?: string | null;
  rationale?: string | null;
  contextSummary?: string | null;
  evidenceRefs?: string[];
  adaptiveStrategySignal?: {
    strategy?: string | null;
    reason?: string | null;
    recommendedCoachMove?: string | null;
    needsTeacherAttention?: boolean;
    evidenceRefs?: string[];
  } | null;
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
  postAcTransferPendingCount?: number;
  postAcTransferSummary?: string | null;
  recurringMisconceptionStudentCount?: number;
  recurringMisconceptionSummary?: string | null;
  selfExplanationWeakStudentCount?: number;
  selfExplanationSummary?: string | null;
  coachAnswerQualitySummary?: {
    promptedCount?: number;
    answeredCount?: number;
    verifiableCount?: number;
    transferReadyCount?: number;
    evidenceInsufficientCount?: number;
    safetyRiskCount?: number;
    coachSafetyRejectionCount?: number;
    teacherAttentionCount?: number;
    dominantGap?: string | null;
    summary?: string | null;
    recommendedAction?: string | null;
    evidenceRefs?: string[];
  } | null;
  coachFollowupImpactSummary?: {
    impactedCount?: number;
    acceptedCount?: number;
    shiftedCount?: number;
    sameIssueCount?: number;
    verdictChangedCount?: number;
    noClearChangeCount?: number;
    awaitingFollowupCount?: number;
    dominantOutcome?: string | null;
    summary?: string | null;
    recommendedAction?: string | null;
    evidenceRefs?: string[];
  } | null;
  aiDependencyRiskStudentCount?: number;
  aiDependencySummary?: string | null;
  masteryGrowthRiskStudentCount?: number;
  masteryGrowthSummary?: string | null;
  teachingActionRiskStudentCount?: number;
  teachingActionSummary?: string | null;
  classTeachingStrategySignal?: {
    strategyKey?: string | null;
    status?: string | null;
    statusLabel?: string | null;
    strategyType?: string | null;
    title?: string | null;
    summary?: string | null;
    focusAbility?: string | null;
    focusTag?: string | null;
    focusLabel?: string | null;
    affectedStudentCount?: number;
    affectedStudentRatio?: number | null;
    priority?: number | null;
    riskLevel?: string | null;
    teacherAction?: string | null;
    exitTicket?: string | null;
    groups?: Array<{
      groupType?: string | null;
      title?: string | null;
      studentProfileIds?: number[];
      studentNames?: string[];
      focus?: string | null;
      action?: string | null;
      evidenceRefs?: string[];
    }>;
    evidenceRefs?: string[];
    sourceSignals?: string[];
    impact?: {
      status?: string | null;
      statusLabel?: string | null;
      summary?: string | null;
      recommendedAction?: string | null;
      needsEscalation?: boolean;
      feedbackActionType?: string | null;
      feedbackAt?: string | null;
      followupSubmissionId?: number | null;
      followupVerdict?: string | null;
      evidenceRefs?: string[];
      matchedTags?: string[];
    } | null;
  } | null;
  topIssues: Array<{
    label: string;
    count: number;
    explanation?: string | null;
    abilityPoint?: string | null;
    recommendedHintPolicy?: string | null;
    interventionSuggestion?: string | null;
    actionPriorityScore?: number | null;
    actionPriorityLabel?: string | null;
    actionPriorityReason?: string | null;
    affectedStudentCount?: number;
    repeatedStudentCount?: number;
    unexecutedActionCount?: number;
    unresolvedAfterInterventionCount?: number;
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
    interventionImpact?: {
      status?: string | null;
      statusLabel?: string | null;
      summary?: string | null;
      recommendedAction?: string | null;
      needsEscalation?: boolean;
      feedbackActionType?: string | null;
      feedbackAt?: string | null;
      followupSubmissionId?: number | null;
      followupVerdict?: string | null;
      evidenceSubmissionIds?: number[];
      matchedTags?: string[];
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
    latestLearningActionEvidence?: LearningActionEvidence | null;
    postAcTransferSignal?: PostAcTransferSignal | null;
    recurringMisconceptionSignal?: RecurringMisconceptionSignal | null;
    selfExplanationMasterySignal?: SelfExplanationMasterySignal | null;
    aiDependencySignal?: AiDependencySignal | null;
    masteryGrowthSignal?: MasteryGrowthSignal | null;
    teachingActionDecision?: TeachingActionDecision | null;
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
  interventionEvalCandidateCount?: number;
  interventionWaitingFollowupCount?: number;
  interventionImprovedCount?: number;
  interventionShiftedCount?: number;
  interventionStillStuckCount?: number;
  lowConfidenceCount: number;
  highLeakRiskCount: number;
  modelFallbackCount: number;
  modelPartialCount: number;
  modelRuntimeFailureCount: number;
  modelCompletedCount: number;
  correctionRate: number;
  lowConfidenceRate: number;
  highLeakRiskRate: number;
  modelFallbackRate: number;
  modelRuntimeFailureRate: number;
  summary?: string | null;
  qualityRiskSummary?: string | null;
  promptSafetyIncidentSignal?: PromptSafetyIncidentSignal | null;
  runtimeAttributionSignal?: RuntimeAttributionSignal | null;
  qualityDimensions: AiQualityDimension[];
  improvementPriorities: AiQualityImprovementPriority[];
  evalReadiness?: AiQualityEvalReadiness | null;
  correctedTags: Array<{
    originalTag: string;
    originalLabel?: string | null;
    correctedTag: string;
    correctedLabel?: string | null;
    count: number;
  }>;
}

export interface AiQualityDimension {
  dimension: string;
  label?: string | null;
  status: "HEALTHY" | "WATCH" | "ACTION_NEEDED" | string;
  score: number;
  summary?: string | null;
  evidenceRefs: string[];
  recommendedAction?: string | null;
}

export interface AiQualityImprovementPriority {
  priority: string;
  dimension: string;
  severity: "HEALTHY" | "WATCH" | "ACTION_NEEDED" | string;
  reason?: string | null;
  recommendedAction?: string | null;
  evidenceRefs: string[];
}

export interface PromptSafetyIncidentSignal {
  status: "HEALTHY" | "WATCH" | "ACTION_NEEDED" | string;
  primaryRiskSource?: string | null;
  totalIncidentCount: number;
  highLeakRiskCount: number;
  safetyDowngradeCount: number;
  highRiskSafetyDowngradeCount: number;
  coachSafetyRiskCount: number;
  summary?: string | null;
  recommendedAction?: string | null;
  evidenceRefs: string[];
}

export interface RuntimeAttributionSignal {
  status: "HEALTHY" | "WATCH" | "ACTION_NEEDED" | string;
  primaryFailureType?: string | null;
  primaryFailureReason?: string | null;
  primaryFailureStage?: string | null;
  primaryTransportMode?: string | null;
  modelCompletedCount: number;
  modelPartialCount: number;
  modelRuntimeFailureCount: number;
  modelFallbackCount: number;
  primaryFailureCount: number;
  runtimeFailureRate: number;
  streamNoContentCount?: number;
  streamInvalidChunkCount?: number;
  streamFallbackRetryCount?: number;
  summary?: string | null;
  recommendedAction?: string | null;
  evidenceRefs: string[];
}

export interface AiQualityEvalReadiness {
  status: "READY" | "PARTIAL" | "NO_SAMPLE" | "INSUFFICIENT_SIGNAL" | string;
  summary?: string | null;
  candidateCount: number;
  correctionCount: number;
  interventionCandidateCount?: number;
  interventionWaitingFollowupCount?: number;
  interventionImprovedCount?: number;
  interventionShiftedCount?: number;
  interventionStillStuckCount?: number;
  recommendedAction?: string | null;
  priorityTags: Array<{
    originalTag: string;
    originalLabel?: string | null;
    correctedTag: string;
    correctedLabel?: string | null;
    count: number;
  }>;
  evidenceRefs: string[];
}

export interface DiagnosisEvalCandidate {
  correctionId: number;
  submissionId: number;
  studentProfileId?: number | null;
  problemId?: number | null;
  problemTitle?: string | null;
  problemDescription?: string | null;
  problemDifficulty?: string | null;
  problemTimeLimit?: number | null;
  problemMemoryLimit?: number | null;
  verdict?: string | null;
  languageName?: string | null;
  sourceCode?: string | null;
  scenario?: string | null;
  originalIssueTag?: string | null;
  originalFineGrainedTag?: string | null;
  correctedIssueTag?: string | null;
  correctedFineGrainedTag?: string | null;
  teacherNote?: string | null;
  analysisHeadline?: string | null;
  analysisSource?: string | null;
  sourceCodePreview?: string | null;
  correctedAt?: string;
}

export interface DiagnosisEvalCandidates {
  assignmentId: number;
  candidateCount: number;
  candidates: DiagnosisEvalCandidate[];
}

export interface DiagnosisEvalFixtureDraft {
  assignmentId: number;
  candidateCount: number;
  fixtureCount: number;
  interventionFixtureCount?: number;
  safetyFixtureCount?: number;
  runtimeFixtureCount?: number;
  summary?: string | null;
  fixtures: DiagnosisEvalFixtureDraftItem[];
  interventionFixtures?: DiagnosisEvalInterventionFixtureDraftItem[];
  safetyFixtures?: DiagnosisEvalSafetyFixtureDraftItem[];
  runtimeFixtures?: DiagnosisEvalRuntimeFixtureDraftItem[];
}

export interface DiagnosisEvalFixtureDraftItem {
  name: string;
  source: string;
  correctionId: number;
  submissionId: number;
  problem: {
    id?: number | null;
    title?: string | null;
    description?: string | null;
    difficulty?: string | null;
    timeLimit?: number | null;
    memoryLimit?: number | null;
  };
  submission: {
    languageName?: string | null;
    verdict?: string | null;
    sourceCode?: string | null;
  };
  caseResults: Array<{
    testCaseNumber?: number | null;
    passed?: boolean | null;
    hidden?: boolean | null;
    inputSnapshot?: string | null;
    actualOutput?: string | null;
    expectedOutput?: string | null;
    executionTime?: number | null;
    memoryUsed?: number | null;
  }>;
  analysis: {
    scenario?: string | null;
    originalIssueTags: string[];
    originalFineGrainedTags: string[];
    analysisHeadline?: string | null;
  };
  teacherCorrection: {
    correctedIssueTag?: string | null;
    correctedFineGrainedTag?: string | null;
    teacherNote?: string | null;
  };
  expectedIssueTags: string[];
  expectedFineTags: string[];
  mustMention: string[];
  mustNotMention: string[];
  sourceMaterial: {
    localFolder?: string | null;
    artifacts: string[];
    anonymizationNote?: string | null;
  };
  quality: {
    bugPattern?: string | null;
    misconception?: string | null;
    expectedStudentMove?: string | null;
    evalPurpose?: string | null;
  };
}

export interface DiagnosisEvalInterventionFixtureDraftItem {
  name: string;
  source: string;
  suggestionKey: string;
  title?: string | null;
  targetAbility?: string | null;
  feedbackActionType?: string | null;
  feedbackNote?: string | null;
  impactStatus?: string | null;
  impactSummary?: string | null;
  followupSubmissionId?: number | null;
  followupVerdict?: string | null;
  evidenceTags: string[];
  evidenceRefs: string[];
  mustMention: string[];
  mustNotMention: string[];
  expectedTeachingActions: string[];
  sourceMaterial: {
    localFolder?: string | null;
    artifacts: string[];
    anonymizationNote?: string | null;
  };
  quality: {
    bugPattern?: string | null;
    misconception?: string | null;
    expectedStudentMove?: string | null;
    evalPurpose?: string | null;
  };
}

export interface DiagnosisEvalSafetyFixtureDraftItem {
  name: string;
  source: string;
  submissionId: number;
  problem: {
    id?: number | null;
    title?: string | null;
    description?: string | null;
    difficulty?: string | null;
    timeLimit?: number | null;
    memoryLimit?: number | null;
  };
  submission: {
    languageName?: string | null;
    verdict?: string | null;
    sourceCode?: string | null;
  };
  analysis: {
    scenario?: string | null;
    originalIssueTags: string[];
    originalFineGrainedTags: string[];
    analysisHeadline?: string | null;
  };
  riskLevel?: string | null;
  riskSources: string[];
  blockedReasons: string[];
  originalHintPreview?: string | null;
  safeHintPreview?: string | null;
  evidenceRefs: string[];
  mustMention: string[];
  mustNotMention: string[];
  expectedSafetyAction?: string | null;
  sourceMaterial: {
    localFolder?: string | null;
    artifacts: string[];
    anonymizationNote?: string | null;
  };
  quality: {
    bugPattern?: string | null;
    misconception?: string | null;
    expectedStudentMove?: string | null;
    evalPurpose?: string | null;
  };
}

export interface DiagnosisEvalRuntimeFixtureDraftItem {
  name: string;
  source: string;
  submissionId: number;
  problem: {
    id?: number | null;
    title?: string | null;
    description?: string | null;
    difficulty?: string | null;
    timeLimit?: number | null;
    memoryLimit?: number | null;
  };
  submission: {
    languageName?: string | null;
    verdict?: string | null;
    sourceCode?: string | null;
  };
  analysis: {
    scenario?: string | null;
    originalIssueTags: string[];
    originalFineGrainedTags: string[];
    analysisHeadline?: string | null;
  };
  runtimeMode?: string | null;
  status?: string | null;
  fallbackUsed?: boolean | null;
  transportMode?: string | null;
  streamChunkCount?: number | null;
  streamContentChunkCount?: number | null;
  streamReasoningChunkCount?: number | null;
  streamInvalidChunkCount?: number | null;
  streamFinishReason?: string | null;
  streamFallbackRetryUsed?: boolean | null;
  failureType?: string | null;
  failureStage?: string | null;
  failureReason?: string | null;
  expectedRuntimeAction?: string | null;
  evidenceRefs: string[];
  mustMention: string[];
  mustNotMention: string[];
  sourceMaterial: {
    localFolder?: string | null;
    artifacts: string[];
    anonymizationNote?: string | null;
  };
  quality: {
    bugPattern?: string | null;
    misconception?: string | null;
    expectedStudentMove?: string | null;
    evalPurpose?: string | null;
  };
}

export interface AiQualityTrend {
  assignmentCount: number;
  analyzedSubmissionCount: number;
  correctionCount: number;
  evalCandidateCount: number;
  interventionEvalCandidateCount?: number;
  interventionWaitingFollowupCount?: number;
  interventionImprovedCount?: number;
  interventionShiftedCount?: number;
  interventionStillStuckCount?: number;
  lowConfidenceCount: number;
  highLeakRiskCount: number;
  promptSafetyIncidentCount?: number;
  promptSafetyDowngradeCount?: number;
  promptSafetyHighRiskDowngradeCount?: number;
  coachSafetyRejectionCount?: number;
  modelCompletedCount?: number;
  modelPartialCount?: number;
  modelRuntimeFailureCount?: number;
  correctionRate: number;
  lowConfidenceRate: number;
  highLeakRiskRate: number;
  promptSafetyIncidentRate?: number;
  modelRuntimeFailureRate?: number;
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
  interventionEvalCandidateCount?: number;
  interventionWaitingFollowupCount?: number;
  interventionImprovedCount?: number;
  interventionShiftedCount?: number;
  interventionStillStuckCount?: number;
  lowConfidenceCount: number;
  highLeakRiskCount: number;
  promptSafetyIncidentCount?: number;
  promptSafetyDowngradeCount?: number;
  promptSafetyHighRiskDowngradeCount?: number;
  coachSafetyRejectionCount?: number;
  modelCompletedCount?: number;
  modelPartialCount?: number;
  modelRuntimeFailureCount?: number;
  correctionRate: number;
  lowConfidenceRate: number;
  highLeakRiskRate: number;
  promptSafetyIncidentRate?: number;
  modelRuntimeFailureRate?: number;
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
  runtimeMode?: string | null;
  failureStage?: string | null;
  failureReason?: string | null;
  transportMode?: string | null;
  fallbackCount?: number;
  modelCompletedCount?: number;
  modelPartialCount?: number;
  modelRuntimeFailureCount?: number;
  streamNoContentCount?: number;
  streamInvalidChunkCount?: number;
  streamFallbackRetryCount?: number;
  analyzedSubmissionCount: number;
  correctionCount: number;
  lowConfidenceCount: number;
  highLeakRiskCount: number;
  promptSafetyIncidentCount?: number;
  promptSafetyDowngradeCount?: number;
  promptSafetyHighRiskDowngradeCount?: number;
  coachSafetyRejectionCount?: number;
  correctionRate: number;
  lowConfidenceRate: number;
  highLeakRiskRate: number;
  promptSafetyIncidentRate?: number;
  modelRuntimeFailureRate?: number;
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
  unresolvedLearningSignalCount: number;
  teacherInterventionRecommendedCount: number;
  clickThroughRate: number;
  followupSubmissionRate: number;
  acceptedFollowupRate: number;
  sameFocusIssueRate: number;
  summary?: string | null;
  byType: RecommendationEffectivenessSegment[];
  byStrategy: RecommendationEffectivenessSegment[];
  focusTags: RecommendationEffectivenessSegment[];
  feedbackSignals: RecommendationFeedbackSignal[];
  actionEvidenceSignals?: RecommendationActionEvidenceSignal[];
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
  unresolvedLearningSignalCount: number;
  teacherInterventionRecommendedCount: number;
  clickThroughRate: number;
  followupSubmissionRate: number;
  acceptedFollowupRate: number;
  sameFocusIssueRate: number;
}

export interface RecommendationFeedbackSignal {
  signal: string;
  strategy?: string | null;
  severity?: string | null;
  evidenceCount: number;
  summary?: string | null;
  recommendedAction?: string | null;
  evidenceTokens?: string[];
}

export interface RecommendationActionEvidenceSignal {
  recommendationToken?: string | null;
  type?: string | null;
  strategy?: string | null;
  riskLevel?: string | null;
  learningHypothesis?: string | null;
  expectedCompletionSignal?: string | null;
  outcome?: string | null;
  summary?: string | null;
  recommendedAdjustment?: string | null;
  needsTeacherAttention?: boolean;
  followupSubmissionId?: number | null;
  followupVerdict?: string | null;
  followupIssueTag?: string | null;
  followupFineGrainedTag?: string | null;
  evidenceRefs?: string[];
  lastEventAt?: string | null;
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
  projectOwner?: string;
  ownershipSignature?: string;
  ownershipClaim?: string;
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
