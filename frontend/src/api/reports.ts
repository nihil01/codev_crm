import api from './client';
import { ReportData } from '../types';

export const reportsApi = {
  getMonthly: async (year: number, month: number): Promise<ReportData> => {
    const { data } = await api.get(`/reports/monthly/year/${year}/month/${month}`);
    return data;
  },
};
