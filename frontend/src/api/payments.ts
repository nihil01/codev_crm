import api from './client';
import { Payment, PaymentMethod, PaymentStatus } from '../types';

export const paymentsApi = {
  getAll: async (): Promise<Payment[]> => {
    const { data } = await api.get('/payments');
    return data;
  },
  getByStudent: async (studentId: number): Promise<Payment[]> => {
    const { data } = await api.get(`/payments/student/${studentId}`);
    return data;
  },
  getByGroup: async (groupId: number): Promise<Payment[]> => {
    const { data } = await api.get(`/payments/group/${groupId}`);
    return data;
  },
  getByGroupAndPeriod: async (groupId: number, year: number, month: number): Promise<Payment[]> => {
    const { data } = await api.get(`/payments/group/${groupId}/year/${year}/month/${month}`);
    return data;
  },
  getByPeriod: async (year: number, month: number): Promise<Payment[]> => {
    const { data } = await api.get(`/payments/year/${year}/month/${month}`);
    return data;
  },
  getOverdue: async (year: number, month: number): Promise<Payment[]> => {
    const { data } = await api.get(`/payments/overdue/year/${year}/month/${month}`);
    return data;
  },
  recordPayment: async (id: number, method: PaymentMethod): Promise<Payment> => {
    const { data } = await api.put(`/payments/${id}/pay?method=${method}`);
    return data;
  },
  updateStatus: async (id: number, status: PaymentStatus): Promise<Payment> => {
    const { data } = await api.put(`/payments/${id}/status?status=${status}`);
    return data;
  },
  create: async (payment: Partial<Payment>): Promise<Payment> => {
    const { data } = await api.post('/payments', payment);
    return data;
  },
  delete: async (id: number): Promise<void> => {
    await api.delete(`/payments/${id}`);
  },
};
