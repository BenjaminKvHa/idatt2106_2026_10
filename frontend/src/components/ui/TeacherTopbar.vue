<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter, type RouteLocationNormalizedLoaded } from 'vue-router'
import { useTeacherTopbar } from '@/composables/useTeacherTopbar'

export interface TopbarTab {
  label: string
  to: string
}

type Crumb = { label: string; to?: string }

const props = defineProps<{
  path?: Crumb[]
  title?: string
  tabs?: TopbarTab[]
}>()

const { tabs: composableTabs } = useTeacherTopbar()

const router = useRouter()
const route = useRoute()

function isActive(to: string) {
  return router.currentRoute.value.path === to
}

function fillParams(path: string, currentRoute: RouteLocationNormalizedLoaded) {
  return path.replace(/:([A-Za-z0-9_]+)/g, (_, key: string) => {
    const v = currentRoute.params[key]
    if (Array.isArray(v)) return String(v[0] ?? '')
    return v != null ? String(v) : ''
  })
}

const autoPath = computed<Crumb[]>(() => {
  const crumbs: Crumb[] = []

  route.matched.forEach((record, index) => {
    const metaBreadcrumb = record.meta?.breadcrumb
    if (!metaBreadcrumb) return

    let label: string | undefined
    if (typeof metaBreadcrumb === 'string') {
      label = metaBreadcrumb
    } else if (typeof metaBreadcrumb === 'function') {
      label = metaBreadcrumb(route)
    }

    if (!label) return

    const isLast = index === route.matched.length - 1
    crumbs.push({
      label,
      to: isLast ? undefined : fillParams(record.path, route),
    })
  })

  return crumbs
})

const effectivePath = computed<Crumb[]>(() => {
  if (props.path?.length) return props.path
  return autoPath.value
})

const autoTabs = computed<TopbarTab[]>(() => {
  for (const record of [...route.matched].reverse()) {
    const metaTabs = record.meta?.tabs
    if (typeof metaTabs === 'function') return metaTabs(route)
    if (Array.isArray(metaTabs)) return metaTabs
  }
  return []
})

const effectiveTabs = computed<TopbarTab[]>(() => {
  if (props.tabs?.length) return props.tabs
  if (composableTabs.value.length) return composableTabs.value
  return autoTabs.value
})

const autoTitle = computed<string>(() => {
  if (props.title) return props.title
  const crumbs = effectivePath.value
  return crumbs[crumbs.length - 1]?.label ?? ''
})
</script>

<template>
  <header class="topbar">
    <div class="topbar-top-row">
      <nav v-if="effectivePath.length" class="topbar-path">
        <template v-for="(crumb, i) in effectivePath" :key="i">
          <router-link v-if="crumb.to" :to="crumb.to" class="crumb crumb-link">
            {{ crumb.label }}
          </router-link>
          <span v-else class="crumb">{{ crumb.label }}</span>
          <span v-if="i < effectivePath.length - 1" class="crumb-sep">/</span>
        </template>
      </nav>
    </div>

    <div v-if="effectiveTabs.length" class="topbar-tabs">
      <router-link
        v-for="tab in effectiveTabs"
        :key="tab.to"
        :to="tab.to"
        class="topbar-tab"
        :class="isActive(tab.to) ? 'button-variant-1 topbar-tab-button-1' : 'button-variant-2 topbar-tab-button-2'"
      >
        {{ tab.label }}
      </router-link>
    </div>
    <h1 v-else-if="autoTitle" class="topbar-title">{{ autoTitle }}</h1>
  </header>
</template>

<style scoped>
.topbar {
  background-color: var(--brown-medium-100);
  padding: var(--thin-padding) var(--medium-padding);
  display: flex;
  flex-direction: column;
  gap: 2px;
  padding-bottom: 0;
}

.topbar-top-row {
  display: flex;
  align-items: center;
  min-height: 20px;
}

.topbar-path {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 0.75rem;
  opacity: 0.75;
  flex: 1;
  min-width: 0;
  overflow: hidden;
}

.crumb {
  color: var(--beige-medium-100);
  white-space: nowrap;
}

.crumb-link {
  text-decoration: none;
  transition: opacity var(--animation-time-1) ease-in-out;
}

.crumb-link:hover, .crumb-link:focus-visible {
  opacity: 1;
  text-decoration: underline;
}

.crumb-sep {
  color: var(--beige-medium-100);
  user-select: none;
}

.topbar-title {
  color: var(--beige-medium-100);
  font-size: 1.4rem;
  font-weight: 700;
  margin: 0;
  padding: 2px 0;
}

.topbar-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: var(--thin-gap);
  padding: var(--thin-padding) 0;
  padding-bottom: 0;
  margin-bottom: 0;
}

.topbar-tab {
  text-decoration: none;
  font-size: 0.85rem;
  font-weight: 600;
  padding: 4px var(--medium-padding);
  cursor: pointer;
  white-space: nowrap;
  border-radius: var(--border-radius-2) var(--border-radius-2) 0 0;
}

.topbar-tab-button-1{
  margin-bottom: 0;
  background-color: var(--beige-medium-100);
  border-style: none;
}

.topbar-tab-button-1:hover, .topbar-tab-button-1:focus-visible {
  background-color: var(--beige-medium-100);
  color: var(--brown-dark-100):
}

.topbar-tab-button-2{
  margin-bottom: 0;
  background-color: var(--background-sec);
  border-style: none;
  color: var(--brown-dark-100);
}

.topbar-tab-button-2:hover, .topbar-tab-button-2:focus-visible {
  background-color: var(--beige-medium-100);
  color: var(--brown-dark-100);
.topbar-tab-button-2:hover {
  background-color: var(--beige-medium-100);
  color: var(--brown-dark-100);
}


@media (max-width: 768px) {
  .topbar-tab {
    border-radius: var(--border-radius-2);
  }
  .topbar-tabs {
    padding: var(--thin-padding);
  }
}
</style>
