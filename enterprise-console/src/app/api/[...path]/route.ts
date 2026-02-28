import { NextRequest, NextResponse } from "next/server";

type Params = { path: string[] };

async function proxy(req: NextRequest, params: Params): Promise<NextResponse> {
  const backendUrl = process.env.BACKEND_URL ?? "http://localhost:19000";
  const path = params.path.join("/");
  const search = req.nextUrl.search;
  const target = `${backendUrl}/api/${path}${search}`;

  const headers: Record<string, string> = {};
  req.headers.forEach((value, key) => {
    if (key !== "host") headers[key] = value;
  });

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
