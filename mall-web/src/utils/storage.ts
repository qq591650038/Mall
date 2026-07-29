const TOKEN_KEY = 'mall_token'
const USER_KEY = 'mall_user'

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY)
}

export function setToken(token: string): void {
  localStorage.setItem(TOKEN_KEY, token)
}

export function removeToken(): void {
  localStorage.removeItem(TOKEN_KEY)
}

export function getUser<T = unknown>(): T | null {
  const str = localStorage.getItem(USER_KEY)
  if (!str) return null
  try {
    return JSON.parse(str) as T
  } catch {
    return null
  }
}

export function setUser(user: unknown): void {
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function removeUser(): void {
  localStorage.removeItem(USER_KEY)
}

export function clearAll(): void {
  removeToken()
  removeUser()
}
