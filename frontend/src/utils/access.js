import { getLoginUser } from '../api/user'

export const ROLE = {
  USER: 'user',
  ADMIN: 'admin',
}

export const ROLE_LABEL = {
  [ROLE.USER]: '普通用户',
  [ROLE.ADMIN]: '管理员',
}

export function checkAccess(loginUser, requiredRole) {
  if (!loginUser) return false
  if (!requiredRole) return true
  if (loginUser.userRole === ROLE.ADMIN) return true
  return loginUser.userRole === requiredRole
}

export function canAccess(loginUser, requiredRole) {
  return checkAccess(loginUser, requiredRole)
}

export async function routeGuard(to, from, next, loginUserRef) {
  let loginUser = loginUserRef?.value
  if (!loginUser) {
    try {
      const res = await getLoginUser()
      loginUser = res.data
      if (loginUserRef) loginUserRef.value = loginUser
    } catch {
      return next({ path: '/login', query: { redirect: to.fullPath } })
    }
  }

  const { requiredRole } = to.meta || {}
  if (requiredRole && !checkAccess(loginUser, requiredRole)) {
    return next({ path: '/403' })
  }

  next()
}
