import { request } from '@/utils/request'

export interface Role { id: number; name: string; code: string; description?: string }
export interface Permission { id: number; name: string; code: string; type?: number }

export const getRoles = () => request<Role[]>({ url: '/access/roles', method: 'get' })
export const getPermissions = () => request<Permission[]>({ url: '/access/permissions', method: 'get' })
export const createRole = (data: Partial<Role>) => request<void>({ url: '/access/roles', method: 'post', data })
export const updateRole = (id: number, data: Partial<Role>) => request<void>({ url: `/access/roles/${id}`, method: 'put', data })
export const deleteRole = (id: number) => request<void>({ url: `/access/roles/${id}`, method: 'delete' })
export const grantPermission = (roleId: number, permissionId: number) => request<void>({ url: `/access/roles/${roleId}/permissions/${permissionId}`, method: 'post' })
export const revokePermission = (roleId: number, permissionId: number) => request<void>({ url: `/access/roles/${roleId}/permissions/${permissionId}`, method: 'delete' })
