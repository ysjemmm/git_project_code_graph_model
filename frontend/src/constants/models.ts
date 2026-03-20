export const modelOptions = [
  { label: 'DeepSeek', value: 'deepseek' },
  { label: 'Claude-Haiku-4.5-20251001', value: 'claude::claude-haiku-4-5-20251001' },
  { label: 'Claude-Opus-4.5-20251101-thinking', value: 'claude::claude-opus-4-5-20251101-thinking' },
  { label: 'Claude-Opus-4.6', value: 'claude::claude-opus-4-6' },
  { label: 'Claude-Opus-4.6-thinking', value: 'claude::claude-opus-4-6-thinking' },
  { label: 'Claude-Sonnet-4.5-20250929', value: 'claude::claude-sonnet-4-5-20250929' },
  { label: 'Claude-Sonnet-4.5-20250929-thinking', value: 'claude::claude-sonnet-4-5-20250929-thinking' },
  { label: 'Claude-Sonnet-4.6', value: 'claude::claude-sonnet-4-6' },
] as const

export function parseModelChoice(
  v: string,
): { provider: 'deepseek' | 'claude'; model?: string } {
  if (!v || v === 'deepseek') return { provider: 'deepseek' }
  if (v.startsWith('claude::'))
    return { provider: 'claude', model: v.slice('claude::'.length) || undefined }
  return { provider: v === 'claude' ? 'claude' : 'deepseek' }
}
