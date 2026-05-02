import { fileURLToPath } from 'node:url'
import { mergeConfig, defineConfig, configDefaults } from 'vitest/config'
import viteConfig from './vite.config'

export default mergeConfig(
  viteConfig,
  defineConfig({
    resolve: {
      alias: {
        '@pdanpdan/vue-keyboard-trap': fileURLToPath(
          new URL('./tests/mocks/vue-keyboard-trap.ts', import.meta.url),
        ),
      },
    },
    test: {
      environment: 'jsdom',
      exclude: [...configDefaults.exclude, 'e2e/**'],
      root: fileURLToPath(new URL('./', import.meta.url)),
      setupFiles: ['tests/setup.ts'],
      coverage: {
        provider: 'v8',
        reporter: ['text', 'html', 'lcov'],
        reportsDirectory: 'coverage',
        include: ['src/**/*.{ts,vue}'],
        exclude: [
          ...(configDefaults.coverage.exclude ?? []),
          'src/main.ts',
        ],
      },
    },
  }),
)
