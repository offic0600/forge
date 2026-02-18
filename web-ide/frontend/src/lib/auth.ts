/**
 * OIDC authentication helper for Keycloak SSO.
 * Uses Authorization Code flow with PKCE for public clients.
 */

const KEYCLOAK_URL =
  process.env.NEXT_PUBLIC_KEYCLOAK_URL || "http://localhost:8180";
const KEYCLOAK_REALM = process.env.NEXT_PUBLIC_KEYCLOAK_REALM || "forge";
const KEYCLOAK_CLIENT_ID =
  process.env.NEXT_PUBLIC_KEYCLOAK_CLIENT_ID || "forge-web-ide";

const TOKEN_KEY = "forge_access_token";
const REFRESH_TOKEN_KEY = "forge_refresh_token";
const TOKEN_EXPIRY_KEY = "forge_token_expiry";
const CODE_VERIFIER_KEY = "forge_code_verifier";

function getBaseUrl(): string {
  return `${KEYCLOAK_URL}/realms/${KEYCLOAK_REALM}/protocol/openid-connect`;
}

function generateCodeVerifier(): string {
  const array = new Uint8Array(32);
  crypto.getRandomValues(array);
  return btoa(String.fromCharCode(...array))
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/, "");
}

async function generateCodeChallenge(verifier: string): Promise<string> {
  const encoder = new TextEncoder();
  const data = encoder.encode(verifier);
  const digest = await crypto.subtle.digest("SHA-256", data);
  return btoa(String.fromCharCode(...new Uint8Array(digest)))
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/, "");
}

export async function login(): Promise<void> {
  const verifier = generateCodeVerifier();
  const challenge = await generateCodeChallenge(verifier);

  sessionStorage.setItem(CODE_VERIFIER_KEY, verifier);
  sessionStorage.setItem("forge_redirect_after_login", window.location.pathname);

  const params = new URLSearchParams({
    client_id: KEYCLOAK_CLIENT_ID,
    redirect_uri: `${window.location.origin}/auth/callback`,
    response_type: "code",
    scope: "openid profile email",
    code_challenge: challenge,
    code_challenge_method: "S256",
  });

  window.location.href = `${getBaseUrl()}/auth?${params}`;
}

export async function handleCallback(code: string): Promise<boolean> {
  const verifier = sessionStorage.getItem(CODE_VERIFIER_KEY);
  if (!verifier) {
    console.error("No code verifier found");
    return false;
  }

  try {
    const response = await fetch(`${getBaseUrl()}/token`, {
      method: "POST",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: new URLSearchParams({
        grant_type: "authorization_code",
        client_id: KEYCLOAK_CLIENT_ID,
        code,
        redirect_uri: `${window.location.origin}/auth/callback`,
        code_verifier: verifier,
      }),
    });

    if (!response.ok) {
      console.error("Token exchange failed:", response.status);
      return false;
    }

    const data = await response.json();
    localStorage.setItem(TOKEN_KEY, data.access_token);
    if (data.refresh_token) {
      localStorage.setItem(REFRESH_TOKEN_KEY, data.refresh_token);
    }
    // Store expiry as timestamp
    const expiresAt = Date.now() + data.expires_in * 1000;
    localStorage.setItem(TOKEN_EXPIRY_KEY, expiresAt.toString());

    sessionStorage.removeItem(CODE_VERIFIER_KEY);
    return true;
  } catch (err) {
    console.error("Token exchange error:", err);
    return false;
  }
}

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function isAuthenticated(): boolean {
  const token = getToken();
  if (!token) return false;

  const expiry = localStorage.getItem(TOKEN_EXPIRY_KEY);
  if (expiry && Date.now() > parseInt(expiry, 10)) {
    // Token expired
    clearTokens();
    return false;
  }

  return true;
}

export function clearTokens(): void {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
  localStorage.removeItem(TOKEN_EXPIRY_KEY);
}

export function logout(): void {
  clearTokens();
  const params = new URLSearchParams({
    client_id: KEYCLOAK_CLIENT_ID,
    post_logout_redirect_uri: window.location.origin,
  });
  window.location.href = `${getBaseUrl()}/logout?${params}`;
}

export function getRedirectAfterLogin(): string {
  const path = sessionStorage.getItem("forge_redirect_after_login");
  sessionStorage.removeItem("forge_redirect_after_login");
  return path || "/";
}

/**
 * Get authorization headers for API requests.
 * Returns empty object if not authenticated.
 */
export function getAuthHeaders(): Record<string, string> {
  const token = getToken();
  if (!token) return {};
  return { Authorization: `Bearer ${token}` };
}
