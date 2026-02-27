type Color = "green" | "red" | "yellow" | "blue" | "gray";

import { ReactNode } from "react";

interface BadgeProps {
  children: ReactNode;
  color?: Color;
}

const colorClasses: Record<Color, string> = {
  green: "bg-green-900/50 text-green-300 border border-green-800",
  red: "bg-destructive/20 text-destructive-foreground border border-destructive/40",
  yellow: "bg-yellow-900/50 text-yellow-300 border border-yellow-800",
  blue: "bg-primary/15 text-primary border border-primary/30",
  gray: "bg-muted text-muted-foreground border border-border",
};

export function Badge({ children, color = "gray" }: BadgeProps) {
  return (
    <span
      className={`inline-block rounded-full px-2 py-0.5 text-xs font-medium ${colorClasses[color]}`}
    >
      {children}
    </span>
  );
}
