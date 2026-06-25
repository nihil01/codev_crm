import api from './client';

export const certificatesApi = {
  generate: async (data: { studentId: number; certName: string; issueDate: string }): Promise<Blob> => {
    const response = await api.post('/certificate/generate', data, {
      responseType: 'blob',
    });
    return response.data;
  },
};
