/**
 * 统一云函数调度 API。
 *
 * 所有对第三方系统的请求，统一通过 POST /api/invoke 发送：
 *   { client: "forward", function: "listOnlineBugs", params: { ... } }
 *
 * 后端负责路由到对应 client 和函数并执行，前端无需关心认证细节。
 */

// ─── 核心调度类型 ──────────────────────────────────────────────────────────────

export interface InvokeRequest {
  client: string
  function: string
  params?: Record<string, unknown>
}

export interface InvokeResponse<T = unknown> {
  ok: boolean
  data?: T
  error?: string
}

// ─── 业务类型（forward 系统） ──────────────────────────────────────────────────

export interface ForwardBugItem {
  id: number
  name: string
  status?: number | null
  statusName?: string | null
  priorityName?: string | null
  proposer?: string | null
  proposerId?: string | null
  operator?: string | null
  operatorId?: string | null
  envName?: string | null
  belongName?: string | null
  categoryName?: string | null
  sourceName?: string | null
  createDate?: string | null
  modifyDate?: string | null
  productLineNames?: string[]
  bizDomainNames?: string[]
  describe?: string | null
  slaRemainHours?: number | null
}

export interface BugListData {
  total: number
  page: number
  page_size: number
  items: ForwardBugItem[]
}

export interface BugListParams {
  name?: string
  status?: number[]
  page?: number
  page_size?: number
  ascription?: string
  product_line_ids?: number[]
  biz_domain_ids?: number[]
}

// ─── 核心调度函数 ──────────────────────────────────────────────────────────────

/**
 * 调用后端云函数。
 *
 * @param client   客户端名称，如 "forward"
 * @param fn       云函数名称，如 "listOnlineBugs"
 * @param params   传给函数的参数（可选）
 * @param token    Bearer Token（可选，通过 X-Token 请求头传递）
 */
export async function invoke<T = unknown>(
  client: string,
  fn: string,
  params: Record<string, unknown> = {},
  token?: string,
): Promise<InvokeResponse<T>> {
  const headers: Record<string, string> = { 'Content-Type': 'application/json' }
  if (token) headers['X-Token'] = token

  const r = await fetch('/api/invoke', {
    method: 'POST',
    headers,
    body: JSON.stringify({ client, function: fn, params }),
  })
  const data = await r.json().catch(() => ({}))
  if (!r.ok) {
    const detail = (data as any)?.detail ?? (data as any)?.message ?? `HTTP ${r.status}`
    return { ok: false, error: detail }
  }
  return data as InvokeResponse<T>
}

// ─── forward 系统便捷封装 ──────────────────────────────────────────────────────

/**
 * 分页查询线上 Bug 列表。
 */
export async function listOnlineBugs(
  params: BugListParams = {},
  token?: string,
): Promise<InvokeResponse<BugListData>> {
  return invoke<BugListData>('forward', 'listOnlineBugs', params as Record<string, unknown>, token)
}

/**
 * 按标题关键词搜索线上 Bug。
 */
export async function searchOnlineBugs(
  q: string,
  page = 1,
  pageSize = 20,
  token?: string,
): Promise<InvokeResponse<BugListData>> {
  return invoke<BugListData>('forward', 'searchOnlineBugs', { q, page, page_size: pageSize }, token)
}

/**
 * 查询线上 Bug 详情。
 */
export async function getOnlineBug(
  id: number,
  token?: string,
): Promise<InvokeResponse<Record<string, unknown>>> {
  return invoke('forward', 'getOnlineBug', { id }, token)
}

export interface UserInfo {
  id?: string        // 工号，如 "mayang"
  userId?: string    // 系统 userId
  name?: string      // 真实姓名，如 "杨思俊"
  alias?: string     // 昵称，用于界面展示，如 "码扬"
  mail?: string
  mobile?: string
  workNum?: string
  job?: string
  env?: string
  hireDate?: string
}

/**
 * 获取当前登录用户信息（依赖 CAS Cookie 认证）。
 */
export async function getUserInfo(): Promise<InvokeResponse<UserInfo>> {
  return invoke<UserInfo>('forward', 'getUserInfo')
}
