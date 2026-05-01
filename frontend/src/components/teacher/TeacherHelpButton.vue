<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { X } from '@lucide/vue'

type TeacherHelpSection = {
  heading?: string
  body?: string[]
  items?: string[]
}

const props = withDefaults(
  defineProps<{
    title: string
    sections: TeacherHelpSection[]
    ariaLabel?: string
    buttonClass?: string
  }>(),
  {
    ariaLabel: 'Hjelp',
    buttonClass: '',
  },
)

const open = ref(false)
const titleId = `teacher-help-title-${Math.random().toString(36).slice(2)}`

const buttonClasses = computed(() => ['icon-btn-circle', 'teacher-help-trigger', props.buttonClass])

function close() {
  open.value = false
}

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape') {
    close()
  }
}

watch(open, (isOpen) => {
  if (isOpen) {
    document.addEventListener('keydown', onKeydown)
  } else {
    document.removeEventListener('keydown', onKeydown)
  }
})

onBeforeUnmount(() => {
  document.removeEventListener('keydown', onKeydown)
})
</script>

<template>
  <button type="button" :class="buttonClasses" :aria-label="ariaLabel" @click="open = true">
    <span class="teacher-help-mark" aria-hidden="true">?</span>
  </button>

  <Teleport v-if="open" to="body">
    <div class="teacher-help-backdrop" @click.self="close">
      <aside
        class="teacher-help-panel teacher-variant-1"
        role="dialog"
        aria-modal="true"
        :aria-labelledby="titleId"
      >
        <header class="teacher-help-header">
          <h2 :id="titleId" class="inika">{{ title }}</h2>
          <button type="button" class="teacher-help-close icon-btn-circle" aria-label="Lukk hjelp" @click="close">
            <X class="teacher-help-icon" />
          </button>
        </header>

        <div class="teacher-help-content">
          <section v-for="(section, sectionIndex) in sections" :key="sectionIndex">
            <h3 v-if="section.heading">{{ section.heading }}</h3>
            <p v-for="(paragraph, paragraphIndex) in section.body" :key="paragraphIndex">
              {{ paragraph }}
            </p>
            <ul v-if="section.items?.length">
              <li v-for="item in section.items" :key="item">{{ item }}</li>
            </ul>
          </section>
        </div>
      </aside>
    </div>
  </Teleport>
</template>

<style scoped>
.teacher-help-trigger {
  --button-diameter: 48px;
}

.teacher-help-mark {
  display: block;
  color: currentColor;
  font-size: 1.75rem;
  font-weight: 600;
  line-height: 1;
}

.teacher-help-trigger:hover .teacher-help-mark {
  color: var(--txt-sec);
}

.teacher-help-backdrop {
  position: fixed;
  inset: 0;
  z-index: 220;
  display: flex;
  justify-content: flex-end;
  background: var(--obscure);
}

.teacher-help-panel {
  width: min(31rem, calc(100vw - 1.2rem));
  height: calc(100dvh - 1.2rem);
  margin: 0.6rem;
  padding: var(--thick-padding);
  overflow: auto;
  box-shadow: -0.5rem 0 1.5rem rgba(0, 0, 0, 0.24);
}

.teacher-help-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--medium-gap);
}

.teacher-help-header h2 {
  margin: 0;
  font-size: clamp(1.35rem, 2vw, 1.8rem);
}

.teacher-help-close {
  --button-diameter: 42px;
}

.teacher-help-icon {
  width: 18px;
  height: 18px;
}

.teacher-help-content {
  display: flex;
  flex-direction: column;
  gap: var(--medium-gap);
  line-height: 1.45;
}

.teacher-help-content section {
  display: flex;
  flex-direction: column;
  gap: 0.45rem;
}

.teacher-help-content h3,
.teacher-help-content p,
.teacher-help-content ul {
  margin: 0;
}

.teacher-help-content h3 {
  font-size: 1.05rem;
}

.teacher-help-content ul {
  padding-left: 1.2rem;
}

.teacher-help-content li + li {
  margin-top: 0.35rem;
}

@media (max-width: 640px) {
  .teacher-help-backdrop {
    justify-content: center;
  }

  .teacher-help-panel {
    width: calc(100vw - 0.8rem);
    height: calc(100dvh - 0.8rem);
    margin: 0.4rem;
    padding: var(--medium-padding);
  }
}
</style>
