import { apiClient } from './client';
import { User, UserRole } from '../types';

interface AuthResponse { accessToken: string; user: { id: string; fullName: string; email: string; role: UserRole; organizationId?: string; organizationName?: string } }
export type ActorType = 'BIDDER' | 'ORGANIZATION' | 'ADMIN';
export interface RegisterAccount { fullName: string; email: string; password: string; actorType: ActorType; organizationName?: string; registrationNumber?: string }
export interface RegistrationResult { status: 'ACTIVE' | 'PENDING' | 'APPROVED'; message: string; user?: User; requestId?: string }
const mapUser = (u: AuthResponse['user']): User => ({ id: u.id, name: u.fullName, email: u.email, role: u.role, organizationId: u.organizationId, organizationName: u.organizationName, designation: u.role.replace(/_/g, ' '), department: u.organizationName || '' });
export const authApi = {
  async csrf() { await apiClient.get('/auth/csrf'); },
  async login(email: string, password: string) { const { data } = await apiClient.post<AuthResponse>('/auth/login', { email, password }); return { token: data.accessToken, user: mapUser(data.user) }; },
  async restore() { await this.csrf(); const { data } = await apiClient.post<AuthResponse>('/auth/refresh'); return { token: data.accessToken, user: mapUser(data.user) }; },
  async register(payload: RegisterAccount): Promise<RegistrationResult> { await this.csrf(); const { data } = await apiClient.post<{status:RegistrationResult['status'];message:string;user?:AuthResponse['user'];requestId?:string}>('/auth/register', payload); return {status:data.status,message:data.message,user:data.user?mapUser(data.user):undefined,requestId:data.requestId}; },
  async logout() { await this.csrf(); await apiClient.post('/auth/logout'); },
  async me() { const { data } = await apiClient.get<AuthResponse['user']>('/auth/me'); return mapUser(data); },
};
