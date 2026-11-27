import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";
import { fileURLToPath } from "node:url";

const srcDir = fileURLToPath(new URL("./src", import.meta.url));

interface MinimalProxyEmitter {
  on: (
    event: string,
    listener: (
      err: Error,
      req: unknown,
      res: {
        headersSent?: boolean;
        writeHead?: (status: number, headers: Record<string, string>) => void;
        end?: (body: string) => void;
      },
    ) => void,
  ) => void;
  removeAllListeners: (event: string) => void;
}

function attachGracefulOfflineProxy(proxy: MinimalProxyEmitter) {
  const handleOffline = (
    _err: Error,
    _req: unknown,
    res: {
      headersSent?: boolean;
      writeHead?: (status: number, headers: Record<string, string>) => void;
      end?: (body: string) => void;
    },
  ) => {
    if (res && typeof res.writeHead === "function" && !res.headersSent) {
      res.writeHead(503, { "Content-Type": "application/json; charset=utf-8" });
      res.end?.(
        JSON.stringify({
          code: "BACKEND_OFFLINE",
          message: "Backend chưa khởi động — đang sử dụng dữ liệu cục bộ.",
        }),
      );
    }
  };
  proxy.on("error", handleOffline);
  queueMicrotask(() => {
    proxy.removeAllListeners("error");
    proxy.on("error", handleOffline);
  });
}

export default defineConfig(({ mode }) => {
  const emitSourcemaps = mode === "development";
  const backendTarget = process.env.VITE_BACKEND_URL || "http://localhost:8080";

  return {
    base: "/",
    build: {
      sourcemap: emitSourcemaps ? "inline" : false,
      minify: !emitSourcemaps,
    },
    plugins: [react(), tailwindcss()],
    resolve: {
      alias: {
        "@": srcDir,
      },
    },
    server: {
      host: true,
      port: parseInt(process.env.PORT || "5173", 10),
      proxy: {
        "/api": {
          target: backendTarget,
          changeOrigin: true,
          configure: (proxy) => attachGracefulOfflineProxy(proxy as unknown as MinimalProxyEmitter),
        },
        "/actuator": {
          target: backendTarget,
          changeOrigin: true,
          configure: (proxy) => attachGracefulOfflineProxy(proxy as unknown as MinimalProxyEmitter),
        },
      },
    },
    preview: {
      host: true,
      port: parseInt(process.env.PORT || "5173", 10),
    },
  };
});
