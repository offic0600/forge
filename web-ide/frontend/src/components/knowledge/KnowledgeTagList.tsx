"use client";

import React from "react";
import { KnowledgeTagView } from "@/lib/knowledge-tag-api";
import { Search, BookMarked, Loader2 } from "lucide-react";

interface KnowledgeTagListProps {
  tags: KnowledgeTagView[];
  selectedTagId: string | null;
  onSelect: (tagId: string) => void;
  loading?: boolean;
  searchQuery: string;
  onSearchChange: (query: string) => void;
}

function getStatusBadge(status: string) {
  switch (status) {
    case "active":
      return { className: "bg-green-500/10 text-green-500", label: "active" };
    case "draft":
      return { className: "bg-amber-500/10 text-amber-500", label: "AI Draft" };
    case "extracting":
      return { className: "bg-blue-500/10 text-blue-500", label: "extracting" };
    case "not_applicable":
      return { className: "bg-gray-500/10 text-gray-400", label: "N/A" };
    case "empty":
      return { className: "bg-gray-500/10 text-gray-400", label: "empty" };
    default:
      return { className: "bg-yellow-500/10 text-yellow-500", label: status };
  }
}

export function KnowledgeTagList({
  tags,
  selectedTagId,
  onSelect,
  loading,
  searchQuery,
  onSearchChange,
}: KnowledgeTagListProps) {
  const filtered = tags.filter((tag) => {
    if (!searchQuery) return true;
    const q = searchQuery.toLowerCase();
    return (
      tag.name.toLowerCase().includes(q) ||
      tag.description.toLowerCase().includes(q)
    );
  });

  const handleClick = (tag: KnowledgeTagView) => {
    if (tag.status === "extracting" || tag.status === "not_applicable") return;
    onSelect(tag.id);
  };

  return (
    <div className="flex h-full flex-col">
      {/* Search */}
      <div className="border-b border-border p-3">
        <div className="relative">
          <Search className="absolute left-2.5 top-2.5 h-4 w-4 text-muted-foreground" />
          <input
            type="text"
            placeholder="Search standards..."
            value={searchQuery}
            onChange={(e) => onSearchChange(e.target.value)}
            className="w-full rounded-md border border-border bg-background py-2 pl-9 pr-3 text-sm placeholder:text-muted-foreground focus:border-primary focus:outline-none"
          />
        </div>
      </div>

      {/* Tag list */}
      <div className="flex-1 overflow-auto">
        {loading ? (
          <div className="space-y-3 p-3">
            {[1, 2, 3, 4].map((i) => (
              <div key={i} className="h-16 animate-pulse rounded bg-muted" />
            ))}
          </div>
        ) : filtered.length === 0 ? (
          <div className="px-3 py-8 text-center text-sm text-muted-foreground">
            No standards found
          </div>
        ) : (
          filtered.map((tag) => {
            const badge = getStatusBadge(tag.status);
            const isDisabled =
              tag.status === "extracting" || tag.status === "not_applicable";
            const isEmpty = tag.status === "empty";

            return (
              <button
                key={tag.id}
                onClick={() => handleClick(tag)}
                className={`w-full border-b px-3 py-3 text-left transition-colors ${
                  isDisabled
                    ? "cursor-not-allowed opacity-50 border-border"
                    : isEmpty
                      ? "border-dashed border-border hover:bg-accent/50"
                      : selectedTagId === tag.id
                        ? "bg-accent border-border"
                        : "hover:bg-accent/50 border-border"
                }${tag.status === "extracting" ? " pointer-events-none" : ""}`}
              >
                <div className="flex items-start gap-3">
                  <span className="mt-0.5 flex h-6 w-6 flex-shrink-0 items-center justify-center rounded bg-primary/10 text-xs font-bold text-primary">
                    {tag.sortOrder + 1}
                  </span>
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2">
                      <span className="truncate text-sm font-medium text-foreground">
                        {tag.name}
                      </span>
                      <span
                        className={`flex items-center gap-1 rounded px-1.5 py-0.5 text-[10px] font-medium ${badge.className}`}
                      >
                        {tag.status === "extracting" && (
                          <Loader2 className="h-3 w-3 animate-spin" />
                        )}
                        {badge.label}
                      </span>
                    </div>
                    <p className="mt-0.5 line-clamp-2 text-xs text-muted-foreground">
                      {tag.description || tag.chapterHeading}
                    </p>
                  </div>
                </div>
              </button>
            );
          })
        )}
      </div>

      {/* Count */}
      <div className="flex items-center gap-1.5 border-t border-border px-3 py-2 text-xs text-muted-foreground">
        <BookMarked className="h-3 w-3" />
        {filtered.length} of {tags.length} standards
      </div>
    </div>
  );
}
