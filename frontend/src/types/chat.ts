import type { AnalysisChainStep, ToolStep } from '../api'

/** 单条消息内的交错段落：一句文字后紧跟这句用到的 tool，按事件顺序 */
export type ChatMsgSegment =
  | { type: 'text'; content: string }
  | { type: 'tool'; steps: ToolStep[] }

export interface ChatMsg {
  role: 'user' | 'ai'
  content: string
  toolSteps?: ToolStep[]
  /**
   * 交错段落：按「说一句 → 显示这句用的 tool」顺序，有则优先用此渲染；
   * 无则回退为 content + toolSteps（整段文字 + 整块工具）
   */
  segments?: ChatMsgSegment[]
  /** 分析链路：拿到问题 → 查图 → 查文件 → …，展示在工具步骤与总结之间 */
  analysisChain?: AnalysisChainStep[]
  /** 是否正在流式输出中（显示光标） */
  streaming?: boolean
}
