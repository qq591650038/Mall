import { request } from '@/utils/request'
export interface ShippingTemplate { id?: number; name: string; deliveryMethod: string; regions?: string; baseFreight: number; freeAmount?: number; status: number }
export function getShippingTemplates() { return request<ShippingTemplate[]>({ url: '/shipping/templates', method: 'get' }) }
export function saveShippingTemplate(data: ShippingTemplate) { return request<void>({ url: '/shipping/templates', method: data.id ? 'put' : 'post', data }) }
export function removeShippingTemplate(id: number) { return request<void>({ url: `/shipping/templates/${id}`, method: 'delete' }) }
