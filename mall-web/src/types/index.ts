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

export interface LoginRequest {
  account: string
  loginType?: number
  password: string
  verifyKey?: string
  verifyCode?: string
}

export interface RegisterRequest {
  username: string
  phone?: string
  email?: string
  password: string
  confirmPassword: string
  verifyKey: string
  verifyCode: string
}

export interface LoginVO {
  token: string
  expiresIn: number
  userInfo: {
    id: number
    username: string
    phone?: string
    email?: string
    avatar?: string
    nickname?: string
  }
}

export interface UserVO {
  id: number
  username: string
  phone?: string
  email?: string
  avatar?: string
  nickname?: string
  gender?: number
  status: number
  lastLoginIp?: string
  lastLoginTime?: string
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
  isRecommend?: number
  description?: string
  categoryId: number
  brandId?: number
  reviewCount?: number
  averageRating?: number
}

export interface ProductSku {
  id: number
  productId: number
  skuCode?: string
  specInfo: string
  price: number
  stock: number
  image?: string
}

export interface ProductImage {
  id: number
  productId: number
  url: string
  sort?: number
}

export interface ProductDetailVO {
  id: number
  name: string
  subtitle?: string
  mainImage?: string
  price: number
  originalPrice?: number
  totalStock: number
  sales: number
  status: number
  isRecommend?: number
  description?: string
  categoryId: number
  categoryName?: string
  brandId?: number
  brandName?: string
  images: string[]
  skus: ProductSkuVO[]
}

export interface ProductSkuVO {
  id: number
  skuCode?: string
  specInfo: string
  price: number
  stock: number
  image?: string
}

export interface Category {
  id: number
  name: string
  parentId?: number
  sort?: number
  icon?: string
}

export interface Brand {
  id: number
  name: string
  logo?: string
  description?: string
}

export interface CartVO {
  id: number
  productId: number
  skuId: number
  productName: string
  productImage?: string
  skuInfo?: string
  price: number
  quantity: number
  selected: number
  stock?: number
  outOfStock?: boolean
}

export interface Region {
  id: number
  parentId: number
  name: string
  level: number
  sort?: number
}

export interface Address {
  id?: number
  userId: number
  receiverName: string
  receiverPhone: string
  province: string
  city: string
  district: string
  detailAddress: string
  isDefault?: number
}

export interface CreateOrderDTO {
  addressId: number
  remark?: string
  items: OrderItemDTO[]
  couponId?: number
}

export interface OrderItemDTO {
  productId: number
  skuId: number
  quantity: number
}

export interface OrderVO {
  id: number
  orderNo: string
  totalAmount: number
  discountAmount: number
  freightAmount: number
  payAmount: number
  payStatus: number
  payTime?: string
  orderStatus: number
  orderStatusText: string
  shipTime?: string
  receiveTime?: string
  remark?: string
  createTime?: string
  addressSnapshot?: {
    receiverName: string
    receiverPhone: string
    province: string
    city: string
    district: string
    detailAddress: string
  }
  items: OrderItemVO[]
  logisticsCompany?: string
  logisticsNo?: string
  autoConfirmDeadline?: string
  expireTime?: string
  timeoutHours?: number
  timeline?: OrderTimelineVO[]
}

export interface OrderTimelineVO {
  status: number
  statusText: string
  time?: string
  description?: string
}

export interface OrderItemVO {
  id: number
  productId: number
  skuId: number
  productName: string
  skuInfo?: string
  productImage?: string
  price: number
  quantity: number
  subtotal: number
}

export interface RefundVO {
  id: number
  orderId: number
  orderNo: string
  refundNo: string
  amount: number
  reason?: string
  images?: string
  status: number
  statusText: string
  reviewRemark?: string
  reviewTime?: string
  paymentNo?: string
  createTime?: string
  orderInfo?: OrderVO
  type?: number
  logisticsCompany?: string
  logisticsNo?: string
  returnAddress?: string
  trackingNo?: string
}

export type RefundStatus = 0 | 1 | 2 | 3 | 4

export interface PayResultVO {
  orderId: number
  orderNo: string
  paymentNo: string
  amount: number
  paymentMethod: number
  paymentStatus: number
  expireSeconds: number
  payUrl: string
}

export interface Banner {
  id: number
  title: string
  imageUrl: string
  linkUrl?: string
  sort?: number
  status: number
}

export interface Coupon {
  id: number
  name: string
  type: number
  value: number
  minAmount?: number
  totalCount: number
  remainCount: number
  startTime: string
  endTime: string
  description?: string
}

export interface UserCoupon {
  id: number
  couponId: number
  status: number
  receiveTime: string
  name: string
  type: number
  value: number
  minAmount?: number
  startTime: string
  endTime: string
}

export interface UsableCoupon {
  id: number
  couponId: number
  name: string
  type: number
  value: number
  minAmount?: number
  status: number
  receiveTime: string
  startTime?: string
  endTime?: string
}

export interface Review {
  id: number
  productId: number
  userId: number
  rating: number
  content: string
  images?: string
  status: number
  reply?: string
  replyTime?: string
  createTime?: string
  parentId?: number
  replyStatus?: number
  children?: Review[]
  productName?: string
}

export interface Favorite {
  id: number
  userId: number
  productId: number
  groupId?: number | null
  originalPrice?: number | null
  priceAlert?: number
  stockAlert?: number
  lastPrice?: number | null
  lastStock?: number | null
  product?: Product
  createTime?: string
  updateTime?: string
}

export interface FavoriteGroup {
  id: number
  userId: number
  name: string
  sort?: number
  createTime?: string
  updateTime?: string
}

export interface BrowseHistory {
  id: number
  userId: number
  productId: number
  browseTime?: string
  product?: Product
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
    remainingStock?: number
  limitPerUser?: number
  productName?: string
  productImage?: string
  soldCount?: number
}

export interface MarketingParticipateResult {
  participantId: number
  activityId: number
  itemId: number
  orderId: number
  orderNo: string
  quantity: number
  status: number
}

export type OrderStatus = 0 | 1 | 2 | 3 | 4
