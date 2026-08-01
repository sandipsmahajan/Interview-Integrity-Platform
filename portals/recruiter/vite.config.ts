import react from '@vitejs/plugin-react';
import { fileURLToPath, URL } from 'node:url';
import { loadEnv } from 'vite';
import { defineConfig } from 'vitest/config';

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');

  return {
    plugins: [react()],
    define: {
      __APP_NAME__: JSON.stringify(env.VITE_APP_NAME ?? 'Integrity Pro Recruiter Portal')
    },
    resolve: {
      alias: {
        '@app': fileURLToPath(new URL('./src', import.meta.url)),
        '@shared-ui': fileURLToPath(new URL('../shared/ui/src', import.meta.url)),
        '@shared-components': fileURLToPath(new URL('../shared/components/src', import.meta.url)),
        '@shared-utils': fileURLToPath(new URL('../shared/utils/src', import.meta.url))
      }
    },
    server: {
      port: 5173,
      allowedHosts: ['.monkeycode-ai.live'],
      proxy: {
        '/api': {
          target: env.VITE_API_PROXY_TARGET ?? 'http://localhost:8080',
          changeOrigin: true
        }
      }
    },
    test: {
      environment: 'jsdom',
      globals: true,
      setupFiles: ['./src/test/setup.ts']
    }
  };
});
