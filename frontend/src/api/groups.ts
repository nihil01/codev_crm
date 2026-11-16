import api from './client';
import { CourseGroup, GroupStudent, GroupTeacher } from '../types';

export const groupsApi = {
  getAll: async (): Promise<CourseGroup[]> => {
    const { data } = await api.get('/groups');
    return data;
  },
  getActive: async (): Promise<CourseGroup[]> => {
    const { data } = await api.get('/groups/active');
    return data;
  },
  getById: async (id: number): Promise<CourseGroup> => {
    const { data } = await api.get(`/groups/${id}`);
    return data;
  },
  create: async (group: Partial<CourseGroup>): Promise<CourseGroup> => {
    const { data } = await api.post('/groups', group);
    return data;
  },
  update: async (id: number, group: Partial<CourseGroup>): Promise<CourseGroup> => {
    const { data } = await api.put(`/groups/${id}`, group);
    return data;
  },
  complete: async (id: number): Promise<void> => {
    await api.put(`/groups/${id}/complete`);
  },
  suspend: async (id: number): Promise<void> => {
    await api.put(`/groups/${id}/suspend`);
  },
  resume: async (id: number): Promise<void> => {
    await api.put(`/groups/${id}/resume`);
  },
  delete: async (id: number): Promise<void> => {
    await api.delete(`/groups/${id}`);
  },

  // Students in group
  getStudents: async (groupId: number): Promise<GroupStudent[]> => {
    const { data } = await api.get(`/groups/${groupId}/students`);
    return data;
  },
  addStudent: async (groupId: number, student: Partial<GroupStudent>): Promise<GroupStudent> => {
    const { data } = await api.post(`/groups/${groupId}/students`, student);
    return data;
  },
  removeStudent: async (groupId: number, studentId: number, reason: string): Promise<void> => {
    await api.delete(`/groups/${groupId}/students/${studentId}`, { params: { reason } });
  },

  // Teachers in group
  getTeachers: async (groupId: number): Promise<GroupTeacher[]> => {
    const { data } = await api.get(`/groups/${groupId}/teachers`);
    return data;
  },
  assignTeacher: async (groupId: number, teacher: Partial<GroupTeacher>): Promise<GroupTeacher> => {
    const { data } = await api.post(`/groups/${groupId}/teachers`, teacher);
    return data;
  },
  removeTeacher: async (groupId: number, teacherId: number): Promise<void> => {
    await api.delete(`/groups/${groupId}/teachers/${teacherId}`);
  },
};
