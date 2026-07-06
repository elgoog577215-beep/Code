# standard-library-review-workflow Specification

## Purpose
定义教师端标准库成长候选审核闭环，让 AI 诊断提出的新错误点或提升点必须经过可查看、可编辑、可批准、可合并、可拒绝或可忽略的审核流程后，才影响正式标准库。

## Requirements
### Requirement: 教师可以审核标准库成长候选
系统 SHALL 在教师端提供标准库成长候选审核入口，使教师能处理 AI 诊断提出的新错误点或提升点。

#### Scenario: 查看待审核候选
- **WHEN** 教师进入 AI 标准库管理页的待审核视图
- **THEN** 系统 SHALL 展示候选名称、归属路径、状态、置信度、出现次数、来源提交和变更理由

#### Scenario: 编辑候选后通过
- **WHEN** 教师修改候选 ID、名称、路径或说明后点击通过
- **THEN** 系统 SHALL 使用教师修改后的内容写入正式标准库
- **AND** 系统 SHALL 将候选状态更新为教师批准或已合并

#### Scenario: 拒绝或忽略候选
- **WHEN** 教师拒绝或忽略候选
- **THEN** 系统 SHALL 要求或保存教师说明
- **AND** 系统 SHALL 保留候选审计记录

### Requirement: 候选审核不得干扰正式标准库浏览
教师端 SHALL 保持正式标准库树状管理空间可用，并通过视图切换进入候选审核。

#### Scenario: 切换视图
- **WHEN** 教师在正式库、待审核和治理视图之间切换
- **THEN** 系统 SHALL 保持当前页面上下文
- **AND** 系统 SHALL NOT 将候选列表混入正式库树节点
