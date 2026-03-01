"use client";

import { useState } from "react";
import { useParams } from "next/navigation";
import { useQuery } from "@tanstack/react-query";
import { ArrowLeft, ChevronLeft, ChevronRight } from "lucide-react";
import { Link } from "@/navigation";
import { api } from "@/lib/api";
import { Button } from "@/components/ui/Button";
import { Badge } from "@/components/ui/Badge";

function actionBadgeColor(action: string): "blue" | "green" | "yellow" | "red" | "gray" {
  if (action.startsWith("ORG_")) return "blue";
  if (action.startsWith("MEMBER_")) return "green";
  if (action.startsWith("INVITATION_")) return "yellow";
  if (action.startsWith("QUOTA_")) return "red";
  return "gray";
}

export default function OrgAuditLogPage() {
  const { id } = useParams<{ id: string }>();
  const [page, setPage] = useState(0);

  const { data, isLoading } = useQuery({
    queryKey: ["orgs", id, "audit-logs", page],
    queryFn: () => api.auditLogs.listByOrg(id, page),
  });

  return (
    <div>
      <div className="mb-6 flex items-center gap-3">
        <Link href={`/orgs/${id}`}>
          <Button variant="ghost" size="sm">
            <ArrowLeft size={14} />
          </Button>
        </Link>
        <h1 className="text-xl font-bold text-foreground">Audit Log</h1>
      </div>

      {isLoading ? (
        <div className="flex items-center justify-center py-20">
          <div className="h-6 w-6 animate-spin rounded-full border-2 border-primary border-t-transparent" />
        </div>
      ) : (
        <>
          <div className="overflow-x-auto rounded-lg border border-border">
            <table className="w-full text-sm">
              <thead className="border-b border-border bg-muted/50">
                <tr>
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase text-muted-foreground">Time</th>
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase text-muted-foreground">Actor</th>
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase text-muted-foreground">Action</th>
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase text-muted-foreground">Target</th>
                  <th className="px-4 py-3 text-left text-xs font-medium uppercase text-muted-foreground">Detail</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border/50">
                {!data || data.content.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="px-4 py-8 text-center text-muted-foreground">
                      No audit log entries yet.
                    </td>
                  </tr>
                ) : (
                  data.content.map((entry) => (
                    <tr key={entry.id} className="hover:bg-accent/30 transition-colors">
                      <td className="px-4 py-3 text-xs font-mono text-muted-foreground whitespace-nowrap">
                        {new Date(entry.createdAt).toLocaleString()}
                      </td>
                      <td className="px-4 py-3 text-xs font-mono text-foreground max-w-[160px] truncate">
                        {entry.actorId}
                      </td>
                      <td className="px-4 py-3">
                        <Badge color={actionBadgeColor(entry.action)}>
                          {entry.action}
                        </Badge>
                      </td>
                      <td className="px-4 py-3 text-xs text-muted-foreground">
                        {entry.targetType && (
                          <span className="font-medium text-foreground">{entry.targetType}</span>
                        )}
                        {entry.targetId && (
                          <span className="ml-1 font-mono">{entry.targetId}</span>
                        )}
                      </td>
                      <td className="px-4 py-3 text-xs text-muted-foreground max-w-[200px] truncate">
                        {entry.detail ?? "—"}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          {/* Pagination */}
          {data && data.totalPages > 1 && (
            <div className="flex items-center justify-between mt-4 text-sm text-muted-foreground">
              <span>Page {page + 1} of {data.totalPages}</span>
              <div className="flex gap-2">
                <Button
                  variant="secondary"
                  size="sm"
                  disabled={page === 0}
                  onClick={() => setPage((p) => p - 1)}
                >
                  <ChevronLeft size={14} />
                  Prev
                </Button>
                <Button
                  variant="secondary"
                  size="sm"
                  disabled={page >= data.totalPages - 1}
                  onClick={() => setPage((p) => p + 1)}
                >
                  Next
                  <ChevronRight size={14} />
                </Button>
              </div>
            </div>
          )}
        </>
      )}
    </div>
  );
}
