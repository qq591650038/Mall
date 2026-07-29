/**
 * 生成支付回调 HMAC-SHA256 签名。
 * 该方法仅用于本地模拟支付；生产环境不应把支付密钥暴露给浏览器。
 */
export async function signPaymentCallback(payload: string): Promise<string> {
  const secret = import.meta.env.VITE_PAYMENT_CALLBACK_SECRET || 'change-me-in-production'
  const encoder = new TextEncoder()
  const key = await crypto.subtle.importKey(
    'raw',
    encoder.encode(secret),
    { name: 'HMAC', hash: 'SHA-256' },
    false,
    ['sign']
  )
  const signature = await crypto.subtle.sign('HMAC', key, encoder.encode(payload))
  return Array.from(new Uint8Array(signature))
    .map(byte => byte.toString(16).padStart(2, '0'))
    .join('')
}
