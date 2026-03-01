export interface KnowledgeTagView {
  id: string;
  name: string;
  description: string;
  chapterHeading: string;
  content: string;
  sortOrder: number;
  status: string;
  sourceFile: string | null;
  workspaceId: string | null;
  tagKey: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface CreateKnowledgeTagRequest {
  id: string;
  name: string;
  description?: string;
  chapterHeading: string;
  content: string;
  sortOrder?: number;
  sourceFile?: string;
}

export interface UpdateKnowledgeTagRequest {
  name?: string;
  description?: string;
  chapterHeading?: string;
  content?: string;
  status?: string;
  sourceFile?: string;
}

function getAuthHeader(): Record<string, string> {
  if (typeof window === "undefined") return {};
  const token = localStorage.getItem("forge_access_token");
  if (!token) return {};
  return { Authorization: `Bearer ${token}` };
}

function handleAuthError(response: Response): void {
  if (response.status === 401 && typeof window !== "undefined") {
    localStorage.removeItem("forge_access_token");
    localStorage.removeItem("forge_refresh_token");
    localStorage.removeItem("forge_token_expiry");
    window.location.href = "/login";
  }
}

class KnowledgeTagApi {
  private baseUrl: string;

  constructor(baseUrl: string = "") {
    this.baseUrl = baseUrl;
  }

  private headers(extra: Record<string, string> = {}): Record<string, string> {
    return { ...getAuthHeader(), ...extra };
  }

  async listTags(workspaceId?: string): Promise<KnowledgeTagView[]> {
    const params = workspaceId ? `?workspaceId=${encodeURIComponent(workspaceId)}` : "";
    const response = await fetch(`${this.baseUrl}/api/knowledge/tags${params}`, {
      headers: this.headers(),
    });
    handleAuthError(response);
    if (!response.ok) throw new Error(`Failed to list tags: ${response.status}`);
    return response.json();
  }

  async getTag(tagId: string): Promise<KnowledgeTagView> {
    const response = await fetch(
      `${this.baseUrl}/api/knowledge/tags/${encodeURIComponent(tagId)}`,
      { headers: this.headers() }
    );
    handleAuthError(response);
    if (!response.ok) throw new Error(`Failed to get tag: ${response.status}`);
    return response.json();
  }

  async createTag(request: CreateKnowledgeTagRequest): Promise<KnowledgeTagView> {
    const response = await fetch(`${this.baseUrl}/api/knowledge/tags`, {
      method: "POST",
      headers: this.headers({ "Content-Type": "application/json" }),
      body: JSON.stringify(request),
    });
    handleAuthError(response);
    if (!response.ok) throw new Error(`Failed to create tag: ${response.status}`);
    return response.json();
  }

  async updateTag(
    tagId: string,
    request: UpdateKnowledgeTagRequest
  ): Promise<KnowledgeTagView> {
    const response = await fetch(
      `${this.baseUrl}/api/knowledge/tags/${encodeURIComponent(tagId)}`,
      {
        method: "PUT",
        headers: this.headers({ "Content-Type": "application/json" }),
        body: JSON.stringify(request),
      }
    );
    handleAuthError(response);
    if (!response.ok) throw new Error(`Failed to update tag: ${response.status}`);
    return response.json();
  }

  async deleteTag(tagId: string): Promise<void> {
    const response = await fetch(
      `${this.baseUrl}/api/knowledge/tags/${encodeURIComponent(tagId)}`,
      { method: "DELETE", headers: this.headers() }
    );
    handleAuthError(response);
    if (!response.ok) throw new Error(`Failed to delete tag: ${response.status}`);
  }

  async importBaseline(): Promise<{ imported: boolean; tags: KnowledgeTagView[] }> {
    const response = await fetch(`${this.baseUrl}/api/knowledge/tags/import`, {
      method: "POST",
      headers: this.headers(),
    });
    handleAuthError(response);
    if (!response.ok) throw new Error(`Failed to import baseline: ${response.status}`);
    return response.json();
  }

  async reorderTags(tagIds: string[]): Promise<KnowledgeTagView[]> {
    const response = await fetch(`${this.baseUrl}/api/knowledge/tags/reorder`, {
      method: "PUT",
      headers: this.headers({ "Content-Type": "application/json" }),
      body: JSON.stringify({ tagIds }),
    });
    handleAuthError(response);
    if (!response.ok) throw new Error(`Failed to reorder tags: ${response.status}`);
    return response.json();
  }
  // --- Extraction API ---

  async triggerExtraction(
    workspaceId: string,
    tagId?: string,
    modelId?: string
  ): Promise<{ jobId: string }> {
    const response = await fetch(
      `${this.baseUrl}/api/knowledge/extraction/trigger`,
      {
        method: "POST",
        headers: this.headers({ "Content-Type": "application/json" }),
        body: JSON.stringify({ workspaceId, tagId, modelId }),
      }
    );
    handleAuthError(response);
    if (!response.ok)
      throw new Error(`Failed to trigger extraction: ${response.status}`);
    return response.json();
  }

  async getJobStatus(jobId: string): Promise<ExtractionJobStatus> {
    const response = await fetch(
      `${this.baseUrl}/api/knowledge/extraction/jobs/${encodeURIComponent(jobId)}`,
      { headers: this.headers() }
    );
    handleAuthError(response);
    if (!response.ok)
      throw new Error(`Failed to get job status: ${response.status}`);
    return response.json();
  }

  async getExtractionLogs(
    tagId?: string,
    limit?: number
  ): Promise<ExtractionLog[]> {
    const params = new URLSearchParams();
    if (tagId) params.set("tagId", tagId);
    if (limit) params.set("limit", String(limit));
    const response = await fetch(
      `${this.baseUrl}/api/knowledge/extraction/logs?${params}`,
      { headers: this.headers() }
    );
    handleAuthError(response);
    if (!response.ok)
      throw new Error(`Failed to get extraction logs: ${response.status}`);
    return response.json();
  }
}

export interface ExtractionJobStatus {
  jobId: string;
  status: string;
  progress: {
    totalTags: number;
    completedTags: number;
    currentTag: string | null;
  };
  results: Array<{
    tagId: string;
    tagName: string;
    applicable: boolean;
    reason: string | null;
    contentLength: number;
  }>;
}

export interface ExtractionLog {
  id: string;
  jobId: string;
  tagId: string;
  tagName: string;
  phase: string;
  status: string;
  applicable: boolean;
  reason: string | null;
  contentLength: number;
  durationMs: number;
  createdAt: string;
}

export const knowledgeTagApi = new KnowledgeTagApi();
