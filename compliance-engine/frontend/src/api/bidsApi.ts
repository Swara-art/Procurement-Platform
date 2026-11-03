import { apiClient } from './client';
import { Bid, BidDocument, SubmitBidRequest } from '../types';

export const bidsApi = {
  async getBidsByTender(tenderId: string): Promise<Bid[]> {
    const response = await apiClient.get<Bid[]>(`/tenders/${tenderId}/bids`);
    return response.data || [];
  },

  async getAll(): Promise<Bid[]> {
    const response = await apiClient.get<Bid[]>('/bids');
    return response.data || [];
  },

  async getById(bidId: string): Promise<Bid> {
    const response = await apiClient.get<Bid>(`/bids/${bidId}`);
    return response.data;
  },

  async submitBid(tenderId: string, payload: SubmitBidRequest): Promise<Bid> {
    const response = await apiClient.post<Bid>(`/tenders/${tenderId}/bids`, payload);
    return response.data;
  },

  async getDocuments(bidId: string): Promise<BidDocument[]> {
    const response = await apiClient.get<BidDocument[]>(`/bids/${bidId}/documents`);
    return response.data || [];
  },

  async uploadDocument(bidId: string, file: File): Promise<BidDocument> {
    const formData = new FormData();
    formData.append('file', file);
    const response = await apiClient.post<BidDocument>(`/bids/${bidId}/documents`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    });
    const doc = response.data;

    return doc;
  },

  async indexDocument(documentId: string): Promise<{ documentId: string; chunksIndexed: number; status: string }> {
    const response = await apiClient.post<{ documentId: string; chunksIndexed: number; status: string }>(
      `/documents/${documentId}/index`
    );
    return response.data;
  },
};
