import { get, post } from './request'

// 登录
export const login = (data) => post('/auth/login', data)

// 注册
export const register = (data) => post('/auth/register', data)

// 刷新Token
export const refreshToken = (refreshToken) => post('/auth/refresh-token?refreshToken=' + refreshToken)

// 登出
export const logout = () => post('/auth/logout')
