<script setup lang="ts">
import TopBar from "@/components/TopBar.vue";

type TaskPicture = {
  id: number;
  label: string;
  imageUrl?: string;
};

// Temporary placeholder-data, change later when connected to task bank
const pictures: TaskPicture[] = [
  {
    id: 1,
    label: "Bilde 1",
    imageUrl: "",
  },
  {
    id: 2,
    label: "Bilde 2",
    imageUrl: "",
  },
];

function chooseFakePicture(picture: TaskPicture) {
  console.log(`Valgte ${picture.label} som falsk`);
}

</script>

<template>
  <TopBar title = "Nyhetskvartalet"/>

  <div class = "page">
    <div class = "center-container">
      <h2>Aktivitet 1</h2>
      <div class = "task-wrapper">
        <p>To avisforsider ligger foran deg.</p>
        <p>Én er ekte og én er falsk.</p>
        <p>Kan du finne ut hvilken som er falsk?</p>
      </div>

      <div class = "picture-wrapper">
        <div v-for="picture in pictures"
             :key="picture.id"
             class="picture-card"
        >
        <div class="picture-placeholder">
          <img
            v-if="picture.imageUrl"
            :src="picture.imageUrl"
            :alt="picture.label"
            class="picture"
          />
          <span v-else>{{ picture.label }}</span>
        </div>
          <button
            class="fake-button"
            @click="chooseFakePicture(picture)"
          >
            Dette er den falske
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
h2 {
  margin: 0;
  font-size: clamp(1.8rem, 4vw, 2.5rem);
}

.page {
  display: flex;
  justify-content: center;
  padding: 2rem 1rem;
}

.center-container {
  margin: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  width: 100%;
  max-width: 1100px;
}

.picture-wrapper {
  display: flex;
  gap: 2rem;
  margin-top: 2rem;
  flex-wrap: wrap;
  justify-content: center;
  width: 100%;
}

.picture-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1rem;
  flex: 1 1 300px;
  max-width: 420px;
  width: 100%;
}

.picture-placeholder {
  width: min(100%, 420px);
  aspect-ratio: 420 / 320;
  background-color: var(--beige-light);
  border: 2px solid var(--brown-dark);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: clamp(1rem, 2vw, 2rem);
  font-weight: 700;
}

.picture {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.fake-button {
  width: 100%;
  max-width: 420px;
  padding: 1rem 2rem;
  border-radius: 38px;
  border: 5px solid var(--brown-dark);
  background-color: var(--orange-medium);
  font-size: clamp(1rem, 2vw, 1.2rem);
  font-weight: 700;
  cursor: pointer;
  transition: background-color 0.2s ease, transform 0.2s ease, filter 0.2s ease;
}

.fake-button:hover {
  filter: brightness(1.1);
  transform: scale(1.02);
}

.fake-button:active {
  transform: scale(0.97);
}

.task-wrapper {
  margin: 0;
  width: min(100%, 800px);
  font-size: clamp(1rem, 2.5vw, 2rem);
  padding: 1rem 2rem;
  background: var(--orange-medium);
  border: 5px solid var(--brown-dark);
  border-radius: 38px;
  box-sizing: border-box;
}

.task-wrapper p {
  margin: 0;
}

/**Scaling for smaller screens*/
@media (max-width: 768px) {
  .page {
    padding: 1rem;
  }

  .picture-wrapper {
    gap: 1rem;
  }

  .task-wrapper {
    padding: 1rem;
    border-width: 3px;
    border-radius: 24px;
  }

  .fake-button {
    border-width: 3px;
    border-radius: 24px;
  }
}
</style>
