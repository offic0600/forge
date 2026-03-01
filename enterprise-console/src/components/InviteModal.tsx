"use client";

import { useState } from "react";
import { X, Copy, Check } from "lucide-react";
import { api } from "@/lib/api";
import { Button } from "@/components/ui/Button";

interface InviteModalProps {
  orgId: string;
  onClose: () => void;
}

export function InviteModal({ orgId, onClose }: InviteModalProps) {
  const [role, setRole] = useState("MEMBER");
  const [inviteLink, setInviteLink] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);

  async function generateLink() {
    setLoading(true);
    setError(null);
    try {
      const inv = await api.invitations.create(orgId, role);
      const origin = window.location.origin;
      const locale = window.location.pathname.split("/")[1] || "zh";
      setInviteLink(`${origin}/${locale}/invite/${inv.token}`);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to generate link");
    } finally {
      setLoading(false);
    }
  }

  async function copyLink() {
    if (!inviteLink) return;
    await navigator.clipboard.writeText(inviteLink);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/50">
      <div className="w-full max-w-md rounded-lg border border-border bg-card shadow-xl">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-border px-4 py-3">
          <h2 className="text-sm font-semibold text-foreground">Invite Member</h2>
          <button
            onClick={onClose}
            className="text-muted-foreground hover:text-foreground transition-colors"
          >
            <X size={16} />
          </button>
        </div>

        {/* Body */}
        <div className="p-4 space-y-4">
          <div className="flex flex-col gap-1">
            <label className="text-xs font-medium text-muted-foreground">Role</label>
            <select
              className="rounded-md border border-input bg-background px-3 py-2 text-sm text-foreground"
              value={role}
              onChange={(e) => setRole(e.target.value)}
            >
              <option value="MEMBER">Member</option>
              <option value="ADMIN">Admin</option>
            </select>
          </div>

          {error && (
            <div className="rounded-md border border-destructive/40 bg-destructive/10 px-3 py-2 text-xs text-destructive">
              {error}
            </div>
          )}

          {inviteLink ? (
            <div className="space-y-2">
              <label className="text-xs font-medium text-muted-foreground">Invite Link</label>
              <div className="flex gap-2">
                <input
                  readOnly
                  value={inviteLink}
                  className="flex-1 rounded-md border border-input bg-muted px-3 py-2 text-xs font-mono text-foreground"
                />
                <Button size="sm" variant="secondary" onClick={copyLink}>
                  {copied ? <Check size={13} /> : <Copy size={13} />}
                  {copied ? "Copied" : "Copy"}
                </Button>
              </div>
              <p className="text-xs text-muted-foreground">
                Link expires in 7 days. Share it with the person you want to invite.
              </p>
            </div>
          ) : (
            <Button
              className="w-full"
              loading={loading}
              onClick={generateLink}
            >
              Generate Link
            </Button>
          )}
        </div>

        {/* Footer */}
        <div className="flex justify-end border-t border-border px-4 py-3">
          <Button variant="secondary" size="sm" onClick={onClose}>
            Close
          </Button>
        </div>
      </div>
    </div>
  );
}
