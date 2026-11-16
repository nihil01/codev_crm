import axios from 'axios';

const api = axios.create({
  baseURL: 'https://crm.codev.academy/api',
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true,
  withXSRFToken: true,
});

export default api;
