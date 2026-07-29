import { request } from '@/utils/request'
export function getPrivacyDocuments() { return request<Record<string, string>>({ url: '/privacy/documents', method: 'get' }) }
export function exportPersonalData() { return request<Record<string, unknown>>({ url: '/privacy/export', method: 'get' }) }
export function closeAccount() { return request<void>({ url: '/privacy/account', method: 'delete' }) }
