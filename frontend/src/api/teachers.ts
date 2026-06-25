import api from './client';
import { Teacher } from '../types';

export const teachersApi = {
  getAll: async (): Promise<Teacher[]> => {
    const { data } = await api.get('/teachers');
    return data;
  },
  getActive: async (): Promise<Teacher[]> => {
    const { data } = await api.get('/teachers/active');
    return data;
  },
  getById: async (id: number): Promise<Teacher> => {
    const { data } = await api.get(`/teachers/${id}`);
    return data;
  },
  create: async (teacher: Partial<Teacher>): Promise<Teacher> => {
    const { data } = await api.post('/teachers', teacher);
    return data;
  },
  update: async (id: number, teacher: Partial<Teacher>): Promise<Teacher> => {
    const { data } = await api.put(`/teachers/${id}`, teacher);
    return data;
  },
  archive: async (id: number): Promise<void> => {
    await api.put(`/teachers/${id}/archive`);
  },
  delete: async (id: number): Promise<void> => {
    await api.delete(`/teachers/${id}`);
  },
};
