#!/usr/bin/env node
import { spawnSync } from "node:child_process";
import { existsSync } from "node:fs";
import path from "node:path";
import { fileURLToPath } from "node:url";

const rootDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const frontendDir = path.join(rootDir, "frontend-react");
const nodeModulesDir = path.join(frontendDir, "node_modules");
const isWindows = process.platform === "win32";
const npmCmd = isWindows ? "npm.cmd" : "npm";

function runNpm(args) {
  const result = spawnSync(npmCmd, args, {
    cwd: frontendDir,
    stdio: "inherit",
    shell: isWindows,
    env: process.env,
  });
  if (result.error) {
    console.error(`[DAVS] Không thể chạy lệnh ${npmCmd}:`, result.error.message);
    process.exit(1);
  }
  return result.status ?? 0;
}

if (!existsSync(nodeModulesDir) || !existsSync(path.join(nodeModulesDir, "vite"))) {
  console.log("[DAVS] Đang cài đặt thư viện Frontend lần đầu (frontend-react)...");
  const installCode = runNpm(["install"]);
  if (installCode !== 0) {
    process.exit(installCode);
  }
}

const scriptArgs = process.argv.slice(2);
if (scriptArgs.length === 0 || scriptArgs[0] === "ensure-deps") {
  process.exit(0);
}

const exitCode = runNpm(["run", ...scriptArgs]);
process.exit(exitCode);
