import request from '../utils/request'

export function uploadFile(file) {
  const formData = new FormData()
  formData.append('file', file)
  return request.post('/file/upload', formData, {
    timeout: 60000,
  })
}

export function getDownloadUrl(fileKey) {
  return `/api/file/download?fileKey=${encodeURIComponent(fileKey)}`
}
