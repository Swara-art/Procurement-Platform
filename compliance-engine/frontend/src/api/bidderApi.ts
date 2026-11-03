import { apiClient } from './client';
export const sectors=['IT_SOFTWARE','HEALTHCARE_IT','CLOUD_INFRASTRUCTURE','MEDICAL_EQUIPMENT','PHARMACEUTICALS','CIVIL_CONSTRUCTION','ELECTRICAL','AGRICULTURE','EDUCATION_SERVICES','TRANSPORTATION','CONSULTING'] as const;
export type Sector=(typeof sectors)[number];
export interface ChecklistItem{documentType:string;required:boolean;uploaded:boolean;status:string;policyCondition?:string;displayName:string;requirementLevel:'REQUIRED'|'CONDITIONAL'|'TENDER_SPECIFIC';alternativeGroup?:string}
export interface CompanyType{code:string;displayName:string;description:string}
export interface LegalDocumentRequirement{documentType:string;displayName:string;description:string;requirementLevel:'REQUIRED'|'CONDITIONAL'|'TENDER_SPECIFIC';conditionExpression?:string;alternativeGroup?:string;sortOrder:number}
export interface Onboarding{legalName:string;entityType:string;registrationNumber:string;businessAddress:string;contactNumber:string;panNumber:string;gstin:string;gstApplicable:boolean;primarySector:string;additionalSectors:string[];capabilities:string[];establishmentDate?:string;experienceYears:number;completedProjects:number;turnovers:{financialYear:string;annualTurnover:string}[];authorizedSignatoryName:string;authorizedSignatoryEmail:string;onboardingStatus:string;documentChecklist:ChecklistItem[];cin:string;llpin:string;governmentOwned:boolean;firmRegistered:boolean;representativeIsSignatory:boolean;msmeBenefitClaimed:boolean;shopRegistrationApplicable:boolean;businessLicenseAvailable:boolean;incorporatedEntity:boolean;panApplicable:boolean;section8TaxRegistrationApplicable:boolean}
export interface StoredDocument{id:string;documentType:string;financialYear?:string;originalFilename:string;fileSize:number;contentType:string;version:number;verificationStatus:string;processingStatus:string;uploadedAt:string}
export const bidderApi={
 async companyTypes(){return (await apiClient.get<CompanyType[]>('/company-types')).data;},
 async legalTypeRequirements(type:string){return (await apiClient.get<LegalDocumentRequirement[]>(`/company-types/${type}/required-documents`)).data;},
 async getOnboarding(){return (await apiClient.get<Onboarding>('/bidder/onboarding')).data;},
 async saveOnboarding(value:Onboarding){return (await apiClient.put<Onboarding>('/bidder/onboarding',value)).data;},
 async checklist(){return (await apiClient.get<ChecklistItem[]>('/bidder/documents/checklist')).data;},
 async documents(){return (await apiClient.get<StoredDocument[]>('/bidder/documents')).data;},
 async uploadDocument(path:string,file:File,financialYear?:string,type?:string){const body=new FormData();body.append('file',file);if(financialYear)body.append('financialYear',financialYear);const url=type?`/bidder/documents/type/${type}`:path;return (await apiClient.post<StoredDocument>(url,body,{headers:{'Content-Type':'multipart/form-data'}})).data;},
 async submitOnboarding(){return (await apiClient.post<Onboarding>('/bidder/onboarding/submit')).data;},
 async downloadDocument(id:string){const res=await apiClient.get(`/bidder/documents/${id}/content`,{responseType:'blob'});return res.data as Blob;},
 async recommended(params:{page:number;size:number;sector?:string;search?:string}){return (await apiClient.get<{content:{tender:{id:string;tenderNumber:string;title:string;description?:string;procurementAuthorityId:string;submissionDeadline:string;status:string};matchingSectors:string[];matchingCapabilities:string[];eligibilityNote:string}[];page:number;size:number;totalElements:number;totalPages:number}>('/bidder/tenders/recommended',{params})).data;},
};
