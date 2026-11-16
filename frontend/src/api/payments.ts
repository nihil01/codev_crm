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
  partial: async (id:number, amount:number, method:PaymentMethod):Promise<Payment> =>
    (await api.post(`/payments/${id}/partial`,null,{params:{amount,method}})).data,
  discount: async (id:number, amount:number, reason:string):Promise<Payment> =>
    (await api.put(`/payments/${id}/discount`,null,{params:{amount,reason}})).data,
  freeze: async (id:number,frozen:boolean):Promise<Payment> =>
    (await api.put(`/payments/${id}/freeze`,null,{params:{frozen}})).data,
  transactions: async(id:number):Promise<{id:number;amount:number;paidAt:string;method:string}[]> =>
    (await api.get(`/payments/${id}/transactions`)).data,
  downloadReceipt: async (id:number):Promise<void> => {
    const res=await api.get(`/payments/transactions/${id}/receipt`,{responseType:'blob'});
    const url=URL.createObjectURL(new Blob([res.data],{type:'application/pdf'}));
    const anchor=document.createElement('a');
    anchor.href=url;anchor.download=`odenis-qebzi-${id}.pdf`;anchor.click();
    setTimeout(()=>URL.revokeObjectURL(url),1000);
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
