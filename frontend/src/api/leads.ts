import api from './client';
import type { Lead } from '../types';

export const leadsApi = {
  convert: async(id:number):Promise<{studentId:number}> => (await api.post(`/leads/${id}/convert`)).data,
  getAll: async (): Promise<Lead[]> => (await api.get('/leads')).data,
  create: async (lead: Partial<Lead>): Promise<Lead> => (await api.post('/leads', lead)).data,
  update: async (id: number, lead: Partial<Lead>): Promise<Lead> => (await api.put(`/leads/${id}`, lead)).data,
  delete: async (id: number): Promise<void> => { await api.delete(`/leads/${id}`); },
};
