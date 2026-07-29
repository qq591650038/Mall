const TOKEN_KEY = 'mall_admin_token'
const ADMIN_KEY = 'mall_admin_info'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

export function removeToken(): void {
  localStorage.removeItem(TOKEN_KEY)
}

export function getAdmin<T = unknown>(): T | null {
  const str = localStorage.getItem(ADMIN_KEY)
  if (!str) return null
  try {
    return JSON.parse(str) as T
  } catch {
    return null
  }
}

export function setAdmin(admin: unknown): void {
  localStorage.setItem(ADMIN_KEY, JSON.stringify(admin))
}

export function removeAdmin(): void {
  localStorage.removeItem(ADMIN_KEY)
}

export function clearAll(): void {
  removeToken()
  removeAdmin()
}
