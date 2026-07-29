import { request } from '@/utils/request'

export interface ShippingTemplate {
  id: number
  name: string
  deliveryMethod: string
  regions?: string
  baseFreight: number
  freeAmount?: number
  status: number
}

export function getShippingTemplates() {
  return request<ShippingTemplate[]>({ url: '/shipping/templates', method: 'get' })
}
