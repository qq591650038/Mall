import { request } from '@/utils/request'
import type { MemberLevel, PageResult } from '@/types'

// 分页获取会员等级
export function getMemberLevelPage(current: number = 1, size: number = 10, status?: number) {
  return request<PageResult<MemberLevel>>({
    url: '/member-levels/page',
    method: 'get',
    params: { current, size, ...(status !== undefined ? { status } : {}) }
  })
}

// 获取会员等级详情
export function getMemberLevelDetail(id: number) {
  return request<MemberLevel>({
    url: `/member-levels/${id}`,
    method: 'get'
  })
}

// 创建会员等级
export function createMemberLevel(data: Partial<MemberLevel>) {
  return request<MemberLevel>({
    url: '/member-levels',
    method: 'post',
    data
  })
}

// 更新会员等级
export function updateMemberLevel(id: number, data: Partial<MemberLevel>) {
  return request<MemberLevel>({
    url: `/member-levels/${id}`,
    method: 'put',
    data
  })
}

// 删除会员等级
export function deleteMemberLevel(id: number) {
  return request<void>({
    url: `/member-levels/${id}`,
    method: 'delete'
  })
}
