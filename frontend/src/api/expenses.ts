import api from './client';
import { Expense, ExpenseType } from '../types';

export interface RecurringRule {
  id:number;type:ExpenseType;amount:number;description?:string;adChannel?:string;
  startYear:number;startMonth:number;active:boolean;
}

export const expensesApi = {
  getRules: async():Promise<RecurringRule[]> => (await api.get('/expenses/rules')).data,
  createRule: async(rule:Omit<RecurringRule,'id'>):Promise<RecurringRule> =>
    (await api.post('/expenses/rules',rule)).data,
  updateRule: async(rule:RecurringRule):Promise<RecurringRule> =>
    (await api.put(`/expenses/rules/${rule.id}`,rule)).data,
  activateRule: async(id:number,active:boolean):Promise<RecurringRule> =>
    (await api.put(`/expenses/rules/${id}/active`,null,{params:{active}})).data,
  restoreMonth: async(id:number):Promise<Expense> =>
    (await api.put(`/expenses/${id}/restore`)).data,
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
