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
    teacherShell: {
      aria: "教师工作台导航",
      kicker: "教师端",
      title: "教学分析",
      footnote: "按班级、作业、题目递进查看客观结果。",
      nav: {
        analytics: "教学分析",
        analyticsDescription: "班级、作业、题目结果",
        roster: "班级名单",
        rosterDescription: "学生导入与班级维护",
        problemBank: "题库管理",
        problemBankDescription: "题目列表与编辑工作台",
        aiLibrary: "AI 标准库",
        aiLibraryDescription: "正式库与候选治理"
      }
    },
    classOverview: {
      title: "班级学情",
      breadcrumb: "作业",
      backAssignments: "返回作业",
      newAssignment: "新建作业"
    },
    teacherAnalytics: {
      kicker: "教学结果分析",
      landing: {
        title: "选择班级",
        description: "先进入班级，再查看这个班级下的作业和题目结果。",
        classList: "班级列表",
        classFallback: "班级",
        noTeacherName: "未设置教师"
      },
      breadcrumb: {
        classes: "班级"
      },
      scope: {
        class: "班级",
        assignment: "作业",
        problem: "题目"
      },
      class: {
        description: "查看这个班级跨作业的提交、正确率、错误分布和知识路径。"
      },
      assignment: {
        description: "查看这次作业的题目结果、错误分布和 AI 知识归因。"
      },
      problem: {
        description: "查看这道题的提交情况、错因分布、知识路径和证据样本。"
      },
      actions: {
        newAssignment: "新建作业",
        backToAnalytics: "返回教学分析"
      },
      loading: {
        classes: "正在读取班级",
        class: "正在读取班级分析",
        assignment: "正在读取作业分析",
        problem: "正在读取题目分析"
      },
      errors: {
        load: "教学分析数据读取失败。"
      },
      granularity: {
        chapter: "按章节",
        knowledgePoint: "按知识点",
        skillUnit: "按能力点",
        mistakePoint: "按易错点"
      },
      metrics: {
        assignments: "作业数",
        students: "学生数",
        submissions: "提交数",
        accuracy: "正确率",
        errorCount: "错误次数",
        affectedStudents: "涉及学生",
        submittedStudents: "提交人数",
        unsubmittedStudents: "未提交人数",
        averageAttempts: "平均提交",
        lowPassProblems: "低通过题",
        passedStudents: "通过人数",
        failedStudents: "未通过人数"
      },
      sections: {
        metrics: "基础指标",
        visualization: "数据可视化",
        ranking: "错误分布",
        share: "占比",
        currentPath: "当前知识路径",
        assignments: "作业",
        problems: "题目"
      },
      scopeTitle: "{{scope}}结果分布",
      pathMeta: "{{count}} 次 · {{students}} 名学生 · {{problems}} 道题",
      pathLabel: "路径",
      status: {
        active: "进行中",
        draft: "草稿",
        closed: "已结束"
      },
      defaultLabels: {
        defaultClass: "默认班级",
        assignmentFallbackWithId: "课堂作业 #{{id}}",
        pilotAssignment: "课堂编程作业",
        arraySequence: "数组与序列",
        ioString: "输入输出与字符串",
        loopRecursion: "循环与递归",
        dataStructure: "数据结构",
        dynamicProgramming: "动态规划",
        general: "综合应用",
        boundary: "边界处理",
        ioFormat: "输入输出规范",
        stateMaintenance: "状态维护",
        problem: "题目",
        submissionRecord: "提交记录",
        studentWithId: "学生 #{{id}}"
      },
      tables: {
        assignmentTitle: "作业列表",
        problemTitle: "题目列表"
      },
      tableLabels: {
        status: "状态",
        problemCount: "题目",
        submissions: "提交",
        accuracy: "正确率",
        passed: "通过",
        issue: "错因"
      },
      units: {
        count: "次",
        problem: "题",
        passed: "通过"
      },
      ai: {
        title: "AI 知识归因",
        class: "班级路径",
        assignment: "作业路径",
        problem: "题目路径"
      },
      fit: {
        HIT: "标准库命中",
        PARTIAL: "标准库半命中",
        MISS: "库外候选",
        UNKNOWN: "归属未确认"
      },
      evidence: {
        title: "证据样本"
      },
      correction: {
        title: "校正归因",
        issue: "错因",
        fineIssue: "细分错因",
        note: "记录",
        submit: "保存校正",
        unavailable: "暂无可校正提交。"
      },
      empty: {
        noClasses: "暂无班级",
        noClassesDescription: "创建班级后，这里会进入班级分析。",
        classNotFound: "班级未找到",
        classNotFoundDescription: "返回班级列表重新选择。",
        assignmentNotFound: "作业未找到",
        assignmentNotFoundDescription: "返回班级页面重新选择作业。",
        problemNotFound: "题目未找到",
        problemNotFoundDescription: "返回作业页面重新选择题目。",
        noAssignments: "当前班级暂无作业数据。",
        noSubmissions: "暂无提交数据，图表会在提交后生成。",
        noInsight: "暂无可归因数据。",
        noEvidence: "暂无提交证据样本。",
        noKnowledgePath: "暂无知识路径。",
        noIssue: "暂无集中错因"
      }
    },
    teacherHome: {
      classOverview: "班级概览",
      classLabel: "班级",
      defaultClass: "默认班级",
      unboundClass: "未绑定班级",
      pilotTitle: "课堂编程作业",
      assignmentFallbackWithId: "课堂作业 #{{id}}",
      classProgress: "班级进度",
      newAssignment: "新建作业",
      assignmentListAria: "教师作业入口",
      assignmentListTitle: "作业",
      assignmentCount: "{{count}} 个",
      loadingAssignments: "正在读取作业",
      emptyAssignmentsTitle: "暂无作业",
      emptyAssignmentsDescription: "新建作业后，这里会显示课堂入口和学生状态。",
      viewDetails: "查看详情",
      needsAttentionWithCount: "需看 {{count}}",
      metrics: {
        assignments: "作业",
        students: "学生",
        needsAttention: "需看",
        recentSubmissions: "新增",
        problems: "题目",
        status: "状态"
      },
      console: {
        title: "作业中心",
        description: "先看有哪些作业，再进入作业里的题目、正确率和学生证据。",
        tableTitle: "作业列表",
        tableHint: "按老师真实批阅路径组织：作业 → 题目 → 学生。",
        classColumn: "班级",
        submitted: "提交",
        passRate: "通过率",
        attention: "需关注",
        recent: "最近",
        action: "进入",
        queueTitle: "待处理队列",
        queueHint: "只放需要老师优先看的学生或题目。",
        queueEmptyTitle: "暂时没有待处理",
        queueEmptyDescription: "学生提交后，这里会自动汇总需关注对象。",
        stable: "稳定",
        waiting: "等待提交",
        noSharedIssue: "暂无集中错因"
      },
      errors: {
        loadFailed: "教师端数据读取失败。",
        serviceUnavailable: "教师端数据读取失败，服务暂时不可用。",
        notFound: "教师端数据读取失败，未找到课堂资源。",
        separator: "，"
      }
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
      },
      console: {
        stageAssignment: "作业题目",
        stageProblem: "题目分析",
        stageStudent: "学生诊断",
        problemListHint: "先看每道题的提交、正确率和集中错因，再进入具体题目。",
        aiPanelTitle: "AI 辅助摘要",
        aiPanelHint: "AI 只解释证据，不替代老师判断。",
        priorityStudent: "优先学生",
        priorityProblem: "优先讲评题",
        lectureSuggestion: "讲评建议",
        noEvidenceYet: "等待学生提交后形成错因和讲评建议。",
        submittedRatio: "提交人数"
      }
    },
    teacherManagement: {
      readiness: {
        aria: "系统状态",
        title: "开课状态",
        aiScan: "AI 配置扫描",
        scanDescription: "检查模型、密钥和调用链是否可用。",
        ready: "可开课",
        blocked: "需处理",
        degraded: "可试用",
        unknown: "读取中",
        readyDescription: "关键服务已通过检查。",
        blockedDescription: "{{count}} 个阻断需要处理。",
        degradedDescription: "{{count}} 个提醒，先试用再完善。",
        loadingDescription: "正在读取系统状态。",
        classes: "班级",
        problems: "题目",
        library: "标准库",
        statsAria: "管理数据",
        refresh: "刷新",
        runAi: "检测 AI",
        running: "检测中",
        detailSummary: "{{blocking}} 个阻断 · {{warnings}} 个提醒",
        checkDetails: "检查详情",
        checkFail: "失败",
        checkWarn: "提醒",
        checkPass: "通过",
        allClear: "全部关键检查已通过。",
        hiddenMore: "还有 {{count}} 项"
      },
      aiLibrary: {
        tabs: {
          library: "正式库",
          governance: "人工治理"
        },
        governance: {
          eyebrow: "标准库人工治理",
          title: "候选审核工作台",
          description: "AI 只提出候选；老师在这里确认、修正、合并或拒绝，正式库不会被自动污染。",
          pendingCount: "待处理 {{count}}",
          refresh: "刷新候选",
          summaryAria: "人工治理摘要",
          highFrequencyPaths: "高频路径",
          weakPaths: "待补强路径",
          noHighFrequencyPaths: "暂无高频候选路径。",
          noWeakPaths: "暂无集中薄弱路径。",
          emptyTitle: "暂无成长候选",
          emptyDescription: "当 AI 诊断发现标准库缺口时，候选会出现在这里。",
          emptyFilteredTitle: "没有匹配候选",
          emptyFilteredDescription: "调整状态、路径或关键词筛选。",
          metrics: {
            total: "候选总数",
            pending: "待审核",
            duplicates: "重复聚合",
            merged: "已入库",
            closed: "拒绝/忽略"
          },
          filters: {
            aria: "筛选成长候选",
            searchAria: "搜索候选名称、ID、路径或证据",
            searchPlaceholder: "名称、ID、路径、证据",
            statusAria: "候选状态",
            allStatus: "全部状态",
            pendingOnly: "只看待处理",
            clear: "清除筛选"
          },
          status: {
            proposed: "待处理",
            needsReview: "待审核",
            blocked: "需修正",
            mergedSimilar: "重复聚合",
            teacherApproved: "教师批准",
            merged: "已入库",
            rejected: "已拒绝",
            ignored: "已忽略"
          },
          evidence: {
            supported: "有证据",
            noDirectCodeEvidence: "无直接代码证据",
            unsupported: "证据无效",
            unknown: "证据待确认"
          }
        }
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
    teacherShell: {
      aria: "Teacher workbench navigation",
      kicker: "Teacher",
      title: "Teaching Analytics",
      footnote: "Drill from class to assignment to problem for objective results.",
      nav: {
        analytics: "Teaching Analytics",
        analyticsDescription: "Class, assignment, and problem results",
        roster: "Class Roster",
        rosterDescription: "Student import and class setup",
        problemBank: "Problem Bank",
        problemBankDescription: "Problem list and editor",
        aiLibrary: "AI Library",
        aiLibraryDescription: "Approved library and governance"
      }
    },
    classOverview: {
      title: "Class Insights",
      breadcrumb: "Assignments",
      backAssignments: "Back to assignments",
      newAssignment: "New assignment"
    },
    teacherAnalytics: {
      kicker: "Teaching Results",
      landing: {
        title: "Choose a class",
        description: "Start with a class, then drill into its assignments and problem results.",
        classList: "Class list",
        classFallback: "Class",
        noTeacherName: "No teacher name"
      },
      breadcrumb: {
        classes: "Classes"
      },
      scope: {
        class: "Class",
        assignment: "Assignment",
        problem: "Problem"
      },
      class: {
        description: "Review cross-assignment submissions, accuracy, error distribution, and knowledge paths."
      },
      assignment: {
        description: "Review problem results, error distribution, and AI knowledge attribution for this assignment."
      },
      problem: {
        description: "Review submissions, error distribution, knowledge path, and evidence samples for this problem."
      },
      actions: {
        newAssignment: "New assignment",
        backToAnalytics: "Back to analytics"
      },
      loading: {
        classes: "Loading classes",
        class: "Loading class analytics",
        assignment: "Loading assignment analytics",
        problem: "Loading problem analytics"
      },
      errors: {
        load: "Failed to load teaching analytics."
      },
      granularity: {
        chapter: "By chapter",
        knowledgePoint: "By knowledge point",
        skillUnit: "By skill unit",
        mistakePoint: "By mistake point"
      },
      metrics: {
        assignments: "Assignments",
        students: "Students",
        submissions: "Submissions",
        accuracy: "Accuracy",
        errorCount: "Errors",
        affectedStudents: "Students affected",
        submittedStudents: "Submitted",
        unsubmittedStudents: "Not submitted",
        averageAttempts: "Avg attempts",
        lowPassProblems: "Low-pass problems",
        passedStudents: "Passed",
        failedStudents: "Failed"
      },
      sections: {
        metrics: "Core metrics",
        visualization: "Data visualization",
        ranking: "Error distribution",
        share: "Share",
        currentPath: "Current knowledge path",
        assignments: "Assignments",
        problems: "Problems"
      },
      scopeTitle: "{{scope}} result distribution",
      pathMeta: "{{count}} times · {{students}} students · {{problems}} problems",
      pathLabel: "Path",
      status: {
        active: "Active",
        draft: "Draft",
        closed: "Closed"
      },
      defaultLabels: {
        defaultClass: "Default class",
        assignmentFallbackWithId: "Class assignment #{{id}}",
        pilotAssignment: "Class coding assignment",
        arraySequence: "Arrays and sequences",
        ioString: "Input, output, and strings",
        loopRecursion: "Loops and recursion",
        dataStructure: "Data structures",
        dynamicProgramming: "Dynamic programming",
        general: "General application",
        boundary: "Boundary handling",
        ioFormat: "Input/output format",
        stateMaintenance: "State maintenance",
        problem: "Problem",
        submissionRecord: "Submission record",
        studentWithId: "Student #{{id}}"
      },
      tables: {
        assignmentTitle: "Assignments",
        problemTitle: "Problems"
      },
      tableLabels: {
        status: "Status",
        problemCount: "Problems",
        submissions: "Submitted",
        accuracy: "Accuracy",
        passed: "Passed",
        issue: "Issue"
      },
      units: {
        count: "times",
        problem: "problems",
        passed: "passed"
      },
      ai: {
        title: "AI Knowledge Attribution",
        class: "Class paths",
        assignment: "Assignment paths",
        problem: "Problem paths"
      },
      fit: {
        HIT: "Library hit",
        PARTIAL: "Partial library match",
        MISS: "Out-of-library candidate",
        UNKNOWN: "Unconfirmed attribution"
      },
      evidence: {
        title: "Evidence samples"
      },
      correction: {
        title: "Correct attribution",
        issue: "Issue",
        fineIssue: "Detailed issue",
        note: "Note",
        submit: "Save correction",
        unavailable: "No correctable submission yet."
      },
      empty: {
        noClasses: "No classes yet",
        noClassesDescription: "Create a class to open class analytics.",
        classNotFound: "Class not found",
        classNotFoundDescription: "Return to the class list and choose again.",
        assignmentNotFound: "Assignment not found",
        assignmentNotFoundDescription: "Return to the class page and choose another assignment.",
        problemNotFound: "Problem not found",
        problemNotFoundDescription: "Return to the assignment page and choose another problem.",
        noAssignments: "No assignment data for this class yet.",
        noSubmissions: "No submission data yet. Charts will appear after submissions arrive.",
        noInsight: "No attribution data yet.",
        noEvidence: "No evidence samples yet.",
        noKnowledgePath: "No knowledge path yet.",
        noIssue: "No shared issue yet"
      }
    },
    teacherHome: {
      classOverview: "Class overview",
      classLabel: "Class",
      defaultClass: "Default class",
      unboundClass: "Unassigned class",
      pilotTitle: "Class Coding Assignment",
      assignmentFallbackWithId: "Class Assignment #{{id}}",
      classProgress: "Class progress",
      newAssignment: "New assignment",
      assignmentListAria: "Teacher assignment entries",
      assignmentListTitle: "Assignments",
      assignmentCount: "{{count}} items",
      loadingAssignments: "Loading assignments",
      emptyAssignmentsTitle: "No assignments yet",
      emptyAssignmentsDescription: "After you create one, classroom entry points and student status will appear here.",
      viewDetails: "View details",
      needsAttentionWithCount: "Review {{count}}",
      metrics: {
        assignments: "Assignments",
        students: "Students",
        needsAttention: "Review",
        recentSubmissions: "New",
        problems: "Problems",
        status: "Status"
      },
      console: {
        title: "Assignments",
        description: "Start with the assignment list, then drill into problems, accuracy, and student evidence.",
        tableTitle: "Assignment List",
        tableHint: "Organized by the teacher workflow: assignment → problem → student.",
        classColumn: "Class",
        submitted: "Submitted",
        passRate: "Pass rate",
        attention: "Attention",
        recent: "Recent",
        action: "Open",
        queueTitle: "Priority Queue",
        queueHint: "Only students or problems that need teacher action appear here.",
        queueEmptyTitle: "Nothing urgent",
        queueEmptyDescription: "After students submit, priority items will appear here.",
        stable: "Stable",
        waiting: "Waiting",
        noSharedIssue: "No shared issue yet"
      },
      errors: {
        loadFailed: "Failed to load teacher data.",
        serviceUnavailable: "Failed to load teacher data. The service is temporarily unavailable.",
        notFound: "Failed to load teacher data. Classroom resources were not found.",
        separator: ": "
      }
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
      },
      console: {
        stageAssignment: "Assignment Problems",
        stageProblem: "Problem Analysis",
        stageStudent: "Student Diagnosis",
        problemListHint: "Review submissions, accuracy, and shared issues before opening a problem.",
        aiPanelTitle: "AI Teaching Summary",
        aiPanelHint: "AI explains evidence; teachers still make the call.",
        priorityStudent: "Priority Student",
        priorityProblem: "Priority Problem",
        lectureSuggestion: "Review Suggestion",
        noEvidenceYet: "Shared issues and review suggestions will appear after students submit.",
        submittedRatio: "Submitted"
      }
    },
    teacherManagement: {
      readiness: {
        aria: "System status",
        title: "Class readiness",
        aiScan: "AI configuration scan",
        scanDescription: "Check model, key, and invocation health.",
        ready: "Ready",
        blocked: "Needs action",
        degraded: "Trial ready",
        unknown: "Loading",
        readyDescription: "Key services passed the checks.",
        blockedDescription: "{{count}} blocker(s) need attention.",
        degradedDescription: "{{count}} warning(s); safe to trial before polishing.",
        loadingDescription: "Loading system status.",
        classes: "Classes",
        problems: "Problems",
        library: "Library",
        statsAria: "Management data",
        refresh: "Refresh",
        runAi: "Check AI",
        running: "Checking",
        detailSummary: "{{blocking}} blockers · {{warnings}} warnings",
        checkDetails: "Check details",
        checkFail: "Fail",
        checkWarn: "Warning",
        checkPass: "Pass",
        allClear: "All key checks passed.",
        hiddenMore: "{{count}} more"
      },
      aiLibrary: {
        tabs: {
          library: "Library",
          governance: "Governance"
        },
        governance: {
          eyebrow: "Standard Library Governance",
          title: "Candidate Review Workbench",
          description: "AI only proposes candidates; teachers confirm, revise, merge, or reject them before the formal library changes.",
          pendingCount: "Pending {{count}}",
          refresh: "Refresh candidates",
          summaryAria: "Governance summary",
          highFrequencyPaths: "Frequent paths",
          weakPaths: "Paths to strengthen",
          noHighFrequencyPaths: "No frequent candidate paths yet.",
          noWeakPaths: "No concentrated weak paths yet.",
          emptyTitle: "No growth candidates",
          emptyDescription: "Candidates will appear here when AI diagnosis finds a standard-library gap.",
          emptyFilteredTitle: "No matching candidates",
          emptyFilteredDescription: "Adjust status, path, or keyword filters.",
          metrics: {
            total: "Candidates",
            pending: "Pending",
            duplicates: "Duplicates",
            merged: "Merged",
            closed: "Rejected/Ignored"
          },
          filters: {
            aria: "Filter growth candidates",
            searchAria: "Search candidate name, ID, path, or evidence",
            searchPlaceholder: "Name, ID, path, evidence",
            statusAria: "Candidate status",
            allStatus: "All statuses",
            pendingOnly: "Pending only",
            clear: "Clear filters"
          },
          status: {
            proposed: "Proposed",
            needsReview: "Needs review",
            blocked: "Needs fixes",
            mergedSimilar: "Duplicate group",
            teacherApproved: "Approved",
            merged: "Merged",
            rejected: "Rejected",
            ignored: "Ignored"
          },
          evidence: {
            supported: "Evidence backed",
            noDirectCodeEvidence: "No direct code evidence",
            unsupported: "Invalid evidence",
            unknown: "Evidence pending"
          }
        }
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
