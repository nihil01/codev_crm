import api from './client';
import { Student } from '../types';

export const studentsApi = {
  getAll: async (): Promise<Student[]> => {
    const { data } = await api.get('/students');
    return data;
  },
  getActive: async (): Promise<Student[]> => {
    const { data } = await api.get('/students/active');
    return data;
  },
  getArchived: async (): Promise<Student[]> => {
    const { data } = await api.get('/students/archived');
    return data;
  },
  getById: async (id: number): Promise<Student> => {
    const { data } = await api.get(`/students/${id}`);
    return data;
  },
  search: async (q: string): Promise<Student[]> => {
    const { data } = await api.get(`/students/search?q=${q}`);
    return data;
  },
  create: async (student: Partial<Student>): Promise<Student> => {
    const { data } = await api.post('/students', student);
    return data;
  },
  update: async (id: number, student: Partial<Student>): Promise<Student> => {
    const { data } = await api.put(`/students/${id}`, student);
    return data;
  },
  archive: async (id: number): Promise<void> => {
    await api.put(`/students/${id}/archive`);
  },
  delete: async (id: number): Promise<void> => {
    await api.delete(`/students/${id}`);
  },
};
