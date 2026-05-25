import { get } from './request'

export const getSubscriptionPlans = () => get('/subscription/plans')

export const getCurrentSubscription = () => get('/subscription/current')
