export function difficultyLabel(value?: string | null): string {
  switch ((value || "").toUpperCase()) {
    case "EASY":
      return "基础";
    case "MEDIUM":
      return "提高";
    case "HARD":
      return "挑战";
    default:
      return value || "-";
  }
}

export function verdictLabel(value?: string | null): string {
  switch ((value || "").toUpperCase()) {
    case "ACCEPTED":
      return "已通过";
    case "WRONG_ANSWER":
      return "答案需修正";
    case "TIME_LIMIT_EXCEEDED":
      return "时间超限";
    case "MEMORY_LIMIT_EXCEEDED":
      return "内存超限";
    case "RUNTIME_ERROR":
      return "运行错误";
    case "COMPILATION_ERROR":
      return "编译错误";
    case "INTERNAL_ERROR":
      return "系统环境提示";
    case "PENDING":
      return "等待评测";
    default:
      return value || "未提交";
  }
}

export function issueLabel(value?: string | null): string {
  const map: Record<string, string> = {
    SYNTAX_ERROR: "语法错误",
    IO_FORMAT: "输入输出格式",
    IO_FORMAT_ERROR: "输入输出格式",
    BOUNDARY_CONDITION: "边界条件",
    BOUNDARY_CONDITION_MISSING: "边界条件",
    CONDITION_BRANCH: "条件分支",
    CONDITIONAL_BRANCH_ERROR: "条件分支",
    LOOP_BOUNDARY: "循环边界",
    LOOP_BOUNDARY_ERROR: "循环边界",
    DATA_STRUCTURE_CHOICE: "数据结构选择",
    DATA_STRUCTURE_MISMATCH: "数据结构选择",
    TIME_COMPLEXITY: "时间复杂度",
    TIME_COMPLEXITY_HIGH: "时间复杂度",
    SPACE_COMPLEXITY: "空间复杂度",
    SPACE_COMPLEXITY_HIGH: "空间复杂度",
    VARIABLE_INITIALIZATION: "变量初始化",
    STATE_TRANSITION: "状态转移",
    STATE_TRANSITION_ERROR: "状态转移",
    RECURSION_EXIT: "递归出口",
    RECURSION_BASE_CASE: "递归出口",
    CODE_READABILITY: "可读性",
    READABILITY_LOW: "可读性",
    SAMPLE_ONLY: "只通过样例",
    SAMPLE_OVERFITTING: "只通过样例",
    NEEDS_MORE_EVIDENCE: "证据不足",
    CODE_QUALITY: "代码质量",
    GENERALIZATION_CHECK: "泛化能力",
    ALGORITHM_STRATEGY: "算法策略",
    PASSED_WITH_REVIEW: "通过后复盘",
    ALGORITHM_DESIGN: "算法设计",
    DEBUGGING_PROCESS: "调试过程",
    RUNTIME_STABILITY: "运行稳定性",
    OFF_BY_ONE: "差一位错误",
    EMPTY_INPUT: "极小输入",
    MAX_BOUNDARY: "最大规模边界",
    DUPLICATE_CASE: "重复元素场景",
    OUTPUT_FORMAT_DETAIL: "输出格式细节",
    INPUT_PARSING: "输入读取理解",
    INITIAL_STATE: "初始状态",
    STATE_RESET: "状态重置",
    OVER_SIMULATION: "过度模拟",
    BRUTE_FORCE_LIMIT: "暴力规模瓶颈",
    GREEDY_ASSUMPTION: "贪心依据不足",
    DP_STATE_DESIGN: "状态定义不清",
    SAMPLE_OVERFIT: "样例过拟合",
    PARTIAL_FIX_REGRESSION: "局部修复回退"
  };
  const key = (value || "").toUpperCase();
  return map[key] || value || "待观察";
}

export function hintPolicyLabel(value?: string | null): string {
  switch ((value || "").toUpperCase()) {
    case "L1":
      return "L1 问题类型";
    case "L2":
      return "L2 定位方向";
    case "L3":
      return "L3 局部解释";
    case "L4":
      return "L4 参考改法";
    default:
      return value || "L2 定位方向";
  }
}

export function assignmentStatusLabel(value?: string | null): string {
  switch ((value || "").toUpperCase()) {
    case "ACTIVE":
      return "进行中";
    case "DRAFT":
      return "草稿";
    case "CLOSED":
      return "已结束";
    default:
      return value || "未设置";
  }
}

export function formatDateTime(value?: string | null): string {
  if (!value) {
    return "-";
  }
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }
  return new Intl.DateTimeFormat("zh-CN", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit"
  }).format(date);
}

export function percent(value: number): string {
  if (!Number.isFinite(value)) {
    return "0%";
  }
  return `${Math.round(value)}%`;
}

export function confidenceLabel(value?: number | null): string {
  if (typeof value !== "number" || !Number.isFinite(value)) {
    return "置信度待定";
  }
  const normalized = value <= 1 ? value * 100 : value;
  return `置信度 ${Math.round(normalized)}%`;
}

export function answerLeakRiskLabel(value?: string | null): string {
  switch ((value || "").toUpperCase()) {
    case "LOW":
      return "泄题风险低";
    case "MEDIUM":
      return "泄题风险中";
    case "HIGH":
      return "泄题风险高";
    default:
      return "泄题风险待查";
  }
}

export function looksCorruptText(value?: string | null): boolean {
  const text = (value || "").trim();
  if (!text) {
    return false;
  }
  if (text.includes("\uFFFD")) {
    return true;
  }
  const compact = text.replace(/\s/g, "");
  const questionMarks = compact.replace(/[^?？]/g, "").length;
  return compact.length >= 2 && questionMarks / compact.length > 0.55;
}

export function displayText(value?: string | null, fallback = "-"): string {
  const text = (value || "").trim();
  if (!text || looksCorruptText(text)) {
    return fallback;
  }
  return text;
}
