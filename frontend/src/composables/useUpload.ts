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
    clearAfterSend,
  }
}
