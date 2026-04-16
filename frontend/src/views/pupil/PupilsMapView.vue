<template>
  <div class="stops-page">
    <div class="stops-card">
      <header class="top-bar">
        <div class="top-left">
          <button class="square-btn" aria-label="Profil">
            <img src="@/assets/icons/profil.png" alt="Profil" class="top-icon-image" />
          </button>

          <button class="square-btn help-btn" aria-label="Hjelp">?</button>
        </div>

        <button class="back-btn" @click="goBack">Tilbake</button>
      </header>

      <div class="map-wrapper">
        <button
          v-if="currentPage > 0"
          class="nav-arrow left"
          @click="prevPage"
          aria-label="Forrige side"
        >
          ←
        </button>

        <div class="map-area">
          <div class="path-line"></div>

          <div
            v-for="(stop, index) in paginatedStops"
            :key="stop.id"
            class="stop-node"
            :class="[positions[index], stop.side]"
          >
            <div class="path-point"></div>
            <div class="connector"></div>

            <div class="stop-content">
              <div class="stars" :class="{ bottomStars: stop.side === 'bottom' }">
                <span
                  v-for="starIndex in 3"
                  :key="starIndex"
                  :class="['star', starIndex <= stop.stars ? 'filled' : 'empty']"
                >
                  ★
                </span>
              </div>

              <button
                class="stop-circle"
                :class="{ locked: stop.locked, unlocked: !stop.locked }"
                :disabled="stop.locked"
                @click="openStop(stop)"
              >
                <div class="image-wrapper">
                  <img :src="stop.image" :alt="stop.name" class="stop-image" />

                  <img v-if="stop.locked" :src="lockImage" class="lock-overlay" alt="Låst" />
                </div>
              </button>

              <h3 class="stop-title">{{ stop.name }}</h3>
            </div>
          </div>
        </div>

        <button
          v-if="currentPage < totalPages - 1"
          class="nav-arrow right"
          @click="nextPage"
          aria-label="Neste side"
        >
          →
        </button>
      </div>

      <div class="pagination">
        <button
          v-for="(_, index) in pages"
          :key="index"
          class="pagination-dot"
          :class="{ active: index === currentPage }"
          @click="goToPage(index)"
          :aria-label="`Gå til side ${index + 1}`"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'

// Bytt disse til filene du laster opp senere
import profileImage from '@/assets/icons/profil.png'
import lockImageFile from '@/assets/icons/lock.png'
import nyhetskvartaletImage from '@/assets/stoppesteder/nyhetskvartalet.png'
import fotografenImage from '@/assets/stoppesteder/fotografen.png'
import postkontoretImage from '@/assets/stoppesteder/postkontoret.png'
import passordbankenImage from '@/assets/stoppesteder/passordbanken.png'
import markedsplassenImage from '@/assets/stoppesteder/markedsplassen.png'
import sosialMoteplassImage from '@/assets/stoppesteder/den-sosiale-moteplassen.png'
import datasenteretImage from '@/assets/stoppesteder/datasenteret.png'

const currentPage = ref(0)
const itemsPerPage = 4

const lockImage = lockImageFile

const stops = ref([
  {
    id: 1,
    name: 'Nyhetskvartalet',
    stars: 0,
    image: nyhetskvartaletImage,
    locked: false,
    side: 'top',
  },
  {
    id: 2,
    name: 'Fotografen',
    stars: 0,
    image: fotografenImage,
    locked: true,
    side: 'bottom',
  },
  {
    id: 3,
    name: 'Postkontoret',
    stars: 0,
    image: postkontoretImage,
    locked: true,
    side: 'top',
  },
  {
    id: 4,
    name: 'Passordbanken',
    stars: 0,
    image: passordbankenImage,
    locked: true,
    side: 'bottom',
  },
  {
    id: 5,
    name: 'Markedsplassen',
    stars: 0,
    image: markedsplassenImage,
    locked: true,
    side: 'top',
  },
  {
    id: 6,
    name: 'Den sosiale møteplassen',
    stars: 0,
    image: sosialMoteplassImage,
    locked: true,
    side: 'bottom',
  },
  {
    id: 7,
    name: 'Datasenteret',
    stars: 0,
    image: datasenteretImage,
    locked: true,
    side: 'top',
  },
])

const positions = ['pos-1', 'pos-2', 'pos-3', 'pos-4']

const totalPages = computed(() => Math.ceil(stops.value.length / itemsPerPage))
const pages = computed(() => Array.from({ length: totalPages.value }))

const paginatedStops = computed(() => {
  const start = currentPage.value * itemsPerPage
  return stops.value.slice(start, start + itemsPerPage)
})

function nextPage() {
  if (currentPage.value < totalPages.value - 1) {
    currentPage.value++
  }
}

function prevPage() {
  if (currentPage.value > 0) {
    currentPage.value--
  }
}

function goToPage(index) {
  currentPage.value = index
}

function openStop(stop) {
  if (stop.locked) return

  console.log('Åpner stoppested:', stop.name)
  // router.push(`/stoppested/${stop.id}`)
}

function goBack() {
  window.history.back()
}
</script>

<style scoped>
.stops-page {
  min-height: 100vh;
  background: #ead3b4;
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.stops-card {
  width: 100%;
  min-height: 100vh;
  background: #ead3b4;
  box-sizing: border-box;
  padding: 28px 36px 24px;
  overflow: hidden;
}

.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.top-left {
  display: flex;
  gap: 18px;
}

.square-btn {
  width: 92px;
  height: 92px;
  border-radius: 22px;

  border: 4px solid #7a5b43;

  background: #ffffff;

  display: flex;
  align-items: center;
  justify-content: center;

  cursor: pointer;
  overflow: hidden;
}

.top-icon-image {
  width: 56px;
  height: 56px;
  object-fit: contain;
}

.help-btn {
  font-size: 2.6rem;
  font-weight: 800;
  color: #1c1712;
}

.back-btn {
  background: #ff433b;
  color: #111;
  border: 5px solid #111;
  border-radius: 26px;
  padding: 18px 38px;
  font-size: 2rem;
  font-weight: 800;
  cursor: pointer;
}

.map-wrapper {
  position: relative;
  padding: 20px 90px 0;
}

.map-area {
  position: relative;
  min-height: 720px;
}

.path-line {
  position: absolute;
  left: 90px;
  right: 90px;
  top: 50%;
  transform: translateY(-50%);
  height: 28px;
  border-radius: 999px;
  background: #d4af7c;
}

.stop-node {
  position: absolute;
  width: 240px;
  left: 0;
}

.stop-node .path-point {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: #bf7b33;
  z-index: 3;
}

.stop-node .connector {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  width: 0;
  border-left: 5px dotted #8a6546;
  z-index: 1;
}

.stop-node .stop-content {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  width: 260px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.stop-node.pos-1 {
  left: 6%;
}

.stop-node.pos-2 {
  left: 28%;
}

.stop-node.pos-3 {
  left: 62%;
}

.stop-node.pos-4 {
  left: 84%;
}

.stop-node .path-point {
  top: 343px;
}

.stop-node.top .connector {
  top: 280px;
  height: 70px;
}

.stop-node.top .stop-content {
  top: 0;
}

.stop-node.bottom .connector {
  top: 377px;
  height: 70px;
}

.stop-node.bottom .stop-content {
  top: 430px;
}

.stars {
  display: flex;
  gap: 6px;
  margin-bottom: 12px;
}

.bottomStars {
  order: 2;
  margin-top: 12px;
  margin-bottom: 0;
}

.stop-node.bottom .stop-circle {
  order: 1;
}

.stop-node.bottom .stop-title {
  order: 3;
  margin-top: 14px;
}

.star {
  font-size: 3rem;
  line-height: 1;
}

.star.empty {
  color: transparent;
  -webkit-text-stroke: 2px #7a5b43;
}

.star.filled {
  color: #f0c33a;
  -webkit-text-stroke: 0;
}

.stop-circle {
  width: 170px;
  height: 170px;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.72);
  display: flex;
  justify-content: center;
  align-items: center;
  box-shadow: inset 0 0 0 2px rgba(122, 91, 67, 0.08);
  overflow: hidden;
  padding: 0;
}

.stop-circle.unlocked {
  cursor: pointer;
}

.stop-circle.unlocked:hover {
  transform: scale(1.03);
  transition: 0.2s;
}

.stop-circle.locked {
  cursor: not-allowed;
  opacity: 0.6;
  filter: grayscale(40%);
}

.stop-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.stop-title {
  margin-top: 12px;
  font-size: 1.75rem;
  font-weight: 800;
  line-height: 1.1;
  color: #111;
  text-align: center;
  max-width: 260px;
}

.nav-arrow {
  position: absolute;
  top: 50%;
  transform: translateY(-50%);
  width: 66px;
  height: 66px;
  border-radius: 50%;
  border: 3px solid #c9873a;
  background: transparent;
  color: #c9873a;
  font-size: 2.2rem;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.nav-arrow.left {
  left: 0;
}

.nav-arrow.right {
  right: 0;
}

.pagination {
  display: flex;
  justify-content: center;
  gap: 18px;
  margin-top: 10px;
}

.pagination-dot {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: none;
  background: #bf7b33;
  opacity: 0.45;
  cursor: pointer;
}

.pagination-dot.active {
  opacity: 1;
  transform: scale(1.15);
}

@media (max-width: 1400px) {
  .map-wrapper {
    padding: 20px 60px 0;
  }

  .path-line {
    left: 60px;
    right: 60px;
  }

  .stop-node {
    width: 220px;
  }

  .stop-node .stop-content {
    width: 220px;
  }

  .stop-circle {
    width: 150px;
    height: 150px;
  }

  .stop-image {
    width: 78px;
    height: 78px;
  }

  .stop-title {
    font-size: 1.5rem;
  }
}

@media (max-width: 1100px) {
  .map-area {
    min-height: 980px;
  }

  .path-line {
    left: 40px;
    right: 40px;
    top: 50%;
  }

  .stop-node.pos-1 {
    left: 4%;
  }

  .stop-node.pos-2 {
    left: 24%;
  }

  .stop-node.pos-3 {
    left: 54%;
  }

  .stop-node.pos-4 {
    left: 76%;
  }

  .back-btn {
    font-size: 1.5rem;
    padding: 14px 26px;
  }

  .square-btn {
    width: 78px;
    height: 78px;
  }
}

@media (max-width: 900px) {
  .stops-card {
    padding: 20px 16px;
  }

  .map-wrapper {
    padding: 12px 44px 0;
  }

  .map-area {
    min-height: 1180px;
  }

  .path-line {
    left: 20px;
    right: 20px;
    height: 22px;
  }

  .stop-node {
    width: 180px;
  }

  .stop-node .stop-content {
    width: 180px;
  }

  .stop-node.pos-1 {
    left: 0%;
  }

  .stop-node.pos-2 {
    left: 20%;
  }

  .stop-node.pos-3 {
    left: 48%;
  }

  .stop-node.pos-4 {
    left: 72%;
  }

  .stop-circle {
    width: 126px;
    height: 126px;
  }

  .stop-image {
    width: 62px;
    height: 62px;
  }

  .star {
    font-size: 2.3rem;
  }

  .stop-title {
    font-size: 1.25rem;
  }

  .nav-arrow {
    width: 54px;
    height: 54px;
    font-size: 1.7rem;
  }
}

@media (max-width: 700px) {
  .top-bar {
    align-items: center;
  }

  .square-btn {
    width: 64px;
    height: 64px;
    border-radius: 18px;
  }

  .top-icon-image {
    width: 40px;
    height: 40px;
  }

  .help-btn {
    font-size: 2rem;
  }

  .back-btn {
    font-size: 1.15rem;
    padding: 10px 18px;
    border-radius: 18px;
  }

  .map-wrapper {
    padding: 10px 36px 0;
  }

  .map-area {
    min-height: 980px;
  }

  .path-line {
    left: 10px;
    right: 10px;
  }

  .stop-node .path-point {
    width: 28px;
    height: 28px;
  }

  .stop-node .path-point {
    top: 328px;
  }

  .stop-node.top .connector {
    top: 248px;
    height: 82px;
  }

  .stop-node.bottom .connector {
    top: 356px;
    height: 82px;
  }

  .stop-node.bottom .stop-content {
    top: 395px;
  }

  .stop-circle {
    width: 108px;
    height: 108px;
  }

  .star {
    font-size: 2rem;
  }

  .stop-title {
    font-size: 1.08rem;
  }
}

.image-wrapper {
  position: relative;
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  border-radius: 50%;
  overflow: hidden;
}

.lock-overlay {
  position: absolute;

  width: 85px;
  height: 85px;

  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);

  background: rgba(0, 0, 0, 0.35);
  border-radius: 50%;
  padding: 16px;

  display: flex;
  align-items: center;
  justify-content: center;
}

.lock-overlay img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}
</style>
