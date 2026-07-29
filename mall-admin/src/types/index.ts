export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

export interface PageResult<T> {
  total: number
  list: T[]
  page: number
  size: number
  totalPages: number
}

export interface AdminLoginRequest {
  username: string
  password: string
}

export interface AdminLoginVO {
  token: string
  expiresIn: number
  adminInfo: {
    id: number
    username: string
    nickname?: string
    avatar?: string
    roleIds?: number[]
  }
}

export interface AdminInfoVO {
  id: number
  username: string
  nickname?: string
  avatar?: string
  email?: string
  phone?: string
  roleIds?: number[]
  status: number
  createTime?: string
}

export interface Product {
  id: number
  name: string
  subtitle?: string
  mainImage?: string
  price: number
  originalPrice?: number
  totalStock: number
  sales: number
  status: number
  categoryId: number
  categoryName?: string
  brandId?: number
  brandName?: string
  description?: string
  createTime?: string
  skus?: ProductSku[]
}

export interface ProductSku {
  id?: number
  productId?: number
  skuCode?: string
  specInfo: string
  price: number
  stock: number
  image?: string
  status?: number
}

export interface ProductImage {
  id: number
  productId: number
  url: string
  sort?: number
}

export interface Category {
  id: number
  name: string
  parentId?: number
  sort?: number
  icon?: string
  children?: Category[]
}

export interface Brand {
  id: number
  name: string
  logo?: string
  description?: string
  sort?: number
  status?: number
  createTime?: string
}

export interface OrderVO {
  id: number
  orderNo: string
  addressSnapshot?: string | { receiverName?: string; receiverPhone?: string; province?: string; city?: string; district?: string; detailAddress?: string }
  freightAmount?: number
  discountAmount?: number
  userId: number
  username?: string
  totalAmount: number
  payAmount: number
  orderStatus: number
  orderStatusText: string
  payStatus: number
  createTime?: string
  updateTime?: string
  payTime?: string
  shipTime?: string
  receiveTime?: string
  receiverName?: string
  receiverPhone?: string
  remark?: string
  logisticsCompany?: string
  logisticsNo?: string
  items?: OrderItemVO[]
  timeline?: OrderTimelineItem[]
}

export interface OrderTimelineItem {
  status: number
  title?: string
  statusText?: string
  description?: string
  time?: string
}

export interface OrderItemVO {
  id: number
  productId: number
  productName: string
  skuId?: number
  skuInfo?: string
  price: number
  quantity: number
  subtotal: number
  productImage?: string
}

export interface RefundVO {
  id: number
  refundNo?: string
  orderNo?: string
  username?: string
  amount: number
  reason?: string
  status: number
  createTime?: string
  type?: number
  logisticsCompany?: string
  logisticsNo?: string
  returnAddress?: string
  trackingNo?: string
  exchangeProductId?: number
  exchangeSkuId?: number
  reviewRemark?: string
  reviewTime?: string
}
export interface InventoryLog { id: number; productId?: number; skuId?: number; quantity: number; operation: string; status: number; errorMessage?: string; retryCount?: number; createTime?: string }

export interface Banner {
  id: number
  title?: string
  imageUrl: string
  linkUrl?: string
  sort?: number
  status: number
  startTime?: string
  endTime?: string
  createTime?: string
}

export interface Coupon {
  id: number
  name: string
  type: number
  value: number
  minAmount?: number
  totalCount: number
  remainCount: number
  startTime?: string
  endTime?: string
  status: number
  description?: string
  createTime?: string
}

export interface AdminProfile {
  id: number
  username: string
  realName?: string
  avatar?: string
  email?: string
  phone?: string
  status: number
  lastLoginIp?: string
  lastLoginTime?: string
  createTime?: string
}

export interface Review {
  id: number
  productId: number
  productName?: string
  userId: number
  username?: string
  rating: number
  content: string
  status: number
  reply?: string
  createTime?: string
}

export interface DashboardStats {
  totalProducts: number
  totalOrders: number
  totalUsers: number
  totalSales: number
  todayOrders: number
  todaySales: number
  lowStockProducts: number
  pendingReviews: number
  recentOrders: OrderVO[]
  salesTrend: { date: string; amount: number }[]
  hotProducts: { id: number; name: string; sales: number }[]
  conversionRate: number
  userTrend: { date: string; newUsers: number; activeUsers: number }[]
  stockWarnings: { id: number; name: string; stock: number; skuInfo?: string }[]
  refundRate: number
  avgOrderAmount: number
  totalRefundAmount: number
  todayRefundCount: number
  todayRefundAmount: number
  categoryHotProducts: {
    categoryId: number
    categoryName: string
    productName: string
    sales: number
    price: number
  }[]
}

export interface MarketingActivity {
  id: number
  name: string
  type: string
  description?: string
  startTime?: string
  endTime?: string
  status: number
  sort?: number
  createdBy?: number
  createTime?: string
  itemCount?: number
  items?: MarketingActivityItem[]
  groupTarget?: number
}

export interface MarketingActivityItem {
  id?: number
  activityId?: number
  productId: number
  skuId?: number
  activityPrice: number
  originalPrice: number
  stock: number
  limitPerUser?: number
  productName?: string
  productImage?: string
  soldCount?: number
}

export interface MarketingGroupMember {
  participantId: number
  userId: number
  username?: string
  quantity: number
  orderId?: number
  orderNo?: string
  participantStatus?: number
  orderStatus?: number
  orderStatusText?: string
  payStatus?: number
}

export interface MarketingGroup {
  groupNo: string
  target?: number
  joinedQuantity: number
  groupStatus: number
  members: MarketingGroupMember[]
}

export interface MarketingActivityCreateRequest {
  name: string
  type: string
  description?: string
  startTime: string
  endTime: string
  sort?: number
  groupTarget?: number
  items: MarketingActivityItem[]
}

export interface AfterSale {
  id: number
  afterSaleNo: string
  orderNo: string
  userId: number
  username?: string
  type: number
  reason?: string
  amount: number
  status: number
  logisticsCompany?: string
  logisticsNo?: string
  createTime?: string
  updateTime?: string
  items?: AfterSaleItem[]
}

export interface AfterSaleItem {
  id: number
  productName: string
  skuInfo?: string
  quantity: number
  price: number
}

export interface MemberLevel {
  id?: number
  name: string
  level: number
  minPoints: number
  maxPoints: number
  pointsRate: number
  discountRate: number
  icon?: string
  description?: string
  status: number
  sort?: number
  createTime?: string
  updateTime?: string
}
