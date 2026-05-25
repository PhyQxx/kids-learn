import { get, post } from './request'

// 登录
export const login = (data) => post('/auth/login', data)

// 注册
export const register = (data) => post('/auth/register', data)

// 刷新Token
export const refreshToken = (refreshToken) => post('/auth/refresh-token', { refreshToken })

// 验证当前账号密码
export const verifyPassword = (password) => post('/user/verify-password', { password })

// 登出
export const logout = () => post('/auth/logout')
