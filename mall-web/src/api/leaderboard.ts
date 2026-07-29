import { request } from '@/utils/request'

export interface UserLeaderboardEntry {
  rank: number
  nickname: string
  avatar?: string
  value: number
}

export interface ProductLeaderboardEntry {
  rank: number
  productId: number
  name: string
  image?: string
  price: number
  sales: number
}

export interface Leaderboard {
  points: UserLeaderboardEntry[]
  spending: UserLeaderboardEntry[]
  products: ProductLeaderboardEntry[]
}

export function getLeaderboard() {
  return request<Leaderboard>({ url: '/leaderboard', method: 'get' })
}
