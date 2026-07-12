import axios from 'axios';

// In local dev, Vite's proxy (vite.config.js) forwards '/api' to the backend,
// so no env var is needed. In production, set VITE_API_BASE_URL to your
// deployed backend's full URL, e.g. https://your-backend.up.railway.app/api
const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api'
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('auction_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

api.interceptors.response.use(
  (res) => res,
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('auction_token');
      localStorage.removeItem('auction_user');
      if (!window.location.pathname.startsWith('/login')) {
        window.location.href = '/login';
      }
    }
    return Promise.reject(err);
  }
);

export default api;
