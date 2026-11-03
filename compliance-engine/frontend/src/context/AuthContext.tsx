import React, { createContext, useContext, useEffect, useState } from 'react';
import { User } from '../types';
import { authApi, RegisterAccount, RegistrationResult } from '../api/authApi';
import { setAccessToken } from '../api/authToken';

interface AuthContextType { user: User | null; isAuthenticated: boolean; isLoading: boolean; login: (email: string, password: string) => Promise<void>; register: (payload: RegisterAccount) => Promise<RegistrationResult>; logout: () => Promise<void> }
const AuthContext = createContext<AuthContextType | undefined>(undefined);
export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  useEffect(() => { let active=true; authApi.restore().then(({token,user: restored})=>{if(active){setAccessToken(token);setUser(restored);}}).catch(()=>{setAccessToken(null);}).finally(()=>{if(active)setIsLoading(false);}); return ()=>{active=false;}; }, []);
  useEffect(()=>{const expire=()=>setUser(null);window.addEventListener('auth:expired',expire);return()=>window.removeEventListener('auth:expired',expire);},[]);
  const login = async (email:string,password:string) => { await authApi.csrf(); const result=await authApi.login(email,password); setAccessToken(result.token); setUser(result.user); };
  const register = async (payload:RegisterAccount) => { const result=await authApi.register(payload); if(result.status==='ACTIVE'){await login(payload.email,payload.password);} return result; };
  const logout = async () => { try { await authApi.logout(); } finally { setAccessToken(null);setUser(null); } };
  return <AuthContext.Provider value={{user,isAuthenticated:!!user,isLoading,login,register,logout}}>{children}</AuthContext.Provider>;
};
export const useAuth = () => { const context=useContext(AuthContext); if(!context)throw new Error('useAuth must be used within an AuthProvider'); return context; };
