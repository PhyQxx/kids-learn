import { get, post, put } from './request'

// 登录
export const login = (data) => post('/auth/login', data)

// 注册
export const register = (data) => post('/auth/register', data)

// 发送验证码
export const sendVerifyCode = (phone, purpose = 'REGISTER') => post('/auth/send-code', { phone, purpose })
export const sendForgotPasswordCode = (phone) => post('/auth/forgot-password/code', { phone })
export const resetForgottenPassword = (phone, code, newPassword) => post('/auth/forgot-password/reset', { phone, code, newPassword })

// 刷新Token
export const refreshToken = (refreshToken) => post('/auth/refresh-token', { refreshToken })

// 验证当前账号密码
export const verifyPassword = (password) => post('/user/verify-password', { password })

export const getParentPinStatus = () => get('/user/parent-pin/status')
export const setupParentPin = (password, pin) => post('/user/parent-pin/setup', { password, pin })
export const verifyParentPin = (pin) => post('/user/parent-pin/verify', { pin })
export const changeParentPin = (currentPin, newPin) => put('/user/parent-pin', { currentPin, newPin })

// 登出
export const logout = () => post('/auth/logout')
