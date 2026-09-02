import {
  deleteJson,
  postDownload,
  type DownloadedFile,
} from '../api/api-client.ts'

export async function exportAccountData(
  currentPassword: string,
): Promise<DownloadedFile> {
  return postDownload(
    '/account/export',
    { currentPassword },
    'itineclair-export.zip',
  )
}

export async function deleteAccount(
  currentPassword: string,
  confirmationEmail: string,
): Promise<void> {
  await deleteJson('/account', {
    currentPassword,
    confirmationEmail,
  })
}
