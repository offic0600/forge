import { getAuthHeaders } from "@/lib/auth";

export interface ModelInfo {
  id: string;
  displayName: string;
  provider: string;
  contextWindow: number;
  maxOutputTokens: number;
  supportsStreaming: boolean;
  supportsVision: boolean;
  costTier: "LOW" | "MEDIUM" | "HIGH";
}

export interface ProviderSummary {
  providers: string[];
  totalModels: number;
  defaultProvider: string;
  modelsByProvider: Record<string, number>;
}

export async function fetchModels(): Promise<ModelInfo[]> {
  const res = await fetch("/api/models", { headers: getAuthHeaders() });
  if (!res.ok) throw new Error(`Failed to fetch models: ${res.status}`);
  return res.json();
}

export async function fetchProviders(): Promise<ProviderSummary> {
  const res = await fetch("/api/models/providers", { headers: getAuthHeaders() });
  if (!res.ok) throw new Error(`Failed to fetch providers: ${res.status}`);
  return res.json();
}

export async function fetchHealthCheck(): Promise<Record<string, boolean>> {
  const res = await fetch("/api/models/health", { headers: getAuthHeaders() });
  if (!res.ok) throw new Error(`Failed to fetch health: ${res.status}`);
  return res.json();
}

// --- User Model Config API ---

export interface UserModelConfigView {
  provider: string;
  hasApiKey: boolean;
  apiKeyMasked: string;
  baseUrl: string;
  region: string;
  enabled: boolean;
  updatedAt: string;
}

export interface UserModelConfigRequest {
  provider: string;
  apiKey?: string;
  baseUrl?: string;
  region?: string;
  enabled?: boolean;
}

export async function fetchUserModelConfigs(): Promise<UserModelConfigView[]> {
  const res = await fetch("/api/user/model-configs", { headers: getAuthHeaders() });
  if (!res.ok) throw new Error(`Failed to fetch user configs: ${res.status}`);
  return res.json();
}

export async function saveUserModelConfig(
  provider: string,
  config: UserModelConfigRequest,
): Promise<UserModelConfigView> {
  const res = await fetch(`/api/user/model-configs/${provider}`, {
    method: "PUT",
    headers: { "Content-Type": "application/json", ...getAuthHeaders() },
    body: JSON.stringify(config),
  });
  if (!res.ok) throw new Error(`Failed to save config: ${res.status}`);
  return res.json();
}

export async function deleteUserModelConfig(provider: string): Promise<void> {
  const res = await fetch(`/api/user/model-configs/${provider}`, {
    method: "DELETE",
    headers: getAuthHeaders(),
  });
  if (!res.ok) throw new Error(`Failed to delete config: ${res.status}`);
}
