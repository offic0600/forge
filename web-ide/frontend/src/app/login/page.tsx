"use client";

import React, { useEffect } from "react";
import { Anvil } from "lucide-react";
import { login, isAuthenticated } from "@/lib/auth";

export default function LoginPage() {
  useEffect(() => {
    // If already authenticated, redirect to home
    if (isAuthenticated()) {
      window.location.href = "/";
    }
  }, []);

  return (
    <div className="flex min-h-screen items-center justify-center bg-background">
      <div className="w-full max-w-sm space-y-8 text-center">
        <div className="flex flex-col items-center gap-3">
          <Anvil className="h-12 w-12 text-primary" />
          <h1 className="text-3xl font-bold tracking-tight">Forge</h1>
          <p className="text-muted-foreground">
            AI-Powered Intelligent Delivery Platform
          </p>
        </div>

        <div className="space-y-4">
          <button
            onClick={() => login()}
            className="w-full rounded-md bg-primary px-4 py-3 text-sm font-medium text-primary-foreground hover:bg-primary/90 transition-colors"
          >
            Sign in with Forge SSO
          </button>
          <p className="text-xs text-muted-foreground">
            Protected by Keycloak SSO. Contact your administrator for access.
          </p>
        </div>

        <div className="border-t border-border pt-4">
          <p className="text-xs text-muted-foreground">
            Demo accounts: admin/admin, dev1/dev1, viewer1/viewer1
          </p>
        </div>
      </div>
    </div>
  );
}
