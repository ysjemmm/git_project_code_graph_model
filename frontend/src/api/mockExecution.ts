/**
 * Mock 执行流 —— 模拟 SSE 事件，不消耗 token，用于演示 AI 分析进度面板。
 */
import type { BugfixSseEvent } from './index'

function delay(ms: number) {
  return new Promise<void>(r => setTimeout(r, ms))
}

/** 逐 chunk 输出文本，模拟流式打字效果 */
async function streamText(chunks: string[], onEvent: (ev: BugfixSseEvent) => void) {
  for (const chunk of chunks) {
    onEvent({ type: 'text_chunk', content: chunk })
    await delay(chunk.length * 16 + 50)
  }
}

export async function runMockExecution(
  onEvent: (ev: BugfixSseEvent) => void,
  opts: { bugId?: string; bugTitle?: string; projName?: string } = {},
) {
  const bugId    = opts.bugId    ?? 'BUG-???'
  const bugTitle = opts.bugTitle ?? '未知 Bug'
  const proj     = opts.projName ?? 'forward'

  // ── 1. 获取 Bug 详情 ──────────────────────────────────────────────────────
  await delay(600)
  onEvent({
    type: 'tool',
    kind: 'get_bug_detail',
    title: `获取 Bug 详情：#${bugId}`,
    content: [
      `## Bug #${bugId} 详情`,
      `**标题**: ${bugTitle}`,
      '**状态**: 问题确认(状态码=3)',
      '**描述**:',
      '调用接口后服务返回 500，日志显示 NullPointerException，',
      '堆栈定位在 PayServiceImpl.execute:142。',
      '**附件列表**: 无可用附件',
    ].join('\n'),
  })

  // ── 2. 查代码图谱 ──────────────────────────────────────────────────────────
  await delay(900)
  onEvent({
    type: 'tool',
    kind: 'query_code_graph',
    title: '代码图谱查询',
    summary: '查找 PayServiceImpl.execute 方法定义与调用关系',
    content: [
      '🎯 **查询目的**',
      '',
      '查找 PayServiceImpl.execute 方法定义与调用关系',
      '',
      '🔎 **Cypher**',
      '',
      '```cypher',
      "MATCH (c:JavaObject {name:'PayServiceImpl'})-[:MEMBER_OF]->(m:JavaMethod {name:'execute'}) RETURN m",
      '```',
      '',
      '📊 **查询结果**',
      '',
      '```json',
      '{',
      '  "method": "execute",',
      '  "start_line": 138,',
      '  "end_line": 165,',
      `  "full_path": "service/src/main/java/com/${proj}/service/impl/PayServiceImpl.java"`,
      '}',
      '```',
    ].join('\n'),
  })

  // ── 3. 读源码 ──────────────────────────────────────────────────────────────
  await delay(800)
  onEvent({
    type: 'tool',
    kind: 'read_source_file',
    title: 'PayServiceImpl.java:138-165',
    content: [
      '```java',
      `PayServiceImpl.java  (lines 138–165 / 320)`,
      ' 138: public PayResult execute(PayRequest req) {',
      ' 139:     Order order = orderService.findById(req.getOrderId());',
      ' 140:     // ❌ 未做 null 校验直接访问 order.getAmount()',
      ' 141:     BigDecimal amount = order.getAmount();',
      ' 142:     return doCharge(order, amount);  // NPE 发生在此行',
      ' 143: }',
      '```',
    ].join('\n'),
  })

  // ── 4. 分析链路 ────────────────────────────────────────────────────────────
  await delay(400)
  onEvent({
    type: 'analysis_chain' as any,
    steps: [
      {
        type: 'problem',
        label: '拿到问题',
        detail: `Bug #${bugId}：${bugTitle}，服务返回 500，NPE 定位在 PayServiceImpl.execute:142`,
      },
      {
        type: 'step',
        label: '获取 Bug 详情',
        detail: '通过 get_bug_detail 工具拉取 Bug 详情，确认状态为「问题确认」，无附件',
        tool_kind: 'get_bug_detail',
      },
      {
        type: 'step',
        label: '查代码图谱',
        detail: 'Cypher 查询 PayServiceImpl.execute 方法节点，获取 full_path 与行号范围 138–165',
        tool_kind: 'query_code_graph',
      },
      {
        type: 'step',
        label: '读源码',
        detail: '读取 PayServiceImpl.java 第 138–165 行，发现 findById 返回值未做 null 校验',
        tool_kind: 'read_source_file',
      },
      {
        type: 'step',
        label: '结论',
        detail: 'NPE 根因：findById 在订单不存在时返回 null，直接调用 .getAmount() 触发空指针',
      },
    ],
  })

  // ── 5. 流式输出分析结论 ────────────────────────────────────────────────────
  await delay(300)
  await streamText(
    [
      '## 分析结论\n\n',
      '**根本原因**\n\n',
      '`PayServiceImpl.execute` 方法第 139 行调用 `orderService.findById(req.getOrderId())`，',
      '当订单 ID 不存在时该方法返回 `null`，',
      '第 141 行直接访问 `order.getAmount()` 触发 **NullPointerException**。\n\n',
      '**涉及文件**\n\n',
      `- \`service/src/main/java/com/${proj}/service/impl/PayServiceImpl.java\` — 第 139–142 行\n\n`,
      '**修复建议**\n\n',
      '```java\n',
      'public PayResult execute(PayRequest req) {\n',
      '    Order order = orderService.findById(req.getOrderId());\n',
      '    if (order == null) {\n',
      '        throw new BizException("订单不存在：" + req.getOrderId());\n',
      '    }\n',
      '    BigDecimal amount = order.getAmount();\n',
      '    return doCharge(order, amount);\n',
      '}\n',
      '```\n\n',
      '**影响范围**：仅 `PayServiceImpl.execute` 入口，不涉及其他调用链。\n\n',
      '✅ 修复后建议补充单元测试：传入不存在的 orderId 验证异常是否正确抛出。',
    ],
    onEvent,
  )

  // ── 6. done ────────────────────────────────────────────────────────────────
  await delay(200)
  onEvent({ type: 'done', ok: true })
}
