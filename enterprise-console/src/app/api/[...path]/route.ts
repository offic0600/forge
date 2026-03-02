import { NextRequest, NextResponse } from "next/server";
import { auth } from "@/auth";

type Params = { path: string[] };

async function proxy(req: NextRequest, params: Params): Promise<NextResponse> {
  const backendUrl = process.env.BACKEND_URL ?? "http://localhost:19000";
  const path = params.path.join("/");
  const search = req.nextUrl.search;
  const target = `${backendUrl}/api/${path}${search}`;

  // Get session for Authorization header
  const session = await auth();

  const headers: Record<string, string> = {};
  req.headers.forEach((value, key) => {
    // Skip host (would confuse backend) and cookie (next-auth session JWT makes
    // the Cookie header too large for Tomcat's default 8KB limit)
    if (key === "host" || key === "cookie") return;
    headers[key] = value;
  });

  // Forward access token to backend
  if (session?.accessToken) {
    headers["Authorization"] = `Bearer ${session.accessToken}`;
  }

  const body =
    req.method === "GET" || req.method === "HEAD"
      ? undefined
      : await req.arrayBuffer();

  try {
    const res = await fetch(target, {
      method: req.method,
      headers,
      body: body ?? undefined,
    });

    const respHeaders = new Headers();
    res.headers.forEach((value, key) => {
      // Node.js fetch auto-decompresses gzip, so drop encoding/length headers
      // to avoid ERR_CONTENT_DECODING_FAILED in the browser
      if (key === "content-encoding" || key === "content-length") return;
      respHeaders.set(key, value);
    });

    return new NextResponse(res.body, {
      status: res.status,
      headers: respHeaders,
    });
  } catch (e) {
    console.error("[proxy] Backend unreachable:", target, e);
    return NextResponse.json(
      { error: "Backend unavailable", target },
      { status: 502 }
    );
  }
}

export async function GET(
  req: NextRequest,
  { params }: { params: Promise<Params> }
) {
  return proxy(req, await params);
}

export async function POST(
  req: NextRequest,
  { params }: { params: Promise<Params> }
) {
  return proxy(req, await params);
}

export async function PUT(
  req: NextRequest,
  { params }: { params: Promise<Params> }
) {
  return proxy(req, await params);
}

export async function DELETE(
  req: NextRequest,
  { params }: { params: Promise<Params> }
) {
  return proxy(req, await params);
}

export async function PATCH(
  req: NextRequest,
  { params }: { params: Promise<Params> }
) {
  return proxy(req, await params);
}
