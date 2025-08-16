import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';
import { Ratelimit } from '@upstash/ratelimit';
import { Redis } from '@upstash/redis';

// 若未配置 Upstash，将自动放行（但建议尽快配置）
const hasUpstash = !!process.env.UPSTASH_REDIS_REST_URL && !!process.env.UPSTASH_REDIS_REST_TOKEN;
const redis = hasUpstash ? new Redis({
  url: process.env.UPSTASH_REDIS_REST_URL!,
  token: process.env.UPSTASH_REDIS_REST_TOKEN!,
}) : null;

const ratelimit = redis ? new Ratelimit({
  redis,
  limiter: Ratelimit.fixedWindow(3, '1 h'), // 一小时 3 次
  analytics: true,
}) : null;

export async function middleware(req: NextRequest) {
  if (!req.nextUrl.pathname.startsWith('/api')) return NextResponse.next();
  if (!ratelimit) return NextResponse.next();

  const ip = req.ip || req.headers.get('x-forwarded-for') || 'unknown';
  const { success, reset, remaining } = await ratelimit.limit(`api:${ip}`);

  if (!success) {
    const retryAfter = Math.max(0, Math.ceil((reset - Date.now()) / 1000));
    return NextResponse.json(
      { ok: false, error: 'RATE_LIMIT', message: `请求过于频繁，请 ${retryAfter}s 后重试` },
      { status: 429 }
    );
  }

  const res = NextResponse.next();
  res.headers.set('X-RateLimit-Remaining', String(remaining));
  return res;
}

export const config = { matcher: ['/api/:path*'] };
