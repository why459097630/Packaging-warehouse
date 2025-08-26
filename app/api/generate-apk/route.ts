// app/api/generate-apk/route.ts
import { NextRequest, NextResponse } from "next/server";

type ReqBody = {
  prompt?: string;
  template?: "core-template" | "form-template" | "simple-template" | string;
  app_name?: string;
  packageName?: string;
};

const GH_OWNER = process.env.GH_OWNER || process.env.NEXT_PUBLIC_GH_OWNER || "";
const GH_REPO  = process.env.GH_REPO  || process.env.NEXT_PUBLIC_GH_REPO  || "Packaging-warehouse";
const GH_TOKEN = process.env.GH_TOKEN || process.env.GH_PAT || "";
const API_BASE = process.env.API_BASE || process.env.NEXT_PUBLIC_API_BASE || "";

export async function POST(req: NextRequest) {
  try {
    if (!GH_OWNER || !GH_REPO || !GH_TOKEN) {
      return NextResponse.json(
        { ok: false, error: "Missing GH_OWNER / GH_REPO / GH_TOKEN env" },
        { status: 500 }
      );
    }

    const body = (await req.json()) as ReqBody;

    const prompt = body.prompt?.trim() || "Generate a demo app";
    const template = (body.template as any) || "form-template";
    const app_name = body.app_name?.trim() || "MyApp";
    const packageName = body.packageName?.trim() || "com.example.app";

    // 统一固定为 generate-apk，避免和工作流 types 不匹配
    const eventType = "generate-apk";

    // 你需要的参数都放 client_payload，工作流里 ${{ github.event.client_payload.xxx }} 能取到
    const clientPayload = {
      prompt,
      template,
      app_name,
      packageName,
      api_base: API_BASE,
      version_name: "1.0.0",
      version_code: "1",
      reason: "api",
      ts: Date.now()
    };

    const resp = await fetch(`https://api.github.com/repos/${GH_OWNER}/${GH_REPO}/dispatches`, {
      method: "POST",
      headers: {
        "Authorization": `token ${GH_TOKEN}`,
        "Accept": "application/vnd.github+json",
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        event_type: eventType,
        client_payload: clientPayload
      })
    });

    const text = await resp.text().catch(() => "");
    if (!resp.ok) {
      return NextResponse.json(
        { ok: false, note: "dispatch failed", status: resp.status, body: text },
        { status: 500 }
      );
    }

    // 204 表示 GitHub 接受到事件
    return NextResponse.json({
      ok: true,
      note: "repository_dispatch sent",
      githubStatus: resp.status,
      payloadSize: JSON.stringify(clientPayload).length
    });
  } catch (e: any) {
    console.error("generate-apk error:", e);
    return NextResponse.json(
      { ok: false, error: e?.message || "unknown" },
      { status: 500 }
    );
  }
}
