import React, { createContext, useContext, useEffect, useState } from 'react';
import type { AuthState, UserInfo } from '../types';
import { authApi } from '../services/api';

interface AuthContextType extends AuthState {
  login: () => void;
  logout: () => void;
  refreshUserInfo: () => Promise<void>;
  hasRole: (role: string) => boolean;
  hasAnyRole: (roles: string[]) => boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [authState, setAuthState] = useState<AuthState>({
    isAuthenticated: false,
    user: null,
    roles: [],
    isLoading: true,
  });

  const fetchUserInfo = async () => {
    try {
      const userInfo: UserInfo = await authApi.getUserInfo();

      setAuthState({
        isAuthenticated: true,
        user: userInfo,
        roles: userInfo.roles || [],
        isLoading: false,
      });
    } catch (error: any) {
      if (error.message === 'UNAUTHORIZED') {
        setAuthState({
          isAuthenticated: false,
          user: null,
          roles: [],
          isLoading: false,
        });
      } else {
        setAuthState({
          isAuthenticated: false,
          user: null,
          roles: [],
          isLoading: false,
        });
      }
    }
  };

  useEffect(() => {
    fetchUserInfo();
  }, []);

  const login = () => {
    authApi.login();
  };

  const logout = () => {
    authApi.logout();
  };

  const refreshUserInfo = async () => {
    await fetchUserInfo();
  };

  const hasRole = (role: string): boolean => {
    return authState.roles.includes(role);
  };

  const hasAnyRole = (roles: string[]): boolean => {
    return roles.some(role => authState.roles.includes(role));
  };

  const contextValue: AuthContextType = {
    ...authState,
    login,
    logout,
    refreshUserInfo,
    hasRole,
    hasAnyRole,
  };

  return (
    <AuthContext.Provider value={contextValue}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = (): AuthContextType => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

export { AuthContext };
