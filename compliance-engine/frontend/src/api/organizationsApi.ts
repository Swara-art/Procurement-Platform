import { apiClient } from './client';
import { CreateOrganizationRequest, Organization, BidderProfileResponse } from '../types';

export const organizationsApi = {
  async getAll(): Promise<Organization[]> {
    const response = await apiClient.get<Organization[]>('/organizations');
    return response.data || [];
  },

  async getById(id: string): Promise<Organization> {
    const response = await apiClient.get<Organization>(`/organizations/${id}`);
    return response.data;
  },

  async create(payload: CreateOrganizationRequest): Promise<Organization> {
    const response = await apiClient.post<Organization>('/organizations', payload);
    return response.data;
  },

  async createBidderProfile(organizationId: string): Promise<BidderProfileResponse> {
    const response = await apiClient.post<BidderProfileResponse>(`/organizations/${organizationId}/bidder-profile`);
    return response.data;
  },
};
