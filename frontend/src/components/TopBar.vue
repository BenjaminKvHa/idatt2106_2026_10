<script setup lang="ts">
import { useRouter } from 'vue-router'
import ProfileButton from '@/components/ProfileButton.vue'
import { ChevronLeftIcon, HomeIcon, SettingsIcon } from '@lucide/vue';
import { getJwtData } from '@/utils/jwtUtils';

const props = defineProps<{
  title?: string
  profileName?: string | null
  profileAvatarUrl?: string | null
  hint?: string
  help?: string

}>()

const tokenData = getJwtData();

function openHelp(): void {
  alert(props.help || "No help")
}

function openHint(): void {
  alert(props.hint || "No hint")
}
</script>

<template>
  <div class="mega-nav-container">
    <div class="nav-container heavy-shadow">
      <div>
        <button @click="$router.go(-1)" class="nav-icon-parent transition">
          <ChevronLeftIcon />
        </button>
        <RouterLink to="/pupil" class="nav-icon-parent transition">
          <HomeIcon />
        </RouterLink>
      </div>
      <div class="nav-center-text">
        {{ title }}
      </div>
      <div>
        <RouterLink to="/pupil/settings" class="nav-icon-parent transition">
          <SettingsIcon />
        </RouterLink>
        <RouterLink to="/pupil/profile" class="avatar-button">
          <img src="http://localhost:3000/api/avatar-assets/costume/detective.png" />
          {{ tokenData?.legalName || tokenData?.email || "No name" }}
        </RouterLink>
      </div>
    </div>
    <div class="extras">
      <button v-if="hint" @click="openHint" class="heavy-shadow transition">
        Få hint!
      </button>
      <button v-if="help" @click="openHelp" class="heavy-shadow transition">
        Hjelp
      </button>
    </div>
  </div>
</template>

<style scoped>
.avatar-button {
  border: var(--border-thin) solid var(--brown-dark);
  border-radius: 1rem;
  background-color: var(--beige-light);
  color: var(--brown-dark);
  text-decoration: none;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: .25rem 1rem;
  font-weight: 600;
  gap: .5rem;

  > img {
    width: 2.5rem;
    height: 2.5rem;
    border-radius: 100%;
    background-color: white;
    border: 1px solid var(--brown-dark-40);
  }
}

.extras {
  position: absolute;
  right: 0;
  top: 5rem;
  display: flex;
  gap: 1rem;
  margin-top: 1rem;
  margin-right: 2rem;
  > button {
    border: var(--border-wide) solid var(--brown-dark);
    border-radius: 1rem;
    padding: .5rem 1rem;
    background-color: var(--beige-light);
    color: var(--brown-dark);
    font-weight: 600;
  }
  > button:hover {
    background-color: var(--brown-dark);
    color: var(--beige-light);
  }
}
.mega-nav-container {
  position: relative;
}
.nav-center-text {
  text-decoration: underline;
  color: var(--brown-dark);
  font-weight: 600;
}
.nav-icon-parent {
  width: 3rem;
  height: 3rem;
  border-radius: 100%;
  border: var(--border-thin) solid var(--brown-dark);
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: var(--beige-light);
  /* background-color: red; */
  stroke: var(--brown-dark);
}
.nav-icon-parent > svg {
  stroke: var(--brown-dark);
}
.nav-icon-parent:hover > svg {
  stroke: var(--beige-light);
}
.nav-icon-parent:hover {
  background-color: var(--brown-dark);
  stroke: var(--beige-light);
}

.nav-container {
  background-color: var(--beige-medium);
  width: 100%;
  height: 5rem;
  display: flex;
  justify-content: space-between;
  padding-left: 2rem;
  padding-right: 2rem;
  border-bottom: var(--border-wide) solid var(--brown-dark);

  > div {
    display: flex;
    justify-content: center;
    align-items: center;
    flex-direction: row;
    gap: 1rem;

  }
}

</style>
