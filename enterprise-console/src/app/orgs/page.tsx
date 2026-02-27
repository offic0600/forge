"use client";

import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { Plus, Building2, Trash2, Edit } from "lucide-react";
import Link from "next/link";
import { api } from "@/lib/api";
import { Button } from "@/components/ui/Button";
import { Badge } from "@/components/ui/Badge";

export default function OrgsPage() {
  const qc = useQueryClient();
  const { data: orgs = [], isLoading } = useQuery({
    queryKey: ["orgs"],
    queryFn: api.orgs.list,
  });

  const deleteMutation = useMutation({
    mutationFn: (id: string) => api.orgs.delete(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ["orgs"] }),
  });

  function statusColor(status: string) {
    if (status === "ACTIVE") return "green" as const;
    if (status === "SUSPENDED") return "yellow" as const;
    return "gray" as const;
  }

  return (
    <div>
      <div className="mb-6 flex items-center justify-between">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Organizations</h1>
          <p className="mt-1 text-sm text-muted-foreground">
            Manage enterprise organizations and their configurations
          </p>
        </div>
        <Link href="/orgs/new">
          <Button>
            <Plus size={14} />
            New Organization
          </Button>
        </Link>
      </div>

      {isLoading ? (
        <div className="flex items-center justify-center py-20">
          <div className="h-6 w-6 animate-spin rounded-full border-2 border-primary border-t-transparent" />
        </div>
      ) : orgs.length === 0 ? (
        <div className="flex flex-col items-center justify-center rounded-lg border border-dashed border-border py-20 text-center">
          <Building2 size={40} className="mb-3 text-muted-foreground" />
          <p className="text-muted-foreground">No organizations yet</p>
          <p className="mt-1 text-sm text-muted-foreground">
            Create your first organization to get started
          </p>
          <Link href="/orgs/new" className="mt-4">
            <Button size="sm">
              <Plus size={13} />
              New Organization
            </Button>
          </Link>
        </div>
      ) : (
        <div className="overflow-x-auto rounded-lg border border-border">
          <table className="w-full text-sm">
            <thead className="border-b border-border bg-muted/50">
              <tr>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">
                  Name
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">
                  Slug
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">
                  Status
                </th>
                <th className="px-4 py-3 text-left text-xs font-medium uppercase tracking-wider text-muted-foreground">
                  Created
                </th>
                <th className="px-4 py-3 text-right text-xs font-medium uppercase tracking-wider text-muted-foreground">
                  Actions
                </th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border/50">
              {orgs.map((org) => (
                <tr key={org.id} className="hover:bg-accent/30 transition-colors">
                  <td className="px-4 py-3">
                    <Link
                      href={`/orgs/${org.id}`}
                      className="font-medium text-foreground hover:text-primary transition-colors"
                    >
                      {org.name}
                    </Link>
                    {org.description && (
                      <p className="mt-0.5 text-xs text-muted-foreground truncate max-w-xs">
                        {org.description}
                      </p>
                    )}
                  </td>
                  <td className="px-4 py-3 text-muted-foreground font-mono text-xs">
                    {org.slug}
                  </td>
                  <td className="px-4 py-3">
                    <Badge color={statusColor(org.status)}>
                      {org.status.toLowerCase()}
                    </Badge>
                  </td>
                  <td className="px-4 py-3 text-muted-foreground text-xs">
                    {new Date(org.createdAt).toLocaleDateString()}
                  </td>
                  <td className="px-4 py-3">
                    <div className="flex items-center justify-end gap-2">
                      <Link href={`/orgs/${org.id}`}>
                        <Button variant="ghost" size="sm">
                          <Edit size={13} />
                        </Button>
                      </Link>
                      <Button
                        variant="ghost"
                        size="sm"
                        className="text-destructive hover:text-destructive hover:bg-destructive/10"
                        onClick={() => {
                          if (
                            confirm(
                              `Delete organization "${org.name}"? This action cannot be undone.`
                            )
                          ) {
                            deleteMutation.mutate(org.id);
                          }
                        }}
                      >
                        <Trash2 size={13} />
                      </Button>
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
