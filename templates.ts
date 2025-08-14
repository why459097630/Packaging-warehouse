import { Octokit } from "octokit";

export default async function handler(req, res) {
  try {
    const octokit = new Octokit({ auth: process.env.GITHUB_TOKEN });
    const owner   = process.env.OWNER || "why459097630";
    const repo    = process.env.REPO  || "Packaging-warehouse";

    const { data } = await octokit.request(
      "GET /repos/{owner}/{repo}/contents/{path}",
      { owner, repo, path: "templates" }
    );

    const names = data
      .filter(item => item.type === "dir")
      .map(item => item.name)
      // 只保留三大模板，防止误选
      .filter(n => ["core-template", "form-template", "simple-template"].includes(n));

    res.status(200).json({ ok: true, templates: names });
  } catch (e) {
    res.status(500).json({ ok: false, error: e?.message || String(e) });
  }
}
