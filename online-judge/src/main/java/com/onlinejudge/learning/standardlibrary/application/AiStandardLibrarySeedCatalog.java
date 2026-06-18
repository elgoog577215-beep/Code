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

    public static final String VERSION = "standard-library-db-v2-full-coverage";

    private AiStandardLibrarySeedCatalog() {
    }

    public static List<AiStandardLibrarySeed> seeds() {
        List<AiStandardLibrarySeed> seeds = new ArrayList<>();
        basics(seeds);
        improvements(seeds);
        generatedFullCoverage(seeds);
        return seeds;
    }

    private static void basics(List<AiStandardLibrarySeed> seeds) {
        b(seeds, "SYNTAX_ERROR", "语法与编译", "语法/编译错误",
                "程序还没有通过语言语法或编译器检查。", "当前先让程序能被解释器或编译器接受。",
                "学生处在基础可运行阶段，应优先处理第一条编译/语法错误。",
                l("compiler error", "syntax error", "missing symbol", "type mismatch"),
                l("括号不配对", "变量未声明", "缩进错误", "函数签名不匹配"),
                l("CE", "COMPILE_ERROR"),
                "先看第一条报错，不要从后面的连锁报错开始。",
                "圈出报错行附近用到的符号、括号和缩进。",
                "只修改一个最小位置后重新提交，观察第一条报错是否变化。",
                "语法与运行环境", "HIGH", l("PYTHON", "CPP17"), l(), "FIX_FIRST_COMPILER_ERROR");
        b(seeds, "PY_INDENTATION", "语法与编译", "Python 缩进层级",
                "Python 代码块缩进不一致或缺少缩进。", "当前先确认每个 if/for/while/def 后面的代码块层级。",
                "学生可能还没有把缩进当作语法结构，需要从第一条报错附近处理。",
                l("IndentationError", "expected an indented block", "unexpected indent"),
                l("冒号后未缩进", "同一代码块缩进混用", "复制代码带入多余空格"),
                l("CE", "COMPILE_ERROR"),
                "先看报错行上一行是否以冒号结束。", "把同一代码块的语句左边界对齐。",
                "只调整一个代码块后重新运行，确认报错是否前移或消失。",
                "语法与运行环境", "HIGH", l("PYTHON"), l("SYNTAX_ERROR"), "FIX_FIRST_COMPILER_ERROR");
        b(seeds, "CPP_HEADER_NAMESPACE", "语法与编译", "C++ 头文件/命名空间",
                "C++ 缺少必要头文件、命名空间或标准库符号不可见。", "当前先处理编译器提示的第一个未声明符号。",
                "学生需要区分算法错误和编译环境/标准库引用问题。",
                l("not declared", "no member named", "undefined reference", "std::"),
                l("漏写 #include", "漏写 std::", "函数名拼写错误", "使用非标准头文件"),
                l("CE", "COMPILE_ERROR"),
                "先找第一个未声明的符号。", "确认它来自哪个头文件或是否需要 std:: 前缀。",
                "只补最小头文件或命名空间后重新编译。",
                "语法与运行环境", "HIGH", l("CPP17"), l("SYNTAX_ERROR"), "FIX_FIRST_COMPILER_ERROR");
        b(seeds, "TYPE_MISMATCH", "语法与编译", "类型不匹配",
                "变量、表达式、函数参数或返回值类型不一致。", "当前先看报错中两边分别是什么类型。",
                "学生需要建立类型流动意识，尤其是字符串/整数、容器元素和函数返回。",
                l("type mismatch", "cannot convert", "incompatible types", "bad operand"),
                l("字符串和整数直接运算", "函数返回类型不匹配", "容器元素类型混用"),
                l("CE", "COMPILE_ERROR"),
                "先读清楚报错中的两个类型。", "追踪这个值从哪里来、要传到哪里去。",
                "把输入转换、变量声明和函数返回类型统一后再试。",
                "类型意识", "HIGH", l("PYTHON", "CPP17"), l("SYNTAX_ERROR"), "FIX_FIRST_COMPILER_ERROR");
        b(seeds, "FUNCTION_SIGNATURE", "语法与编译", "函数签名/调用不一致",
                "函数定义、调用参数数量或返回值使用方式不一致。", "当前先核对函数定义和调用处。",
                "适合处理自定义函数、递归函数和 C++ main/返回类型问题。",
                l("too few arguments", "too many arguments", "missing return", "argument"),
                l("调用时少传参数", "形参顺序混淆", "递归调用漏参数", "返回值没有使用"),
                l("CE", "COMPILE_ERROR"),
                "先把函数定义和调用处放在一起比较。", "数清参数个数、顺序和类型。",
                "用一个最小调用验证函数是否能独立工作。",
                "函数抽象", "MEDIUM", l("PYTHON", "CPP17"), l("SYNTAX_ERROR"), "FIX_FIRST_COMPILER_ERROR");
        b(seeds, "RUNTIME_STABILITY", "运行时", "运行时稳定性",
                "代码运行中触发越界、空引用、除零或递归过深等异常。", "当前先定位程序在哪类输入下中断。",
                "学生需要学会从异常信息和极小输入倒推状态。",
                l("runtime error", "index out of range", "division by zero", "segmentation fault"),
                l("数组下标未检查", "空容器取值", "递归无出口", "除数来自输入"),
                l("RE", "RUNTIME_ERROR"),
                "先确认程序是算错了，还是运行中断了。", "用最小输入追踪关键变量进入危险操作前的值。",
                "检查访问数组、取模除法、递归调用前是否满足题意条件。",
                "程序稳定性", "HIGH", l("PYTHON", "CPP17"), l("EMPTY_INPUT"), "CHECK_RUNTIME_GUARDS");
        b(seeds, "ARRAY_INDEX_OUT_OF_RANGE", "运行时", "数组/字符串越界",
                "访问了不存在的数组、列表或字符串位置。", "当前先追踪下标范围和容器长度。",
                "常见于循环端点、1/0 编号混用、空容器访问。",
                l("index out of range", "out_of_range", "segmentation fault", "subscript"),
                l("访问 a[n]", "字符串取 s[i+1]", "空列表取首元素", "1-based 编号直接作为 0-based 下标"),
                l("RE", "WA"),
                "先打印或手推访问前的下标和长度。", "确认下标最小值和最大值是否都合法。",
                "用长度为 0、1、2 的输入检查每次访问。",
                "程序稳定性", "HIGH", l("PYTHON", "CPP17"), l("OFF_BY_ONE", "EMPTY_INPUT"), "TRACE_VARIABLES");
        b(seeds, "DIVISION_BY_ZERO", "运行时", "除零/取模零",
                "除数或取模数在某些输入下可能为 0。", "当前先确认除法或取模前除数来源。",
                "学生容易默认输入满足非零，但题面或中间计算未必保证。",
                l("division by zero", "ZeroDivisionError", "floating point exception"),
                l("用输入值作除数", "差值可能为 0", "计数器还没累加就求平均"),
                l("RE"),
                "先找所有 / 和 %。", "追踪除数在最小输入和特殊输入下的值。",
                "在除法前确认题意是否保证非零，或是否需要分支处理。",
                "程序稳定性", "HIGH", l("PYTHON", "CPP17"), l("BOUNDARY_CONDITION"), "CHECK_RUNTIME_GUARDS");
        b(seeds, "RECURSION_DEPTH", "运行时", "递归深度/无限递归",
                "递归没有正确收敛或深度超过语言运行限制。", "当前先检查递归出口和参数是否靠近出口。",
                "适合搜索、递推和树形题的基础排查。",
                l("RecursionError", "stack overflow", "segmentation fault", "maximum recursion depth"),
                l("递归参数不变化", "出口条件写反", "图搜索未标记 visited"),
                l("RE", "TLE"),
                "先画出前 3 次递归调用。", "检查每次调用参数是否更接近终止条件。",
                "补充 visited 或出口条件后，用最小递归样例验证。",
                "递归与搜索", "HIGH", l("PYTHON", "CPP17"), l("RECURSION_EXIT"), "DRAW_RECURSION_TREE");
        b(seeds, "IO_FORMAT", "输入输出", "输入输出格式",
                "读入结构或输出格式与题面要求不一致。", "当前先核对题面要求的输入/输出单位。",
                "适合让学生逐项比较题面格式、实际读取和实际输出。",
                l("wrong output shape", "missing output line", "extra output", "input format"),
                l("只读一组数据", "输出多余调试内容", "换行或空格不一致", "未读取查询次数"),
                l("WA", "VISIBLE_OUTPUT_MISMATCH"),
                "先不要改算法，先核对输入输出格式。", "数一数题面要求读几组、输出几行，和代码实际行为比较。",
                "用一个最小样例手动写出应读内容和应输出内容，再对照代码。",
                "题意读取", "HIGH", l("PYTHON", "CPP17"), l("INPUT_PARSING", "OUTPUT_FORMAT_DETAIL"), "COMPARE_INPUT_SPEC");
        b(seeds, "INPUT_PARSING", "输入输出", "输入读取理解",
                "题面输入结构、多组数据或多次查询的读取方式理解错。", "当前先看代码是否完整消费了题面给出的输入。",
                "常见于多组数据、q 次查询、矩阵/列表长度不匹配等问题。",
                l("candidate input parsing", "remaining input", "fewer output lines"),
                l("没有按查询次数处理", "循环次数和输入组数不一致", "嵌套数据只读一层"),
                l("WA", "VISIBLE_OUTPUT_MISMATCH"),
                "先关注读入次数，不急着改计算逻辑。", "数一数题面有几段输入，代码实际读取了几段。",
                "构造两组以上输入，观察输出次数是否和题面一致。",
                "题意读取", "HIGH", l("PYTHON", "CPP17"), l(), "COMPARE_INPUT_SPEC");
        b(seeds, "MULTI_CASE_INPUT", "输入输出", "多组数据处理",
                "题目包含多组数据，但代码只处理了一组或状态跨组污染。", "当前先确认第一行是否是 T/q 或是否读到 EOF。",
                "学生常把样例中的一组输入当作完整输入格式。",
                l("multiple cases", "query count", "EOF", "fewer output lines"),
                l("只调用一次 solve", "没有循环 T 次", "每组输出格式遗漏", "case 间变量未清空"),
                l("WA", "VISIBLE_OUTPUT_MISMATCH"),
                "先确认题目要求输出几次。", "手推两组输入时代码会读几次、输出几行。",
                "构造两组差异明显的数据，检查第二组是否独立处理。",
                "题意读取", "HIGH", l("PYTHON", "CPP17"), l("INPUT_PARSING", "STATE_RESET"), "COMPARE_INPUT_SPEC");
        b(seeds, "EOF_INPUT_LOOP", "输入输出", "读到文件结束",
                "题目可能没有给出组数，需要读到输入结束。", "当前先看代码是否错误等待更多输入或提前停止。",
                "适合处理 while(cin>>x)、sys.stdin 逐行和不定组数据。",
                l("EOF", "read until end", "blocking input", "fewer output lines"),
                l("固定读一行", "误把第一项当组数", "没有遍历所有输入行"),
                l("WA", "RUNTIME_ERROR"),
                "先确认题目有没有明确组数。", "如果没有组数，检查代码是否遍历了所有输入。",
                "用三行输入验证代码是否产生三次处理结果。",
                "题意读取", "MEDIUM", l("PYTHON", "CPP17"), l("INPUT_PARSING"), "COMPARE_INPUT_SPEC");
        b(seeds, "OUTPUT_FORMAT_DETAIL", "输入输出", "输出格式细节",
                "换行、空格、大小写、多余字符或调试输出影响判题。", "当前先逐字符比较实际输出与期望输出。",
                "适合在算法证据不足时优先排除格式问题。",
                l("whitespace mismatch", "case mismatch", "extra debug output"),
                l("print 调试变量", "末尾多空格", "大小写与题面不一致", "输出顺序不一致"),
                l("WA", "PRESENTATION_ERROR"),
                "先逐字符比较输出，不急着换算法。", "把期望输出和实际输出按行对齐，看多了什么或少了什么。",
                "删除调试输出后，用最小样例复核换行和空格。",
                "题意读取", "MEDIUM", l("PYTHON", "CPP17"), l(), "COMPARE_OUTPUT");
        b(seeds, "DEBUG_OUTPUT_LEFT", "输入输出", "调试输出残留",
                "代码输出了题目没有要求的中间变量或提示文字。", "当前先删去所有调试打印再复核。",
                "学生可能只在本地看懂了输出，但评测要求完全匹配。",
                l("extra output", "debug", "unexpected token", "visible mismatch"),
                l("输出数组内容", "输出提示语", "打印循环变量", "输出日志前缀"),
                l("WA", "PRESENTATION_ERROR"),
                "先看实际输出是否比期望多内容。", "注释掉所有与最终答案无关的输出。",
                "只保留题面要求的结果后重新提交。",
                "题意读取", "MEDIUM", l("PYTHON", "CPP17"), l("OUTPUT_FORMAT_DETAIL"), "COMPARE_OUTPUT");
        b(seeds, "CASE_SENSITIVITY", "输入输出", "大小写要求",
                "输出大小写与题面不一致。", "当前先核对 YES/NO、True/False 等字面值。",
                "适合可见输出只差大小写的情况。",
                l("case mismatch", "YES", "NO", "True", "False"),
                l("输出 Yes 而题面要 YES", "中英文标点混用", "大小写转换误用"),
                l("WA", "PRESENTATION_ERROR"),
                "先核对题面要求的大小写。", "把代码输出和期望输出逐字符对齐。",
                "统一所有分支中的输出字面值。",
                "题意读取", "LOW", l("PYTHON", "CPP17"), l("OUTPUT_FORMAT_DETAIL"), "COMPARE_OUTPUT");
        b(seeds, "OUTPUT_ORDER", "输入输出", "输出顺序",
                "输出元素、行或答案顺序与题目要求不一致。", "当前先确认题目要求原顺序、排序后还是任意顺序。",
                "常见于排序、查询结果、路径输出和多答案输出。",
                l("wrong order", "line order", "sorted output"),
                l("集合遍历顺序不稳定", "排序方向反了", "多组答案输出顺序错"),
                l("WA"),
                "先确认顺序是否是答案的一部分。", "比较期望输出和实际输出每一行的含义。",
                "如果需要排序，写清排序键和升降序。",
                "题意读取", "MEDIUM", l("PYTHON", "CPP17"), l("OUTPUT_FORMAT_DETAIL"), "COMPARE_OUTPUT");

        b(seeds, "VARIABLE_INITIALIZATION", "变量与状态", "变量初始化/状态重置",
                "变量初值或多轮处理之间的状态重置不符合题意。", "当前先检查变量在第一次使用前和每轮循环前的状态。",
                "适合处理累计变量、标志变量、多组数据残留等问题。",
                l("state not reset", "unexpected carry-over", "wrong initial state"),
                l("累加器未清零", "flag 沿用上一轮", "列表复用", "默认值不符合题意"),
                l("WA"), "先看变量第一次参与判断前是什么值。", "追踪一轮结束后，下一轮开始前哪些状态还保留着。",
                "用两组连续数据测试，观察第二组是否受到第一组影响。",
                "状态维护", "MEDIUM", l("PYTHON", "CPP17"), l("INITIAL_STATE", "STATE_RESET"), "TRACE_STATE");
        b(seeds, "INITIAL_STATE", "变量与状态", "初始状态",
                "初始变量、初始集合或初始答案没有覆盖题意的起点。", "当前先核对题目中“开始时”的状态。",
                "常见于最值初始化、DP 起点、空集/单元素起点。",
                l("initial state", "minimum case mismatch"), l("最值初始为 0", "DP 初值遗漏", "起点元素未计入"),
                l("WA"), "先找题目里最开始的状态。", "用最小输入手推初值应该是什么。",
                "检查循环开始前的变量是否已经表达了题意起点。",
                "状态维护", "MEDIUM", l("PYTHON", "CPP17"), l(), "TRACE_STATE");
        b(seeds, "MIN_MAX_INITIALIZATION", "变量与状态", "最值初始化",
                "最大值/最小值初始值不适合题目数据范围。", "当前先检查初值是否可能压过真实答案。",
                "常见于负数最大值、正数最小值和空数据场景。",
                l("minimum", "maximum", "negative numbers", "initial answer"),
                l("最大值初始为 0", "最小值初始过小", "没有用首个元素初始化"),
                l("WA"), "先看题目数据是否可能为负或很大。", "用一个全负或单元素输入手推答案。",
                "让初值来自题意边界或首个有效元素。",
                "状态维护", "MEDIUM", l("PYTHON", "CPP17"), l("INITIAL_STATE"), "TRACE_STATE");
        b(seeds, "FLAG_UPDATE", "变量与状态", "标志变量更新",
                "flag 的初值、更新位置或判断时机不符合题意。", "当前先追踪 flag 什么时候变、什么时候被使用。",
                "适合质数判断、是否存在、是否满足条件等题。",
                l("flag", "boolean", "exists", "prime"),
                l("找到反例后没有 break", "每轮重置 flag", "判断位置在循环内部"),
                l("WA"), "先给 flag 写出含义。", "追踪它在循环前、循环中、循环后的值。",
                "用一个满足和一个不满足的样例检查 flag 变化。",
                "状态维护", "MEDIUM", l("PYTHON", "CPP17"), l("VARIABLE_INITIALIZATION"), "TRACE_STATE");
        b(seeds, "ACCUMULATOR_UPDATE", "变量与状态", "累加/计数更新",
                "累计值、计数器或答案变量更新次数不符合题意。", "当前先确认每个元素是否应该贡献一次。",
                "常见于求和、计数、统计频次和前缀累计。",
                l("counter", "sum", "accumulator", "frequency"),
                l("漏加首项", "重复累加", "满足条件才该累加却无条件累加", "计数器位置不对"),
                l("WA"), "先圈出答案变量在哪里变化。", "用 3 个元素手推每一步累计值。",
                "确认每次更新都对应题意中的一次贡献。",
                "状态维护", "MEDIUM", l("PYTHON", "CPP17"), l("VARIABLE_INITIALIZATION"), "TRACE_STATE");
        b(seeds, "STATE_RESET", "变量与状态", "状态重置",
                "多组数据、多轮循环或重复尝试之间状态没有清空。", "当前先检查每轮开始前状态是否干净。",
                "适合处理多 case 累计、缓存残留、数组复用等问题。",
                l("state reset", "multi-case carry-over"), l("全局数组未重置", "容器未 clear", "计数器跨 case 累加"),
                l("WA"), "先看第二组数据是否被第一组影响。", "追踪每轮开始前所有关键状态。",
                "构造两组差异很大的输入，检查后一组结果是否异常。",
                "状态维护", "MEDIUM", l("PYTHON", "CPP17"), l(), "TRACE_STATE");
        b(seeds, "MUTABLE_ALIASING", "变量与状态", "可变对象复用/别名",
                "多个状态意外指向同一个列表、数组或对象。", "当前先检查复制和初始化方式。",
                "Python 二维数组、路径记录、DP 表初始化中很常见。",
                l("alias", "same list", "mutable", "copy"),
                l("[[0]*m]*n", "路径列表直接 append 引用", "浅拷贝导致多行一起变"),
                l("WA"), "先改动一个位置，观察其他位置是否也跟着变。", "检查列表/数组初始化是否复用了同一对象。",
                "需要独立状态时使用逐行创建或显式拷贝。",
                "状态维护", "MEDIUM", l("PYTHON"), l("STATE_RESET"), "TRACE_STATE");

        b(seeds, "CONDITION_BRANCH", "条件分支", "条件分支覆盖",
                "判断条件、分支顺序或互斥关系没有覆盖所有情况。", "当前先列出题目要求区分的情况。",
                "学生需要把自然语言条件转成完整的分支表。",
                l("branch condition", "missing branch", "wrong branch order"),
                l("if/else 顺序错误", "边界值落入错误分支", "条件有重叠或遗漏"),
                l("WA"), "先列题目中所有情况，不急着改条件。", "给每个分支写一个能进入它的最小例子。",
                "检查边界值是否进入了你预期的分支。",
                "条件分支推理", "MEDIUM", l("PYTHON", "CPP17"), l(), "CHECK_BRANCH_COVERAGE");
        b(seeds, "OVERLAPPING_CONDITIONS", "条件分支", "重叠条件优先级",
                "多个条件同时成立时，代码先进入了较宽泛的分支。", "当前先找是否存在包含关系或重叠情况。",
                "典型于 FizzBuzz、区间分类、等级判断和特殊值优先级。",
                l("wrong branch order", "overlap", "FizzBuzz", "range category"),
                l("先判断一般条件再判断特殊条件", "elif 截断了后续分支", "区间端点重叠"),
                l("WA"), "先找一个同时满足两个条件的输入。", "看这个输入实际进入哪个分支。",
                "把分支按特殊情况和一般情况重新梳理。",
                "条件分支推理", "MEDIUM", l("PYTHON", "CPP17"), l("CONDITION_BRANCH"), "CHECK_BRANCH_COVERAGE");
        b(seeds, "MISSING_ELSE_CASE", "条件分支", "遗漏兜底分支",
                "代码没有覆盖不满足任何显式条件的情况。", "当前先列出所有没有命中的输入类别。",
                "常见于分类、查找失败、无解输出和默认值。",
                l("missing else", "default case", "no solution"),
                l("没有处理不满足条件", "无解时不输出", "默认答案沿用旧值"),
                l("WA"), "先找一个不满足任何条件的输入。", "检查代码对它有没有明确输出或状态。",
                "给每类情况都写一个最小测试。",
                "条件分支推理", "MEDIUM", l("PYTHON", "CPP17"), l("CONDITION_BRANCH"), "CHECK_BRANCH_COVERAGE");
        b(seeds, "COMPARISON_OPERATOR", "条件分支", "比较符号错误",
                "大于/大于等于、小于/小于等于、等于/不等于使用错误。", "当前先把边界值代入每个条件。",
                "适合处理区间、阈值、排名和二分条件。",
                l("comparison", ">=", "<=", "threshold", "boundary"),
                l("漏等号", "等号方向反了", "把赋值和比较混淆", "闭区间写成开区间"),
                l("WA"), "先把等于边界的值代进去。", "检查题面是包含还是不包含这个端点。",
                "用端点前后各一个值测试分支。",
                "条件分支推理", "MEDIUM", l("PYTHON", "CPP17"), l("BOUNDARY_CONDITION"), "CHECK_BRANCH_COVERAGE");
        b(seeds, "BOOLEAN_LOGIC", "条件分支", "布尔逻辑组合错误",
                "and/or/not 的组合没有表达题意。", "当前先用真值表或反例检查条件。",
                "常见于多个限制同时满足、排除条件和区间外判断。",
                l("and", "or", "not", "boolean"),
                l("应该同时满足却用了 or", "区间外条件写反", "not 作用范围不清"),
                l("WA"), "先把条件翻译成自然语言。", "列出一个应该为真和一个应该为假的输入。",
                "必要时拆成多个中间布尔变量复核。",
                "条件分支推理", "MEDIUM", l("PYTHON", "CPP17"), l("CONDITION_BRANCH"), "CHECK_BRANCH_COVERAGE");

        b(seeds, "LOOP_BOUNDARY", "循环与边界", "循环边界",
                "循环起点、终点、步长或退出条件少处理/多处理元素。", "当前先追踪循环变量经过了哪些值。",
                "常见于 range 终点、数组长度、二分边界和 while 退出条件。",
                l("loop boundary", "off by one", "missed element"),
                l("少处理第一个/最后一个元素", "while 条件过早结束", "二分区间不收缩"),
                l("WA", "TLE"), "先看循环到底跑了哪些下标。", "用 1 个和 2 个元素的样例列出循环变量表。",
                "核对每次循环后区间或下标是否更接近结束。",
                "循环与边界", "HIGH", l("PYTHON", "CPP17"), l("OFF_BY_ONE"), "TRACE_VARIABLES");
        b(seeds, "OFF_BY_ONE", "循环与边界", "差一位错误",
                "索引、计数或区间端点多算/少算一个位置。", "当前先用极小规模检查端点是否被处理。",
                "适合处理数组、字符串、排名、天数、闭区间/开区间。",
                l("off by one", "index one-based zero-based"),
                l("0/1 编号混用", "range 右端点误解", "长度和最后下标混淆"),
                l("WA", "RE"), "先怀疑端点，不要先怀疑整体算法。", "把编号体系写清楚：题面从 1 还是从 0 开始。",
                "用长度为 1、2 的输入手推每个下标是否被处理一次。",
                "循环与边界", "HIGH", l("PYTHON", "CPP17"), l(), "TRACE_VARIABLES");
        b(seeds, "WHILE_TERMINATION", "循环与边界", "while 退出条件",
                "while 循环条件或循环内更新没有保证终止。", "当前先确认每轮循环后是否更接近退出。",
                "适合二分、模拟、输入读取和指针推进。",
                l("while", "infinite loop", "termination", "not progress"),
                l("循环变量没更新", "条件永远成立", "continue 前漏更新", "二分边界不收缩"),
                l("TLE", "WA"), "先看循环变量是否变化。", "列出前 3 轮循环条件和变量值。",
                "确保每条分支都会推进到退出条件。",
                "循环与边界", "HIGH", l("PYTHON", "CPP17"), l("LOOP_BOUNDARY"), "TRACE_VARIABLES");
        b(seeds, "NESTED_LOOP_SCOPE", "循环与边界", "嵌套循环作用域",
                "循环内外变量更新或输出位置放错层级。", "当前先确认某条语句应该执行几次。",
                "常见于每行输出、二维数组统计、找到答案后退出。",
                l("nested loop", "indent", "scope", "output count"),
                l("输出放在内层循环", "计数器放在外层", "break 只跳出一层"),
                l("WA"), "先数这条语句应该执行几次。", "检查它现在位于哪一层循环。",
                "用 2x2 或两组数据验证执行次数。",
                "循环与边界", "MEDIUM", l("PYTHON", "CPP17"), l("LOOP_BOUNDARY"), "TRACE_VARIABLES");
        b(seeds, "TWO_POINTER_PROGRESS", "循环与边界", "双指针推进",
                "双指针或左右端点没有按条件正确推进。", "当前先检查每轮至少一个指针是否移动。",
                "适合有序数组、滑动窗口、回文、区间收缩。",
                l("two pointer", "left right", "sliding window", "pointer progress"),
                l("只移动一侧", "满足/不满足条件时推进方向反了", "窗口统计与移动不同步"),
                l("WA", "TLE"), "先写清左右指针各代表什么。", "手推前 3 轮指针位置和窗口内容。",
                "检查每个分支后窗口状态是否仍符合定义。",
                "循环与边界", "MEDIUM", l("PYTHON", "CPP17"), l("LOOP_BOUNDARY"), "TRACE_VARIABLES");

        b(seeds, "BOUNDARY_CONDITION", "边界条件", "边界条件",
                "极小、极大、空值、重复值或特殊输入没有被正确处理。", "当前先构造最小/最大/特殊输入。",
                "学生需要形成可复用的边界检查清单。",
                l("boundary", "minimum input", "maximum input", "duplicate values"),
                l("空列表未处理", "单元素逻辑不同", "重复元素判断失效", "最大值溢出"),
                l("WA", "RE", "TLE"), "先找最小、最大和特殊重复情况。", "挑一个最小边界样例，手推代码每一步。",
                "把样例、最小值、最大值、重复值各测一次。",
                "边界条件意识", "MEDIUM", l("PYTHON", "CPP17"), l("EMPTY_INPUT", "MAX_BOUNDARY", "DUPLICATE_CASE"), "ASK_MIN_CASE");
        b(seeds, "EMPTY_INPUT", "边界条件", "极小输入",
                "空、单元素、最小规模输入没有被覆盖。", "当前先只用题目允许的最小输入测试。",
                "适合排查初始化、循环是否进入、数组访问等问题。",
                l("empty input", "single element", "minimum input"),
                l("默认至少两个元素", "空容器取值", "循环完全不进入"),
                l("WA", "RE"), "先用题目允许的最小输入。", "追踪每个函数在最小输入下收到什么、返回什么。",
                "检查没有进入循环时答案是否仍符合题意。",
                "边界条件意识", "MEDIUM", l("PYTHON", "CPP17"), l(), "ASK_MIN_CASE");
        b(seeds, "SINGLE_ELEMENT_CASE", "边界条件", "单元素场景",
                "只有一个元素或一次操作时逻辑与一般情况不同。", "当前先用规模 1 手推代码。",
                "常见于数组统计、字符串处理、最值和相邻关系。",
                l("single element", "n=1", "one item"), l("访问相邻元素", "循环从第二个开始", "默认存在两个元素"),
                l("WA", "RE"), "先把 n=1 单独手推一遍。", "检查是否访问了不存在的前后元素。",
                "确认答案初值是否已经覆盖唯一元素。",
                "边界条件意识", "MEDIUM", l("PYTHON", "CPP17"), l("EMPTY_INPUT"), "ASK_MIN_CASE");
        b(seeds, "ZERO_ONE_CASE", "边界条件", "0/1 特殊值",
                "0、1 或空结果在题意中有特殊定义。", "当前先找题目中对 0/1 的定义。",
                "典型于阶乘、质数、组合计数、空路径和乘法单位元。",
                l("zero", "one", "0!", "prime 1", "identity"),
                l("把 1 当质数", "0! 漏处理", "空乘积初值错误"),
                l("WA"), "先查题目如何定义 0 和 1。", "用 0、1、2 分别测试。",
                "把特殊定义写成明确分支或初值。",
                "边界条件意识", "MEDIUM", l("PYTHON", "CPP17"), l("EMPTY_INPUT", "INITIAL_STATE"), "ASK_MIN_CASE");
        b(seeds, "MAX_BOUNDARY", "边界条件", "最大规模边界",
                "最大输入规模下出现超时、溢出或内存压力。", "当前先估算最大规模操作次数和数据范围。",
                "适合作为从样例正确走向规模正确的关键训练。",
                l("large constraints", "max boundary", "timeout", "overflow"),
                l("嵌套枚举全范围", "int 溢出", "保存过多中间状态"),
                l("TLE", "MLE", "WA"), "先看最大 n 可能有多大。", "估算核心循环在最大输入下会执行多少次。",
                "把变量最大值范围写出来，检查类型和空间是否承受得住。",
                "算法复杂度", "HIGH", l("PYTHON", "CPP17"), l(), "COUNT_COMPLEXITY");
        b(seeds, "DUPLICATE_CASE", "边界条件", "重复元素场景",
                "重复值、重复状态或相等边界破坏当前判断。", "当前先构造包含重复元素的最小反例。",
                "常见于排序、去重、计数、贪心和二分边界。",
                l("duplicate values", "equal boundary"), l("相等时分支遗漏", "set 去重破坏计数", "重复元素顺序影响结果"),
                l("WA"), "先测一组有重复值的输入。", "观察相等时走的是哪个分支。",
                "构造所有元素相同或只差一个元素的反例。",
                "迁移泛化", "MEDIUM", l("PYTHON", "CPP17"), l(), "ASK_MIN_CASE");
        b(seeds, "NEGATIVE_NUMBER_CASE", "边界条件", "负数场景",
                "题目允许负数时，初始化、取模、比较或排序逻辑没有覆盖。", "当前先用负数样例检查变量变化。",
                "常见于最大子段、求和、差值、排序和整除。",
                l("negative", "minus", "signed"), l("最大值初始为 0", "绝对值遗漏", "负数取模理解错误"),
                l("WA"), "先确认题目是否允许负数。", "构造全负、一正一负、含 0 的输入。",
                "检查初值、比较和取模是否仍符合题意。",
                "边界条件意识", "MEDIUM", l("PYTHON", "CPP17"), l("MIN_MAX_INITIALIZATION"), "ASK_MIN_CASE");

        b(seeds, "DATA_STRUCTURE_CHOICE", "数据结构", "数据结构选择",
                "当前容器或组织方式不适合题目操作和规模。", "当前先列出题目需要支持的核心操作。",
                "适合引导学生比较查找、插入、删除、区间查询等成本。",
                l("data structure", "lookup cost", "memory layout"), l("频繁线性查找", "需要按序却用无序结构", "重复扫描列表"),
                l("TLE", "MLE"), "先列题目真正频繁的操作。", "估算当前容器完成这些操作的代价。",
                "比较是否需要支持快速查找、排序或区间信息。",
                "数据结构", "MEDIUM", l("PYTHON", "CPP17"), l(), "COMPARE_STRUCTURES");
        b(seeds, "HASH_LOOKUP_MISSING", "数据结构", "哈希查找缺失",
                "需要快速存在性或计数查询时仍在反复线性扫描。", "当前先找是否存在大量 membership 查询。",
                "常见于两数和、去重、频次统计和映射关系。",
                l("lookup", "contains", "frequency", "hash"), l("每次用循环查找", "没有维护计数字典", "重复遍历历史元素"),
                l("TLE"), "先数一数查找操作发生多少次。", "比较线性查找和哈希查找的代价。",
                "把频繁查询的信息维护成 set/map 后再估算复杂度。",
                "数据结构", "MEDIUM", l("PYTHON", "CPP17"), l("TIME_COMPLEXITY"), "COMPARE_STRUCTURES");
        b(seeds, "SORTING_ORDER_KEY", "数据结构", "排序键/排序方向",
                "排序依据或升降序没有匹配题目要求。", "当前先明确每个元素应按什么比较。",
                "适合区间排序、结构体排序、按第二关键字排序等场景。",
                l("sort", "order", "key", "comparator"), l("升降序反了", "只按第一关键字", "相等时第二关键字漏掉"),
                l("WA"), "先写清排序后相邻元素应满足什么关系。", "用三个元素手排期望顺序。",
                "检查代码排序 key 是否表达了这个顺序。",
                "数据结构", "MEDIUM", l("PYTHON", "CPP17"), l("OUTPUT_ORDER"), "COMPARE_STRUCTURES");
        b(seeds, "QUEUE_STACK_MISUSE", "数据结构", "队列/栈使用混淆",
                "需要先进先出或后进先出时，容器操作顺序选错。", "当前先确认状态处理顺序。",
                "典型于 BFS/DFS、括号匹配、单调栈和模拟队列。",
                l("queue", "stack", "BFS", "DFS"), l("BFS 用栈", "栈顶/队首取错", "入队时机不对"),
                l("WA", "TLE"), "先说清下一步应该处理最早加入还是最后加入。", "手推前三个状态的进出顺序。",
                "用最小图或括号串验证容器顺序。",
                "数据结构", "MEDIUM", l("PYTHON", "CPP17"), l("ALGORITHM_STRATEGY"), "COMPARE_STRUCTURES");

        b(seeds, "TIME_COMPLEXITY", "复杂度", "时间复杂度",
                "算法执行次数随输入规模增长过快。", "当前先估算最大输入下核心循环次数。",
                "这是信息竞赛中从会写到能过大数据的关键能力。",
                l("time limit", "large n", "nested loop", "operation count"),
                l("双重/三重循环枚举", "重复排序", "每次查询全量扫描"),
                l("TLE"), "先估算次数，不急着找技巧。", "把核心循环次数写成 n、m 或 q 的表达。",
                "代入最大数据范围，判断是否明显超过可接受数量级。",
                "算法复杂度", "HIGH", l("PYTHON", "CPP17"), l("OVER_SIMULATION", "BRUTE_FORCE_LIMIT"), "COUNT_COMPLEXITY");
        b(seeds, "OVER_SIMULATION", "复杂度", "过度模拟",
                "代码逐步模拟过多细节，导致规模上不可承受。", "当前先看是否每一步都必须真的执行。",
                "适合引导学生从模拟走向规律、预处理或状态压缩。",
                l("step simulation", "large range", "repeated process"),
                l("按时间一秒一秒模拟", "按区间每个点更新", "重复做相同计算"),
                l("TLE"), "先问：每一步都必须模拟吗？", "找出重复发生、结果可合并的部分。",
                "用小规模手推，观察是否存在可直接维护的状态或规律。",
                "算法设计", "HIGH", l("PYTHON", "CPP17"), l(), "COUNT_COMPLEXITY");
        b(seeds, "BRUTE_FORCE_LIMIT", "复杂度", "暴力规模瓶颈",
                "暴力枚举在最大规模下无法通过。", "当前先确认暴力枚举的对象和层数。",
                "适合训练学生从枚举范围、剪枝和等价状态入手优化。",
                l("brute force", "enumeration limit", "timeout"), l("枚举所有对/三元组", "每次重新扫描", "无剪枝搜索"),
                l("TLE"), "先数枚举了多少种可能。", "估算最坏情况下枚举总量。",
                "找一找哪些枚举结果可以提前排除或复用。",
                "算法复杂度", "HIGH", l("PYTHON", "CPP17"), l(), "COUNT_COMPLEXITY");
        b(seeds, "REPEATED_WORK", "复杂度", "重复计算",
                "同一结果被反复计算，没有缓存、预处理或增量维护。", "当前先找相同输入对应的重复操作。",
                "常见于多次查询、递归、区间统计和字符串匹配。",
                l("repeated work", "memo", "precompute", "query"), l("每个查询从头算", "递归重复子问题", "重复统计同一区间"),
                l("TLE"), "先找有没有一段计算被多次执行。", "比较预处理一次和每次重新计算的次数。",
                "把可复用结果保存或改成增量更新。",
                "算法复杂度", "HIGH", l("PYTHON", "CPP17"), l("TIME_COMPLEXITY"), "COUNT_COMPLEXITY");
        b(seeds, "SPACE_COMPLEXITY", "复杂度", "空间复杂度",
                "保存的数据或中间状态过多，超过内存或不必要。", "当前先估算最大输入下会开多少空间。",
                "适合处理大数组、二维 DP、缓存全量结果等问题。",
                l("memory limit", "large array", "space complexity"),
                l("二维数组过大", "保存所有历史状态", "复制大容器"),
                l("MLE", "RE"), "先估算开了多少个元素。", "把数组维度和最大范围相乘。",
                "检查是否只需要上一层、局部窗口或统计量。",
                "空间管理", "MEDIUM", l("PYTHON", "CPP17"), l(), "COMPARE_STRUCTURES");
        b(seeds, "INTEGER_OVERFLOW", "复杂度", "整数溢出",
                "中间结果超过变量类型范围。", "当前先估算最大可能值。",
                "C++ int、乘法、平方、组合计数和前缀和中很常见。",
                l("overflow", "large value", "int", "long long"), l("int 存乘积", "比较前先溢出", "前缀和范围超过 32 位"),
                l("WA", "RE"), "先估算最大结果数量级。", "检查每一步中间值是否也可能超范围。",
                "在 C++ 中将相关变量和乘法提升到 long long。",
                "数值范围", "HIGH", l("CPP17"), l("MAX_BOUNDARY"), "COUNT_COMPLEXITY");
        b(seeds, "FLOAT_PRECISION", "复杂度", "浮点精度",
                "浮点比较、除法或输出精度没有满足题目要求。", "当前先核对误差范围和输出格式。",
                "适合几何、平均值、概率和实数二分。",
                l("precision", "double", "float", "epsilon"), l("直接比较浮点相等", "输出小数位不足", "整数除法误用"),
                l("WA"), "先看题面允许误差。", "检查是否用了整数除法或直接 == 比较。",
                "用 eps 比较并按题面要求格式化输出。",
                "数值范围", "MEDIUM", l("PYTHON", "CPP17"), l("OUTPUT_FORMAT_DETAIL"), "COMPARE_OUTPUT");

        b(seeds, "ALGORITHM_STRATEGY", "算法策略", "算法策略",
                "当前思路没有匹配题目结构或规模。", "当前先判断题目属于哪类模型。",
                "适合在基础错误排除后，引导学生比较策略而非直接给解法。",
                l("algorithm strategy", "wrong model", "hidden failure"),
                l("样例能过但一般情况不成立", "局部规则代替全局目标", "没有利用单调性/结构"),
                l("WA", "TLE"), "先描述你现在的思路依赖什么假设。", "找一个不满足这个假设的小反例。",
                "比较题目目标更像排序、搜索、DP、贪心还是数学规律。",
                "算法设计", "MEDIUM", l("PYTHON", "CPP17"), l("GREEDY_ASSUMPTION", "DP_STATE_DESIGN"), "CHECK_INVARIANT");
        b(seeds, "GREEDY_ASSUMPTION", "算法策略", "贪心依据不足",
                "当前贪心选择缺少可验证依据或存在反例。", "当前先验证每一步局部选择是否不影响全局最优。",
                "适合训练交换论证、反例构造和不变量意识。",
                l("greedy", "local choice", "counterexample"),
                l("按当前最大/最小直接选", "排序后单向决策", "没有考虑未来代价"),
                l("WA"), "先说清每一步为什么这样选。", "构造一个局部看起来好但可能全局差的例子。",
                "尝试说明交换后不会更差，或找出反例。",
                "算法设计", "MEDIUM", l("PYTHON", "CPP17"), l(), "CHECK_INVARIANT");
        b(seeds, "MONOTONICITY_MISSING", "算法策略", "单调性未识别",
                "题目存在可行性或答案空间单调，但代码仍线性或暴力尝试。", "当前先判断答案变大/变小时性质是否保持。",
                "适合二分答案、边界查找和有序结构。",
                l("monotonic", "binary search", "ordered", "feasible"), l("逐个答案尝试", "没有检查函数", "边界含义不清"),
                l("TLE", "WA"), "先问：某个答案可行时，更大或更小是否也可行？", "写出检查一个候选答案的逻辑。",
                "明确左右边界分别表示什么。",
                "算法设计", "MEDIUM", l("PYTHON", "CPP17"), l("LOOP_BOUNDARY"), "CHECK_INVARIANT");
        b(seeds, "PREFIX_SUM_MISSING", "算法策略", "前缀和/差分意识不足",
                "区间求和、区间更新或频繁统计没有利用可预处理结构。", "当前先看是否有大量区间查询或区间修改。",
                "适合数组统计、二维表、差分更新。",
                l("prefix sum", "range query", "difference array"), l("每次查询遍历区间", "区间更新逐点修改", "前缀边界少 1"),
                l("TLE", "WA"), "先列出查询或更新次数。", "思考能否用前缀表示任意区间。",
                "用小数组手推前缀数组和区间公式。",
                "算法设计", "MEDIUM", l("PYTHON", "CPP17"), l("TIME_COMPLEXITY", "OFF_BY_ONE"), "COUNT_COMPLEXITY");
        b(seeds, "GRAPH_VISITED", "图论与搜索", "访问标记",
                "图或网格搜索没有正确标记已访问状态。", "当前先确认节点是否会重复进入队列/递归。",
                "常见于 BFS/DFS、连通块、最短路预处理。",
                l("visited", "BFS", "DFS", "cycle"), l("入队后未标记", "出队才标记导致重复", "不同状态共用一个 visited"),
                l("TLE", "WA"), "先看一个节点会不会被访问多次。", "确认 visited 的维度是否包含完整状态。",
                "在加入搜索结构时就标记，并用小环图验证。",
                "图论搜索", "HIGH", l("PYTHON", "CPP17"), l("RECURSION_EXIT"), "DRAW_RECURSION_TREE");
        b(seeds, "GRAPH_DIRECTION", "图论与搜索", "图方向/建边错误",
                "有向、无向、权值或边的含义建错。", "当前先核对题面每条关系的方向。",
                "适合图论、依赖关系、网格移动和传递关系。",
                l("edge", "directed", "undirected", "weight"), l("无向图只加一条边", "有向边反了", "权值读错列"),
                l("WA"), "先把一条样例边画出来。", "确认从哪个点能走到哪个点。",
                "用两个点一条边的最小图验证建图。",
                "图论建模", "HIGH", l("PYTHON", "CPP17"), l("INPUT_PARSING"), "CHECK_INVARIANT");
        b(seeds, "SHORTEST_PATH_RELAXATION", "图论与搜索", "最短路松弛",
                "最短路距离初始化、松弛条件或优先队列使用不正确。", "当前先检查距离含义和更新条件。",
                "适合 Dijkstra、Bellman-Ford、BFS 最短步数。",
                l("shortest path", "distance", "relax", "priority queue"), l("初始距离为 0", "松弛方向反了", "优先队列旧状态未跳过"),
                l("WA", "TLE"), "先定义 dist[x] 的含义。", "检查起点、不可达值和每次松弛条件。",
                "用三点图手推一次更新过程。",
                "图论建模", "HIGH", l("PYTHON", "CPP17"), l("INITIAL_STATE", "DATA_STRUCTURE_CHOICE"), "DEFINE_STATE");
        b(seeds, "RECURSION_EXIT", "递归与搜索", "递归出口",
                "递归终止条件或回溯过程存在问题。", "当前先画出最小递归树。",
                "适合 DFS、回溯、分治和递归 DP。",
                l("recursion exit", "base case", "backtracking"),
                l("缺少出口", "出口顺序在递归后", "回溯没有撤销选择"),
                l("RE", "TLE", "WA"), "先找递归什么时候应该停止。", "用最小输入画出前几层调用。",
                "检查每条路径是否都会到达出口并恢复现场。",
                "递归与搜索", "HIGH", l("PYTHON", "CPP17"), l(), "DRAW_RECURSION_TREE");
        b(seeds, "SEARCH_STATE_DUPLICATE", "递归与搜索", "搜索状态重复",
                "不同路径反复搜索同一状态，导致超时或重复计数。", "当前先定义一个状态由哪些变量决定。",
                "常见于记忆化、BFS 状态压缩和回溯去重。",
                l("duplicate state", "memo", "visited", "state"), l("没有记忆化", "visited 维度不足", "重复排列未去重"),
                l("TLE", "WA"), "先写清状态包含哪些变量。", "判断两次搜索是否其实等价。",
                "给等价状态加记忆或去重规则。",
                "递归与搜索", "HIGH", l("PYTHON", "CPP17"), l("GRAPH_VISITED"), "DEFINE_STATE");
        b(seeds, "PRUNING_UNSAFE", "递归与搜索", "剪枝不安全",
                "剪枝条件删除了可能产生正确答案的分支。", "当前先证明被剪掉的分支一定无效。",
                "适合搜索优化后 WA 的情况。",
                l("pruning", "branch and bound", "search"), l("按当前局部值过早停止", "排序后剪枝条件不成立", "界限估计过强"),
                l("WA"), "先找一个被剪掉的小分支。", "判断它是否真的不可能更优。",
                "只保留能用题意或上界证明安全的剪枝。",
                "递归与搜索", "MEDIUM", l("PYTHON", "CPP17"), l("ALGORITHM_STRATEGY"), "CHECK_INVARIANT");
        b(seeds, "STATE_TRANSITION", "状态建模", "状态转移",
                "状态更新关系不完整、顺序错误或覆盖了仍需使用的旧状态。", "当前先定义状态含义再看转移。",
                "适合动态规划、模拟、递推类任务。",
                l("state transition", "recurrence", "update order"), l("转移少一种来源", "先更新导致旧值丢失", "答案位置取错"),
                l("WA"), "先用一句话定义状态含义。", "列出状态可以从哪些前一步来。",
                "检查初值、转移顺序和最终答案是否一致。",
                "状态建模", "HIGH", l("PYTHON", "CPP17"), l("DP_STATE_DESIGN"), "DEFINE_STATE");
        b(seeds, "DP_STATE_DESIGN", "状态建模", "DP 状态定义不清",
                "动态规划状态没有保存题目所需的完整信息。", "当前先确认状态维度是否足够区分未来。",
                "常见于背包、序列 DP、区间 DP 和计数 DP。",
                l("dp state", "recurrence", "dimension"), l("只存当前位置但还需要上一次选择", "状态少了容量/次数/限制"),
                l("WA"), "先不要写公式，先解释 dp 的含义。", "判断未来决策还需要哪些信息。",
                "补全状态维度后再写初值和转移。",
                "状态建模", "HIGH", l("PYTHON", "CPP17"), l(), "DEFINE_STATE");
        b(seeds, "DP_INITIALIZATION", "状态建模", "DP 初值",
                "DP 初始状态或不可达状态设置错误。", "当前先写出最小规模时每个状态的值。",
                "适合背包、路径、计数和最值 DP。",
                l("dp init", "base case", "infinity"), l("不可达状态设为 0", "起点没有初始化", "空选择价值写错"),
                l("WA"), "先手推最小规模的 dp 表。", "区分可达、不可达和初始答案。",
                "用只有一个元素或容量为 0 的样例验证初值。",
                "状态建模", "HIGH", l("PYTHON", "CPP17"), l("INITIAL_STATE"), "DEFINE_STATE");
        b(seeds, "DP_UPDATE_ORDER", "状态建模", "DP 更新顺序",
                "一维优化或原地更新时遍历方向错误。", "当前先确认当前轮能否使用本轮新值。",
                "典型于 0/1 背包、完全背包和滚动数组。",
                l("dp order", "knapsack", "rolling array"), l("0/1 背包正序更新", "完全背包倒序更新", "滚动数组覆盖旧状态"),
                l("WA"), "先判断是否允许重复使用当前物品。", "检查循环方向是否保留了需要的旧值。",
                "用一个物品能否被重复选择的小例子验证。",
                "状态建模", "HIGH", l("PYTHON", "CPP17"), l("DP_STATE_DESIGN"), "DEFINE_STATE");
        b(seeds, "IN_PLACE_STATE_PROGRESS", "状态建模", "原地状态推进",
                "原地修改过程中，当前位置的新值或新状态没有继续处理到稳定。", "当前先跟踪一次交换或更新后状态是否满足不变量。",
                "适合数组原地交换、模拟消除、指针压缩。",
                l("in-place", "swap", "state progress"), l("交换后直接跳过当前位置", "删除元素后下标继续增加", "新状态未复查"),
                l("WA"), "先看修改当前位置后，它是否已经稳定。", "如果当前位置变成了新元素，是否需要重新检查。",
                "用连续需要处理的元素构造反例。",
                "状态建模", "MEDIUM", l("PYTHON", "CPP17"), l(), "DEFINE_STATE");
        b(seeds, "MODULAR_ARITHMETIC", "数学规律", "取模/整除关系",
                "取模、整除、余数或周期关系理解不完整。", "当前先列出余数分类。",
                "常见于倍数判断、周期模拟、计数和同余。",
                l("mod", "remainder", "divisible", "period"), l("负数取模", "整除条件写反", "周期起点偏移"),
                l("WA"), "先列出所有可能余数。", "用边界余数和特殊值测试条件。",
                "把周期起点和周期长度分开处理。",
                "数学抽象", "MEDIUM", l("PYTHON", "CPP17"), l("CONDITION_BRANCH"), "ASK_MIN_CASE");
        b(seeds, "COMBINATORICS_COUNT", "数学规律", "计数重复/遗漏",
                "组合、排列或方案数统计重复计算或漏算。", "当前先定义每个方案被数了几次。",
                "适合排列组合、路径计数、子集枚举。",
                l("counting", "combination", "permutation", "duplicate"), l("顺序不同重复计数", "漏掉空集/全集", "乘法原理条件不独立"),
                l("WA"), "先给一个小规模列出所有方案。", "标出代码会数到哪些方案、数几次。",
                "明确是否区分顺序、是否允许重复选择。",
                "数学抽象", "MEDIUM", l("PYTHON", "CPP17"), l("DUPLICATE_CASE"), "CHECK_INVARIANT");
        b(seeds, "SAMPLE_ONLY", "泛化", "只通过样例",
                "代码可能只覆盖了样例，没有泛化到隐藏场景。", "当前先构造一个不同于样例的最小反例。",
                "适合样例通过但隐藏失败的情况。",
                l("sample only", "hidden failure", "overfit"), l("硬编码样例", "只处理常见路径", "默认输入形态和样例一样"),
                l("WA"), "先不要再看样例。", "改一个样例中没有覆盖的条件。",
                "构造一个更小但不同结构的输入验证思路。",
                "迁移泛化", "MEDIUM", l("PYTHON", "CPP17"), l("SAMPLE_OVERFIT"), "BUILD_COUNTEREXAMPLE");
        b(seeds, "SAMPLE_OVERFIT", "泛化", "样例过拟合",
                "代码针对样例现象写了特殊逻辑，无法覆盖一般输入。", "当前先检查是否存在硬编码或样例模式假设。",
                "适合输出固定答案、固定长度、固定排序模式等情况。",
                l("hardcoded", "sample overfit", "fixed answer"), l("直接判断样例值", "固定循环次数", "只处理样例长度"),
                l("WA"), "先找有没有和样例数字绑定的代码。", "换一个同类输入看代码是否仍合理。",
                "把样例现象替换成题目的一般条件。",
                "迁移泛化", "MEDIUM", l("PYTHON", "CPP17"), l(), "BUILD_COUNTEREXAMPLE");
        b(seeds, "PARTIAL_FIX_REGRESSION", "学习轨迹", "局部修复回退",
                "一次局部修改修好旧问题但引入新问题。", "当前先比较两次提交的首个失败点变化。",
                "适合结合学习轨迹判断学生是否在有效推进。",
                l("regression", "submission diff", "previous attempt"), l("只改一处但 verdict 变差", "新提交首个失败点前移", "旧样例重新失败"),
                l("WA", "RE", "CE"), "先比较这次和上次失败点有什么变化。", "找出新问题是修复副作用还是旧问题暴露。",
                "保留能通过的最小修改，再逐步验证每个新增改动。",
                "问题定位", "MEDIUM", l("PYTHON", "CPP17"), l(), "COMPARE_SUBMISSIONS");
        b(seeds, "CODE_READABILITY", "代码表达", "代码可读性",
                "代码结构和命名让排查成本变高。", "当前先区分输入、计算、输出和调试代码。",
                "适合作为次要提醒或通过后的复盘建议。",
                l("readability", "debug branch", "unclear helper"), l("变量名难懂", "逻辑混在一起", "临时分支残留"),
                l("AC", "WA"), "先把主要逻辑和调试代码区分开。", "给关键变量换成能表达含义的名字。",
                "把输入、计算、输出分成清楚的几段复盘。",
                "代码表达", "LOW", l("PYTHON", "CPP17"), l(), "EXPLAIN_GENERALITY");
        b(seeds, "CODE_QUALITY", "代码表达", "代码质量",
                "通过后仍可优化结构、复用和可解释性。", "当前先关注是否便于下次复用和讲解。",
                "适合作为 AC 后提高层入口。",
                l("code quality", "accepted review", "refactor opportunity"), l("重复代码", "函数职责不清", "缺少复盘注释"),
                l("AC"), "先确认它已经解决当前题。", "找出最重复或最难解释的一段。",
                "把可复用的思路整理成一句话和一个函数边界。",
                "代码表达与复盘", "LOW", l("PYTHON", "CPP17"), l(), "EXPLAIN_GENERALITY");
        b(seeds, "GENERALIZATION_CHECK", "泛化", "泛化检查",
                "通过后仍需要确认复杂度、边界和思路可迁移。", "当前先让学生讲清为什么能覆盖一般情况。",
                "适合作为 AC 后复盘与迁移训练。",
                l("accepted", "generalization", "review"), l("只知道过了但说不清原因", "没有总结边界", "无法迁移到变式"),
                l("AC"), "先说清这题为什么能过。", "列一个这题最容易错的边界。",
                "把本题方法迁移到一个相似题口头验证。",
                "迁移泛化", "LOW", l("PYTHON", "CPP17"), l(), "EXPLAIN_GENERALITY");
        b(seeds, "NEEDS_MORE_EVIDENCE", "证据", "证据不足",
                "当前证据不足以可靠判断主因。", "当前先补一条可观察证据，而不是猜答案。",
                "适合隐藏测试不可见、信号冲突或代码片段不足的情况。",
                l("low confidence", "conflicting signals", "hidden data unavailable"),
                l("没有失败样例", "只看到隐藏测试失败", "多个信号互相冲突"),
                l("UNKNOWN", "WA"), "先不要猜隐藏数据。", "补一个最小可见样例或变量追踪结果。",
                "把当前判断建立在可复现的输入、输出或报错上。",
                "问题定位", "LOW", l("PYTHON", "CPP17"), l(), "COLLECT_EVIDENCE");
    }

    private static void improvements(List<AiStandardLibrarySeed> seeds) {
        i(seeds, "COMPLEXITY", "复杂度", "复杂度估算",
                "训练学生从样例规模推进到最大规模判断。",
                "当题面约束较大、出现 TLE、嵌套循环或重复扫描时使用。",
                "帮助学生知道代码为什么小数据能跑、大数据不行。",
                "教师可要求学生写出核心操作次数和最大数据代入结果。",
                l("constraints", "loop count", "TLE", "MAX_BOUNDARY"),
                "先估算最核心循环执行多少次。", "把次数写成 n、m、q 的表达。",
                "代入最大范围，判断数量级是否可接受。",
                "算法复杂度", l("TIME_COMPLEXITY", "MAX_BOUNDARY", "BRUTE_FORCE_LIMIT", "OVER_SIMULATION"));
        i(seeds, "ALGORITHM_MODELING", "算法策略", "算法模型选择",
                "训练学生识别题目更像哪类算法模型。",
                "当当前思路和题目结构不匹配，或隐藏失败说明样例规律不够时使用。",
                "帮助学生从写代码转向识别题型结构。",
                "教师可让学生比较模拟、枚举、贪心、DP、搜索、数学规律的适用条件。",
                l("ALGORITHM_STRATEGY", "hidden failure", "large constraints"),
                "先描述当前思路依赖的假设。", "找一个能破坏这个假设的小例子。",
                "比较题目是否存在单调性、最优子结构或可合并状态。",
                "算法设计", l("ALGORITHM_STRATEGY", "SAMPLE_OVERFIT"));
        i(seeds, "DATA_STRUCTURE_FIT", "数据结构", "数据结构匹配",
                "训练学生按操作需求选择容器。",
                "当代码频繁查找、插入、删除、排序、区间查询或内存压力明显时使用。",
                "帮助学生理解数据结构不是模板，而是服务操作。",
                "教师可要求学生列出操作频率和每种容器的代价。",
                l("DATA_STRUCTURE_CHOICE", "SPACE_COMPLEXITY", "operation frequency"),
                "先列题目最频繁的操作。", "估算当前结构完成这些操作的代价。",
                "比较是否需要快速查找、有序维护、队列或区间信息。",
                "数据结构", l("DATA_STRUCTURE_CHOICE", "SPACE_COMPLEXITY"));
        i(seeds, "BINARY_SEARCH_MODEL", "算法技巧", "二分模型",
                "训练学生识别单调性、边界和答案检查函数。",
                "当题目存在有序区间、可行性单调或搜索答案空间时使用。",
                "帮助学生把二分从模板变成模型理解。",
                "教师可让学生说明单调条件、左右边界含义和检查函数含义。",
                l("monotonic", "ordered range", "boundary"),
                "先判断答案或位置是否有单调性。", "说明左边界和右边界分别代表什么。",
                "构造边界样例检查收缩后是否仍保留答案。",
                "算法设计", l("LOOP_BOUNDARY", "OFF_BY_ONE", "MONOTONICITY_MISSING"));
        i(seeds, "PREFIX_SUM_MODEL", "算法技巧", "前缀和/差分模型",
                "训练学生把重复区间操作转成预处理或差分维护。",
                "当存在多次区间查询、区间更新或连续统计时使用。",
                "帮助学生减少重复扫描，形成区间问题基本模型。",
                "教师可让学生手推前缀数组和任意区间公式。",
                l("PREFIX_SUM_MISSING", "range query", "difference array"),
                "先确认是否反复查询或更新区间。", "写出一个区间能否由两个前缀值表示。",
                "用长度 5 的数组手推前缀和或差分恢复过程。",
                "算法设计", l("PREFIX_SUM_MISSING", "OFF_BY_ONE"));
        i(seeds, "DP_STATE_DESIGN", "动态规划", "DP 状态设计",
                "训练学生先定义状态，再写转移。",
                "当题目需要从历史状态递推，或当前 DP 状态缺失信息时使用。",
                "帮助学生建立状态、转移、初值、答案位置四件套。",
                "教师可要求学生用自然语言解释每个状态含义。",
                l("STATE_TRANSITION", "DP_STATE_DESIGN", "recurrence"),
                "先用一句话定义状态含义。", "写出状态需要保存哪些信息。",
                "检查初值、转移和最终答案是否都符合状态定义。",
                "状态建模", l("STATE_TRANSITION", "DP_STATE_DESIGN", "INITIAL_STATE"));
        i(seeds, "GREEDY_PROOF", "贪心", "贪心依据",
                "训练学生验证局部选择是否能推出全局最优。",
                "当代码依赖排序、最大/最小优先或局部选择时使用。",
                "帮助学生避免只凭直觉写贪心。",
                "教师可要求学生给出反例检验或交换论证雏形。",
                l("GREEDY_ASSUMPTION", "local choice", "counterexample"),
                "先说清每一步为什么这样选。", "构造一个局部看起来好但可能全局差的例子。",
                "尝试说明交换后不会更差，或找出反例。",
                "算法设计", l("GREEDY_ASSUMPTION", "ALGORITHM_STRATEGY"));
        i(seeds, "SEARCH_PRUNING", "搜索", "搜索剪枝",
                "训练学生控制搜索规模和无效分支。",
                "当 DFS/BFS/回溯出现超时或重复状态时使用。",
                "帮助学生从能搜到，推进到搜得动。",
                "教师可要求学生列出状态数量、重复状态和剪枝条件的安全性。",
                l("RECURSION_EXIT", "TLE", "repeated state"),
                "先估算搜索状态数量。", "找出哪些分支一定不可能产生答案。",
                "验证剪枝不会删掉可能正确的路径。",
                "递归与搜索", l("RECURSION_EXIT", "TIME_COMPLEXITY", "SEARCH_STATE_DUPLICATE"));
        i(seeds, "GRAPH_MODELING", "图论", "图建模",
                "训练学生把关系、移动、依赖或状态转换成图结构。",
                "当题目包含点边关系、网格移动、连通性或最短路径时使用。",
                "帮助学生先建对图，再选择 BFS/DFS/最短路。",
                "教师可要求学生画出一个最小样例图。",
                l("GRAPH_DIRECTION", "GRAPH_VISITED", "SHORTEST_PATH_RELAXATION"),
                "先确定什么是点、什么是边。", "确认边的方向、权值和移动规则。",
                "用两个点或一个 2x2 网格验证建图。",
                "图论建模", l("GRAPH_DIRECTION", "GRAPH_VISITED"));
        i(seeds, "MATH_PATTERN", "数学规律", "数学规律抽象",
                "训练学生从枚举结果中发现可证明规律。",
                "当题目存在周期、整除、组合计数、前缀性质或模拟重复时使用。",
                "帮助学生把观察变成可解释的规律。",
                "教师可让学生列小表、找不变量，再解释规律为什么成立。",
                l("OVER_SIMULATION", "large range", "pattern"),
                "先列出几个小规模结果。", "观察差值、周期、奇偶或整除关系。",
                "说明规律为什么对下一组数据也成立。",
                "数学抽象", l("OVER_SIMULATION", "MODULAR_ARITHMETIC", "COMBINATORICS_COUNT"));
        i(seeds, "TESTING_HABIT", "自测", "自测与反例构造",
                "训练学生主动构造非样例测试。",
                "当出现边界、隐藏失败、样例过拟合、输入输出错误或修复回退时使用。",
                "帮助学生在提交前发现问题，而不是只依赖评测。",
                "教师可要求学生提交一组能暴露问题的自造样例。",
                l("boundary", "hidden failure", "sample overfit", "visible mismatch"),
                "先写一个不同于样例的小测试。", "覆盖最小值、最大值、重复值或多组输入。",
                "说明这个测试为什么能验证当前思路。",
                "测试与调试", l("BOUNDARY_CONDITION", "SAMPLE_ONLY", "IO_FORMAT", "PARTIAL_FIX_REGRESSION"));
        i(seeds, "DEBUGGING_TRACE", "调试", "变量追踪调试",
                "训练学生用关键变量表定位错误，而不是盲改代码。",
                "当错因涉及状态、循环、分支或下标时使用。",
                "帮助学生建立可复现、可解释的排查习惯。",
                "教师可要求学生提交一张关键变量变化表。",
                l("TRACE_STATE", "TRACE_VARIABLES", "visible mismatch"),
                "先选一个失败样例。", "列出每轮循环或分支后的关键变量。",
                "比较变量表和手推过程第一次分叉的位置。",
                "测试与调试", l("VARIABLE_INITIALIZATION", "LOOP_BOUNDARY", "CONDITION_BRANCH"));
        i(seeds, "CODE_ORGANIZATION", "代码表达", "代码组织",
                "训练学生把输入、计算、输出和调试代码分清楚。",
                "当代码难以排查、调试输出残留或函数职责混乱时使用。",
                "帮助学生减少低级错误，提高复盘效率。",
                "教师可要求学生指出每一段代码负责什么。",
                l("CODE_READABILITY", "DEBUG_CLEANUP", "IO_FORMAT"),
                "先把输入、计算、输出分成三段看。", "删除或隔离临时调试输出。",
                "给关键函数和变量写出一句职责说明。",
                "代码表达", l("CODE_READABILITY", "CODE_QUALITY", "OUTPUT_FORMAT_DETAIL"));
        i(seeds, "TRANSFER_REVIEW", "复盘迁移", "AC 后复盘迁移",
                "训练学生通过后总结可迁移能力。",
                "当代码 AC、接近 AC，或当前问题已定位后使用。",
                "帮助学生把一次通过沉淀成下一题可复用的方法。",
                "教师可要求学生复述题型、关键边界和复杂度理由。",
                l("AC", "GENERALIZATION_CHECK", "CODE_QUALITY"),
                "先说清这题真正考的是什么。", "总结一个最容易错的边界和一个复杂度理由。",
                "把本题方法迁移到一个相似题口头验证。",
                "迁移泛化", l("GENERALIZATION_CHECK", "CODE_QUALITY", "SAMPLE_ONLY"));
        i(seeds, "BOUNDARY_AWARENESS", "边界", "边界意识",
                "训练学生系统检查极小、极大、重复、特殊值。",
                "当出现边界条件、循环端点、状态初值或运行时问题时使用。",
                "帮助学生建立每题都能复用的边界清单。",
                "教师可要求学生列出本题至少三类边界输入。",
                l("BOUNDARY_CONDITION", "OFF_BY_ONE", "EMPTY_INPUT", "MAX_BOUNDARY"),
                "先列最小、最大和特殊值。", "每类边界至少构造一个输入。",
                "说明代码在这些边界下关键变量如何变化。",
                "边界条件意识", l("BOUNDARY_CONDITION", "LOOP_BOUNDARY", "VARIABLE_INITIALIZATION"));
        i(seeds, "PROOF_INVARIANT", "证明意识", "不变量与正确性说明",
                "训练学生说明算法为什么始终保持正确。", "当算法依赖循环、贪心、二分或状态维护时使用。",
                "帮助学生从会写代码提升到能解释正确性。",
                "教师可要求学生写出循环过程中始终成立的一句话。",
                l("invariant", "proof", "correctness"),
                "先说清每轮循环后什么性质保持不变。", "用一个反例尝试破坏这个性质。",
                "如果性质不成立，回到状态定义或分支条件修正。",
                "算法表达", l("GREEDY_ASSUMPTION", "MONOTONICITY_MISSING", "STATE_TRANSITION"));
        i(seeds, "LANGUAGE_RUNTIME_AWARENESS", "语言特性", "语言运行特性",
                "训练学生理解语言差异对提交结果的影响。", "当 Python 超时、C++ 溢出、递归限制或输入速度影响结果时使用。",
                "帮助学生把环境因素和算法问题区分开。",
                "教师可要求学生列出当前语言的主要风险点。",
                l("PYTHON", "CPP17", "runtime", "input speed"),
                "先确认是算法复杂度、语言限制还是实现细节。", "检查输入读取、整数范围、递归深度。",
                "针对语言特性做最小改动，不要误换算法。",
                "语法与运行环境", l("INTEGER_OVERFLOW", "RECURSION_DEPTH", "TIME_COMPLEXITY"));
        i(seeds, "READ_PROBLEM_CONSTRAINTS", "题意读取", "约束驱动读题",
                "训练学生从数据范围反推算法级别。", "当学生只看样例、不看 n/m/q 范围时使用。",
                "帮助学生把题面约束转化成复杂度目标。",
                "教师可要求学生先写出可接受复杂度再编码。",
                l("constraints", "n", "m", "q", "time limit"),
                "先圈出所有数据范围。", "估算 O(n)、O(nlogn)、O(n^2) 分别会跑多少。",
                "把复杂度目标写在方案前面。",
                "题意读取", l("TIME_COMPLEXITY", "MAX_BOUNDARY", "ALGORITHM_STRATEGY"));
        i(seeds, "EDGE_CASE_CATALOG", "测试", "边界用例清单",
                "训练学生为每题建立固定边界检查清单。", "当错误和边界、特殊值、重复值相关时使用。",
                "帮助学生把一次排错沉淀成通用自测方法。",
                "教师可要求学生列出本题的边界表。",
                l("boundary", "edge case", "test catalog"),
                "先写最小、最大、重复、特殊值。", "每类至少给一个输入和预期输出。",
                "提交前用清单逐项运行。",
                "测试与调试", l("BOUNDARY_CONDITION", "DUPLICATE_CASE", "ZERO_ONE_CASE"));
        i(seeds, "REFACTOR_AFTER_AC", "复盘迁移", "通过后重构复盘",
                "训练学生在 AC 后整理代码和思路。", "当代码已经通过但表达混乱或可迁移性弱时使用。",
                "帮助学生形成可复用模板和思路笔记。",
                "教师可要求学生用 3 句话总结方法、边界和复杂度。",
                l("AC", "CODE_QUALITY", "review"),
                "先保留 AC 版本。", "只重构命名、函数边界和注释，不改核心逻辑。",
                "重构后用原测试集确认没有回退。",
                "代码表达与复盘", l("CODE_QUALITY", "GENERALIZATION_CHECK"));
        i(seeds, "TEACHER_INTERVENTION_SIGNAL", "教师介入", "教师介入信号",
                "识别学生是否需要老师介入，而不是继续自动提示。", "当同类错误反复出现、修复回退或证据冲突时使用。",
                "帮助课堂及时发现卡住的学生。",
                "教师可查看提交轨迹和重复错因后决定是否点拨。",
                l("repeated issue", "regression", "low confidence"),
                "先看同类问题是否重复出现。", "比较最近两次提交是否有有效推进。",
                "如果连续卡住，建议老师用问题而不是答案介入。",
                "课堂干预", l("PARTIAL_FIX_REGRESSION", "NEEDS_MORE_EVIDENCE"));
    }

    private static void b(List<AiStandardLibrarySeed> seeds,
                          String code,
                          String category,
                          String name,
                          String description,
                          String studentExplanation,
                          String teacherExplanation,
                          List<String> evidenceSignals,
                          List<String> commonCodePatterns,
                          List<String> judgeSignals,
                          String hintL1,
                          String hintL2,
                          String hintL3,
                          String abilityPoint,
                          String severity,
                          List<String> applicableLanguages,
                          List<String> relatedItems,
                          String teachingAction) {
        seeds.add(new AiStandardLibrarySeed(
                AiStandardLibraryLayer.BASIC_CAUSE,
                code,
                category,
                name,
                description,
                studentExplanation,
                teacherExplanation,
                evidenceSignals,
                commonCodePatterns,
                judgeSignals,
                List.of(),
                "",
                "",
                hintL1,
                hintL2,
                hintL3,
                abilityPoint,
                severity,
                applicableLanguages,
                relatedItems,
                knowledgeNodesFor(code),
                teachingAction,
                VERSION
        ));
    }

    private static void i(List<AiStandardLibrarySeed> seeds,
                          String code,
                          String category,
                          String name,
                          String description,
                          String whenToUse,
                          String studentBenefit,
                          String teacherExplanation,
                          List<String> requiredEvidence,
                          String hintL1,
                          String hintL2,
                          String hintL3,
                          String abilityPoint,
                          List<String> relatedItems) {
        seeds.add(new AiStandardLibrarySeed(
                AiStandardLibraryLayer.IMPROVEMENT_POINT,
                code,
                category,
                name,
                description,
                "",
                teacherExplanation,
                List.of(),
                List.of(),
                List.of(),
                requiredEvidence,
                whenToUse,
                studentBenefit,
                hintL1,
                hintL2,
                hintL3,
                abilityPoint,
                "",
                l("PYTHON", "CPP17"),
                relatedItems,
                knowledgeNodesFor(code),
                "",
                VERSION
        ));
    }

    private static void generatedFullCoverage(List<AiStandardLibrarySeed> seeds) {
        Set<String> existingBasicCodes = codes(seeds, AiStandardLibraryLayer.BASIC_CAUSE);
        Set<String> existingImprovementCodes = codes(seeds, AiStandardLibraryLayer.IMPROVEMENT_POINT);
        for (InformaticsKnowledgeSeed knowledge : InformaticsKnowledgeSeedCatalog.seeds()) {
            if (knowledge.type() == InformaticsKnowledgeNodeType.KNOWLEDGE_POINT) {
                AiStandardLibrarySeed basic = generatedBasicCause(knowledge);
                if (existingBasicCodes.add(basic.code())) {
                    seeds.add(basic);
                }
                AiStandardLibrarySeed improvement = generatedKnowledgePointImprovement(knowledge, basic.code());
                if (existingImprovementCodes.add(improvement.code())) {
                    seeds.add(improvement);
                }
            } else if (knowledge.type() == InformaticsKnowledgeNodeType.TOPIC) {
                AiStandardLibrarySeed improvement = generatedTopicImprovement(knowledge);
                if (existingImprovementCodes.add(improvement.code())) {
                    seeds.add(improvement);
                }
            }
        }
    }

    private static Set<String> codes(List<AiStandardLibrarySeed> seeds, AiStandardLibraryLayer layer) {
        Set<String> result = new LinkedHashSet<>();
        seeds.stream()
                .filter(seed -> seed.layer() == layer)
                .map(AiStandardLibrarySeed::code)
                .forEach(result::add);
        return result;
    }

    private static AiStandardLibrarySeed generatedBasicCause(InformaticsKnowledgeSeed knowledge) {
        String name = safeName(knowledge.name() + "掌握偏差", 120);
        String category = safeName("知识点错因/" + domainName(knowledge), 80);
        String abilityPoint = safeName(knowledge.name(), 120);
        return new AiStandardLibrarySeed(
                AiStandardLibraryLayer.BASIC_CAUSE,
                generatedCode("KB", knowledge.code()),
                category,
                name,
                "学生在「" + knowledge.path() + "」相关代码中出现概念理解、边界处理或应用迁移偏差。",
                "先回到「" + knowledge.name() + "」这个知识点，确认题目里它对应的是输入、状态、循环、结构还是算法选择。",
                "该条目用于覆盖知识点「" + knowledge.path() + "」下的常见基础层错因，教师可根据课堂题型继续细化证据信号。",
                List.of(
                        "knowledge_node:" + knowledge.code(),
                        "student code touches " + knowledge.name(),
                        "judge result conflicts with expected behavior near " + knowledge.name(),
                        "local evidence graph or teacher review points to " + knowledge.name()
                ),
                generatedCodePatterns(knowledge),
                generatedJudgeSignals(knowledge),
                List.of(),
                "",
                "",
                "先判断这题是否真的用到了「" + knowledge.name() + "」。",
                "把代码里和「" + knowledge.name() + "」有关的变量、循环或状态单独圈出来。",
                "用一个最小样例手推这部分逻辑，确认它和题意中的定义是否一致。",
                abilityPoint,
                generatedSeverity(knowledge),
                List.of("PYTHON", "CPP17"),
                generatedRelatedItems(knowledge),
                List.of(knowledge.code()),
                generatedTeachingAction(knowledge),
                VERSION
        );
    }

    private static AiStandardLibrarySeed generatedKnowledgePointImprovement(InformaticsKnowledgeSeed knowledge,
                                                                            String relatedBasicCode) {
        return new AiStandardLibrarySeed(
                AiStandardLibraryLayer.IMPROVEMENT_POINT,
                generatedCode("KI", knowledge.code()),
                safeName("知识点提升/" + domainName(knowledge), 80),
                safeName(knowledge.name() + "迁移提升", 120),
                "在掌握「" + knowledge.name() + "」的基本用法后，进一步训练其边界、复杂度、证明或迁移应用能力。",
                "",
                "教师可要求学生用自己的话说明「" + knowledge.name() + "」的定义、适用条件、边界和一个反例。",
                List.of(),
                List.of(),
                List.of(),
                List.of(
                        "submission is accepted or close to accepted",
                        "basic cause confidence is low but knowledge node is relevant",
                        "student needs transfer practice for " + knowledge.code(),
                        "teacher wants post-AC reflection"
                ),
                "用于学生已经接近通过、已经 AC 或基础错误不明显时，引导其把「" + knowledge.name() + "」迁移到相邻题型。",
                "帮助学生把一次代码修正沉淀为可迁移的知识点理解，而不是只记住本题改法。",
                "先用一句话总结「" + knowledge.name() + "」解决了什么问题。",
                "再写一个和原题不同的最小例子，说明这个知识点仍然适用或不适用。",
                "最后比较当前写法的复杂度、边界和可迁移性。",
                safeName(knowledge.name(), 120),
                "",
                List.of("PYTHON", "CPP17"),
                List.of(relatedBasicCode),
                List.of(knowledge.code()),
                "",
                VERSION
        );
    }

    private static AiStandardLibrarySeed generatedTopicImprovement(InformaticsKnowledgeSeed knowledge) {
        return new AiStandardLibrarySeed(
                AiStandardLibraryLayer.IMPROVEMENT_POINT,
                generatedCode("KT", knowledge.code()),
                safeName("主题提升/" + domainName(knowledge), 80),
                safeName(knowledge.name() + "专题复盘", 120),
                "围绕「" + knowledge.path() + "」形成专题化复盘，帮助学生把多个细知识点串成解题方法。",
                "",
                "教师可把该主题下的错题合并成一次小复盘，要求学生说明共性结构。",
                List.of(),
                List.of(),
                List.of(),
                List.of(
                        "multiple related knowledge points are involved",
                        "same topic appears across recent submissions",
                        "teacher review groups errors by topic",
                        "post-AC transfer or class review is needed"
                ),
                "当学生在同一主题下多个知识点反复出错，或通过单题后需要总结题型时使用。",
                "帮助学生从单点修错上升到专题方法，比如读题、建模、边界、复杂度和验证习惯。",
                "先找出这几题共同属于哪个主题。",
                "把共同的输入结构、状态变量或算法选择列出来。",
                "总结一条下次遇到同类题时可复用的检查清单。",
                safeName(knowledge.name(), 120),
                "",
                List.of("PYTHON", "CPP17"),
                List.of(),
                List.of(knowledge.code()),
                "",
                VERSION
        );
    }

    private static String generatedCode(String prefix, String knowledgeCode) {
        String slug = knowledgeCode.toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        if (slug.length() > 54) {
            slug = slug.substring(0, 54).replaceAll("_+$", "");
        }
        String hash = Integer.toUnsignedString(knowledgeCode.hashCode(), 16).toUpperCase(Locale.ROOT);
        return (prefix + "_" + slug + "_" + hash).replaceAll("__+", "_");
    }

    private static List<String> generatedCodePatterns(InformaticsKnowledgeSeed knowledge) {
        String code = knowledge.code();
        if (code.contains(".IO.")) {
            return List.of("读入次数和题面不一致", "输出行数、空格或小数格式不符合题意", "多组数据下状态没有独立处理");
        }
        if (code.contains(".LOOP.") || code.contains(".ARRAY.") || code.contains(".STRING.")) {
            return List.of("循环端点没有覆盖最小/最大下标", "0 基和 1 基编号混用", "对空串、单元素或最后一个元素处理不一致");
        }
        if (code.contains(".DP.")) {
            return List.of("状态含义没有覆盖题目所需信息", "初始值或转移顺序不完整", "滚动数组覆盖了仍需使用的旧状态");
        }
        if (code.contains(".GRAPH.") || code.contains(".SEARCH.")) {
            return List.of("访问标记时机不正确", "状态重复入队或递归未恢复", "边方向、权值或终点判断与题意不一致");
        }
        if (code.contains(".GREEDY.") || code.contains(".BINARY.")) {
            return List.of("缺少单调性或交换依据", "边界返回值与题意目标不一致", "只用样例验证没有构造反例");
        }
        if (code.contains(".COMPLEXITY.") || code.contains(".ENUM.")) {
            return List.of("核心循环次数超过数据范围可承受规模", "重复计算没有缓存或预处理", "最大输入下可能超时");
        }
        return List.of("知识点定义和代码变量没有对应清楚", "边界样例和普通样例表现不一致", "题意中的条件没有完整映射到代码结构");
    }

    private static List<String> generatedJudgeSignals(InformaticsKnowledgeSeed knowledge) {
        String code = knowledge.code();
        if (code.contains(".ERROR.COMPILE")) {
            return List.of("CE", "COMPILE_ERROR");
        }
        if (code.contains(".ERROR.RUNTIME") || code.contains(".ARRAY.") || code.contains(".RECURSION.")) {
            return List.of("RE", "WA");
        }
        if (code.contains(".COMPLEXITY.") || code.contains(".TIME") || code.contains(".ENUM.")) {
            return List.of("TLE", "WA_ON_MAX_CASE");
        }
        if (code.contains(".IO.") || code.contains(".FORMAT")) {
            return List.of("WA", "VISIBLE_OUTPUT_MISMATCH", "PRESENTATION_ERROR");
        }
        return List.of("WA", "HIDDEN_CASE_FAILED");
    }

    private static List<String> generatedRelatedItems(InformaticsKnowledgeSeed knowledge) {
        List<String> result = new ArrayList<>();
        if (knowledge.parentCode() != null && !knowledge.parentCode().isBlank()) {
            result.add(knowledge.parentCode());
        }
        if (knowledge.prerequisites() != null) {
            result.addAll(knowledge.prerequisites());
        }
        return result.stream().distinct().toList();
    }

    private static String generatedSeverity(InformaticsKnowledgeSeed knowledge) {
        String code = knowledge.code();
        if (code.contains(".IO.") || code.contains(".LOOP.") || code.contains(".ARRAY.")
                || code.contains(".ERROR.") || code.contains(".COMPLEXITY.")) {
            return "HIGH";
        }
        if (code.contains(".STYLE.") || code.contains(".SUBMIT.")) {
            return "LOW";
        }
        return "MEDIUM";
    }

    private static String generatedTeachingAction(InformaticsKnowledgeSeed knowledge) {
        String code = knowledge.code();
        if (code.contains(".IO.")) {
            return "COMPARE_INPUT_SPEC";
        }
        if (code.contains(".LOOP.") || code.contains(".ARRAY.") || code.contains(".STRING.")) {
            return "TRACE_VARIABLES";
        }
        if (code.contains(".BRANCH.")) {
            return "CHECK_BRANCH_COVERAGE";
        }
        if (code.contains(".DP.") || code.contains(".SIM.")) {
            return "DEFINE_STATE";
        }
        if (code.contains(".RECURSION.") || code.contains(".SEARCH.")) {
            return "DRAW_RECURSION_TREE";
        }
        if (code.contains(".COMPLEXITY.") || code.contains(".ENUM.")) {
            return "COUNT_COMPLEXITY";
        }
        if (code.contains(".GREEDY.") || code.contains(".BINARY.")) {
            return "CHECK_INVARIANT";
        }
        if (code.contains(".DEBUG.") || code.contains(".SUBMIT.")) {
            return "ASK_MIN_CASE";
        }
        return "TRACE_STATE";
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

    private static List<String> knowledgeNodesFor(String code) {
        return switch (code) {
            case "SYNTAX_ERROR" -> l("ENG.ERROR.COMPILE.语法拼写", "ENG.ERROR.COMPILE.括号不匹配");
            case "PY_INDENTATION" -> l("ENG.ERROR.COMPILE.语法拼写", "BASIC.BRANCH.IF.嵌套分支");
            case "CPP_HEADER_NAMESPACE" -> l("ENG.ERROR.COMPILE.未声明标识符", "BASIC.FUNCTION.DEF.函数命名");
            case "TYPE_MISMATCH" -> l("BASIC.TYPE.VARIABLE.声明与初始化", "ENG.ERROR.COMPILE.类型不匹配");
            case "FUNCTION_SIGNATURE" -> l("BASIC.FUNCTION.DEF.参数设计", "BASIC.FUNCTION.RETURN.完整返回路径");
            case "RUNTIME_STABILITY" -> l("ENG.ERROR.RUNTIME.数组越界", "ENG.ERROR.RUNTIME.除零");
            case "ARRAY_INDEX_OUT_OF_RANGE" -> l("BASIC.ARRAY.INDEX.越界访问", "ENG.ERROR.RUNTIME.数组越界");
            case "DIVISION_BY_ZERO" -> l("ENG.ERROR.RUNTIME.除零", "BASIC.EXPR.ARITH.取模含义");
            case "RECURSION_DEPTH" -> l("BASIC.RECURSION.BASE.终止条件", "ENG.ERROR.RUNTIME.递归爆栈");
            case "IO_FORMAT" -> l("BASIC.IO.STDOUT.按要求换行", "ENG.ERROR.FORMAT.缺少换行");
            case "INPUT_PARSING" -> l("BASIC.IO.STDIN.输入顺序映射", "CONTEST.READING.INPUT.输入结构识别");
            case "MULTI_CASE_INPUT" -> l("BASIC.IO.MULTI_CASE.显式_T_组循环", "BASIC.IO.MULTI_CASE.每组状态重置");
            case "EOF_INPUT_LOOP" -> l("BASIC.IO.MULTI_CASE.未知组数读到_EOF", "BASIC.IO.STDIN.读到文件结束");
            case "OUTPUT_FORMAT_DETAIL" -> l("BASIC.IO.STDOUT.空格分隔", "ENG.ERROR.FORMAT.多余空格");
            case "DEBUG_OUTPUT_LEFT" -> l("BASIC.IO.STDOUT.禁止多余调试输出", "ENG.ERROR.FORMAT.调试输出");
            case "CASE_SENSITIVITY" -> l("BASIC.IO.STDOUT.大小写与标点一致", "ENG.ERROR.FORMAT.大小写错误");
            case "OUTPUT_ORDER" -> l("BASIC.IO.STDOUT.按要求换行", "ALGO.SORT.BASIC.排序后遍历");
            case "VARIABLE_INITIALIZATION" -> l("BASIC.TYPE.VARIABLE.声明与初始化", "ENG.DEBUG.TRACE.循环状态");
            case "INITIAL_STATE" -> l("ALGO.SIM.STATE.状态初始化", "BASIC.TYPE.VARIABLE.声明与初始化");
            case "MIN_MAX_INITIALIZATION" -> l("BASIC.ARRAY.TRAVERSE.全量遍历", "ALGO.DP.INIT.初始值");
            case "FLAG_UPDATE" -> l("BASIC.TYPE.CHAR_BOOL.逻辑标志变量", "BASIC.LOOP.CONTROL.标志变量");
            case "ACCUMULATOR_UPDATE" -> l("BASIC.ARRAY.PREFIX.频次统计", "MATH.COUNT.RECUR.递推式");
            case "STATE_RESET" -> l("BASIC.IO.MULTI_CASE.每组状态重置", "ALGO.SIM.STATE.状态初始化");
            case "MUTABLE_ALIASING" -> l("BASIC.ARRAY.UPDATE.覆盖风险", "BASIC.FUNCTION.PARAM.可变对象风险");
            case "CONDITION_BRANCH" -> l("BASIC.BRANCH.CASE.覆盖所有情况", "BASIC.BRANCH.IF.多分支链");
            case "OVERLAPPING_CONDITIONS" -> l("BASIC.BRANCH.CASE.互斥条件", "BASIC.BRANCH.CASE.优先级条件前置");
            case "MISSING_ELSE_CASE" -> l("BASIC.BRANCH.CASE.默认分支", "BASIC.BRANCH.CASE.覆盖所有情况");
            case "COMPARISON_OPERATOR" -> l("BASIC.EXPR.COMPARE.边界比较", "BASIC.LOOP.BOUNDARY.最后一次迭代");
            case "BOOLEAN_LOGIC" -> l("BASIC.EXPR.LOGIC.条件组合", "BASIC.EXPR.LOGIC.德摩根变换");
            case "LOOP_BOUNDARY" -> l("BASIC.LOOP.BOUNDARY.左闭右开", "BASIC.LOOP.BOUNDARY.左闭右闭");
            case "OFF_BY_ONE" -> l("BASIC.LOOP.BOUNDARY.最后一次迭代", "BASIC.ARRAY.INDEX.长度与最后下标");
            case "WHILE_TERMINATION" -> l("BASIC.LOOP.WHILE.循环条件", "BASIC.LOOP.WHILE.状态推进");
            case "NESTED_LOOP_SCOPE" -> l("BASIC.LOOP.NESTED.内外层变量区分", "BASIC.LOOP.NESTED.重复初始化位置");
            case "TWO_POINTER_PROGRESS" -> l("ALGO.TWO_POINTERS.WINDOW.窗口扩张", "ALGO.TWO_POINTERS.WINDOW.窗口收缩");
            case "BOUNDARY_CONDITION" -> l("ENG.DEBUG.BOUNDARY.最小输入", "ENG.DEBUG.BOUNDARY.最大输入");
            case "EMPTY_INPUT" -> l("ENG.DEBUG.BOUNDARY.空结果", "BASIC.ARRAY.INDEX.越界访问");
            case "SINGLE_ELEMENT_CASE" -> l("ENG.DEBUG.BOUNDARY.最小输入", "BASIC.ARRAY.INDEX.长度与最后下标");
            case "ZERO_ONE_CASE" -> l("ENG.DEBUG.BOUNDARY.极端值", "MATH.NUMBER.DIVISIBILITY.倍数判断");
            case "MAX_BOUNDARY" -> l("ENG.DEBUG.BOUNDARY.最大输入", "ENG.COMPLEXITY.TIME.数据范围反推");
            case "DUPLICATE_CASE" -> l("DS.SET_MAP.SET.去重", "ALGO.SORT.APPLICATION.去重统计");
            case "NEGATIVE_NUMBER_CASE" -> l("BASIC.TYPE.INTEGER.int_范围", "MATH.NUMBER.MOD.负数取模修正");
            case "DATA_STRUCTURE_CHOICE" -> l("DS.LINEAR.VECTOR.随机访问", "DS.SET_MAP.MAP.键值关系");
            case "HASH_LOOKUP_MISSING" -> l("DS.SET_MAP.HASH.空间换时间", "DS.SET_MAP.MAP.频次统计");
            case "SORTING_ORDER_KEY" -> l("ALGO.SORT.BASIC.自定义比较", "ALGO.SORT.APPLICATION.区间排序");
            case "QUEUE_STACK_MISUSE" -> l("DS.LINEAR.STACK.后进先出", "DS.LINEAR.QUEUE.先进先出");
            case "TIME_COMPLEXITY", "COMPLEXITY" -> l("ENG.COMPLEXITY.TIME.数据范围反推", "CONTEST.PATTERN.RANGE.大范围_O_N_LOG_N");
            case "OVER_SIMULATION" -> l("ALGO.SIM.PROCESS.时间推进", "ENG.COMPLEXITY.TRADEOFF.预处理");
            case "BRUTE_FORCE_LIMIT" -> l("ALGO.ENUM.COMPLEXITY.数据范围反推", "CONTEST.PATTERN.RANGE.中等范围优化");
            case "REPEATED_WORK" -> l("ENG.COMPLEXITY.TRADEOFF.缓存", "ENG.COMPLEXITY.TRADEOFF.重复计算消除");
            case "SPACE_COMPLEXITY" -> l("ENG.COMPLEXITY.SPACE.数组规模", "ENG.COMPLEXITY.SPACE.二维空间");
            case "INTEGER_OVERFLOW" -> l("BASIC.TYPE.INTEGER.long_long_范围", "MATH.NUMBER.MOD.大数防溢出");
            case "FLOAT_PRECISION" -> l("BASIC.TYPE.FLOAT.浮点误差", "BASIC.TYPE.FLOAT.小数比较");
            case "ALGORITHM_STRATEGY", "ALGORITHM_MODELING" -> l("CONTEST.PATTERN.STRUCTURE.结构特征", "CONTEST.PATTERN.OBJECTIVE.目标特征");
            case "GREEDY_ASSUMPTION", "GREEDY_PROOF" -> l("ALGO.GREEDY.CHOICE.反例检查", "ALGO.GREEDY.CHOICE.交换论证");
            case "MONOTONICITY_MISSING", "BINARY_SEARCH_MODEL" -> l("ALGO.BINARY.ANSWER.单调性证明", "ALGO.BINARY.ANSWER.check_函数");
            case "PREFIX_SUM_MISSING", "PREFIX_SUM_MODEL" -> l("ALGO.PREFIX.SUM.区间查询", "ALGO.PREFIX.DIFF.区间加");
            case "GRAPH_VISITED" -> l("DS.GRAPH.TRAVERSE.访问标记", "ALGO.GRAPH.CONNECT.连通块");
            case "GRAPH_DIRECTION" -> l("DS.GRAPH.MODEL.有向无向", "DS.GRAPH.STORE.双向边添加");
            case "SHORTEST_PATH_RELAXATION" -> l("ALGO.GRAPH.SHORTEST.松弛操作", "ALGO.GRAPH.SHORTEST.距离初始化");
            case "RECURSION_EXIT" -> l("BASIC.RECURSION.BASE.终止条件", "ALGO.SEARCH.DFS.搜索边界");
            case "SEARCH_STATE_DUPLICATE" -> l("ALGO.SEARCH.STATE.判重", "ALGO.SEARCH.BFS.访问标记");
            case "PRUNING_UNSAFE", "SEARCH_PRUNING" -> l("ALGO.ENUM.PRUNE.不可能条件", "ALGO.SEARCH.DFS.剪枝位置");
            case "STATE_TRANSITION" -> l("ALGO.SIM.STATE.状态转移", "ALGO.DP.TRANSITION.枚举决策");
            case "DP_STATE_DESIGN" -> l("ALGO.DP.STATE.状态含义", "ALGO.DP.STATE.维度选择");
            case "DP_INITIALIZATION" -> l("ALGO.DP.INIT.初始值", "ALGO.DP.INIT.边界状态");
            case "DP_UPDATE_ORDER" -> l("ALGO.DP.TRANSITION.从前往后", "ALGO.DP.TRANSITION.从后往前");
            case "IN_PLACE_STATE_PROGRESS" -> l("BASIC.ARRAY.UPDATE.原地修改", "ALGO.DP.STATE.状态压缩直觉");
            case "MODULAR_ARITHMETIC" -> l("MATH.NUMBER.MOD.加法取模", "MATH.NUMBER.MOD.乘法取模");
            case "COMBINATORICS_COUNT" -> l("MATH.COUNT.PERM.顺序是否重要", "MATH.COUNT.INCLUSION.集合重叠");
            case "SAMPLE_ONLY", "SAMPLE_OVERFIT" -> l("CONTEST.SUBMIT.CHECKLIST.边界复测", "CONTEST.SUBMIT.REVIEW.最小反例构造");
            case "PARTIAL_FIX_REGRESSION" -> l("CONTEST.SUBMIT.REVIEW.同类错因记录", "CONTEST.SUBMIT.REVIEW.再次提交策略");
            case "CODE_READABILITY", "CODE_QUALITY", "CODE_ORGANIZATION", "REFACTOR_AFTER_AC" -> l("ENG.STYLE.NAME.变量含义", "ENG.STYLE.STRUCTURE.函数拆分");
            case "GENERALIZATION_CHECK", "TRANSFER_REVIEW" -> l("CONTEST.SUBMIT.REVIEW.知识点回补", "CONTEST.PATTERN.STRUCTURE.结构特征");
            case "NEEDS_MORE_EVIDENCE", "TESTING_HABIT", "EDGE_CASE_CATALOG" -> l("ENG.DEBUG.BOUNDARY.边界测试", "CONTEST.SUBMIT.CHECKLIST.样例复测");
            case "DATA_STRUCTURE_FIT" -> l("DS.SET_MAP.MAP.频次统计", "DS.LINEAR.QUEUE.BFS_队列");
            case "MATH_PATTERN" -> l("MATH.NUMBER.GCD.欧几里得算法", "MATH.COUNT.RECUR.递推式");
            case "DEBUGGING_TRACE" -> l("ENG.DEBUG.TRACE.循环状态", "ENG.DEBUG.TRACE.数组状态");
            case "BOUNDARY_AWARENESS" -> l("ENG.DEBUG.BOUNDARY.最小输入", "ENG.DEBUG.BOUNDARY.最大输入");
            case "PROOF_INVARIANT" -> l("ENG.STYLE.INVARIANT.循环不变量", "ENG.STYLE.INVARIANT.DP_状态含义");
            case "LANGUAGE_RUNTIME_AWARENESS" -> l("ENG.ERROR.RUNTIME.递归爆栈", "BASIC.TYPE.INTEGER.整型溢出");
            case "READ_PROBLEM_CONSTRAINTS" -> l("CONTEST.READING.CONSTRAINT.数据范围", "CONTEST.READING.CONSTRAINT.时间限制");
            case "TEACHER_INTERVENTION_SIGNAL" -> l("CONTEST.SUBMIT.REVIEW.同类错因记录", "CONTEST.SUBMIT.REVIEW.再次提交策略");
            default -> l("CONTEST.READING.CONSTRAINT.数据范围");
        };
    }

    private static List<String> l(String... values) {
        return List.of(values);
    }
}
