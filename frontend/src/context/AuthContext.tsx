import React, { createContext, useContext, useState, useEffect } from 'react';
import { User, Role } from '../types';
import { authApi } from '../api/auth';

interface AuthContextType {
  user: User | null;
  login: (email: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  isAuthenticated: boolean;
  hasRole: (...roles: Role[]) => boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Check if user has active session
    const checkAuth = async () => {
      try {
        const userData = await authApi.me();
        setUser({
          email: userData.email,
          fullName: userData.fullName,
          role: userData.role as Role,
          id: userData.id,
        });
      } catch {
        setUser(null);
      } finally {
        setLoading(false);
      }
    };
    checkAuth();
  }, []);

  const login = async (email: string, password: string) => {
    const userData = await authApi.login(email, password);
    setUser({
      email: userData.email,
      fullName: userData.fullName,
      role: userData.role as Role,
      id: userData.id,
    });
  };

  const logout = async () => {
    try {
      await authApi.logout();
    } catch {
      // ignore
    }
    setUser(null);
  };

  const hasRole = (...roles: Role[]) => {
    return user !== null && roles.includes(user.role);
  };

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-slate-950">
        <div className="w-8 h-8 border-2 border-indigo-500 border-t-transparent rounded-full animate-spin" />
      </div>
    );
  }

  return (
    <AuthContext.Provider value={{ user, login, logout, isAuthenticated: !!user, hasRole }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) throw new Error('useAuth must be used within AuthProvider');
  return context;
};
