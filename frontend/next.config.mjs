import { securityHeaders } from "./lib/security-headers.mjs";

/** @type {import('next').NextConfig} */
const nextConfig = {
  async headers() {
    return securityHeaders(process.env.NODE_ENV, process.env.MYFITNESS_FRONTEND_PUBLIC_ORIGIN);
  }
};

export default nextConfig;
