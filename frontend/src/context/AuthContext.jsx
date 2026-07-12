import { createContext, useContext, useState, useCallback } from 'react';
import api from '../api/axios';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(() => {
    const stored = localStorage.getItem('auction_user');
    return stored ? JSON.parse(stored) : null;
  });

  const persist = (authResponse) => {
    localStorage.setItem('auction_token', authResponse.token);
    localStorage.setItem('auction_user', JSON.stringify(authResponse));
    setUser(authResponse);
  };

  const login = useCallback(async (username, password) => {
    const res = await api.post('/auth/login', { username, password });
    persist(res.data);
    return res.data;
  }, []);

  const register = useCallback(async (payload) => {
    const res = await api.post('/auth/register', payload);
    persist(res.data);
    return res.data;
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('auction_token');
    localStorage.removeItem('auction_user');
    setUser(null);
  }, []);

  return (
    <AuthContext.Provider value={{ user, login, register, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used inside AuthProvider');
  return ctx;
}
