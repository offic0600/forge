export interface KnowledgeTagView {
  id: string;
  name: string;
  description: string;
  chapterHeading: string;
  content: string;
  sortOrder: number;
  status: string;
  sourceFile: string | null;
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

  async listTags(): Promise<KnowledgeTagView[]> {
    const response = await fetch(`${this.baseUrl}/api/knowledge/tags`, {
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
}

export const knowledgeTagApi = new KnowledgeTagApi();
