"use client";

import React, { useState, useEffect } from "react";

interface SessionSummary {
  id: string;
  sessionId: string;
  profile: string;
  summary: string;
  completedWork: string;
  artifacts: string;
  decisions: string;
  unresolved: string;
  nextSteps: string;
  turnCount: number;
  toolCallCount: number;
  createdAt: string;
}

interface SessionHistoryViewProps {
  workspaceId: string;
}

function parseJsonArray(json: string): string[] {
  try {
    return JSON.parse(json) as string[];
  } catch {
    return [];
  }
}

export function SessionHistoryView({ workspaceId }: SessionHistoryViewProps) {
  const [sessions, setSessions] = useState<SessionSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [expandedId, setExpandedId] = useState<string | null>(null);

  useEffect(() => {
    (async () => {
      try {
        const res = await fetch(
          `/api/memory/sessions/${workspaceId}?limit=20`
        );
        if (res.ok) {
          const data = (await res.json()) as SessionSummary[];
          setSessions(data);
        }
      } catch {
        // ignore
      } finally {
        setLoading(false);
      }
    })();
  }, [workspaceId]);

  if (loading) {
    return <p className="text-xs text-muted-foreground">加载中...</p>;
  }

  if (sessions.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-8 text-center">
        <p className="text-sm text-muted-foreground">暂无会话历史</p>
        <p className="mt-1 text-xs text-muted-foreground">
          每次对话结束后会自动生成会话摘要
        </p>
      </div>
    );
  }

  return (
    <div className="space-y-2">
      {sessions.map((session) => {
        const isExpanded = expandedId === session.id;
        const artifacts = parseJsonArray(session.artifacts);
        const decisions = parseJsonArray(session.decisions);
        const completedWork = parseJsonArray(session.completedWork);
        const unresolved = parseJsonArray(session.unresolved);
        const createdAt = new Date(session.createdAt);

        return (
          <div
            key={session.id}
            className="rounded-md border border-border bg-muted/20"
          >
            <button
              onClick={() => setExpandedId(isExpanded ? null : session.id)}
              className="flex w-full items-center justify-between px-3 py-2 text-left"
            >
              <div className="flex flex-col gap-0.5">
                <div className="flex items-center gap-2">
                  <span className="text-xs font-medium text-foreground">
                    {createdAt.toLocaleDateString()}{" "}
                    {createdAt.toLocaleTimeString([], {
                      hour: "2-digit",
                      minute: "2-digit",
                    })}
                  </span>
                  <span className="rounded bg-primary/10 px-1.5 py-0.5 text-[10px] text-primary">
                    {session.profile.replace("-profile", "")}
                  </span>
                </div>
                <p className="text-xs text-muted-foreground line-clamp-1">
                  {session.summary}
                </p>
              </div>
              <div className="flex flex-shrink-0 items-center gap-2 text-xs text-muted-foreground">
                <span>{session.turnCount}T</span>
                <span>{session.toolCallCount} tools</span>
              </div>
            </button>

            {isExpanded && (
              <div className="space-y-2 border-t border-border px-3 py-2">
                <p className="text-xs text-foreground">{session.summary}</p>

                {completedWork.length > 0 && (
                  <div>
                    <p className="text-xs font-medium text-foreground">
                      完成工作
                    </p>
                    <ul className="mt-0.5 space-y-0.5">
                      {completedWork.map((item, i) => (
                        <li key={i} className="text-xs text-muted-foreground">
                          - {item}
                        </li>
                      ))}
                    </ul>
                  </div>
                )}

                {artifacts.length > 0 && (
                  <div>
                    <p className="text-xs font-medium text-foreground">
                      产出物
                    </p>
                    <ul className="mt-0.5 space-y-0.5">
                      {artifacts.map((item, i) => (
                        <li
                          key={i}
                          className="text-xs font-mono text-muted-foreground"
                        >
                          {item}
                        </li>
                      ))}
                    </ul>
                  </div>
                )}

                {decisions.length > 0 && (
                  <div>
                    <p className="text-xs font-medium text-foreground">
                      关键决策
                    </p>
                    <ul className="mt-0.5 space-y-0.5">
                      {decisions.map((item, i) => (
                        <li key={i} className="text-xs text-muted-foreground">
                          - {item}
                        </li>
                      ))}
                    </ul>
                  </div>
                )}

                {unresolved.length > 0 && (
                  <div>
                    <p className="text-xs font-medium text-yellow-500">
                      未解决
                    </p>
                    <ul className="mt-0.5 space-y-0.5">
                      {unresolved.map((item, i) => (
                        <li key={i} className="text-xs text-yellow-400/80">
                          - {item}
                        </li>
                      ))}
                    </ul>
                  </div>
                )}
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
}
