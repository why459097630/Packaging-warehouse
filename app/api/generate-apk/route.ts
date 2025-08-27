// /app/api/generate-apk/route.ts
import { NextRequest, NextResponse } from "next/server";
import { commitAndBuild } from "@/lib/ndjc/generator";
import { buildDiffFilesFromGroq } from "@/lib/ndjc/diff"; // 你的差量注入器

export async function POST(req: NextRequest) {
  const body = await req.json();

  // 1) 调 GROQ，得到代码/结构化片段（略）
  // const groqResult = await callGroq(body.prompt, ...);

  // 2) 用差量注入器产出最终 files[]
  const files = await buildDiffFilesFromGroq(/* groqResult, templates, ... */);

  // 3) 落盘+触发构建
  const { requestId } = await commitAndBuild({
    owner: process.env.GH_OWNER!,
    repo: process.env.PACKAGING_REPO!,
    branch: process.env.PACKAGING_BRANCH || "main",
    files,
    meta: { prompt: body.prompt, template: body.template, appName: body.appName },
    githubToken: process.env.GH_TOKEN!   // ← Fine-grained Token / PAT
  });

  return NextResponse.json({ ok: true, requestId });
}
