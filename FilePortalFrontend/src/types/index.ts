export interface UserInfo {
  username: string;
  email: string;
  name: string;
  roles: string[];
  sub: string;
}

export interface AuthState {
  isAuthenticated: boolean;
  user: UserInfo | null;
  roles: string[];
  isLoading: boolean;
}

export interface FileMetadata {
  fileId: string;
  filename: string;
  ownerId: string;
  ownerName?: string;
  size: number;
  contentType: string;
  uploadedAt: string;
  modifiedAt?: string;
  version?: number;
  isOwner?: boolean;
  isShared?: boolean;
  sharedWith?: string[];
}

export interface SharedFile {
  shareId: string;
  fileId: string;
  filename: string;
  contentType: string;
  size: number;
  permission: 'READ' | 'WRITE';
  sharedAt: string;
  ownerId: string;
  ownerName?: string;
  isOwner: boolean;
}

export interface PagedFileResponse {
  content: FileMetadata[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface ApiError {
  message: string;
  status: number;
  timestamp?: string;
}

export enum UserRole {
  ADMIN = 'admin',
  MANAGER = 'manager',
  STAFF = 'staff'
}

// AI Processing Status
export type AiProcessingStatus = 'PENDING' | 'COMPLETED' | 'FAILED';

// Sensitivity levels
export type SensitivityLevel = 'PUBLIC' | 'INTERNAL' | 'CONFIDENTIAL' | 'HIGHLY_CONFIDENTIAL';

// File version with AI metadata
export interface FileVersionDTO {
  id: string;
  versionNumber: number;
  filename: string | null;
  contentType: string | null;
  size: number;
  uploadDate: string;
  // AI Metadata
  summary: string | null;
  tags: string[];
  sensitivity: SensitivityLevel | null;
  aiProcessingStatus: AiProcessingStatus;
}

// File detail with versions
export interface FileDetailDTO {
  id: string;
  filename: string | null;
  contentType: string | null;
  size: number | null;
  createdAt: string;
  updatedAt: string;
  ownerId: string | null;
  sharingRulesJson: string | null;
  versions: FileVersionDTO[];
}

// RAG Query types
export interface DocumentQueryRequest {
  question: string;
  fileId?: string;
  sensitivity?: SensitivityLevel;
  topK?: number;
}

export interface DocumentSource {
  fileVersionId: string;
  filename: string;
  chunkContent: string;
  similarity: number;
}

export interface DocumentQueryResponse {
  answer: string;
  sources: DocumentSource[];
}


