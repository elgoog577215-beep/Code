import { createContext, ReactNode, useContext, useEffect, useMemo, useState } from "react";

type Locale = "zh" | "en";
type TranslationValue = string | { [key: string]: TranslationValue };
type TranslationDictionary = Record<string, TranslationValue>;

const STORAGE_KEY = "wzai:locale";

const dictionaries: Record<Locale, TranslationDictionary> = {
  zh: {
    common: {
      language: "语言",
      chinese: "中文",
      english: "English",
      appName: "温中编程学习平台",
      mainNavigation: "主导航",
      openNavigation: "展开导航",
      closeNavigation: "收起导航",
      skipToMain: "跳到主要内容",
      login: "登录",
      logout: "退出",
      studentSide: "学生端",
      teacherSide: "教师端",
      teacherWorkbench: "教师工作台",
      assignmentCenter: "作业中心",
      resourceManagement: "资源管理",
      loadingPage: "正在加载页面",
      theme: {
        toggle: "切换主题",
        toLight: "切换到白天模式",
        toDark: "切换到夜间模式",
        light: "白天",
        dark: "夜间"
      }
    },
    routeHub: {
      eyebrow: "入口",
      title: "学习平台",
      description: "学生学习和教师工作台各走自己的路径，入口清楚，任务直接。",
      studentTitle: "学生学习",
      studentDescription: "进入课堂作业、公共题库和个人提交记录。",
      teacherTitle: "教师工作台",
      teacherDescription: "查看班级、作业、题目和学生，也管理名单、题库与 AI 标准库。"
    },
    assignmentDetail: {
      fallbackTitle: "课堂作业",
      pilotTitle: "课堂编程作业",
      defaultClass: "默认班级",
      unknownStudent: "学生 #{{id}}",
      loading: "正在加载作业详情",
      notFound: "作业未找到",
      notFoundDescription: "返回作业中心重新选择作业。",
      backToAssignmentCenter: "返回作业中心",
      header: {
        problemCount: "{{count}} 题",
        submitted: "{{count}} 人已提交",
        passRate: "{{rate}}% 通过",
        startReview: "开始批阅",
        viewProblem: "看题目"
      },
      status: {
        active: "进行中",
        draft: "草稿",
        closed: "已结束",
        unset: "未设置",
        pending: "待提交",
        mastered: "已掌握",
        needsLecture: "需讲评",
        progressing: "推进中"
      },
      difficulty: {
        easy: "基础",
        medium: "提高",
        hard: "挑战",
        unknown: "-"
      },
      focus: {
        aria: "作业待处理",
        eyebrow: "待处理",
        title: "下一步",
        noStudentTitle: "暂无需关注学生",
        noStudentDescription: "目前没有学生被标记为需关注，先看题目推进即可。",
        studentLabel: "先看学生",
        studentAction: "查看学生",
        defaultStudentReason: "有新的卡点需要确认。",
        repeatedIssue: "{{issue}}反复出现",
        problemLabel: "先讲题目",
        noProblemTitle: "等待提交",
        noProblemDescription: "学生提交后会自动推荐最需要讲评的题目。",
        problemWithIssue: "{{count}} 名需关注，主要错因是 {{issue}}。",
        problemWithoutIssue: "{{count}} 名需关注，先看学生明细再决定讲评。",
        stable: "稳定"
      },
      metrics: {
        aria: "作业关键指标",
        submittedStudents: "提交人数",
        passedStudents: "通过人数",
        totalSubmissions: "总提交",
        needsAttention: "需关注",
        recentSubmissions: "最近提交",
        submittedPeople: "{{count}} 人"
      },
      trend: {
        eyebrow: "提交推进",
        title: "作业增长情况",
        chartAria: "提交推进曲线",
        attempts: "提交次数",
        submitted: "提交人数",
        passed: "通过人数",
        onePoint: "已有 {{attempts}} 次提交，等待形成趋势。{{submitted}} 人提交，{{passed}} 人通过。",
        empty: "等待第一次提交。"
      },
      problems: {
        aria: "题目推进列表",
        eyebrow: "题目",
        title: "题目推进",
        problem: "题目",
        difficulty: "难度",
        submitted: "提交",
        passed: "通过",
        attention: "需看",
        issue: "集中问题",
        action: "操作",
        view: "看题目",
        noIssue: "暂无集中错因"
      }
    }
  },
  en: {
    common: {
      language: "Language",
      chinese: "中文",
      english: "English",
      appName: "Wenzhong Coding Learning Platform",
      mainNavigation: "Main navigation",
      openNavigation: "Open navigation",
      closeNavigation: "Close navigation",
      skipToMain: "Skip to main content",
      login: "Log in",
      logout: "Log out",
      studentSide: "Student",
      teacherSide: "Teacher",
      teacherWorkbench: "Teacher Workbench",
      assignmentCenter: "Assignments",
      resourceManagement: "Resources",
      loadingPage: "Loading page",
      theme: {
        toggle: "Toggle theme",
        toLight: "Switch to light mode",
        toDark: "Switch to dark mode",
        light: "Light",
        dark: "Dark"
      }
    },
    routeHub: {
      eyebrow: "Entry",
      title: "Learning Platform",
      description: "Students and teachers each get a clear path, with direct access to the work that matters.",
      studentTitle: "Student Learning",
      studentDescription: "Open class assignments, public practice, and personal submission records.",
      teacherTitle: "Teacher Workbench",
      teacherDescription: "Review classes, assignments, problems, and students, plus manage rosters, problems, and the AI library."
    },
    assignmentDetail: {
      fallbackTitle: "Class Assignment",
      pilotTitle: "Class Coding Assignment",
      defaultClass: "Default class",
      unknownStudent: "Student #{{id}}",
      loading: "Loading assignment details",
      notFound: "Assignment not found",
      notFoundDescription: "Go back to assignments and choose another item.",
      backToAssignmentCenter: "Back to assignments",
      header: {
        problemCount: "{{count}} problems",
        submitted: "{{count}} submitted",
        passRate: "{{rate}}% pass",
        startReview: "Start review",
        viewProblem: "View problem"
      },
      status: {
        active: "Active",
        draft: "Draft",
        closed: "Closed",
        unset: "Unset",
        pending: "Waiting",
        mastered: "Mastered",
        needsLecture: "Needs review",
        progressing: "In progress"
      },
      difficulty: {
        easy: "Basic",
        medium: "Intermediate",
        hard: "Challenge",
        unknown: "-"
      },
      focus: {
        aria: "Assignment triage",
        eyebrow: "Triage",
        title: "Next step",
        noStudentTitle: "No priority student",
        noStudentDescription: "No student is flagged right now. Start from problem progress.",
        studentLabel: "Student first",
        studentAction: "View student",
        defaultStudentReason: "A new blocker needs a quick check.",
        repeatedIssue: "{{issue}} appears repeatedly",
        problemLabel: "Problem first",
        noProblemTitle: "Waiting for submissions",
        noProblemDescription: "A review recommendation will appear after students submit.",
        problemWithIssue: "{{count}} need attention; main issue: {{issue}}.",
        problemWithoutIssue: "{{count}} need attention; inspect student details before deciding the review.",
        stable: "Stable"
      },
      metrics: {
        aria: "Assignment key metrics",
        submittedStudents: "Submitted",
        passedStudents: "Passed",
        totalSubmissions: "Attempts",
        needsAttention: "Attention",
        recentSubmissions: "Recent",
        submittedPeople: "{{count}} students"
      },
      trend: {
        eyebrow: "Submission pace",
        title: "Submission trend",
        chartAria: "Submission progress chart",
        attempts: "Attempts",
        submitted: "Students submitted",
        passed: "Students passed",
        onePoint: "{{attempts}} attempts so far. Waiting for one more data point. {{submitted}} submitted, {{passed}} passed.",
        empty: "Waiting for the first submission."
      },
      problems: {
        aria: "Problem progress list",
        eyebrow: "Problems",
        title: "Problem Progress",
        problem: "Problem",
        difficulty: "Level",
        submitted: "Submitted",
        passed: "Passed",
        attention: "Review",
        issue: "Main issue",
        action: "Action",
        view: "Open",
        noIssue: "No shared issue yet"
      }
    }
  }
};

type I18nContextValue = {
  locale: Locale;
  setLocale: (locale: Locale) => void;
  t: (key: string, params?: Record<string, string | number>) => string;
};

const I18nContext = createContext<I18nContextValue | null>(null);

function readLocale(): Locale {
  try {
    return localStorage.getItem(STORAGE_KEY) === "en" ? "en" : "zh";
  } catch {
    return "zh";
  }
}

function saveLocale(locale: Locale): void {
  try {
    localStorage.setItem(STORAGE_KEY, locale);
  } catch {
    // The chosen language still applies in memory when storage is blocked.
  }
}

function lookup(dictionary: TranslationDictionary, key: string): string | undefined {
  const value = key.split(".").reduce<TranslationValue | undefined>((current, part) => {
    if (!current || typeof current === "string") {
      return undefined;
    }
    return current[part];
  }, dictionary);
  return typeof value === "string" ? value : undefined;
}

function interpolate(template: string, params?: Record<string, string | number>): string {
  if (!params) {
    return template;
  }
  return template.replace(/\{\{(\w+)}}/g, (_, key: string) => String(params[key] ?? ""));
}

export function I18nProvider({ children }: { children: ReactNode }) {
  const [locale, setLocaleState] = useState<Locale>(readLocale);

  useEffect(() => {
    saveLocale(locale);
    document.documentElement.lang = locale === "en" ? "en" : "zh-CN";
    document.documentElement.dataset.locale = locale;
  }, [locale]);

  const value = useMemo<I18nContextValue>(() => {
    function t(key: string, params?: Record<string, string | number>) {
      const template = lookup(dictionaries[locale], key) ?? lookup(dictionaries.zh, key) ?? key;
      return interpolate(template, params);
    }

    return { locale, setLocale: setLocaleState, t };
  }, [locale]);

  return <I18nContext.Provider value={value}>{children}</I18nContext.Provider>;
}

export function useTranslation() {
  const context = useContext(I18nContext);
  if (!context) {
    throw new Error("useTranslation must be used inside I18nProvider");
  }
  return context;
}
