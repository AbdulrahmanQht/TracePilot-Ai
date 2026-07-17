import path from "path";
import { defineConfig, mergeConfig } from "vitest/config";
import viteConfig from "./vite.config";

export default mergeConfig(
  viteConfig,
  defineConfig({
    test: {
      environment: "jsdom",
      exclude: [
      "**/node_modules/**",
      "**/dist/**",
      "**/src/e2e/**",
    ],
      globals: true,
      setupFiles: ["./src/test/setup.ts"],
      css: true,
    },
    resolve: {
      alias: { "@": path.resolve(__dirname, "./src") },
    },
  })
);