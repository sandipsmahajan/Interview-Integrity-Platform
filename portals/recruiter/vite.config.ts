import react from '@vitejs/plugin-react';
import { loadEnv, type Plugin } from 'vite';
import { defineConfig } from 'vitest/config';
import {
  copyFileSync,
  createReadStream,
  existsSync,
  mkdirSync,
  readdirSync,
  statSync
} from 'node:fs';
import { dirname, extname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const ROOT = dirname(fileURLToPath(import.meta.url));
const IMAGES_ROOT = resolve(ROOT, '../images');

const MIME_TYPES: Record<string, string> = {
  '.ico': 'image/x-icon',
  '.png': 'image/png',
  '.svg': 'image/svg+xml',
  '.jpeg': 'image/jpeg',
  '.jpg': 'image/jpeg',
  '.webmanifest': 'application/manifest+json'
};

/**
 * Serves and copies the shared portal images (portals/images) so the recruiter
 * app can reference them as /favicons/* and /logos/* without duplicating files.
 */
function sharedPortalImages(): Plugin {
  return {
    name: 'shared-portal-images',
    configureServer(server) {
      server.middlewares.use((req, res, next) => {
        const url = decodeURIComponent((req.url ?? '').split('?')[0]);
        const isImage = url.startsWith('/favicons/') || url.startsWith('/logos/');
        if (!isImage) {
          next();
          return;
        }
        const file = resolve(IMAGES_ROOT, url.slice(1));
        if (!file.startsWith(IMAGES_ROOT) || !existsSync(file) || !statSync(file).isFile()) {
          next();
          return;
        }
        res.setHeader(
          'Content-Type',
          MIME_TYPES[extname(file).toLowerCase()] ?? 'application/octet-stream'
        );
        res.setHeader('Cache-Control', 'no-cache');
        createReadStream(file).pipe(res);
      });
    },
    closeBundle() {
      copyTree(IMAGES_ROOT, join(ROOT, 'dist'));
    }
  };
}

function copyTree(source: string, target: string): void {
  if (!existsSync(source)) return;
  for (const entry of readdirSync(source)) {
    const from = join(source, entry);
    if (statSync(from).isDirectory()) {
      copyTree(from, target);
      continue;
    }
    const relativePath = from.replace(IMAGES_ROOT, '');
    const to = join(target, relativePath);
    mkdirSync(dirname(to), { recursive: true });
    copyFileSync(from, to);
  }
}

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');

  return {
    plugins: [react(), sharedPortalImages()],
    define: {
      __APP_NAME__: JSON.stringify(env.VITE_APP_NAME ?? 'Integrity Pro Recruiter Portal')
    },
    publicDir: false,
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
