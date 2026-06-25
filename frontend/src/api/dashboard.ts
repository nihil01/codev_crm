import api from './client';
import { DashboardData } from '../types';

export const dashboardApi = {
  get: async (): Promise<DashboardData> => {
    const { data } = await api.get('/dashboard');
    return data;
  },
};
