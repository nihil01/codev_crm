import api from './client';

interface UserInfo {
  id: number;
  email: string;
  fullName: string;
  role: string;
}

export const authApi = {
  login: async (email: string, password: string): Promise<UserInfo> => {
    await api.get('/auth/csrf');
    const { data } = await api.post('/auth/login', { email, password });
    return data;
  },
  logout: async (): Promise<void> => {
    await api.post('/auth/logout');
  },
  me: async (): Promise<UserInfo> => {
    const { data } = await api.get('/auth/me');
    return data;
  },
};
