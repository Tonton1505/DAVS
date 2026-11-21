import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";
import path from "node:path";

export default defineConfig(({ mode }) => {
  const emitSourcemaps = mode === "development";

  return {
    base: "/",
    build: {
      sourcemap: emitSourcemaps ? "inline" : false,
      minify: !emitSourcemaps,
    },
    plugins: [react(), tailwindcss()],
    resolve: {
      alias: {
        "@": path.resolve(import.meta.dirname, "./src"),
      },
    },
    server: {
      host: true,
      port: parseInt(process.env.PORT || "5173"),
      proxy: {
        "/api": {
          target: process.env.VITE_BACKEND_URL || "https://davs-backend.onrender.com",
          changeOrigin: true,
          secure: false,
        },
        "/actuator": {
          target: process.env.VITE_BACKEND_URL || "https://davs-backend.onrender.com",
          changeOrigin: true,
          secure: false,
        },
      },
    },
    preview: {
      host: true,
      port: parseInt(process.env.PORT || "5173"),
    },
  };
});
