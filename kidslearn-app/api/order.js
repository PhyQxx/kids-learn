import { get, post } from './request'

export const createOrder = (planType, payChannel = 'mock') => post('/order/create', {
  planType,
  payChannel,
})

export const getMyOrders = () => get('/order/my')
