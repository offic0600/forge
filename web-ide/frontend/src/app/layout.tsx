"use client";

import React, { useState, useEffect } from "react";
import { QueryClient, QueryClientProvider } from "@tanstack/react-query";
import { Header } from "@/components/common/Header";
import { Sidebar } from "@/components/common/Sidebar";
import { isAuthenticated } from "@/lib/auth";
import "./globals.css";

function makeQueryClient(): QueryClient {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 60 * 1000,
        retry: 1,
      },
    },
  });
}

let browserQueryClient: QueryClient | undefined;

function getQueryClient(): QueryClient {
  if (typeof window === "undefined") {
    return makeQueryClient();
  }
  if (!browserQueryClient) {
    browserQueryClient = makeQueryClient();
  }
  return browserQueryClient;
}

// Pages that don't require authentication
const publicPaths = ["/login", "/auth/callback"];

function isPublicPath(path: string): boolean {
  return publicPaths.some((p) => path.startsWith(p));
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const queryClient = getQueryClient();
  const [sidebarCollapsed, setSidebarCollapsed] = useState(false);
  const [role, setRole] = useState<"developer" | "product">("developer");
  const [authChecked, setAuthChecked] = useState(false);
  const [isPublic, setIsPublic] = useState(false);

  useEffect(() => {
    const path = window.location.pathname;
    const pub = isPublicPath(path);
    setIsPublic(pub);

    if (!pub && !isAuthenticated()) {
      // Check if security is enabled by trying an API call
      // If security is disabled, all requests succeed without auth
      fetch("/api/auth/me")
        .then((res) => {
          if (res.status === 401) {
            window.location.href = "/login";
          } else {
            setAuthChecked(true);
          }
        })
        .catch(() => {
          // Backend not reachable, show the page anyway
          setAuthChecked(true);
        });
    } else {
      setAuthChecked(true);
    }
  }, []);

  // Public pages (login, callback) render without shell
  if (isPublic) {
    return (
      <html lang="en" className="dark">
        <body className="min-h-screen bg-background font-sans antialiased">
          <QueryClientProvider client={queryClient}>
            {children}
          </QueryClientProvider>
        </body>
      </html>
    );
  }

  // Wait for auth check
  if (!authChecked) {
    return (
      <html lang="en" className="dark">
        <body className="min-h-screen bg-background font-sans antialiased">
          <div className="flex h-screen items-center justify-center">
            <div className="h-8 w-8 animate-spin rounded-full border-2 border-primary border-t-transparent" />
          </div>
        </body>
      </html>
    );
  }

  return (
    <html lang="en" className="dark">
      <body className="min-h-screen bg-background font-sans antialiased">
        <QueryClientProvider client={queryClient}>
          <div className="flex h-screen flex-col overflow-hidden">
            <Header
              role={role}
              onRoleChange={setRole}
            />
            <div className="flex flex-1 overflow-hidden">
              <Sidebar
                collapsed={sidebarCollapsed}
                onToggleCollapse={() => setSidebarCollapsed(!sidebarCollapsed)}
                role={role}
              />
              <main className="flex-1 overflow-auto">
                {children}
              </main>
            </div>
          </div>
        </QueryClientProvider>
      </body>
    </html>
  );
}
