<script setup lang="ts">
type AvatarAsset = {
  assetType: string
  name: string
  filetype: string
}

const props = defineProps<{
  title: string
  assets: AvatarAsset[]
  selectedAssetName: string | null
}>()

const emit = defineEmits<{
  select: [asset: AvatarAsset]
}>()

function selectAsset(asset: AvatarAsset) {
  emit('select', asset)
}
</script>

<template>
  <section class="asset-group">
    <h2>{{ props.title }}</h2>

    <div class="asset-grid">
      <button
        v-for="asset in props.assets"
        :key="`${asset.assetType}-${asset.name}`"
        type="button"
        class="asset-button"
        :class="{ active: props.selectedAssetName === asset.name }"
        @click="selectAsset(asset)"
      >
        <div class="asset-preview">
          <img :src="`/api/avatar-assets/${asset.assetType}/${asset.name}.${asset.filetype}`" :alt="asset.name" />
        </div>
      </button>
    </div>
  </section>
</template>

<style scoped>
.asset-group {
  display: flex;
  flex-direction: column;
  gap: 0.75rem;
}

.asset-group h2 {
  margin: 0;
  color: var(--brown-dark);
}

.asset-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(5.75rem, 5.75rem));
  gap: 0.75rem;
  justify-content: start;
}

.asset-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 5.75rem;
  height: 5.75rem;
  padding: 0.5rem;
  border-radius: 1rem;
  border: var(--border-thin) solid var(--brown-dark);
  background: white;
  cursor: pointer;
  color: var(--brown-dark);
  font: inherit;
  font-weight: 600;
}

.asset-preview {
  position: relative;
  width: 100%;
  height: 100%;
  border-radius: 0.85rem;
  overflow: hidden;
  background: radial-gradient(circle at top, #fff8ec 0%, #f2d7a1 100%);
}

.asset-preview img {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  object-fit: contain;
  padding: 0.35rem;
  box-sizing: border-box;
}

.asset-button.active {
  background: var(--orange-light, #f8d8a8);
  box-shadow: inset 0 0 0 2px rgba(85, 50, 27, 0.18);
}

.asset-button:hover {
  transform: translateY(-1px);
}
</style>
