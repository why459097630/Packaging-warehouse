// app/api/generate-apk/route.ts
import { NextRequest, NextResponse } from "next/server";

// —— 你已有的三件套 ——
// 1) GROQ：把自然语言变成结构化 JSON 规范（失败回退本地生成器）
// 2) 规范→计划（把规范映射为文件编辑计划）
// 3) GitHub Writer：把计划写进仓库并触发 CI
import { callGroqToSpec } from "@/lib/ndjc/groq-client";
import { planFromSpec, generatePlan } from "@/lib/ndjc/generator";
import {
  commitEdits,
  touchRequestFile,
  type FileEdit,
} from "@/lib/ndjc/github-writer";

type Body = {
  prompt: string;
  template?: "core-template" | "form-template" | "simple-template";
  appName?: string;
  packageName?: string;
};

export async function POST(req: NextRequest) {
  try {
    const { prompt, template, appName, packageName } =
      (await req.json()) as Body;

    if (!prompt || typeof prompt !== "string") {
      return NextResponse.json(
        { ok: false, error: "Missing prompt" },
        { status: 400 }
      );
    }

    // 基础信息兜底
    const baseInfo = {
      appName: (appName || "NDJC App").trim(),
      packageName: (packageName || "com.example.app").trim(),
      template: template || "form-template",
    };

    // 1) —— 先 GROQ，失败再回退本地生成器 —— //
    let plan: any;
    try {
      const spec = await callGroqToSpec({
        prompt,
        appName: baseInfo.appName,
        packageName: baseInfo.packageName,
        template: baseInfo.template,
      });
      plan = planFromSpec(spec);
    } catch (err) {
      console.error("[NDJC] GROQ failed, fallback to local:", err);
      plan = await generatePlan({
        prompt,
        appName: baseInfo.appName,
        packageName: baseInfo.packageName,
        template: baseInfo.template,
      });
    }

    if (!plan || !Array.isArray(plan.files) || plan.files.length === 0) {
      throw new Error("NDJC: no edits produced (空包保护)");
    }

    // 2) —— 把 NdjcPatch 正规化为 FileEdit[] —— //
    //    - create/replace: 用 content / contentBase64
    //    - patch: 规范化为 patches: [{ patch: string }]
    const rawEdits = plan.files.map((f: any) => {
      const normMode =
        f.mode === "replace" ? "replace" : f.mode === "patch" ? "patch" : "create";

      if (normMode === "patch") {
        const patches =
          Array.isArray(f.patches) && f.patches.length > 0
            ? f.patches // 已是 [{ patch: string, ... }] 之类结构
            : [{ patch: f.patch ?? "" }]; // 兜底把单条字符串 patch 包成数组

        return {
          path: f.path,
          mode: "patch",
          patches,
        };
      }

      if (f.contentBase64) {
        return {
          path: f.path,
          mode: normMode,
          contentBase64: f.contentBase64,
        };
      }

      return {
        path: f.path,
        mode: normMode,
        content: f.content ?? "",
      };
    });

    // 关键：整体 cast，避免与你项目里 github-writer 的 FileEdit 泛型细节不匹配导致 TS 报错
    const edits = rawEdits as unknown as FileEdit[];

    // 3) —— 生成 requestId，落地计划，写入补丁，触发 CI —— //
    const requestId = `${Date.now()}`;

    // 把这次的“计划”也写入仓库，便于回放/诊断
    const metaFile: FileEdit = {
      path: `requests/${requestId}.plan.json`,
      mode: "create",
      content: JSON.stringify(
        {
          requestId,
          prompt,
          ...baseInfo,
          plan,
          createdAt: new Date().toISOString(),
        },
        null,
        2
      ),
    };

    // 统一提交（commit 一次），push 触发你现有的 GitHub Actions（矩阵打包工作流）
    await commitEdits(
      [metaFile, ...edits],
      `NDJC: apply plan for "${plan.appName}" [${baseInfo.template}]`
    );

    // 也顺手打一个“触发文件”，如果你的某些工作流需要它
    await touchRequestFile(requestId, { from: "generate-apk" });

    return NextResponse.json({ ok: true, requestId });
  } catch (e: any) {
    console.error("[NDJC] /api/generate-apk error:", e);
    return NextResponse.json(
      { ok: false, error: e?.message || String(e) },
      { status: 500 }
    );
  }
}
