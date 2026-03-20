export type SecondPartyRule = {
  id: number
  enabled: boolean
  sort_order: number
  group_id_regex: string
  artifact_id_regex: string
  target_project_name: string
  created_at: string
  updated_at: string
}

export type SecondPartyRuleListResponse = {
  ok: boolean
  items: SecondPartyRule[]
}

export type SecondPartyRuleResponse = {
  ok: boolean
  message?: string
  item?: SecondPartyRule | null
}

export async function fetchSecondPartyRules(): Promise<SecondPartyRuleListResponse> {
  const resp = await fetch('/api/second-party-rules')
  if (!resp.ok) throw new Error(`HTTP ${resp.status}`)
  return (await resp.json()) as SecondPartyRuleListResponse
}

export async function createSecondPartyRule(payload: Omit<SecondPartyRule, 'id' | 'created_at' | 'updated_at'>) {
  const resp = await fetch('/api/second-party-rules', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
  if (!resp.ok) throw new Error(`HTTP ${resp.status}`)
  return (await resp.json()) as SecondPartyRuleResponse
}

export async function updateSecondPartyRule(ruleId: number, payload: Omit<SecondPartyRule, 'id' | 'created_at' | 'updated_at'>) {
  const resp = await fetch(`/api/second-party-rules/${ruleId}`, {
    method: 'PUT',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(payload),
  })
  if (!resp.ok) throw new Error(`HTTP ${resp.status}`)
  return (await resp.json()) as SecondPartyRuleResponse
}

export async function deleteSecondPartyRule(ruleId: number) {
  const resp = await fetch(`/api/second-party-rules/${ruleId}`, { method: 'DELETE' })
  if (!resp.ok) throw new Error(`HTTP ${resp.status}`)
  return (await resp.json()) as { ok: boolean; message?: string; deleted_id?: number }
}

