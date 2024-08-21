/**
 * plugins/index.ts
 *
 * Automatically included in `./src/main.ts`
 */

// Plugins
import { router } from '@/router'
import vuetify from './vuetify'

// Types
import type { App } from 'vue'

import { createPinia } from 'pinia'

const pinia = createPinia();

export function registerPlugins (app: App) {
  app.use(vuetify).use(router).use(pinia)
}
