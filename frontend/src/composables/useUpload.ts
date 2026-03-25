import { ref } from 'vue'
import type { RepoItem } from '../types'
import { uploadFiles } from '../api'

export function useUpload(
  getRepo: () => RepoItem | undefined,
  getUploadRef: () => string,
) {
  const uploadFileList = ref<any[]>([])
  const uploading = ref(false)
  const uploadResult = ref<string | null>(null)
  const sessionUploadedFileNames = ref<string[]>([])

  async function customUpload(opt: {
    file: any
    onSuccess?: () => void
    onError?: (e: Error) => void
  }) {
    const { file, onSuccess, onError } = opt
    const repo = getRepo()
    const refVal = getUploadRef()
    if (!repo?.name || !refVal) {
      onError?.(new Error('请先选择仓库与 Branch 或 CommitId'))
      return
    }
    uploading.value = true
    uploadResult.value = null
    try {
      const data = await uploadFiles(repo.name, refVal, file.originFileObj ?? file)
      const names = (data.saved ?? []).map((x: any) => x.filename).filter(Boolean)
      sessionUploadedFileNames.value.push(...names)
      uploadResult.value = null
      uploadFileList.value = uploadFileList.value.filter((f) => f.uid !== file.uid)
      onSuccess?.()
    } catch (e: any) {
      onError?.(e)
      throw e
    } finally {
      uploading.value = false
    }
  }

  /**
   * 批量上传当前 uploadFileList 中的文件到指定 session 目录。
   * 返回上传成功的文件名列表（供 SSE 接口 uploaded_file_names 使用）。
   */
  async function batchUploadForSession(sessionId: string): Promise<string[]> {
    const repo = getRepo()
    const refVal = getUploadRef()
    if (!uploadFileList.value.length) return []
    uploading.value = true
    const uploadedNames: string[] = []
    try {
      for (const f of uploadFileList.value) {
        const rawFile: File = f.originFileObj ?? f
        const projName = repo?.name ?? 'unknown'
        const refStr = refVal || 'unknown'
        try {
          const data = await uploadFiles(projName, refStr, rawFile, sessionId)
          const names = (data.saved ?? []).map((x: any) => x.filename).filter(Boolean)
          uploadedNames.push(...names)
        } catch (e) {
          console.warn(`上传文件失败：${rawFile.name}`, e)
        }
      }
    } finally {
      uploading.value = false
    }
    return uploadedNames
  }

  function clearAfterSend() {
    sessionUploadedFileNames.value = []
    uploadFileList.value = []
    uploadResult.value = null
  }

  return {
    uploadFileList,
    uploading,
    uploadResult,
    sessionUploadedFileNames,
    customUpload,
    batchUploadForSession,
    clearAfterSend,
  }
}
