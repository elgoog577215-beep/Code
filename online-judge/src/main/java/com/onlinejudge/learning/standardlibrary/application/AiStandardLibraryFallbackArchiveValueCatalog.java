package com.onlinejudge.learning.standardlibrary.application;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class AiStandardLibraryFallbackArchiveValueCatalog {

    private AiStandardLibraryFallbackArchiveValueCatalog() {
    }

    public static List<FallbackArchiveValueSignal> signals() {
        List<FallbackArchiveValueSignal> signals = new ArrayList<>();
        directAbsorptionSignals().forEach(signals::add);
        typeRewriteSignals().forEach(signals::add);

        Set<String> classifiedCodes = signals.stream()
                .map(FallbackArchiveValueSignal::knowledgeCode)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> activeKnowledgeCodes = AiStandardLibrarySeedCatalog.seeds().stream()
                .flatMap(seed -> seed.knowledgeNodeCodes().stream())
                .collect(Collectors.toSet());
        AiStandardLibrarySeedCatalog.archivedGeneratedFallbackSeeds().stream()
                .flatMap(seed -> seed.knowledgeNodeCodes().stream())
                .distinct()
                .filter(code -> !activeKnowledgeCodes.contains(code))
                .filter(code -> !classifiedCodes.contains(code))
                .map(code -> new FallbackArchiveValueSignal(
                        code,
                        FallbackArchiveTreatment.ARCHIVE_ONLY,
                        topicCode(code),
                        "暂不拆成独立活跃条目，继续作为后续扩张的存档候选。",
                        List.of("保留知识树覆盖线索", "后续批次复查是否需要精修")))
                .forEach(signals::add);
        return List.copyOf(signals);
    }

    public static FallbackArchiveTreatment treatmentFor(String knowledgeCode) {
        return signals().stream()
                .filter(signal -> signal.knowledgeCode().equals(knowledgeCode))
                .map(FallbackArchiveValueSignal::treatment)
                .findFirst()
                .orElse(FallbackArchiveTreatment.ARCHIVE_ONLY);
    }

    public static List<String> directAbsorptionKnowledgeCodes() {
        return directAbsorptionSignals().stream()
                .map(FallbackArchiveValueSignal::knowledgeCode)
                .toList();
    }

    public static List<String> typeRewriteKnowledgeCodes() {
        return typeRewriteSignals().stream()
                .map(FallbackArchiveValueSignal::knowledgeCode)
                .toList();
    }

    private static List<FallbackArchiveValueSignal> directAbsorptionSignals() {
        return List.of(
                direct("BASIC.LOOP.CONTROL.break_使用", "循环控制", "break 使用本身就是高频代码行为，适合直接精修成循环退出契约。"),
                direct("BASIC.LOOP.CONTROL.continue_使用", "循环控制", "continue 容易跳过维护量更新，适合直接形成易错点。"),
                direct("BASIC.LOOP.CONTROL.提前结束", "循环控制", "提前结束会改变答案更新时机，适合直接吸收。"),
                direct("BASIC.LOOP.CONTROL.标志变量", "循环控制", "标志变量生命周期和重置问题高发，适合直接吸收。"),
                direct("BASIC.LOOP.CONTROL.多层循环退出", "循环控制", "多层退出常导致只退出内层或状态未同步，适合直接吸收。"),
                direct("BASIC.ARRAY.PREFIX.前缀和定义", "前缀统计", "前缀定义是区间查询和差分训练的基础，适合直接吸收。"),
                direct("BASIC.ARRAY.PREFIX.区间和查询", "前缀统计", "区间和公式边界高发，适合直接吸收。"),
                direct("BASIC.ARRAY.PREFIX.差分直觉", "前缀统计", "差分更新点和还原时机容易错，适合直接吸收。"),
                direct("BASIC.ARRAY.PREFIX.计数数组", "前缀统计", "计数数组的值域映射和初始化高发，适合直接吸收。"),
                direct("BASIC.STRING.BUILD.拼接效率", "字符串构造", "拼接效率会直接影响超时和语言选择，适合直接吸收。"),
                direct("BASIC.STRING.BUILD.格式化构造", "字符串构造", "格式化构造和输出要求强绑定，适合直接吸收。"),
                direct("BASIC.STRING.BUILD.删除替换", "字符串构造", "删除替换容易错过下标推进和重叠影响，适合直接吸收。"),
                direct("BASIC.BRANCH.IF.单分支判断", "分支覆盖", "单分支容易遗漏未命中路径，适合直接吸收。"),
                direct("BASIC.BRANCH.IF.双分支判断", "分支覆盖", "双分支需要互斥和默认路径，适合直接吸收。"),
                direct("BASIC.BRANCH.IF.多分支链", "分支覆盖", "多分支链高发优先级错误，适合直接吸收。"),
                direct("BASIC.BRANCH.IF.嵌套分支", "分支覆盖", "嵌套分支容易隐藏未覆盖路径，适合直接吸收。"),
                direct("BASIC.BRANCH.IF.条件变量准备", "分支覆盖", "条件变量旧值和未赋值问题高发，适合直接吸收。")
        );
    }

    private static List<FallbackArchiveValueSignal> typeRewriteSignals() {
        return List.of(
                rewrite("DS.LINEAR.STACK.括号匹配", "栈队列状态", "兜底文本偏概念，但暴露出栈顶匹配和空栈防护类型。"),
                rewrite("DS.LINEAR.STACK.单调栈雏形", "栈队列状态", "需要提炼成维护单调性和弹栈条件，而不是保留模板描述。"),
                rewrite("DS.LINEAR.STACK.递归栈模拟", "栈队列状态", "可提炼为显式栈状态与递归调用帧对应关系。"),
                rewrite("DS.LINEAR.QUEUE.循环队列", "栈队列状态", "可提炼为头尾指针、取模和队满队空类型。"),
                rewrite("DS.LINEAR.QUEUE.双端队列", "栈队列状态", "可提炼为两端操作方向和单调队列雏形。"),
                rewrite("DS.LINEAR.QUEUE.队空判断", "栈队列状态", "可提炼为访问前保护和 BFS 层次推进类型。"),
                rewrite("DS.SET_MAP.HASH.均摊复杂度", "哈希建模", "兜底内容不够细，但提示了均摊复杂度和最坏情况风险。"),
                rewrite("DS.SET_MAP.HASH.哈希冲突直觉", "哈希建模", "可提炼为哈希退化、键设计和冲突处理方向。"),
                rewrite("DS.SET_MAP.HASH.字符串键", "哈希建模", "可提炼为字符串规范化和键值角色设计。"),
                rewrite("ENG.DEBUG.TRACE.递归栈", "调试追踪", "可提炼为调用帧、参数变化和返回值追踪。"),
                rewrite("ENG.DEBUG.TRACE.DP_表变化", "调试追踪", "可提炼为表格行列、旧值来源和转移前后对照。"),
                rewrite("CONTEST.SUBMIT.CHECKLIST.初始化检查", "提交检查", "可提炼为提交前按变量、数组和多组数据检查初始化。")
        );
    }

    private static FallbackArchiveValueSignal direct(String knowledgeCode, String theme, String reason) {
        return new FallbackArchiveValueSignal(
                knowledgeCode,
                FallbackArchiveTreatment.DIRECT_ABSORPTION,
                theme,
                reason,
                List.of("主题本身可教学", "改写为手写能力点和具体易错点"));
    }

    private static FallbackArchiveValueSignal rewrite(String knowledgeCode, String theme, String reason) {
        return new FallbackArchiveValueSignal(
                knowledgeCode,
                FallbackArchiveTreatment.TYPE_REWRITE,
                theme,
                reason,
                List.of("保留错因类型和选题方向", "丢弃兜底原文后重写"));
    }

    private static String topicCode(String knowledgeCode) {
        String[] parts = knowledgeCode == null ? new String[0] : knowledgeCode.split("\\.");
        if (parts.length < 3) {
            return knowledgeCode == null ? "" : knowledgeCode;
        }
        return parts[0] + "." + parts[1] + "." + parts[2];
    }

    public enum FallbackArchiveTreatment {
        DIRECT_ABSORPTION,
        TYPE_REWRITE,
        ARCHIVE_ONLY
    }

    public record FallbackArchiveValueSignal(
            String knowledgeCode,
            FallbackArchiveTreatment treatment,
            String theme,
            String reason,
            List<String> extractedValues
    ) {
    }
}
