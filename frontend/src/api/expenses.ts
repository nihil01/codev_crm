import api from './client';
import { Expense } from '../types';

export const expensesApi = {
  getByPeriod: async (year: number, month: number): Promise<Expense[]> => {
    const { data } = await api.get(`/expenses/year/${year}/month/${month}`);
    return data;
  },
  getRecurring: async (): Promise<Expense[]> => {
    const { data } = await api.get('/expenses/recurring');
    return data;
  },
  create: async (expense: Partial<Expense>): Promise<Expense> => {
    const { data } = await api.post('/expenses', expense);
    return data;
  },
  update: async (id: number, expense: Partial<Expense>): Promise<Expense> => {
    const { data } = await api.put(`/expenses/${id}`, expense);
    return data;
  },
  delete: async (id: number): Promise<void> => {
    await api.delete(`/expenses/${id}`);
  },
};
