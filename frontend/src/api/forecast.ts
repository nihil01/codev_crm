import api from './client';
import { ForecastData } from '../types';

export const forecastApi = {
  get: async (): Promise<ForecastData> => {
    const { data } = await api.get('/forecast');
    return data;
  },
};
