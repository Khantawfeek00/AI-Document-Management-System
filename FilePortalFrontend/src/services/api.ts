import type { UserInfo, FileMetadata, PagedFileResponse, FileDetailDTO, DocumentQueryRequest, DocumentQueryResponse } from '../types';

const API_BASE_URL = '/api';

async function apiFetch<T>(
  url: string,
  options: RequestInit = {}
): Promise<T> {
  const response = await fetch(url, {
    ...options,
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      ...options.headers,
    },
  });

  if (!response.ok) {
    if (response.status === 401) {
      throw new Error('UNAUTHORIZED');
    }
    if (response.status === 403) {
      throw new Error('FORBIDDEN');
    }
    const errorText = await response.text();
    throw new Error(errorText || `HTTP ${response.status}`);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json();
}

export const authApi = {
  getUserInfo: async (): Promise<UserInfo> => {
    return apiFetch<UserInfo>(`${API_BASE_URL}/auth/userinfo`);
  },

  login: () => {
    window.location.href = '/oauth2/authorization/gateway-client';
  },

  logout: () => {
    window.location.href = '/logout';
  },
};

function mapBackendFileToFileMetadata(f: any): FileMetadata {
  return {
    fileId: f.id || f.fileId || '',
    filename: f.filename || f.name || 'unknown',
    ownerId: f.ownerId || f.owner || '',
    ownerName: f.ownerName || undefined,
    size: (f.size as number) || 0,
    contentType: f.contentType || 'application/octet-stream',
    uploadedAt: f.createdAt || f.uploadedAt || new Date().toISOString(),
    modifiedAt: f.updatedAt || undefined,
    version: (f.version as number) || undefined,
    isOwner: f.isOwner || false,
    isShared: f.isShared || false,
    sharedWith: f.sharedWith || [],
  };
}

export const fileApi = {
  listFiles: async (scope?: string, page?: number, size?: number): Promise<PagedFileResponse> => {
    let url = `${API_BASE_URL}/files`;
    const params: string[] = [];
    if (scope) params.push(`scope=${encodeURIComponent(scope)}`);
    if (page !== undefined) params.push(`page=${page}`);
    if (size !== undefined) params.push(`size=${size}`);
    if (params.length > 0) url = `${url}?${params.join('&')}`;
    const raw = await apiFetch<any>(url);

    const mapped = {
      ...raw,
      content: (raw.content || []).map((f: any) => mapBackendFileToFileMetadata(f)),
    };

    return mapped as PagedFileResponse;
  },

  getFileMetadata: async (fileId: string): Promise<FileMetadata> => {
    const raw = await apiFetch<any>(`${API_BASE_URL}/files/${fileId}/metadata`);
    return mapBackendFileToFileMetadata(raw);
  },

  uploadFile: async (file: File): Promise<FileMetadata> => {
    const uploadMetadata = `filename ${btoa(file.name)},contenttype ${btoa(file.type)}`;

    const createResponse = await fetch(`${API_BASE_URL}/uploads/`, {
      method: 'POST',
      credentials: 'include',
      headers: {
        'Tus-Resumable': '1.0.0',
        'Upload-Length': file.size.toString(),
        'Upload-Metadata': uploadMetadata,
      },
    });

    if (!createResponse.ok) {
      if (createResponse.status === 401) throw new Error('UNAUTHORIZED');
      if (createResponse.status === 403) throw new Error('FORBIDDEN');
      throw new Error(`Upload creation failed: ${createResponse.statusText}`);
    }

    const uploadLocation = createResponse.headers.get('Location');

    if (!uploadLocation) {
      throw new Error('No upload location returned from server');
    }

    const uploadId = uploadLocation.split('/').pop();
    const uploadUrl = `${API_BASE_URL}/uploads/${uploadId}`;

    const fileBlob = new Blob([file], { type: 'application/offset+octet-stream' });

    const uploadResponse = await fetch(uploadUrl, {
      method: 'PATCH',
      credentials: 'include',
      headers: {
        'Tus-Resumable': '1.0.0',
        'Upload-Offset': '0',
      },
      body: fileBlob,
    });

    if (!uploadResponse.ok) {
      if (uploadResponse.status === 401) throw new Error('UNAUTHORIZED');
      if (uploadResponse.status === 403) throw new Error('FORBIDDEN');
      throw new Error(`Upload failed: ${uploadResponse.statusText}`);
    }

    return {
      fileId: uploadId || '',
      filename: file.name,
      ownerId: '',
      size: file.size,
      contentType: file.type,
      uploadedAt: new Date().toISOString(),
    };
  },

  deleteFile: async (fileId: string): Promise<void> => {
    return apiFetch<void>(`${API_BASE_URL}/files/${fileId}`, {
      method: 'DELETE',
    });
  },

  downloadFile: async (fileId: string, filename: string): Promise<void> => {
    const response = await fetch(`${API_BASE_URL}/files/${fileId}/download`, {
      credentials: 'include',
    });

    if (!response.ok) {
      throw new Error(`Download failed: ${response.statusText}`);
    }

    const blob = await response.blob();
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    window.URL.revokeObjectURL(url);
    document.body.removeChild(a);
  },

  getFileShares: async (fileId: string): Promise<any[]> => {
    return apiFetch<any[]>(`${API_BASE_URL}/files/${fileId}/shares`);
  },

  createShare: async (fileId: string, userId: string, permission: 'READ' | 'WRITE'): Promise<any> => {
    const url = `${API_BASE_URL}/files/${fileId}/shares?userId=${encodeURIComponent(userId)}&permission=${permission}`;
    return apiFetch<any>(url, {
      method: 'POST',
    });
  },

  deleteShare: async (fileId: string, shareId: string): Promise<void> => {
    return apiFetch<void>(`${API_BASE_URL}/files/${fileId}/shares/${shareId}`, {
      method: 'DELETE',
    });
  },

  getSharedWithMe: async (): Promise<any[]> => {
    const userInfo = await authApi.getUserInfo();
    return apiFetch<any[]>(`${API_BASE_URL}/files/shared-with/${userInfo.sub}`);
  },

  getFileDetails: async (fileId: string): Promise<FileDetailDTO> => {
    return apiFetch<FileDetailDTO>(`${API_BASE_URL}/files/${fileId}`);
  },

  queryDocuments: async (request: DocumentQueryRequest): Promise<DocumentQueryResponse> => {
    return apiFetch<DocumentQueryResponse>(`${API_BASE_URL}/files/query`, {
      method: 'POST',
      body: JSON.stringify(request),
    });
  },
};

export const userApi = {
    listUsers: async (q?: string): Promise<any> => {
        const url = q ? `${API_BASE_URL}/users?q=${encodeURIComponent(q)}` : `${API_BASE_URL}/users`;
        return apiFetch<any>(url);
    },

    getUserDetails: async (userId: string): Promise<any> => {
        return apiFetch<any>(`${API_BASE_URL}/users/${userId}`);
    },

    updateUser: async (userId: string, dto: any): Promise<any> => {
        return apiFetch<any>(`${API_BASE_URL}/users/${userId}`, {
            method: 'PUT',
            body: JSON.stringify(dto),
        });
    },

    deleteUser: async (userId: string): Promise<void> => {
        return apiFetch<void>(`${API_BASE_URL}/users/${userId}`, {
            method: 'DELETE',
        });
    },

    getRoleOptions: async (): Promise<string[]> => {
        return apiFetch<any>(`${API_BASE_URL}/users/roles`);
    },

    createUser: async (dto: any): Promise<any> => {
        return apiFetch<any>(`${API_BASE_URL}/users`, {
            method: 'POST',
            body: JSON.stringify(dto),
        });
    },
};
