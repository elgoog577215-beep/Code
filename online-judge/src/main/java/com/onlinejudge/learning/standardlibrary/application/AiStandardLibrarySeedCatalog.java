package com.onlinejudge.learning.standardlibrary.application;

import com.onlinejudge.learning.knowledge.application.InformaticsKnowledgeSeed;
import com.onlinejudge.learning.knowledge.application.InformaticsKnowledgeSeedCatalog;
import com.onlinejudge.learning.knowledge.domain.InformaticsKnowledgeNodeType;
import com.onlinejudge.learning.standardlibrary.domain.AiStandardLibraryLayer;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class AiStandardLibrarySeedCatalog {

    public static final String VERSION = "standard-library-v3-skill-mistake";

    private AiStandardLibrarySeedCatalog() {
    }

    public static List<AiStandardLibrarySeed> seeds() {
        List<AiStandardLibrarySeed> seeds = new ArrayList<>();
        highQualitySamples(seeds);
        compatibilityMistakePoints(seeds);
        AiStandardLibraryV6ExpansionSeeds.addTo(seeds);
        AiStandardLibraryV7DetailSeeds.addTo(seeds);
        generatedFullCoverage(seeds);
        return dedupe(seeds);
    }

    private static void highQualitySamples(List<AiStandardLibrarySeed> seeds) {
        skill(seeds,
                "SK_LOOP_ENDPOINT_INCLUSION",
                "能力点/循环边界",
                "判断循环端点是否包含",
                "能把题目中的“包含、不超过、至多、到第 r 个”等边界语义，转换成代码中的 <、<=、range 右端和循环终止条件。",
                "读题时先把区间写成数学形式，再决定代码循环条件。",
                List.of("BASIC.LOOP.BOUNDARY.左闭右开", "BASIC.LOOP.BOUNDARY.左闭右闭", "BASIC.EXPR.COMPARE.边界比较"),
                List.of("BASIC.EXPR.COMPARE.边界比较"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_LOOP_STRICT_INEQUALITY_WHEN_EQUAL_ALLOWED",
                "易错点/循环边界",
                "允许相等时误用严格不等号",
                "题目允许等于边界，但代码使用 < 或 > 排除了刚好等于的合法情况。",
                "把“不超过、至多、不大于”凭感觉写成小于，或没有单独代入等于边界检查。",
                "SK_LOOP_ENDPOINT_INCLUSION",
                "BOUNDARY",
                List.of("BASIC.LOOP.BOUNDARY.左闭右闭", "BASIC.EXPR.COMPARE.边界比较"),
                List.of("BASIC.EXPR.COMPARE.边界比较"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_LOOP_RANGE_RIGHT_ENDPOINT_MISREAD",
                "易错点/循环边界",
                "误把 Python range 右端当作包含",
                "使用 range(l, r) 时以为会遍历到 r，导致最后一个位置没有被处理。",
                "只记住从 l 开始，没有意识到 Python range 的右端是第一个不取到的位置。",
                "SK_LOOP_ENDPOINT_INCLUSION",
                "BOUNDARY",
                List.of("BASIC.LOOP.BOUNDARY.左闭右开", "BASIC.LOOP.FOR.终点设计"),
                List.of("BASIC.LOOP.FOR.终点设计"),
                "HIGH",
                List.of("PYTHON"));
        mistake(seeds,
                "MP_LOOP_LAST_ELEMENT_SKIPPED",
                "易错点/循环边界",
                "最后一个元素漏处理",
                "循环终点、数组长度或退出条件设计错误，导致最后一个元素或最后一次状态没有进入计算。",
                "把长度 n、最后下标 n-1、闭区间右端混在一起，未用最小样例验证最后一次迭代。",
                "SK_LOOP_ENDPOINT_INCLUSION",
                "BOUNDARY",
                List.of("BASIC.LOOP.BOUNDARY.最后一次迭代", "BASIC.ARRAY.INDEX.长度与最后下标"),
                List.of("BASIC.ARRAY.INDEX.长度与最后下标"),
                "HIGH",
                List.of("PYTHON", "CPP17"));

        skill(seeds,
                "SK_INDEX_BASE_MAPPING",
                "能力点/数组下标",
                "统一题面编号和代码下标",
                "能判断题面从 1 编号还是从 0 编号，并在读入、访问和输出时保持同一种下标体系。",
                "先确定题面编号体系，再决定数组是否补一位或统一减一。",
                List.of("BASIC.ARRAY.INDEX.0_基下标", "BASIC.ARRAY.INDEX.1_基下标", "BASIC.ARRAY.INDEX.下标映射"),
                List.of("BASIC.ARRAY.INDEX.长度与最后下标"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_ARRAY_ZERO_ONE_BASE_MIXED",
                "易错点/数组下标",
                "0 基和 1 基下标混用",
                "同一段逻辑里题面编号和代码下标没有统一，导致访问位置整体偏移一位。",
                "读入时按 1 开始理解，访问时又直接当作 0 基数组下标使用。",
                "SK_INDEX_BASE_MAPPING",
                "BOUNDARY",
                List.of("BASIC.ARRAY.INDEX.0_基下标", "BASIC.ARRAY.INDEX.1_基下标", "BASIC.ARRAY.INDEX.下标映射"),
                List.of("BASIC.LOOP.BOUNDARY.0_基下标循环"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_ARRAY_ACCESS_N_AS_LAST_INDEX",
                "易错点/数组下标",
                "把长度 n 当作最后下标",
                "数组长度为 n 时最后合法下标应是 n-1，却访问了 a[n] 或等价位置。",
                "没有区分“元素个数”和“最后一个位置编号”。",
                "SK_INDEX_BASE_MAPPING",
                "BOUNDARY",
                List.of("BASIC.ARRAY.INDEX.越界访问", "BASIC.ARRAY.INDEX.长度与最后下标"),
                List.of("BASIC.LOOP.BOUNDARY.最后一次迭代"),
                "HIGH",
                List.of("PYTHON", "CPP17"));

        skill(seeds,
                "SK_IO_STRUCTURE_MAPPING",
                "能力点/输入输出",
                "把题面输入结构映射成读取流程",
                "能分清单组、多组、查询次数、矩阵、字符串和读到 EOF，并让代码完整消费题面输入。",
                "先数清题面有几段输入、每段循环几次，再写读取代码。",
                List.of("BASIC.IO.STDIN.输入顺序映射", "BASIC.IO.MULTI_CASE.显式_T_组循环", "CONTEST.READING.INPUT.数据组数"),
                List.of("CONTEST.READING.INPUT.数据组数"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_IO_ONLY_READS_ONE_CASE",
                "易错点/输入输出",
                "多组数据只处理一组",
                "题面要求处理 T 组或多次查询，但代码只读取并输出了一次。",
                "把样例中的单组输入误认为所有输入格式，没有把第一行 T/q 转换成外层循环。",
                "SK_IO_STRUCTURE_MAPPING",
                "IO_FORMAT",
                List.of("BASIC.IO.MULTI_CASE.显式_T_组循环", "BASIC.IO.MULTI_CASE.样例单组与隐藏多组差异"),
                List.of("BASIC.LOOP.FOR.循环次数计算"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_IO_DEBUG_OUTPUT_LEFT",
                "易错点/输入输出",
                "调试输出残留",
                "代码输出了题目没有要求的中间变量、提示语或日志，导致输出不完全匹配。",
                "本地调试时为了观察变量加了输出，提交前没有只保留最终答案。",
                "SK_IO_STRUCTURE_MAPPING",
                "IO_FORMAT",
                List.of("BASIC.IO.STDOUT.禁止多余调试输出", "ENG.ERROR.FORMAT.调试输出"),
                List.of("BASIC.IO.STDOUT.按要求换行"),
                "MEDIUM",
                List.of("PYTHON", "CPP17"));

        skill(seeds,
                "SK_BINARY_ANSWER_CHECK",
                "能力点/二分答案",
                "设计二分答案的 check 函数",
                "能把候选答案是否可行写成单调的判断函数，并保证 check 的边界语义和题意一致。",
                "先说明 cap、mid 或答案候选值的含义，再写可行性判断。",
                List.of("ALGO.BINARY.ANSWER.check_函数", "ALGO.BINARY.ANSWER.单调性证明", "ALGO.BINARY.ANSWER.左右边界"),
                List.of("BASIC.EXPR.COMPARE.边界比较", "ENG.COMPLEXITY.TIME.数据范围反推"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_BINARY_CHECK_EQUAL_CASE_REJECTED",
                "易错点/二分答案",
                "check 函数错误拒绝等于边界",
                "check 中把刚好满足限制的候选答案误判为不可行，破坏了真实题意下的可行性判断。",
                "写 check 时只凭经验选 < 或 >，没有用“刚好卡边界”的例子单独验证。",
                "SK_BINARY_ANSWER_CHECK",
                "BOUNDARY",
                List.of("ALGO.BINARY.ANSWER.check_函数", "BASIC.EXPR.COMPARE.边界比较"),
                List.of("BASIC.LOOP.BOUNDARY.左闭右闭"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_BINARY_LOWER_BOUND_TOO_LOOSE",
                "易错点/二分答案",
                "答案下界没有利用必要条件",
                "二分下界从不可能的值开始，虽然有时不影响答案，但会让 check 接收大量无意义候选并掩盖边界理解问题。",
                "没有先问答案至少要满足什么条件，例如容量至少能放下最大单件物品。",
                "SK_BINARY_ANSWER_CHECK",
                "MODELING",
                List.of("ALGO.BINARY.ANSWER.左右边界", "ALGO.BINARY.ANSWER.最大最小答案"),
                List.of("CONTEST.READING.CONSTRAINT.数据范围"),
                "MEDIUM",
                List.of("PYTHON", "CPP17"));

        skill(seeds,
                "SK_DP_STATE_MEANING",
                "能力点/DP 状态",
                "用一句话定义 DP 状态含义",
                "能先说明 dp 数组每个维度、每个下标和每个值表示什么，再根据定义写初值、转移和答案位置。",
                "写转移前先写状态定义，避免公式和含义脱节。",
                List.of("ALGO.DP.STATE.状态含义", "ALGO.DP.STATE.维度选择", "ALGO.DP.TRANSITION.枚举决策"),
                List.of("BASIC.ARRAY.INDEX.下标映射", "ALGO.DP.INIT.初始值"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_DP_STATE_MISSING_DIMENSION",
                "易错点/DP 状态",
                "DP 状态少了一维必要信息",
                "当前状态没有保存未来决策所需的关键信息，导致不同情况被合并成同一个状态。",
                "只按位置建状态，但题目还需要容量、次数、上一次选择、是否使用过某条件等信息。",
                "SK_DP_STATE_MEANING",
                "STATE",
                List.of("ALGO.DP.STATE.维度选择", "ALGO.DP.STATE.状态含义"),
                List.of("ALGO.DP.INIT.初始值"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_DP_INIT_UNREACHABLE_AS_ZERO",
                "易错点/DP 状态",
                "不可达状态错误初始化为 0",
                "最值或计数 DP 中把不可达状态当成普通 0 值，导致非法路径参与转移。",
                "没有区分初始合法状态、暂时不可达状态和真实值为 0 的状态。",
                "SK_DP_STATE_MEANING",
                "INITIALIZATION",
                List.of("ALGO.DP.INIT.初始值", "ALGO.DP.INIT.无穷大", "ALGO.DP.STATE.非法状态"),
                List.of("BASIC.TYPE.INTEGER.long_long_范围"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_DP_ROLLING_ARRAY_WRONG_ORDER",
                "易错点/DP 状态",
                "滚动数组遍历方向覆盖旧状态",
                "空间压缩后遍历方向错误，把本轮新值当成上一轮旧值使用。",
                "只看到二维 DP 可以压成一维，没有重新判断转移依赖的是旧层还是当前层。",
                "SK_DP_STATE_MEANING",
                "TRANSITION",
                List.of("ALGO.DP.TRANSITION.从后往前", "ALGO.DP.STATE.状态压缩直觉"),
                List.of("BASIC.LOOP.BOUNDARY.最后一次迭代"),
                "HIGH",
                List.of("PYTHON", "CPP17"));

        skill(seeds,
                "SK_RECURSION_BASE_PROGRESS",
                "能力点/递归搜索",
                "保证递归有出口且规模推进",
                "能说明递归什么时候停止、每次调用参数如何靠近出口，以及回溯后状态是否恢复。",
                "画出前三层调用，比直接看代码更容易发现出口和恢复问题。",
                List.of("BASIC.RECURSION.BASE.终止条件", "BASIC.RECURSION.STATE.规模缩小", "ALGO.SEARCH.DFS.回溯恢复"),
                List.of("BASIC.FUNCTION.DEF.参数设计"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_RECURSION_BASE_CONDITION_MISSING",
                "易错点/递归搜索",
                "递归出口缺失或过晚",
                "递归没有在最小规模及时返回，或者出口条件写在递归调用之后，导致无限递归或栈溢出。",
                "只关注递归式，没有先处理最小规模和非法状态。",
                "SK_RECURSION_BASE_PROGRESS",
                "RUNTIME",
                List.of("BASIC.RECURSION.BASE.终止条件", "ENG.ERROR.RUNTIME.递归爆栈"),
                List.of("BASIC.FUNCTION.RETURN.完整返回路径"),
                "HIGH",
                List.of("PYTHON", "CPP17"));

        skill(seeds,
                "SK_SEARCH_VISITED_STATE",
                "能力点/搜索判重",
                "定义完整搜索状态并正确判重",
                "能判断 visited 需要记录哪些维度，并选择合适的标记时机避免重复入队或漏搜。",
                "先定义状态，再设计 visited，而不是只按点编号机械标记。",
                List.of("ALGO.SEARCH.STATE.判重", "ALGO.SEARCH.BFS.访问标记", "DS.GRAPH.TRAVERSE.访问标记"),
                List.of("DS.GRAPH.MODEL.点的定义"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_SEARCH_VISITED_DIMENSION_INCOMPLETE",
                "易错点/搜索判重",
                "visited 维度不足",
                "不同状态被错误合并为同一个 visited 标记，导致可行路径被跳过或答案错误。",
                "只按位置标记，但题目状态还包含步数、钥匙、方向、剩余资源等额外信息。",
                "SK_SEARCH_VISITED_STATE",
                "STATE",
                List.of("ALGO.SEARCH.STATE.状态编码", "ALGO.SEARCH.STATE.判重"),
                List.of("DS.GRAPH.MODEL.状态转图"),
                "HIGH",
                List.of("PYTHON", "CPP17"));

        skill(seeds,
                "SK_COMPLEXITY_CONSTRAINT_READING",
                "能力点/复杂度",
                "从数据范围反推复杂度目标",
                "能把 n、m、q、边数、时间限制转成可接受的数量级，并据此判断当前算法是否可能通过。",
                "写代码前先估算复杂度，样例通过不能代表大数据可过。",
                List.of("ENG.COMPLEXITY.TIME.数据范围反推", "CONTEST.READING.CONSTRAINT.数据范围", "ALGO.ENUM.COMPLEXITY.数据范围反推"),
                List.of("BASIC.LOOP.NESTED.复杂度估算"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_COMPLEXITY_IGNORES_MAX_CONSTRAINTS",
                "易错点/复杂度",
                "只看样例不看最大数据范围",
                "代码在样例或小数据上能运行，但核心循环次数在最大数据下明显超过可接受范围。",
                "用样例规模判断算法是否可行，没有把题面最大范围代入复杂度表达式。",
                "SK_COMPLEXITY_CONSTRAINT_READING",
                "COMPLEXITY",
                List.of("ENG.COMPLEXITY.TIME.数据范围反推", "ALGO.ENUM.COMPLEXITY.二重循环"),
                List.of("BASIC.LOOP.NESTED.复杂度估算"),
                "HIGH",
                List.of("PYTHON", "CPP17"));

        skill(seeds,
                "SK_BRANCH_CASE_COVERAGE",
                "能力点/条件分支",
                "覆盖分类讨论的全部边界",
                "能把题目中的分类条件拆成互斥且完整的分支，并明确等号、默认情况和特殊输入属于哪一类。",
                "先列出所有可能类别，再给每个边界值标注归属分支。",
                List.of("BASIC.BRANCH.CASE.互斥条件", "BASIC.BRANCH.CASE.覆盖所有情况", "BASIC.BRANCH.CASE.边界归属"),
                List.of("BASIC.EXPR.LOGIC.条件组合"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_BRANCH_EQUAL_CASE_FALLS_WRONG_SIDE",
                "易错点/条件分支",
                "等于边界落入错误分支",
                "分支条件把刚好等于阈值的情况划入了错误类别，导致边界样例答案错误。",
                "只按小于和大于思考，没有单独追问“等于时应该归哪一类”。",
                "SK_BRANCH_CASE_COVERAGE",
                "BOUNDARY",
                List.of("BASIC.BRANCH.CASE.边界归属", "BASIC.EXPR.COMPARE.边界比较"),
                List.of("BASIC.EXPR.LOGIC.条件组合"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_BRANCH_DEFAULT_CASE_MISSING",
                "易错点/条件分支",
                "分类讨论缺少兜底情况",
                "代码只处理了题面常见情况，没有覆盖剩余合法输入或空结果情况。",
                "把样例出现的类别误认为全部类别，没有检查分支是否完整覆盖输入空间。",
                "SK_BRANCH_CASE_COVERAGE",
                "LOGIC",
                List.of("BASIC.BRANCH.CASE.覆盖所有情况", "BASIC.BRANCH.GUARD.空结果处理"),
                List.of("CONTEST.READING.CONSTRAINT.隐藏特殊条件"),
                "HIGH",
                List.of("PYTHON", "CPP17"));

        skill(seeds,
                "SK_SIMULATION_STATE_SYNC",
                "能力点/模拟",
                "同步维护模拟状态和事件顺序",
                "能把题面规则拆成状态变量、事件顺序、更新时机和终止条件，并保证每一步推进后所有相关状态同步。",
                "模拟题先写状态表和事件顺序，再写代码更新。",
                List.of("ALGO.SIM.STATE.状态变量选择", "ALGO.SIM.STATE.状态同步", "ALGO.SIM.PROCESS.事件顺序", "ALGO.SIM.PROCESS.终止条件"),
                List.of("BASIC.BRANCH.CASE.优先级条件前置", "ENG.STYLE.INVARIANT.循环不变量"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_SIM_UPDATE_ORDER_USES_STALE_STATE",
                "易错点/模拟",
                "模拟更新顺序使用旧状态",
                "当前步骤已经改变了某个状态，但后续判断仍使用更新前的旧值，导致事件顺序和题面规则不一致。",
                "把一轮模拟里的所有变量当成同时可用，没有区分先发生、后发生和同步更新。",
                "SK_SIMULATION_STATE_SYNC",
                "STATE",
                List.of("ALGO.SIM.PROCESS.事件顺序", "ALGO.SIM.STATE.状态同步"),
                List.of("ENG.DEBUG.TRACE.循环状态"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_SIM_MULTI_OBJECT_STATE_DESYNC",
                "易错点/模拟",
                "多对象状态没有同步更新",
                "题目中多个对象、队列、位置或计数相互影响，但代码只更新其中一部分，导致下一轮状态不完整。",
                "只盯住主变量变化，忽略伴随变化的标记、位置、剩余量或统计值。",
                "SK_SIMULATION_STATE_SYNC",
                "STATE",
                List.of("ALGO.SIM.STATE.状态变量选择", "ALGO.SIM.PROCESS.多对象模拟"),
                List.of("BASIC.ARRAY.TRAVERSE.同步遍历"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_SIM_TERMINATION_ONE_STEP_EARLY_OR_LATE",
                "易错点/模拟",
                "模拟终止条件早一步或晚一步",
                "循环在事件尚未完成时提前停止，或在答案已经确定后多执行一步，导致最终状态偏差。",
                "没有把“本轮开始前检查”还是“本轮结束后检查”写清楚。",
                "SK_SIMULATION_STATE_SYNC",
                "BOUNDARY",
                List.of("ALGO.SIM.PROCESS.终止条件", "ALGO.SIM.CORNER.极限步数"),
                List.of("BASIC.LOOP.WHILE.循环条件"),
                "HIGH",
                List.of("PYTHON", "CPP17"));

        skill(seeds,
                "SK_STRING_SLICE_ENDPOINT",
                "能力点/字符串",
                "统一子串起点、终点和长度语义",
                "能区分子串的起点、终点、长度参数和半开区间，并处理空串、末尾切片和语言差异。",
                "把子串先写成 [l, r) 或 [l, r]，再翻译成 Python 切片或 C++ substr。",
                List.of("BASIC.STRING.SUBSTRING.起止位置", "BASIC.STRING.SUBSTRING.长度参数", "BASIC.STRING.SUBSTRING.Python_CPP_切片差异"),
                List.of("BASIC.ARRAY.INDEX.长度与最后下标"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_STRING_LENGTH_USED_AS_END_INDEX",
                "易错点/字符串",
                "把子串长度当作结束下标",
                "C++ substr 或手写切片中混淆长度参数和结束位置，导致多取或少取字符。",
                "记住了要取一段字符串，但没有区分“取到哪里”和“取多少个”。",
                "SK_STRING_SLICE_ENDPOINT",
                "BOUNDARY",
                List.of("BASIC.STRING.SUBSTRING.长度参数", "BASIC.STRING.SUBSTRING.起止位置"),
                List.of("BASIC.ARRAY.INDEX.下标映射"),
                "HIGH",
                List.of("CPP17", "PYTHON"));
        mistake(seeds,
                "MP_STRING_EMPTY_SUBSTRING_MISSED",
                "易错点/字符串",
                "空串或末尾切片没有处理",
                "代码假设子串一定非空，遇到空结果、长度为 0 或切到末尾时逻辑失效。",
                "只用普通长度字符串调试，没有构造空串、单字符和刚好到末尾的样例。",
                "SK_STRING_SLICE_ENDPOINT",
                "BOUNDARY",
                List.of("BASIC.STRING.SUBSTRING.空串处理", "BASIC.STRING.SUBSTRING.越界保护"),
                List.of("ENG.DEBUG.BOUNDARY.最小输入"),
                "MEDIUM",
                List.of("PYTHON", "CPP17"));

        skill(seeds,
                "SK_STRING_SEARCH_AND_BUILD",
                "能力点/字符串",
                "处理字符串查找、统计和构造顺序",
                "能区分查找成功与失败、重叠与不重叠统计、字符追加顺序和结果构造效率。",
                "字符串题不要只看样例位置，要单独检查找不到、重叠、空串和结果顺序。",
                List.of("BASIC.STRING.MATCH.查找出现位置", "BASIC.STRING.MATCH.统计出现次数", "BASIC.STRING.MATCH.前后缀判断", "BASIC.STRING.BUILD.追加字符", "BASIC.STRING.BUILD.结果顺序"),
                List.of("BASIC.STRING.SUBSTRING.起止位置", "BASIC.ARRAY.INDEX.下标映射"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_STRING_FIND_NOT_FOUND_USED_AS_INDEX",
                "易错点/字符串",
                "查找失败结果直接当下标使用",
                "字符串查找没有命中时返回 -1、npos 或空结果，但代码仍把它当作有效位置继续切片或访问。",
                "只测试了能找到的样例，没有处理目标子串不存在的分支。",
                "SK_STRING_SEARCH_AND_BUILD",
                "BOUNDARY",
                List.of("BASIC.STRING.MATCH.查找出现位置", "BASIC.BRANCH.GUARD.空结果处理"),
                List.of("ENG.ERROR.RUNTIME.数组越界"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_STRING_OVERLAPPING_MATCH_SKIPPED",
                "易错点/字符串",
                "重叠子串统计被跳过",
                "题目要求统计所有出现位置，但代码每次命中后直接跳过整个模式长度，漏掉重叠出现。",
                "没有判断题目中的出现次数是否允许重叠，只按不重叠匹配习惯移动指针。",
                "SK_STRING_SEARCH_AND_BUILD",
                "LOGIC",
                List.of("BASIC.STRING.MATCH.统计出现次数", "BASIC.STRING.MATCH.逐位比较"),
                List.of("BASIC.LOOP.FOR.步长变化"),
                "MEDIUM",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_STRING_BUILD_ORDER_REVERSED",
                "易错点/字符串",
                "结果字符串构造顺序反了",
                "字符追加、删除替换或递归拼接时顺序和题目要求相反，导致字符集合正确但排列错误。",
                "只关注选了哪些字符，没有维护结果中字符出现的先后关系。",
                "SK_STRING_SEARCH_AND_BUILD",
                "STATE",
                List.of("BASIC.STRING.BUILD.结果顺序", "BASIC.STRING.BUILD.追加字符"),
                List.of("ALGO.SEARCH.DFS.路径记录"),
                "MEDIUM",
                List.of("PYTHON", "CPP17"));

        skill(seeds,
                "SK_ARRAY_TRAVERSAL_UPDATE_SAFETY",
                "能力点/数组",
                "保护数组遍历和原地更新的旧值",
                "能判断遍历时哪些值仍要作为旧状态使用，何时需要临时数组、逆序遍历或同步下标。",
                "更新数组前先问：后面的计算还要不要用更新前的值。",
                List.of("BASIC.ARRAY.TRAVERSE.同步遍历", "BASIC.ARRAY.TRAVERSE.逆序遍历", "BASIC.ARRAY.UPDATE.原地修改", "BASIC.ARRAY.UPDATE.临时数组", "BASIC.ARRAY.UPDATE.覆盖风险"),
                List.of("BASIC.LOOP.BOUNDARY.最后一次迭代"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_ARRAY_IN_PLACE_UPDATE_OVERWRITES_SOURCE",
                "易错点/数组",
                "原地更新覆盖后续仍需使用的旧值",
                "数组在遍历过程中被直接修改，后续位置又依赖原数组旧值，导致计算混入本轮新状态。",
                "只想节省空间，没有判断当前题目是否允许原地覆盖。",
                "SK_ARRAY_TRAVERSAL_UPDATE_SAFETY",
                "STATE",
                List.of("BASIC.ARRAY.UPDATE.覆盖风险", "BASIC.ARRAY.UPDATE.临时数组"),
                List.of("ALGO.DP.STATE.状态压缩直觉"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_ARRAY_PARALLEL_TRAVERSE_MISALIGNED",
                "易错点/数组",
                "多个数组同步遍历下标错位",
                "两个数组、字符串或列表需要按同一对象对应遍历，但代码使用了不同起点、不同步长或不同长度。",
                "没有先说明第 i 个元素在每个数组中分别代表同一个什么对象。",
                "SK_ARRAY_TRAVERSAL_UPDATE_SAFETY",
                "BOUNDARY",
                List.of("BASIC.ARRAY.TRAVERSE.同步遍历", "BASIC.ARRAY.INDEX.下标映射"),
                List.of("ENG.STYLE.NAME.下标命名"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_ARRAY_ACCUMULATOR_NOT_RESET_PER_CASE",
                "易错点/数组",
                "多组数据数组或累计状态未重置",
                "上一组数据的数组、计数或累计值残留到下一组，导致单组样例通过但多组隐藏数据失败。",
                "只按单组样例写初始化，没有把初始化放进每组数据的循环内部。",
                "SK_ARRAY_TRAVERSAL_UPDATE_SAFETY",
                "INITIALIZATION",
                List.of("BASIC.IO.MULTI_CASE.每组状态重置", "BASIC.ARRAY.UPDATE.累计更新"),
                List.of("BASIC.IO.MULTI_CASE.显式_T_组循环"),
                "HIGH",
                List.of("PYTHON", "CPP17"));

        skill(seeds,
                "SK_PREFIX_RANGE_QUERY",
                "能力点/前缀和",
                "用前缀和表达区间查询",
                "能定义 prefix[i] 的含义，区分 0 号空前缀、1 基前缀和原数组下标，并写出正确区间公式。",
                "先写 prefix[i] 表示前多少个元素，再推导 [l, r] 的查询式。",
                List.of("ALGO.PREFIX.SUM.前缀定义", "ALGO.PREFIX.SUM.区间查询", "ALGO.PREFIX.SUM.下标偏移"),
                List.of("BASIC.ARRAY.PREFIX.前缀和定义", "BASIC.ARRAY.INDEX.下标映射"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_PREFIX_QUERY_LEFT_OFFSET_WRONG",
                "易错点/前缀和",
                "区间左端偏移错误",
                "查询 [l, r] 时使用了 prefix[r] - prefix[l] 或类似错误公式，少算左端元素。",
                "没有明确 prefix 的下标含义，凭记忆套公式。",
                "SK_PREFIX_RANGE_QUERY",
                "BOUNDARY",
                List.of("ALGO.PREFIX.SUM.区间查询", "ALGO.PREFIX.SUM.下标偏移"),
                List.of("BASIC.ARRAY.INDEX.1_基下标"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_PREFIX_ACCUMULATE_INT_OVERFLOW",
                "易错点/前缀和",
                "前缀累计值溢出",
                "单个元素在 int 范围内，但区间累计和超过 int，导致查询结果错误。",
                "只看单个输入值范围，没有估算 n 个值累加后的最大量级。",
                "SK_PREFIX_RANGE_QUERY",
                "VALUE_RANGE",
                List.of("ALGO.PREFIX.SUM.long_long_累计", "BASIC.TYPE.INTEGER.long_long_范围"),
                List.of("ENG.COMPLEXITY.SPACE.数组规模"),
                "HIGH",
                List.of("CPP17"));

        skill(seeds,
                "SK_GRAPH_EDGE_MODELING",
                "能力点/图建模",
                "把题面关系建模为点和边",
                "能判断点代表什么、边代表什么、是否有向、是否带权，以及输入中的一条关系需要加入几条边。",
                "读边前先写清点、边、方向和权值四件事。",
                List.of("DS.GRAPH.MODEL.点的定义", "DS.GRAPH.MODEL.边的定义", "DS.GRAPH.MODEL.有向无向", "DS.GRAPH.STORE.双向边添加"),
                List.of("BASIC.ARRAY.INDEX.下标映射"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_GRAPH_UNDIRECTED_EDGE_ADDED_ONCE",
                "易错点/图建模",
                "无向边只加了一次",
                "题目关系是无向图，但邻接表只添加 u 到 v，没有添加 v 到 u，导致遍历或最短路漏边。",
                "看到输入是一行 u v 就只写了一次 push，没有把无向关系翻译成双向邻接。",
                "SK_GRAPH_EDGE_MODELING",
                "MODELING",
                List.of("DS.GRAPH.MODEL.有向无向", "DS.GRAPH.STORE.双向边添加"),
                List.of("DS.GRAPH.STORE.邻接表"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_GRAPH_WEIGHT_MEANING_IGNORED",
                "易错点/图建模",
                "边权含义没有进入算法",
                "输入包含权值、代价或距离，但代码按无权图处理，导致路径或最优值错误。",
                "只把边当连通关系，没有追问权值代表长度、花费、容量还是限制。",
                "SK_GRAPH_EDGE_MODELING",
                "MODELING",
                List.of("DS.GRAPH.MODEL.权值含义", "ALGO.GRAPH.SHORTEST.Dijkstra"),
                List.of("CONTEST.READING.INPUT.图结构"),
                "HIGH",
                List.of("PYTHON", "CPP17"));

        skill(seeds,
                "SK_SHORTEST_DISTANCE_RELAX",
                "能力点/最短路",
                "维护距离数组和松弛条件",
                "能为起点、不可达点和每条边松弛定义清楚的距离含义，并选择 BFS 或 Dijkstra 等匹配算法。",
                "先判断边权是否相同，再决定 BFS 最短步数还是带权最短路。",
                List.of("ALGO.GRAPH.SHORTEST.BFS_最短路", "ALGO.GRAPH.SHORTEST.Dijkstra", "ALGO.GRAPH.SHORTEST.距离初始化", "ALGO.GRAPH.SHORTEST.松弛操作"),
                List.of("DS.GRAPH.STORE.邻接表"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_SHORTEST_DIST_INITIALIZED_AS_ZERO",
                "易错点/最短路",
                "不可达距离错误初始化为 0",
                "除起点外的距离没有设为无穷大，导致未访问节点像已经有最短距离一样参与比较。",
                "把默认 0 当成方便初值，没有区分起点距离和未知距离。",
                "SK_SHORTEST_DISTANCE_RELAX",
                "INITIALIZATION",
                List.of("ALGO.GRAPH.SHORTEST.距离初始化", "ALGO.DP.INIT.无穷大"),
                List.of("BASIC.TYPE.INTEGER.long_long_范围"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_SHORTEST_WEIGHTED_GRAPH_USES_BFS",
                "易错点/最短路",
                "带权图误用普通 BFS",
                "边权不相同但代码按层数 BFS 求最短路，忽略了不同边的代价差异。",
                "把“最少边数”和“最小总代价”混为一谈。",
                "SK_SHORTEST_DISTANCE_RELAX",
                "MODELING",
                List.of("ALGO.GRAPH.SHORTEST.BFS_最短路", "ALGO.GRAPH.SHORTEST.Dijkstra", "DS.GRAPH.MODEL.权值含义"),
                List.of("DS.LINEAR.QUEUE.BFS_队列"),
                "HIGH",
                List.of("PYTHON", "CPP17"));

        skill(seeds,
                "SK_GREEDY_EXCHANGE_REASONING",
                "能力点/贪心",
                "说明贪心选择依据",
                "能说清排序依据、每一步选择维护的性质，并用交换、反例或不变量检查局部选择是否可靠。",
                "不要只说“看起来最优”，要说明为什么晚一点换成当前选择不会更差。",
                List.of("ALGO.GREEDY.CHOICE.排序依据", "ALGO.GREEDY.CHOICE.交换论证", "ENG.STYLE.INVARIANT.贪心维护量"),
                List.of("ALGO.SORT.BASIC.自定义比较"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_GREEDY_SORT_KEY_REVERSED",
                "易错点/贪心",
                "贪心排序关键字方向相反",
                "应按右端点、截止时间、代价或收益的某个方向排序，但代码使用了相反或无关的关键字。",
                "记住了要排序，却没有把排序关键字和贪心理由绑定起来。",
                "SK_GREEDY_EXCHANGE_REASONING",
                "MODELING",
                List.of("ALGO.GREEDY.CHOICE.排序依据", "ALGO.SORT.BASIC.升序降序"),
                List.of("ALGO.SORT.BASIC.自定义比较"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_GREEDY_NO_COUNTEREXAMPLE_CHECK",
                "易错点/贪心",
                "局部选择没有反例检查",
                "代码依赖直觉贪心，遇到局部最优不等于全局最优的数据会失败。",
                "用样例验证了一个选择规则，但没有尝试构造让该规则失败的小反例。",
                "SK_GREEDY_EXCHANGE_REASONING",
                "MODELING",
                List.of("ALGO.GREEDY.CHOICE.反例检查", "ALGO.GREEDY.CHOICE.交换论证"),
                List.of("ENG.DEBUG.SAMPLE.最小反例"),
                "MEDIUM",
                List.of("PYTHON", "CPP17"));

        skill(seeds,
                "SK_WINDOW_INVARIANT",
                "能力点/双指针",
                "维护滑动窗口不变量",
                "能说明窗口内始终满足什么条件、何时扩张、何时收缩，以及答案应该在扩张前后哪个时刻更新。",
                "每次移动指针后都检查窗口是否合法和计数是否同步。",
                List.of("ALGO.TWO_POINTERS.WINDOW.窗口扩张", "ALGO.TWO_POINTERS.WINDOW.窗口收缩", "ALGO.TWO_POINTERS.WINDOW.合法性判断", "ALGO.TWO_POINTERS.WINDOW.答案更新时机"),
                List.of("BASIC.ARRAY.TRAVERSE.同步遍历"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_WINDOW_SHRINK_CONDITION_TOO_LATE",
                "易错点/双指针",
                "窗口收缩条件过晚",
                "窗口已经不合法但代码仍先更新答案，导致把非法区间计入结果。",
                "没有明确答案更新前窗口必须满足的条件。",
                "SK_WINDOW_INVARIANT",
                "STATE",
                List.of("ALGO.TWO_POINTERS.WINDOW.窗口收缩", "ALGO.TWO_POINTERS.WINDOW.合法性判断"),
                List.of("ENG.STYLE.INVARIANT.窗口不变量"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_WINDOW_COUNTER_NOT_SYNCED",
                "易错点/双指针",
                "窗口计数与指针移动不同步",
                "左端移出或右端移入时没有同步更新频次、总和或状态变量，导致后续判断基于旧窗口。",
                "指针移动写了，维护量更新漏了一侧。",
                "SK_WINDOW_INVARIANT",
                "STATE",
                List.of("ALGO.TWO_POINTERS.WINDOW.计数维护", "ENG.STYLE.INVARIANT.窗口不变量"),
                List.of("DS.SET_MAP.MAP.频次统计"),
                "HIGH",
                List.of("PYTHON", "CPP17"));

        skill(seeds,
                "SK_HASH_FREQUENCY_MAPPING",
                "能力点/映射计数",
                "建立键、值和默认值关系",
                "能判断哈希表中的键代表什么、值存什么，访问不存在键时应给什么默认值，以及何时更新频次。",
                "先写清 key 和 value 的含义，再写读、查、改三步。",
                List.of("DS.SET_MAP.MAP.键值关系", "DS.SET_MAP.MAP.频次统计", "DS.SET_MAP.MAP.默认值", "DS.SET_MAP.MAP.键不存在处理"),
                List.of("BASIC.TYPE.VARIABLE.变量命名"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_HASH_MISSING_DEFAULT_VALUE",
                "易错点/映射计数",
                "不存在键没有默认值保护",
                "访问 map/dict 中尚未出现的键时没有初始化或使用默认值，导致运行错误或计数错误。",
                "以为所有键都会先出现，没有考虑第一次遇到某个值的情况。",
                "SK_HASH_FREQUENCY_MAPPING",
                "RUNTIME",
                List.of("DS.SET_MAP.MAP.默认值", "DS.SET_MAP.MAP.键不存在处理"),
                List.of("ENG.ERROR.RUNTIME.空容器访问"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_HASH_KEY_VALUE_ROLE_REVERSED",
                "易错点/映射计数",
                "键和值角色写反",
                "本应按元素、字符串或状态作为键，却把频次、位置或答案值当成键，导致查找关系错误。",
                "没有先用一句话说明 key 表示什么、value 表示什么。",
                "SK_HASH_FREQUENCY_MAPPING",
                "MODELING",
                List.of("DS.SET_MAP.MAP.键值关系", "ENG.STYLE.NAME.变量含义"),
                List.of("BASIC.FUNCTION.DEF.参数设计"),
                "MEDIUM",
                List.of("PYTHON", "CPP17"));

        skill(seeds,
                "SK_INTEGER_RANGE_PROTECTION",
                "能力点/整数范围",
                "估算整数运算的最大量级",
                "能根据输入范围估算加法、乘法、平方、累计和取模前的最大值，并选择合适整数类型或取模时机。",
                "不要只看单个变量范围，要看表达式和累计过程的最大可能值。",
                List.of("BASIC.TYPE.INTEGER.int_范围", "BASIC.TYPE.INTEGER.long_long_范围", "BASIC.TYPE.INTEGER.整型溢出", "MATH.NUMBER.MOD.大数防溢出"),
                List.of("BASIC.EXPR.ARITH.加减乘除顺序"),
                "HIGH",
                List.of("CPP17", "PYTHON"));
        mistake(seeds,
                "MP_INTEGER_MULTIPLY_OVERFLOW_BEFORE_CAST",
                "易错点/整数范围",
                "乘法先溢出再转 long long",
                "两个 int 先相乘已经溢出，之后再赋给 long long 也无法恢复正确结果。",
                "只看接收变量类型，没有注意表达式计算发生在转换之前。",
                "SK_INTEGER_RANGE_PROTECTION",
                "VALUE_RANGE",
                List.of("BASIC.TYPE.INTEGER.整型溢出", "BASIC.TYPE.INTEGER.long_long_范围"),
                List.of("BASIC.EXPR.ARITH.加减乘除顺序"),
                "HIGH",
                List.of("CPP17"));
        mistake(seeds,
                "MP_INTEGER_DIVISION_TRUNCATES_RESULT",
                "易错点/整数范围",
                "整数除法截断小数",
                "表达式需要保留小数或比例，但两个整数相除先发生截断，导致后续结果偏小。",
                "以为赋给浮点变量就会自动得到小数，没有注意除法运算本身的类型。",
                "SK_INTEGER_RANGE_PROTECTION",
                "VALUE_RANGE",
                List.of("BASIC.TYPE.FLOAT.整数除法误用", "BASIC.EXPR.ARITH.整数除法"),
                List.of("BASIC.TYPE.FLOAT.精度输出"),
                "HIGH",
                List.of("CPP17", "PYTHON"));

        skill(seeds,
                "SK_FUNCTION_CONTRACT",
                "能力点/函数",
                "定义函数参数、返回值和副作用边界",
                "能说清函数需要哪些输入、返回什么结果、是否修改外部状态，并让调用处和定义处保持一致。",
                "写函数前先用一句话描述参数含义、返回含义和是否输出。",
                List.of("BASIC.FUNCTION.DEF.参数设计", "BASIC.FUNCTION.DEF.返回值设计", "BASIC.FUNCTION.RETURN.输出与返回分离"),
                List.of("BASIC.TYPE.VARIABLE.变量命名"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_FUNCTION_OUTPUT_RETURN_MIXED",
                "易错点/函数",
                "输出和返回职责混在一起",
                "函数内部已经 print/cout 输出，调用处又把返回值当答案处理，或者函数应返回结果却只输出不返回。",
                "没有区分“给判题系统输出答案”和“给调用者返回中间结果”。",
                "SK_FUNCTION_CONTRACT",
                "MODELING",
                List.of("BASIC.FUNCTION.RETURN.输出与返回分离", "BASIC.FUNCTION.DEF.返回值设计"),
                List.of("BASIC.IO.STDOUT.按要求换行"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_FUNCTION_PARAMETER_ORDER_MISMATCH",
                "易错点/函数",
                "函数参数顺序与含义错位",
                "函数定义和调用处参数个数相同，但顺序或含义不一致，导致内部逻辑使用了错误变量。",
                "只核对了参数数量，没有逐个核对每个参数代表的题面对象。",
                "SK_FUNCTION_CONTRACT",
                "MODELING",
                List.of("BASIC.FUNCTION.DEF.参数设计", "BASIC.FUNCTION.PARAM.值传递"),
                List.of("ENG.STYLE.NAME.变量含义"),
                "MEDIUM",
                List.of("PYTHON", "CPP17"));

        skill(seeds,
                "SK_RECURSION_BACKTRACK_STATE",
                "能力点/回溯",
                "恢复递归搜索中的可变状态",
                "能判断递归进入前修改了哪些数组、集合、路径或计数变量，并在返回后恢复到进入前状态。",
                "把每次递归看成一次借用状态：改了什么，回来的时候就恢复什么。",
                List.of("BASIC.RECURSION.STATE.回溯恢复", "ALGO.SEARCH.DFS.回溯恢复", "ALGO.SEARCH.DFS.路径记录"),
                List.of("BASIC.RECURSION.BASE.终止条件"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_BACKTRACK_STATE_NOT_RESTORED",
                "易错点/回溯",
                "回溯后状态没有恢复",
                "递归分支修改了 visited、path、sum 或计数变量，但返回上一层时没有撤销，导致兄弟分支继承错误状态。",
                "只记得往下搜索时要标记，没有意识到回到上一层要把现场还原。",
                "SK_RECURSION_BACKTRACK_STATE",
                "STATE",
                List.of("BASIC.RECURSION.STATE.回溯恢复", "ALGO.SEARCH.DFS.回溯恢复"),
                List.of("ENG.DEBUG.TRACE.递归栈"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_RECURSION_RESULT_IGNORED",
                "易错点/递归",
                "递归返回结果没有向上传递",
                "递归子调用已经找到结果或计算出值，但上一层没有接收、返回或合并该结果。",
                "把递归调用当成会自动改变答案，没有明确返回值如何回到上一层。",
                "SK_RECURSION_BACKTRACK_STATE",
                "STATE",
                List.of("BASIC.RECURSION.STATE.递归转移", "BASIC.FUNCTION.RETURN.完整返回路径"),
                List.of("BASIC.FUNCTION.DEF.返回值设计"),
                "HIGH",
                List.of("PYTHON", "CPP17"));

        skill(seeds,
                "SK_MATRIX_COORDINATE_MAPPING",
                "能力点/二维数组",
                "统一矩阵行列和坐标含义",
                "能区分行 row、列 col、x/y 坐标和方向偏移，并在输入、访问、输出中保持同一套坐标解释。",
                "先写清 grid[r][c] 中 r 和 c 各自对应题面哪一维。",
                List.of("BASIC.ARRAY.MATRIX.行列含义", "BASIC.ARRAY.MATRIX.矩阵输入", "MATH.GEOMETRY.COORD.行列坐标", "MATH.GEOMETRY.COORD.坐标偏移"),
                List.of("BASIC.ARRAY.INDEX.下标映射"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_MATRIX_ROW_COL_SWAPPED",
                "易错点/二维数组",
                "行列下标写反",
                "读入或访问矩阵时把行和列交换使用，导致访问到错误格子或越界。",
                "把数学坐标的 x/y 和代码数组的 row/col 混在一起。",
                "SK_MATRIX_COORDINATE_MAPPING",
                "BOUNDARY",
                List.of("BASIC.ARRAY.MATRIX.行列含义", "MATH.GEOMETRY.COORD.x_y_坐标"),
                List.of("BASIC.ARRAY.INDEX.下标映射"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_MATRIX_DIRECTION_BOUNDARY_CHECK_INCOMPLETE",
                "易错点/二维数组",
                "方向移动后边界检查不完整",
                "上下左右移动后只检查了一维或检查顺序错误，导致越界格子进入访问逻辑。",
                "只关注方向数组写法，没有把新坐标是否合法作为进入下一步的前置条件。",
                "SK_MATRIX_COORDINATE_MAPPING",
                "BOUNDARY",
                List.of("BASIC.ARRAY.MATRIX.方向遍历", "BASIC.ARRAY.MATRIX.边界检查", "MATH.GEOMETRY.COORD.越界判断"),
                List.of("BASIC.BRANCH.GUARD.非法输入保护"),
                "HIGH",
                List.of("PYTHON", "CPP17"));

        skill(seeds,
                "SK_SORT_COMPARATOR_CONTRACT",
                "能力点/排序比较",
                "设计排序关键字和比较规则",
                "能根据题目目标确定排序主关键字、副关键字、升降序和是否需要保留原下标。",
                "排序前先写出比较两个元素时谁应该排在前面以及原因。",
                List.of("ALGO.SORT.BASIC.自定义比较", "ALGO.SORT.BASIC.升序降序", "ALGO.SORT.BASIC.原索引保存", "ALGO.SORT.APPLICATION.区间排序"),
                List.of("BASIC.FUNCTION.DEF.返回值设计"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_SORT_TIE_BREAKER_MISSING",
                "易错点/排序",
                "排序缺少必要的并列规则",
                "主关键字相同的数据需要按第二关键字或原顺序处理，但比较规则没有定义，导致后续贪心或输出不稳定。",
                "只考虑了不同主关键字的情况，没有分析相等时题目要求什么。",
                "SK_SORT_COMPARATOR_CONTRACT",
                "MODELING",
                List.of("ALGO.SORT.BASIC.自定义比较", "ALGO.SORT.BASIC.稳定排序"),
                List.of("ALGO.GREEDY.CHOICE.排序依据"),
                "MEDIUM",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_SORT_ORIGINAL_INDEX_LOST",
                "易错点/排序",
                "排序后丢失原始位置",
                "题目最终需要输出原编号或按原顺序恢复，但排序前没有保存原下标。",
                "只看到排序能方便计算，没有注意答案输出仍依赖原始身份。",
                "SK_SORT_COMPARATOR_CONTRACT",
                "MODELING",
                List.of("ALGO.SORT.BASIC.原索引保存", "ALGO.SORT.BASIC.排序后遍历"),
                List.of("CONTEST.READING.OUTPUT.输出路径"),
                "MEDIUM",
                List.of("PYTHON", "CPP17"));

        skill(seeds,
                "SK_SET_MAP_OPERATION_CHOICE",
                "能力点/集合映射",
                "按操作频率选择 set、map 或排序",
                "能根据题目主要操作是去重、成员判断、频次统计、键值映射还是有序遍历，选择合适容器。",
                "先列出题目反复做的操作，再决定用集合、映射、排序还是数组计数。",
                List.of("DS.SET_MAP.SET.成员判断", "DS.SET_MAP.MAP.频次统计", "DS.SET_MAP.HASH.空间换时间", "ALGO.SORT.APPLICATION.去重统计"),
                List.of("ENG.COMPLEXITY.TIME.数据范围反推"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_SET_MAP_IGNORES_DUPLICATES",
                "易错点/集合映射",
                "用 set 去重后丢失次数信息",
                "题目需要统计出现次数或重复贡献，但代码把数据放入 set 后只保留是否出现。",
                "把“出现过”和“出现了几次”混为一谈。",
                "SK_SET_MAP_OPERATION_CHOICE",
                "MODELING",
                List.of("DS.SET_MAP.SET.去重", "DS.SET_MAP.MAP.频次统计"),
                List.of("CONTEST.READING.OUTPUT.输出方案数"),
                "HIGH",
                List.of("PYTHON", "CPP17"));

        skill(seeds,
                "SK_UNION_FIND_COMPONENT",
                "能力点/并查集",
                "维护连通块代表元和合并关系",
                "能用 parent 表示集合代表元，区分 find、union、路径压缩和合并方向，并判断何时查询连通性。",
                "每次合并前先找代表元，查询时比较代表元而不是原编号。",
                List.of("ALGO.GRAPH.CONNECT.并查集", "ALGO.GRAPH.CONNECT.路径压缩", "ALGO.GRAPH.CONNECT.合并方向", "ALGO.GRAPH.CONNECT.连通块"),
                List.of("DS.GRAPH.MODEL.点的定义"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_UNION_FIND_PARENT_WITHOUT_FIND",
                "易错点/并查集",
                "直接比较 parent 未找代表元",
                "查询两个点是否连通时直接比较 parent[x] 和 parent[y]，没有先 find 到最终代表元。",
                "以为 parent 当前值就是集合编号，没有意识到路径可能还没有压缩。",
                "SK_UNION_FIND_COMPONENT",
                "STATE",
                List.of("ALGO.GRAPH.CONNECT.并查集", "ALGO.GRAPH.CONNECT.路径压缩"),
                List.of("BASIC.FUNCTION.RETURN.返回集合"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_UNION_FIND_REVERSES_DIRECTED_MEANING",
                "易错点/并查集",
                "把有方向关系误建成无向连通",
                "题目关系存在方向、依赖或先后约束，但代码用并查集合并成无向连通块，丢失关系方向。",
                "看到两个对象有关联就合并，没有判断这种关系是否真的具备连通等价性。",
                "SK_UNION_FIND_COMPONENT",
                "MODELING",
                List.of("ALGO.GRAPH.CONNECT.并查集", "DS.GRAPH.MODEL.有向无向"),
                List.of("ALGO.GRAPH.TOPO.依赖关系"),
                "HIGH",
                List.of("PYTHON", "CPP17"));

        skill(seeds,
                "SK_TOPO_INDEGREE_PROCESS",
                "能力点/拓扑排序",
                "维护入度和依赖推进顺序",
                "能把先修、依赖、工序等关系建成有向边，维护入度为 0 的队列，并用处理数量判断是否有环。",
                "拓扑排序先明确边方向：谁必须在谁之前。",
                List.of("ALGO.GRAPH.TOPO.入度", "ALGO.GRAPH.TOPO.队列推进", "ALGO.GRAPH.TOPO.环检测", "ALGO.GRAPH.TOPO.依赖关系"),
                List.of("DS.GRAPH.MODEL.有向无向", "DS.LINEAR.QUEUE.BFS_队列"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_TOPO_EDGE_DIRECTION_REVERSED",
                "易错点/拓扑排序",
                "依赖边方向建反",
                "题目表示 A 必须在 B 前，但代码把边建成 B -> A，导致入度和输出顺序整体反向。",
                "没有先用一句话确定边从前置任务指向后置任务，还是相反。",
                "SK_TOPO_INDEGREE_PROCESS",
                "MODELING",
                List.of("ALGO.GRAPH.TOPO.依赖关系", "DS.GRAPH.MODEL.有向无向"),
                List.of("CONTEST.READING.INPUT.图结构"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_TOPO_CYCLE_NOT_CHECKED",
                "易错点/拓扑排序",
                "没有检查有向环",
                "拓扑队列结束后没有比较已处理点数和总点数，导致有环数据被误认为成功排序。",
                "只关注能输出一个顺序，没有判断依赖是否全部被解除。",
                "SK_TOPO_INDEGREE_PROCESS",
                "STATE",
                List.of("ALGO.GRAPH.TOPO.环检测", "ALGO.GRAPH.TOPO.队列推进"),
                List.of("ENG.DEBUG.TRACE.队列变化"),
                "HIGH",
                List.of("PYTHON", "CPP17"));

        skill(seeds,
                "SK_NUMBER_THEORY_BOUNDARY",
                "能力点/数论",
                "处理质数、gcd、取模的定义边界",
                "能区分 0、1、负数、大数和模意义，保证质数判断、gcd/lcm 和取模运算符合数学定义。",
                "数论题先写定义边界，再写循环或公式。",
                List.of("MATH.NUMBER.PRIME.1_不是质数", "MATH.NUMBER.GCD.除零边界", "MATH.NUMBER.MOD.负数取模修正", "MATH.NUMBER.MOD.模意义保持"),
                List.of("BASIC.EXPR.ARITH.取模含义"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_PRIME_ONE_TREATED_AS_PRIME",
                "易错点/数论",
                "把 1 当作质数",
                "质数判断没有先排除 n <= 1，导致 1 或更小数被误判为质数。",
                "只记得从 2 开始试除，没有先写质数定义的最小边界。",
                "SK_NUMBER_THEORY_BOUNDARY",
                "BOUNDARY",
                List.of("MATH.NUMBER.PRIME.1_不是质数", "MATH.NUMBER.PRIME.试除判定"),
                List.of("BASIC.BRANCH.GUARD.特殊情况前置"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_LCM_OVERFLOW_BEFORE_DIVIDE",
                "易错点/数论",
                "lcm 先乘后除导致溢出",
                "计算最小公倍数时直接 a*b/gcd，a*b 先溢出，导致结果错误。",
                "只记住 lcm 公式，没有调整为先除后乘并估算中间量级。",
                "SK_NUMBER_THEORY_BOUNDARY",
                "VALUE_RANGE",
                List.of("MATH.NUMBER.GCD.最小公倍数", "MATH.NUMBER.MOD.大数防溢出"),
                List.of("BASIC.TYPE.INTEGER.long_long_范围"),
                "HIGH",
                List.of("CPP17"));

        skill(seeds,
                "SK_BITMASK_STATE_MEANING",
                "能力点/位运算",
                "定义掩码中每一位的状态含义",
                "能说明 mask 的第 i 位代表哪个对象或条件，并正确使用与、或、异或、移位检查和更新状态。",
                "写位运算前先画出二进制位和题面对象的对应表。",
                List.of("MATH.BIT.MASK.集合掩码", "MATH.BIT.MASK.状态判断", "MATH.BIT.OP.取某一位", "MATH.BIT.OP.设置清除位"),
                List.of("MATH.BIT.BINARY.位权"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_BIT_SHIFT_PRECEDENCE_WRONG",
                "易错点/位运算",
                "位移和比较优先级误判",
                "表达式中移位、按位与、比较没有加括号，导致判断的不是目标位是否为 1。",
                "凭直觉写位运算表达式，没有用括号明确先算哪一步。",
                "SK_BITMASK_STATE_MEANING",
                "SYNTAX",
                List.of("MATH.BIT.OP.取某一位", "BASIC.EXPR.PRIORITY.位运算优先级"),
                List.of("BASIC.EXPR.PRIORITY.括号明确意图"),
                "HIGH",
                List.of("CPP17", "PYTHON"));
        mistake(seeds,
                "MP_BITMASK_INDEX_OFF_BY_ONE",
                "易错点/位运算",
                "对象编号和掩码位偏移一位",
                "题面对象从 1 编号，但代码直接使用 1 << id，导致第 0 位空置或访问越界。",
                "没有把题面编号映射到从 0 开始的二进制位编号。",
                "SK_BITMASK_STATE_MEANING",
                "BOUNDARY",
                List.of("MATH.BIT.MASK.集合掩码", "BASIC.ARRAY.INDEX.下标映射"),
                List.of("BASIC.ARRAY.INDEX.0_基下标"),
                "HIGH",
                List.of("PYTHON", "CPP17"));

        skill(seeds,
                "SK_DEBUG_MINIMAL_COUNTEREXAMPLE",
                "能力点/调试",
                "构造最小反例定位错因",
                "能把失败输入缩小到最少元素、最小边界或最短路径，并用手算对照中间状态定位问题。",
                "调试时先让反例变小，再观察变量变化。",
                List.of("ENG.DEBUG.SAMPLE.最小反例", "ENG.DEBUG.SAMPLE.手算对照", "ENG.DEBUG.BOUNDARY.最小输入", "ENG.DEBUG.TRACE.循环状态"),
                List.of("CONTEST.SUBMIT.REVIEW.错误类型定位"),
                "MEDIUM",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_DEBUG_ONLY_RETESTS_SAMPLE",
                "易错点/调试",
                "只反复测试样例",
                "样例已经通过后仍只重复运行样例，没有构造边界或最小反例来区分错因。",
                "把样例当作充分证明，没有意识到隐藏数据通常覆盖边界和极端情况。",
                "SK_DEBUG_MINIMAL_COUNTEREXAMPLE",
                "DEBUGGING",
                List.of("ENG.DEBUG.SAMPLE.最小反例", "ENG.DEBUG.BOUNDARY.最小输入"),
                List.of("CONTEST.SUBMIT.CHECKLIST.边界复测"),
                "MEDIUM",
                List.of("PYTHON", "CPP17"));

        skill(seeds,
                "SK_READING_OBJECTIVE_CONSTRAINT",
                "能力点/读题建模",
                "从题面提取目标、约束和结构",
                "能把自然语言题目拆成输入对象、输出目标、数据范围、关系结构和隐藏特殊条件。",
                "读题时先写出“求什么、限制是什么、对象之间有什么关系”。",
                List.of("CONTEST.READING.OUTPUT.输出最值", "CONTEST.READING.CONSTRAINT.数据范围", "CONTEST.PATTERN.STRUCTURE.图结构"),
                List.of("CONTEST.READING.INPUT.数据组数"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_READING_OBJECTIVE_MISIDENTIFIED",
                "易错点/读题建模",
                "把输出目标理解错",
                "题目要求最值、计数、可行性、路径或构造之一，但代码实现了另一类目标。",
                "只看样例输出的表面形态，没有抽象题目真正要回答的问题。",
                "SK_READING_OBJECTIVE_CONSTRAINT",
                "MODELING",
                List.of("CONTEST.READING.OUTPUT.输出最值", "CONTEST.READING.OUTPUT.输出方案数", "CONTEST.READING.OUTPUT.输出是否可行"),
                List.of("CONTEST.PATTERN.OBJECTIVE.最值"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_READING_HIDDEN_CONSTRAINT_IGNORED",
                "易错点/读题建模",
                "忽略隐藏特殊条件",
                "题面给出了值域、空结果、重复元素、无解或多答案要求，但代码只覆盖普通情况。",
                "读题时只摘了输入输出格式，没有把约束和特殊条件转成分支或算法选择依据。",
                "SK_READING_OBJECTIVE_CONSTRAINT",
                "MODELING",
                List.of("CONTEST.READING.CONSTRAINT.隐藏特殊条件", "CONTEST.READING.OUTPUT.多答案要求"),
                List.of("ENG.DEBUG.BOUNDARY.重复元素"),
                "HIGH",
                List.of("PYTHON", "CPP17"));

        skill(seeds,
                "SK_READING_SAMPLE_CONSTRAINT_CROSSCHECK",
                "能力点/读题建模",
                "用样例、约束和输出规则交叉校验题意",
                "能把样例形态、数据范围、输出规则和特殊说明放在一起检查，避免只按样例表面写代码。",
                "读题不是抄样例流程，而是用样例验证你对目标、约束和输出规则的理解。",
                List.of("CONTEST.READING.INPUT.数组规模", "CONTEST.READING.OUTPUT.多答案要求", "CONTEST.READING.CONSTRAINT.数据范围", "CONTEST.READING.CONSTRAINT.隐藏特殊条件", "CONTEST.SUBMIT.CHECKLIST.复杂度检查"),
                List.of("ENG.COMPLEXITY.TIME.数据范围反推"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_READING_SAMPLE_SHAPE_OVERFITS_FORMAT",
                "易错点/读题建模",
                "把样例形态误当成完整格式",
                "代码按样例中的单组、固定长度或特定排列写死，遇到隐藏数据的多组、不同长度或不同顺序就失败。",
                "只模仿样例长相，没有回到输入格式说明确认循环次数和数据结构。",
                "SK_READING_SAMPLE_CONSTRAINT_CROSSCHECK",
                "MODELING",
                List.of("CONTEST.READING.INPUT.数据组数", "CONTEST.READING.INPUT.数组规模", "BASIC.IO.MULTI_CASE.样例单组与隐藏多组差异"),
                List.of("BASIC.IO.STDIN.输入顺序映射"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_READING_CONSTRAINT_ALGORITHM_MISMATCH",
                "易错点/读题建模",
                "数据范围和算法复杂度不匹配",
                "题面最大规模要求更低复杂度，但代码仍使用暴力枚举或重复计算，导致隐藏数据超时。",
                "先写出能过样例的思路，再回头才看数据范围，错过了算法选择信号。",
                "SK_READING_SAMPLE_CONSTRAINT_CROSSCHECK",
                "COMPLEXITY",
                List.of("CONTEST.READING.CONSTRAINT.数据范围", "CONTEST.PATTERN.RANGE.大范围_NLOGN", "ENG.COMPLEXITY.TIME.数据范围反推"),
                List.of("ALGO.ENUM.COMPLEXITY.数据范围反推"),
                "HIGH",
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                "MP_READING_TIE_RULE_IGNORED",
                "易错点/读题建模",
                "忽略并列和多答案规则",
                "题面规定并列最优时的输出顺序、字典序、最小编号或任意方案，但代码没有实现对应规则。",
                "只求出了一个最优值，没有继续读输出说明里对并列情况的要求。",
                "SK_READING_SAMPLE_CONSTRAINT_CROSSCHECK",
                "MODELING",
                List.of("CONTEST.READING.OUTPUT.多答案要求", "CONTEST.READING.OUTPUT.输出路径", "ALGO.SORT.BASIC.自定义比较"),
                List.of("ALGO.SORT.BASIC.稳定排序"),
                "MEDIUM",
                List.of("PYTHON", "CPP17"));
    }

    private static void compatibilityMistakePoints(List<AiStandardLibrarySeed> seeds) {
        compat(seeds, "SYNTAX_ERROR", "语法与编译", "语法/编译错误", "代码还未通过语言语法或编译器检查。", "语法符号、缩进、声明或函数调用不符合语言规则。", "CE", "HIGH", List.of("ENG.ERROR.COMPILE.语法拼写"));
        compat(seeds, "IO_FORMAT", "输入输出", "输入输出格式", "读入结构或输出格式与题面要求不一致。", "没有完整映射输入结构，或输出多余/缺少内容。", "IO_FORMAT", "HIGH", List.of("BASIC.IO.STDIN.输入顺序映射", "BASIC.IO.STDOUT.按要求换行"));
        compat(seeds, "LOOP_BOUNDARY", "循环与边界", "循环边界", "循环起点、终点、步长或退出条件少处理/多处理元素。", "没有用最小规模和端点样例核对循环变量经过的值。", "BOUNDARY", "HIGH", List.of("BASIC.LOOP.BOUNDARY.左闭右开", "BASIC.LOOP.BOUNDARY.最后一次迭代"));
        compat(seeds, "OFF_BY_ONE", "循环与边界", "差一位错误", "索引、计数或区间端点多算/少算一个位置。", "把长度、最后下标、闭区间右端或编号体系混在一起。", "BOUNDARY", "HIGH", List.of("BASIC.ARRAY.INDEX.长度与最后下标"));
        compat(seeds, "BOUNDARY_CONDITION", "边界条件", "边界条件", "极小、极大、空值、重复值或特殊输入没有被正确处理。", "只验证普通样例，没有构造边界样例。", "BOUNDARY", "MEDIUM", List.of("ENG.DEBUG.BOUNDARY.最小输入", "ENG.DEBUG.BOUNDARY.最大输入"));
        compat(seeds, "TIME_COMPLEXITY", "复杂度", "时间复杂度", "算法执行次数随输入规模增长过快。", "没有把最大数据范围代入核心循环次数。", "COMPLEXITY", "HIGH", List.of("ENG.COMPLEXITY.TIME.数据范围反推"));
        compat(seeds, "ALGORITHM_STRATEGY", "算法策略", "算法策略", "当前思路没有匹配题目结构或规模。", "用样例规律或局部直觉替代了一般算法模型。", "MODELING", "MEDIUM", List.of("CONTEST.PATTERN.STRUCTURE.图结构"));
        compat(seeds, "DP_STATE_DESIGN", "状态建模", "DP 状态定义不清", "DP 状态没有保存题目所需的完整信息。", "没有先用自然语言定义状态含义就开始写转移。", "STATE", "HIGH", List.of("ALGO.DP.STATE.状态含义"));
        compat(seeds, "DATA_STRUCTURE_CHOICE", "数据结构", "数据结构选择", "当前容器或组织方式不适合题目操作和规模。", "没有按操作频率选择查找、插入、排序或区间结构。", "MODELING", "MEDIUM", List.of("DS.SET_MAP.MAP.键值关系"));
        compat(seeds, "NEEDS_MORE_EVIDENCE", "证据", "证据不足", "当前证据不足以可靠判断主因。", "缺少可复现样例、错误输出、报错信息或关键变量轨迹。", "DEBUGGING", "LOW", List.of("ENG.DEBUG.TRACE.循环状态"));
    }

    private static void generatedFullCoverage(List<AiStandardLibrarySeed> seeds) {
        Set<String> skillCodes = codes(seeds, AiStandardLibraryLayer.SKILL_UNIT);
        Set<String> mistakeCodes = codes(seeds, AiStandardLibraryLayer.MISTAKE_POINT);
        for (InformaticsKnowledgeSeed knowledge : InformaticsKnowledgeSeedCatalog.seeds()) {
            if (knowledge.type() != InformaticsKnowledgeNodeType.KNOWLEDGE_POINT) {
                continue;
            }
            AiStandardLibrarySeed skill = generatedSkill(knowledge);
            if (skillCodes.add(skill.code())) {
                seeds.add(skill);
            }
            AiStandardLibrarySeed mistake = generatedMistake(knowledge, skill.code());
            if (mistakeCodes.add(mistake.code())) {
                seeds.add(mistake);
            }
        }
    }

    private static AiStandardLibrarySeed generatedSkill(InformaticsKnowledgeSeed knowledge) {
        return skillSeed(
                generatedCode("SK", knowledge.code()),
                safeName("能力点/" + domainName(knowledge), 80),
                safeName(generatedSkillName(knowledge), 120),
                generatedSkillDescription(knowledge),
                generatedLearningGoal(knowledge),
                List.of(knowledge.code()),
                prerequisites(knowledge),
                difficultyToSeverity(knowledge.difficulty()),
                List.of("PYTHON", "CPP17")
        );
    }

    private static AiStandardLibrarySeed generatedMistake(InformaticsKnowledgeSeed knowledge, String skillCode) {
        String mistakeType = mistakeTypeFor(knowledge.code());
        return mistakeSeed(
                generatedCode("MP", knowledge.code()),
                safeName("易错点/" + domainName(knowledge), 80),
                safeName(generatedMistakeName(knowledge, mistakeType), 120),
                generatedMistakeDefinition(knowledge, mistakeType),
                commonMisconceptionFor(knowledge),
                skillCode,
                mistakeType,
                List.of(knowledge.code()),
                prerequisites(knowledge),
                severityFor(knowledge.code()),
                List.of("PYTHON", "CPP17")
        );
    }

    static void skill(List<AiStandardLibrarySeed> seeds,
                      String code,
                      String category,
                      String name,
                      String description,
                      String learningGoal,
                      List<String> knowledgeNodeCodes,
                      List<String> prerequisites,
                      String severity,
                      List<String> languages) {
        seeds.add(skillSeed(code, category, name, description, learningGoal, knowledgeNodeCodes, prerequisites, severity, languages));
    }

    private static AiStandardLibrarySeed skillSeed(String code,
                                                   String category,
                                                   String name,
                                                   String description,
                                                   String learningGoal,
                                                   List<String> knowledgeNodeCodes,
                                                   List<String> prerequisites,
                                                   String severity,
                                                   List<String> languages) {
        return new AiStandardLibrarySeed(
                AiStandardLibraryLayer.SKILL_UNIT,
                code,
                category,
                name,
                description,
                learningGoal,
                "",
                "",
                "",
                "",
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                "",
                "",
                "",
                "",
                "",
                name,
                severity,
                languages,
                List.of(),
                knowledgeNodeCodes,
                prerequisites,
                "",
                VERSION
        );
    }

    static void mistake(List<AiStandardLibrarySeed> seeds,
                        String code,
                        String category,
                        String name,
                        String definition,
                        String commonMisconception,
                        String skillUnitCode,
                        String mistakeType,
                        List<String> knowledgeNodeCodes,
                        List<String> prerequisites,
                        String severity,
                        List<String> languages) {
        seeds.add(mistakeSeed(code, category, name, definition, commonMisconception, skillUnitCode,
                mistakeType, knowledgeNodeCodes, prerequisites, severity, languages));
    }

    private static AiStandardLibrarySeed mistakeSeed(String code,
                                                     String category,
                                                     String name,
                                                     String definition,
                                                     String commonMisconception,
                                                     String skillUnitCode,
                                                     String mistakeType,
                                                     List<String> knowledgeNodeCodes,
                                                     List<String> prerequisites,
                                                     String severity,
                                                     List<String> languages) {
        return new AiStandardLibrarySeed(
                AiStandardLibraryLayer.MISTAKE_POINT,
                code,
                category,
                name,
                definition,
                "",
                "该易错点用于约束 AI 返回标准化错因 ID 和名称，具体诊断、修正建议和提高建议由 AI 结合题目、代码与判题结果生成。",
                skillUnitCode,
                mistakeType,
                commonMisconception,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                "",
                "",
                "",
                "",
                "",
                "",
                severity,
                languages,
                skillUnitCode == null || skillUnitCode.isBlank() ? List.of() : List.of(skillUnitCode),
                knowledgeNodeCodes,
                prerequisites,
                "",
                VERSION
        );
    }

    private static void compat(List<AiStandardLibrarySeed> seeds,
                               String code,
                               String category,
                               String name,
                               String definition,
                               String commonMisconception,
                               String mistakeType,
                               String severity,
                               List<String> knowledgeNodeCodes) {
        String skillCode = "SK_COMPAT_" + code;
        skill(seeds,
                skillCode,
                "兼容能力/" + category,
                name + "识别",
                "能识别「" + name + "」相关的知识点和错误表现。",
                "用于兼容旧 AI 标准库标签，后续会逐步收敛到更细能力点。",
                knowledgeNodeCodes,
                List.of(),
                severity,
                List.of("PYTHON", "CPP17"));
        mistake(seeds,
                code,
                "兼容易错点/" + category,
                name,
                definition,
                commonMisconception,
                skillCode,
                mistakeType,
                knowledgeNodeCodes,
                List.of(),
                severity,
                List.of("PYTHON", "CPP17"));
    }

    private static List<AiStandardLibrarySeed> dedupe(List<AiStandardLibrarySeed> seeds) {
        Set<String> seen = new LinkedHashSet<>();
        List<AiStandardLibrarySeed> result = new ArrayList<>();
        for (AiStandardLibrarySeed seed : seeds) {
            String key = seed.layer().name() + "/" + seed.code();
            if (seen.add(key)) {
                result.add(seed);
            }
        }
        return result;
    }

    private static Set<String> codes(List<AiStandardLibrarySeed> seeds, AiStandardLibraryLayer layer) {
        Set<String> result = new LinkedHashSet<>();
        seeds.stream()
                .filter(seed -> seed.layer() == layer)
                .map(AiStandardLibrarySeed::code)
                .forEach(result::add);
        return result;
    }

    private static String generatedCode(String prefix, String knowledgeCode) {
        String slug = knowledgeCode.toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (slug.length() > 58) {
            slug = slug.substring(0, 58).replaceAll("_+$", "");
        }
        String hash = Integer.toUnsignedString(knowledgeCode.hashCode(), 16).toUpperCase(Locale.ROOT);
        return (prefix + "_" + slug + "_" + hash).replaceAll("__+", "_");
    }

    private static List<String> prerequisites(InformaticsKnowledgeSeed knowledge) {
        List<String> result = new ArrayList<>();
        if (knowledge.parentCode() != null && !knowledge.parentCode().isBlank()) {
            result.add(knowledge.parentCode());
        }
        if (knowledge.prerequisites() != null) {
            result.addAll(knowledge.prerequisites());
        }
        return result.stream().distinct().toList();
    }

    private static String generatedSkillName(InformaticsKnowledgeSeed knowledge) {
        String code = knowledge.code();
        if (code.contains(".IO.")) {
            return "映射" + knowledge.name() + "到读写流程";
        }
        if (code.contains(".BRANCH.")) {
            return "覆盖" + knowledge.name() + "的分支条件";
        }
        if (code.contains(".PREFIX.")) {
            return "使用" + knowledge.name() + "处理区间";
        }
        if (code.contains(".TWO_POINTERS.")) {
            return "维护" + knowledge.name() + "的窗口不变量";
        }
        if (code.contains(".LOOP.")) {
            return "设计" + knowledge.name() + "的循环边界";
        }
        if (code.contains(".ARRAY.INDEX")) {
            return "统一" + knowledge.name() + "的下标体系";
        }
        if (code.contains(".ARRAY.") || code.contains(".STRING.")) {
            return "处理" + knowledge.name() + "的序列边界";
        }
        if (code.contains(".DP.")) {
            return "定义" + knowledge.name() + "的状态与转移";
        }
        if (code.contains(".SEARCH.") || code.contains(".RECURSION.")) {
            return "约束" + knowledge.name() + "的状态和出口";
        }
        if (code.contains(".FUNCTION.")) {
            return "定义" + knowledge.name() + "的函数契约";
        }
        if (code.contains(".GRAPH.")) {
            return "建模" + knowledge.name() + "的点边关系";
        }
        if (code.contains(".MATRIX.") || code.contains(".GEOMETRY.COORD")) {
            return "统一" + knowledge.name() + "的坐标含义";
        }
        if (code.contains(".BINARY.")) {
            return "验证" + knowledge.name() + "的单调边界";
        }
        if (code.contains(".GREEDY.")) {
            return "说明" + knowledge.name() + "的选择依据";
        }
        if (code.contains(".SORT.")) {
            return "设计" + knowledge.name() + "的比较规则";
        }
        if (code.contains(".SET_MAP.") || code.contains(".HASH.")) {
            return "建立" + knowledge.name() + "的键值关系";
        }
        if (code.contains(".COMPLEXITY.") || code.contains(".ENUM.")) {
            return "评估" + knowledge.name() + "的规模上限";
        }
        if (code.contains(".TYPE.INTEGER") || code.contains(".MOD.")) {
            return "保护" + knowledge.name() + "的数值范围";
        }
        if (code.contains(".NUMBER.") || code.contains(".GCD") || code.contains(".PRIME")) {
            return "处理" + knowledge.name() + "的数学边界";
        }
        if (code.contains(".BIT.")) {
            return "定义" + knowledge.name() + "的二进制状态含义";
        }
        if (code.contains(".DEBUG.") || code.contains(".SUBMIT.")) {
            return "用" + knowledge.name() + "定位和复盘错误";
        }
        if (code.contains(".READING.") || code.contains(".PATTERN.")) {
            return "从题面识别" + knowledge.name();
        }
        return "落实" + knowledge.name() + "到题目和代码";
    }

    private static String generatedSkillDescription(InformaticsKnowledgeSeed knowledge) {
        String code = knowledge.code();
        if (code.contains(".IO.")) {
            return "能把题面输入段、输出要求和多组数据规则，准确翻译成读取循环、输出格式和状态重置位置。";
        }
        if (code.contains(".BRANCH.")) {
            return "能把题目条件拆成完整、互斥且边界归属明确的分支，并处理默认和特殊情况。";
        }
        if (code.contains(".PREFIX.")) {
            return "能定义前缀数组或差分数组的下标含义，并用它稳定表达区间查询或区间更新。";
        }
        if (code.contains(".TWO_POINTERS.")) {
            return "能说明左右指针移动规则、窗口合法性和维护量含义，保证每次移动后状态同步。";
        }
        if (code.contains(".LOOP.") || code.contains(".ARRAY.") || code.contains(".STRING.")) {
            return "能把长度、下标、端点、遍历范围和更新时机对应到代码，避免首尾位置和空结果漏处理。";
        }
        if (code.contains(".DP.")) {
            return "能先用自然语言定义状态含义，再推出初值、转移来源、计算顺序和答案位置。";
        }
        if (code.contains(".SEARCH.") || code.contains(".RECURSION.")) {
            return "能定义搜索或递归状态、终止条件、访问标记和回溯恢复规则，避免漏搜、重复或无法停止。";
        }
        if (code.contains(".FUNCTION.")) {
            return "能明确函数参数、返回值、局部状态和副作用边界，使定义、调用和题目目标保持一致。";
        }
        if (code.contains(".MATRIX.") || code.contains(".GEOMETRY.COORD")) {
            return "能区分行列、x/y、方向偏移和边界检查，把二维位置稳定映射到数组访问。";
        }
        if (code.contains(".GRAPH.")) {
            return "能把题面对象关系转化为点、边、方向、权值和存储结构，并选择匹配的图算法。";
        }
        if (code.contains(".BINARY.")) {
            return "能证明单调性，定义 check 函数语义，并让左右边界和返回值与题意一致。";
        }
        if (code.contains(".GREEDY.")) {
            return "能说明局部选择维护的性质，用排序依据、交换或反例检查支撑贪心正确性。";
        }
        if (code.contains(".SORT.")) {
            return "能根据题目目标设计排序关键字、升降序、稳定性和排序后的遍历或分组逻辑。";
        }
        if (code.contains(".SET_MAP.") || code.contains(".HASH.")) {
            return "能明确键和值分别表示什么，处理不存在键、重复值和频次更新。";
        }
        if (code.contains(".COMPLEXITY.") || code.contains(".ENUM.")) {
            return "能把数据范围代入循环层数、状态数或候选数，判断当前方案是否可能通过。";
        }
        if (code.contains(".TYPE.INTEGER") || code.contains(".MOD.")) {
            return "能估算表达式、累计和乘法的最大量级，并选择合适类型或取模时机。";
        }
        if (code.contains(".NUMBER.") || code.contains(".GCD") || code.contains(".PRIME")) {
            return "能先写清数学定义和特殊值边界，再设计循环、公式、取模或整除判断。";
        }
        if (code.contains(".BIT.")) {
            return "能把题面对象映射到二进制位，并用括号明确位运算、移位和比较的计算顺序。";
        }
        if (code.contains(".DEBUG.") || code.contains(".SUBMIT.")) {
            return "能根据错误类型构造最小反例、边界样例和变量轨迹，定位问题是否来自格式、边界、状态或复杂度。";
        }
        if (code.contains(".READING.") || code.contains(".PATTERN.")) {
            return "能从题面提取输入对象、输出目标、数据范围和结构特征，再决定算法方向。";
        }
        return "能在题目、代码和调试过程中准确识别并使用「" + knowledge.path() + "」这一精细知识点。";
    }

    private static String generatedLearningGoal(InformaticsKnowledgeSeed knowledge) {
        String code = knowledge.code();
        if (code.contains(".DP.")) {
            return "先写状态含义，再写初值、转移、顺序和答案位置。";
        }
        if (code.contains(".GRAPH.") || code.contains(".SEARCH.")) {
            return "先定义状态和边，再设计访问标记、队列/递归和结束条件。";
        }
        if (code.contains(".FUNCTION.")) {
            return "先写清函数契约，再写函数体和调用处。";
        }
        if (code.contains(".MATRIX.") || code.contains(".GEOMETRY.COORD")) {
            return "先统一行列和坐标含义，再写方向数组与边界判断。";
        }
        if (code.contains(".PREFIX.") || code.contains(".ARRAY.") || code.contains(".STRING.")) {
            return "先把下标和区间写成数学形式，再翻译成代码。";
        }
        if (code.contains(".COMPLEXITY.") || code.contains(".ENUM.")) {
            return "先估算最大数据下的操作次数，再决定是否提交当前思路。";
        }
        if (code.contains(".GREEDY.") || code.contains(".BINARY.")) {
            return "先说清选择或判断函数为什么成立，再套模板。";
        }
        if (code.contains(".NUMBER.") || code.contains(".BIT.")) {
            return "先写定义、边界和位/数值含义，再写公式或运算表达式。";
        }
        if (code.contains(".DEBUG.") || code.contains(".SUBMIT.")) {
            return "先缩小反例，再观察关键变量或状态表。";
        }
        if (code.contains(".READING.") || code.contains(".PATTERN.")) {
            return "先提取目标、约束和结构，再选择算法。";
        }
        return "把「" + knowledge.name() + "」和题目中的变量、条件、状态或算法步骤建立对应关系。";
    }

    private static String generatedMistakeName(InformaticsKnowledgeSeed knowledge, String mistakeType) {
        return switch (mistakeType) {
            case "IO_FORMAT" -> knowledge.name() + "读写结构映射错误";
            case "BOUNDARY" -> knowledge.name() + "边界或下标偏移";
            case "STATE" -> knowledge.name() + "状态定义不完整";
            case "COMPLEXITY" -> knowledge.name() + "规模估算不足";
            case "MODELING" -> knowledge.name() + "建模依据不清";
            case "RUNTIME" -> knowledge.name() + "运行出口或资源保护不足";
            case "SYNTAX" -> knowledge.name() + "语法规则未落实";
            case "VALUE_RANGE" -> knowledge.name() + "数值范围估算不足";
            default -> knowledge.name() + "适用条件混用";
        };
    }

    private static String generatedMistakeDefinition(InformaticsKnowledgeSeed knowledge, String mistakeType) {
        return switch (mistakeType) {
            case "IO_FORMAT" -> "学生没有把「" + knowledge.path() + "」对应的输入段、输出格式或多组循环完整落实到代码。";
            case "BOUNDARY" -> "学生在「" + knowledge.path() + "」相关逻辑中混淆长度、端点、下标或等号归属，导致多算、少算或越界。";
            case "STATE" -> "学生没有把「" + knowledge.path() + "」需要维护的状态、初值、转移或恢复规则定义完整。";
            case "COMPLEXITY" -> "学生没有把「" + knowledge.path() + "」对应的候选数、状态数或循环次数代入最大数据范围。";
            case "MODELING" -> "学生没有把「" + knowledge.path() + "」的题面对象、关系、排序依据或算法前提建模清楚。";
            case "RUNTIME" -> "学生在「" + knowledge.path() + "」相关代码中缺少出口、默认值、边界保护或资源规模判断。";
            case "SYNTAX" -> "学生知道「" + knowledge.path() + "」要表达的逻辑，但语法、声明、调用或符号书写未符合语言规则。";
            case "VALUE_RANGE" -> "学生没有估算「" + knowledge.path() + "」相关表达式、累计值或中间乘法的最大量级。";
            default -> "学生在使用「" + knowledge.path() + "」时，没有把知识点定义、适用条件或边界要求准确落实到当前代码。";
        };
    }

    private static String commonMisconceptionFor(InformaticsKnowledgeSeed knowledge) {
        String code = knowledge.code();
        if (code.contains(".IO.")) {
            return "只按样例读写，没有把题面输入输出结构完整映射到代码。";
        }
        if (code.contains(".BRANCH.")) {
            return "把普通情况写成了分支，但没有检查等于边界、默认分支和特殊输入是否都有归属。";
        }
        if (code.contains(".PREFIX.")) {
            return "记住了前缀和公式，但没有先定义前缀下标含义和区间端点。";
        }
        if (code.contains(".TWO_POINTERS.")) {
            return "知道要移动左右指针，但没有维护窗口合法性、计数变量和答案更新时机。";
        }
        if (code.contains(".LOOP.") || code.contains(".ARRAY.") || code.contains(".STRING.")) {
            return "没有区分长度、下标、端点和编号体系，普通样例能过但边界样例容易失败。";
        }
        if (code.contains(".DP.")) {
            return "急着写转移公式，没有先定义状态含义、初值、转移来源和答案位置。";
        }
        if (code.contains(".FUNCTION.")) {
            return "写出了函数形式，但没有明确参数、返回值和输出副作用之间的边界。";
        }
        if (code.contains(".MATRIX.") || code.contains(".GEOMETRY.COORD")) {
            return "把行列、x/y 或方向偏移混用，普通位置能过但边界和转向容易失败。";
        }
        if (code.contains(".GRAPH.SHORTEST")) {
            return "把最短步数、最小代价和距离初始化混在一起，没有根据边权选择算法。";
        }
        if (code.contains(".SEARCH.") || code.contains(".GRAPH.") || code.contains(".RECURSION.")) {
            return "只关注搜索过程，没有完整定义状态、访问标记、终止条件和恢复规则。";
        }
        if (code.contains(".BINARY.")) {
            return "记住了二分模板，但没有证明单调性、边界含义和 check 函数是否准确。";
        }
        if (code.contains(".COMPLEXITY.") || code.contains(".ENUM.")) {
            return "只验证样例规模，没有把最大数据范围代入操作次数和空间规模。";
        }
        if (code.contains(".GREEDY.")) {
            return "把局部最优选择当成直觉，没有构造反例或说明交换依据。";
        }
        if (code.contains(".SORT.")) {
            return "记住了要排序，但没有说明排序关键字、升降序和排序后遍历规则。";
        }
        if (code.contains(".SET_MAP.") || code.contains(".HASH.")) {
            return "使用 map/dict 时没有先说明键和值的含义，也没有处理第一次出现的键。";
        }
        if (code.contains(".TYPE.INTEGER") || code.contains(".MOD.")) {
            return "只看单个变量范围，没有估算乘法、累计或取模前的中间结果。";
        }
        if (code.contains(".NUMBER.")) {
            return "记住了数论公式，但没有先处理 0、1、负数、大数和整除定义边界。";
        }
        if (code.contains(".BIT.")) {
            return "知道要用位运算，但没有说明每一位代表什么，也没有用括号控制优先级。";
        }
        if (code.contains(".DEBUG.") || code.contains(".SUBMIT.")) {
            return "只重复运行样例，没有构造最小反例、边界样例或状态轨迹来定位错因。";
        }
        if (code.contains(".READING.") || code.contains(".PATTERN.")) {
            return "只摘了输入输出格式，没有把目标、数据范围和结构特征转成算法选择依据。";
        }
        return "知道这个知识点的名称，但没有把它准确落实到当前题目的变量、条件、状态或算法结构中。";
    }

    private static String mistakeTypeFor(String code) {
        if (code.contains(".IO.") || code.contains(".FORMAT")) {
            return "IO_FORMAT";
        }
        if (code.contains(".TYPE.INTEGER") || code.contains(".MOD.") || code.contains(".FLOAT.")) {
            return "VALUE_RANGE";
        }
        if (code.contains(".BRANCH.") || code.contains(".LOOP.") || code.contains(".ARRAY.")
                || code.contains(".STRING.") || code.contains(".PREFIX.") || code.contains(".BOUNDARY.")) {
            return "BOUNDARY";
        }
        if (code.contains(".DP.") || code.contains(".STATE.") || code.contains(".SIM.") || code.contains(".FUNCTION.")
                || code.contains(".TWO_POINTERS.") || code.contains(".WINDOW.")) {
            return "STATE";
        }
        if (code.contains(".COMPLEXITY.") || code.contains(".ENUM.")) {
            return "COMPLEXITY";
        }
        if (code.contains(".BINARY.") || code.contains(".GREEDY.") || code.contains(".GRAPH.")
                || code.contains(".SEARCH.") || code.contains(".SORT.") || code.contains(".SET_MAP.") || code.contains(".HASH.")
                || code.contains(".NUMBER.") || code.contains(".BIT.") || code.contains(".READING.") || code.contains(".PATTERN.")) {
            return "MODELING";
        }
        if (code.contains(".ERROR.COMPILE")) {
            return "SYNTAX";
        }
        if (code.contains(".ERROR.RUNTIME") || code.contains(".RECURSION.")) {
            return "RUNTIME";
        }
        return "CONCEPT";
    }

    private static String severityFor(String code) {
        if (code.contains(".IO.") || code.contains(".LOOP.") || code.contains(".ARRAY.")
                || code.contains(".DP.") || code.contains(".BINARY.") || code.contains(".COMPLEXITY.")
                || code.contains(".PREFIX.") || code.contains(".GRAPH.") || code.contains(".SEARCH.")
                || code.contains(".FUNCTION.") || code.contains(".MATRIX.") || code.contains(".NUMBER.")
                || code.contains(".BIT.") || code.contains(".READING.") || code.contains(".PATTERN.")
                || code.contains(".TYPE.INTEGER") || code.contains(".ERROR.")) {
            return "HIGH";
        }
        if (code.contains(".STYLE.") || code.contains(".SUBMIT.") || code.contains(".REVIEW.")) {
            return "LOW";
        }
        return "MEDIUM";
    }

    private static String difficultyToSeverity(String difficulty) {
        if (difficulty == null) {
            return "MEDIUM";
        }
        String value = difficulty.toLowerCase(Locale.ROOT);
        if (value.contains("提高") || value.contains("困难") || value.contains("高")) {
            return "HIGH";
        }
        if (value.contains("入门") || value.contains("基础")) {
            return "MEDIUM";
        }
        return "MEDIUM";
    }

    private static String domainName(InformaticsKnowledgeSeed knowledge) {
        String first = knowledge.path() == null ? "" : knowledge.path().split(" / ")[0];
        return first.isBlank() ? "信息学知识" : first;
    }

    private static String safeName(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }
}
