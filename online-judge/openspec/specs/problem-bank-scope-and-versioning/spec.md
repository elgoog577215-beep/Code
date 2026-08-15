# problem-bank-scope-and-versioning Specification

## Purpose
为公共免费题、教师共建题和教师私有题建立清晰的可见性、审核及不可变版本规则，保证进行中作业不被后续编辑改变。
## Requirements
### Requirement: 题目必须具有范围与版本状态
题目 SHALL 记录所有者、`PUBLIC | SHARED | PRIVATE` 范围、版本状态、系列和版本号；现有正式题 SHALL 迁移为平台所有的 `PUBLIC/PUBLISHED`。

#### Scenario: 匿名访问公共题库
- **WHEN** 匿名用户查询 `/api/problems/catalog`
- **THEN** 系统 SHALL 只返回 `PUBLIC/PUBLISHED` 且未归档的题目

#### Scenario: 教师查看题库
- **WHEN** 已审核教师查询教师题库
- **THEN** 系统 SHALL 返回公共已发布题、共享已发布题和自己的私有题
- **AND** MUST NOT 返回其他教师私有题

### Requirement: 共建审核必须产生不可变版本
提交私有草稿审核 SHALL 复制出 `REVIEW_PENDING` 版本；审核通过后 SHALL 成为只读 `SHARED/PUBLISHED` 版本。

#### Scenario: 修订已发布共建题
- **WHEN** 所有者请求修订已发布题
- **THEN** 系统 SHALL 创建新的私有草稿版本
- **AND** 原发布版本及其作业引用 SHALL 保持不变

### Requirement: 私有草稿发布作业必须冻结快照
作业引用私有草稿时，系统 SHALL 创建不可编辑的 `PRIVATE/FROZEN` 快照，并 SHALL 让作业任务引用该快照。

#### Scenario: 发布后编辑原草稿
- **WHEN** 教师在作业发布后修改原私有草稿
- **THEN** 已发布作业的题目内容和测试用例 SHALL 不变

### Requirement: 提升公共题必须复制版本
管理员将共建题提升为公共题时 SHALL 创建 `PUBLIC/PUBLISHED` 新版本，而不是直接修改共享版本范围。

#### Scenario: 共建题被提升为公共题
- **WHEN** 管理员批准公开发布
- **THEN** 新公共版本 SHALL 保留来源版本引用
- **AND** 原共享版本 SHALL 继续存在且不可变

### Requirement: 删除必须改为归档
题目删除操作 SHALL 归档题目；已有作业或提交引用的题目 MUST NOT 被级联删除。

#### Scenario: 归档已被作业引用的题目
- **WHEN** 所有者归档已被作业引用的题目
- **THEN** 新题库查询 SHALL 隐藏该题
- **AND** 历史作业和提交 SHALL 仍可读取原版本
