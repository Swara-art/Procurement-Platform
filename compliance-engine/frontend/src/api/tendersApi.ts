import { apiClient } from './client';
import { CreateTenderRequest, CreateTenderRequirementRequest, Tender, TenderRequirement } from '../types';

export const tendersApi = {
  async getAll(status?: string): Promise<Tender[]> {
    const response = await apiClient.get<Tender[]>('/tenders', {
      params: status && status !== 'ALL' ? { status } : undefined,
    });
    return response.data || [];
  },

  async getById(id: string): Promise<Tender> {
    const response = await apiClient.get<Tender>(`/tenders/${id}`);
    return response.data;
  },

  async create(payload: CreateTenderRequest): Promise<Tender> {
    const response = await apiClient.post<Tender>('/tenders', payload);
    return response.data;
  },

  async publish(id: string): Promise<Tender> {
    const response = await apiClient.patch<Tender>(`/tenders/${id}/publish`);
    return response.data;
  },

  async getRequirements(tenderId: string): Promise<TenderRequirement[]> {
    const response = await apiClient.get<TenderRequirement[]>(`/tenders/${tenderId}/requirements`);
    return response.data || [];
  },

  async createRequirement(tenderId: string, payload: CreateTenderRequirementRequest): Promise<TenderRequirement> {
    const response = await apiClient.post<TenderRequirement>(`/tenders/${tenderId}/requirements`, payload);
    return response.data;
  },
};
