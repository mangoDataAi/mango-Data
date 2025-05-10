import { defineConfig } from 'vite'
import baseConfig from './vite.config'

export default defineConfig({
  ...baseConfig,
  server: {
    ...baseConfig.server,
    force: true,
    watch: {
      usePolling: true
    }
  }
}) 