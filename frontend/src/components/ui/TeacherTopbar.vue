<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter, type RouteLocationNormalizedLoaded } from 'vue-router'

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

const router = useRouter()
const route = useRoute()

function isActive(to: string) {
  return router.currentRoute.value.path === to
}

function humanize(value: string) {
  return value
    .replace(/[-_]/g, ' ')
    .replace(/\b\w/g, (c) => c.toUpperCase())
}

function fillParams(path: string, currentRoute: RouteLocationNormalizedLoaded) {
  return path.replace(/:([A-Za-z0-9_]+)/g, (_, key: string) => {
    const v = currentRoute.params[key]
    if (Array.isArray(v)) return String(v[0] ?? '')
    return v != null ? String(v) : ''
  })
}

const autoPath = computed<Crumb[]>(() => {
  if (!route.meta?.isTeacherRoute) return []

  const crumbs: Crumb[] = []

  route.matched.forEach((record, index) => {
    let label: string | undefined

    const metaBreadcrumb = record.meta?.breadcrumb
    if (typeof metaBreadcrumb === 'string') {
      label = metaBreadcrumb
    } else if (typeof metaBreadcrumb === 'function') {
      label = metaBreadcrumb(route)
    } else if (typeof record.name === 'string' && record.name.length) {
      label = humanize(record.name)
    } else {
      const segment = record.path.split('/').filter(Boolean).at(-1) ?? ''
      label = segment.startsWith(':') ? undefined : humanize(segment)
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
</script>

<template>
  <header class="topbar">
    <nav v-if="effectivePath.length" class="topbar-path">
      <template v-for="(crumb, i) in effectivePath" :key="i">
        <router-link v-if="crumb.to" :to="crumb.to" class="crumb crumb-link">
          {{ crumb.label }}
        </router-link>
        <span v-else class="crumb">{{ crumb.label }}</span>
        <span v-if="i < effectivePath.length - 1" class="crumb-sep">/</span>
      </template>
    </nav>

    <!-- title -->
    <h1 v-if="title" class="topbar-title">{{ title }}</h1>

    <!-- tabs -->
    <div v-else-if="tabs?.length" class="topbar-tabs">
      <router-link
        v-for="tab in tabs"
        :key="tab.to"
        :to="tab.to"
        class="topbar-tab"
        :class="isActive(tab.to) ? 'button-variant-2' : 'button-variant-1'"
      >
        {{ tab.label }}
      </router-link>
    </div>
  </header>
</template>

<style scoped>
.topbar {
  background-color: var(--accent);
  padding: var(--thin-padding) var(--medium-padding);
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.topbar-path {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 0.75rem;
  opacity: 0.75;
}

.crumb {
  color: var(--txt-sec);
}

.crumb-link {
  text-decoration: none;
  transition: opacity var(--animation-time-1) ease-in-out;
}

.crumb-link:hover {
  opacity: 1;
  text-decoration: underline;
}

.crumb-sep {
  color: var(--txt-sec);
  user-select: none;
}

.topbar-title {
  color: var(--txt-sec);
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
}

.topbar-tab {
  text-decoration: none;
  font-size: 0.85rem;
  font-weight: 600;
  padding: 4px var(--medium-padding);
  cursor: pointer;
  white-space: nowrap;
}
</style>