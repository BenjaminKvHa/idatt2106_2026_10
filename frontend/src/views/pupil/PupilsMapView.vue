<template>
  <div class="stops-page">
    <div class="stops-card">
      <TopBar title="Kart" @help="openHelpPopup" @hint="openHintPopup" />

      <div class="map-wrapper">
        <div class="map-area">
          <div class="path-line"></div>

          <div
            v-for="(stop, index) in stops"
            :key="stop.id"
            class="stop-node"
            :class="[
              !stop.isHome && !stop.isEnd ? positions[index - 1] : '',
              stop.side,
              { homeNode: stop.isHome, endNode: stop.isEnd },
            ]"
          >
            <div class="path-point"></div>

            <div class="stop-content">
              <div
                v-if="!stop.isHome"
                class="stars"
                :class="{ bottomStars: stop.side === 'bottom' }"
              >
                <span
                  v-for="starIndex in stop.maxStars"
                  :key="starIndex"
                  :class="['star', starIndex <= stop.stars ? 'filled' : 'empty']"
                  >★</span
                >
              </div>

              <div v-else class="stars home-stars-spacer" aria-hidden="true"></div>

              <button
                class="stop-circle"
                :class="{
                  locked: !isUnlocked(stop),
                  unlocked: isUnlocked(stop),
                  home: stop.isHome,
                }"
                @click="openStop(stop)"
              >
                <div class="image-wrapper">
                  <img :src="stop.image" :alt="stop.name" class="stop-image" />
                  <img
                    v-if="!isUnlocked(stop) && !stop.isHome"
                    :src="lockImage"
                    class="lock-overlay"
                    alt="Låst"
                  />
                </div>
              </button>

              <h3 class="stop-title">{{ stop.name }}</h3>
            </div>
          </div>
        </div>
      </div>

      <p v-if="loadingProgress" class="map-status" role="status">Laster progresjon...</p>
      <p v-else-if="progressError" class="map-status pupil-error" role="alert">
        {{ progressError }}
      </p>
    </div>
    <p v-if="loadingProgress" class="map-status" role="status">Laster progresjon...</p>
    <p v-else-if="progressError" class="map-status pupil-error" role="alert">{{ progressError }}</p>

    <div v-if="activeLockedStop" class="pupil-popup-overlay" @click="closeLockedPopup">
      <div class="locked-popup" @click.stop>
        <h2 class="pupil-popup-title">{{ activeLockedStop.name }} er låst.</h2>
        <p class="pupil-popup-text">
          Få 3 stjerner på {{ activeLockedStop.requiredPreviousStop }} for å låse det opp!
        </p>
        <div class="pupil-popup-actions">
          <button class="pupil-popup-btn popup-btn-back" @click="closeLockedPopup">Tilbake</button>
          <button class="pupil-popup-btn popup-btn-play" @click="goToRequiredStop">Spill</button>
        </div>
      </div>
    </div>

    <div v-if="showHelpPopup" class="pupil-popup-overlay" @click="closeHelpPopup">
      <div class="pupil-hint-popup" @click.stop>
        <h2 class="pupil-popup-title">Trenger du hjelp?</h2>
        <div class="hint-text">
          <p>
            Inne på denne siden kan du trykke deg inn på de ulike stoppestedene og gjøre ulike
            oppgaver knyttet til de.
          </p>
          <ul>
            <li>Begynn med Nyhetskvartalet.</li>
            <li>Fullfør oppgavene og tjen stjerner for å låse opp neste stoppested.</li>
            <li>Fullfør alle oppgavene på alle stoppestedene og slå bossen i Datasenteret.</li>
            <li>Trykk på Hjem for å gå tilbake til hovedskjermen.</li>
          </ul>
        </div>
        <div class="popup-actions">
          <button class="popup-btn popup-btn-play" @click="closeHelpPopup">Ok</button>
        </div>
      </div>
    </div>

    <div v-if="showHintPopup" class="popup-overlay" @click="closeHintPopup">
      <div class="hint-popup" @click.stop>
        <h2 class="popup-title">Tips 💡</h2>
        <div class="hint-text">
          <p>Tenk på kartet som en rekkefølge du må følge.</p>
          <ul>
            <li>Stoppesteder med lås kan ikke åpnes før forrige kapittel er fullført.</li>
            <li>Hvis du står fast, gå tilbake og fullfør tidligere oppgaver.</li>
            <li>Se på stjernene under hvert stoppested for å følge progresjonen din.</li>
          </ul>
        </div>
        <div class="pupil-popup-actions">
          <button class="pupil-popup-btn popup-btn-play" @click="closeHintPopup">Ok</button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onActivated, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import TopBar from '@/components/pupil/TopBar.vue'
import { get } from '@/utils/apiUtils'

import detektivkontoretImage from '@/assets/icons/stoppesteder/detektivkontoret.png'
import lockImage from '@/assets/icons/topbar/lock.png'
import nyhetskvartaletImage from '@/assets/icons/stoppesteder/nyhetskvartalet.png'
import fotografenImage from '@/assets/icons/stoppesteder/fotografen.png'
import datasenteretImage from '@/assets/icons/stoppesteder/datasenteret.png'

type StopSide = 'top' | 'bottom'

type PupilChapterProgressResponse = {
  chapterId: number
  highscore: number
  unlocked: boolean
}

type Stop = {
  id: number
  name: string
  chapterId: number | null
  maxStars: number
  stars: number
  unlocked: boolean
  image: string
  side: StopSide
  isHome?: boolean
  isEnd?: boolean
  routePath: string | null
  requiredPreviousStop: string | null
}

const router = useRouter()

const activeLockedStop = ref<Stop | null>(null)
const showHelpPopup = ref<boolean>(false)
const showHintPopup = ref<boolean>(false)
const loadingProgress = ref<boolean>(false)
const progressError = ref<string>('')

const positions = ['pos-1', 'pos-2'] as const

const stops = ref<Stop[]>([
  {
    id: 0,
    name: 'Hjem',
    chapterId: null,
    maxStars: 0,
    stars: 0,
    unlocked: true,
    image: detektivkontoretImage,
    side: 'top',
    isHome: true,
    routePath: '/pupil',
    requiredPreviousStop: null,
  },
  {
    id: 1,
    name: 'Nyhetskvartalet',
    chapterId: 1,
    maxStars: 3,
    stars: 0,
    unlocked: false,
    image: nyhetskvartaletImage,
    side: 'top',
    routePath: '/pupil/classroom/map/news',
    requiredPreviousStop: null,
  },
  {
    id: 2,
    name: 'Fotografen',
    chapterId: 2,
    maxStars: 3,
    stars: 0,
    unlocked: false,
    image: fotografenImage,
    side: 'bottom',
    routePath: '/pupil/classroom/map/photographer',
    requiredPreviousStop: 'Nyhetskvartalet',
  },
  {
    id: 3,
    name: 'Datasenteret',
    chapterId: 3,
    maxStars: 5,
    stars: 0,
    unlocked: false,
    image: datasenteretImage,
    side: 'top',
    isEnd: true,
    routePath: '/pupil/classroom/map/datacenter',
    requiredPreviousStop: 'Fotografen',
  },
])

async function loadProgress(): Promise<void> {
  loadingProgress.value = true
  progressError.value = ''

  try {
    const progress = await get<PupilChapterProgressResponse[]>('/api/pupil/progress')
    const progressByChapterId = new Map(progress.map((entry) => [entry.chapterId, entry]))

    stops.value = stops.value.map((stop) => {
      if (stop.isHome) {
        return { ...stop, unlocked: true, stars: 0 }
      }

      if (stop.chapterId === null) {
        return { ...stop, stars: 0, unlocked: false }
      }

      const chapterProgress = progressByChapterId.get(stop.chapterId)
      if (!chapterProgress) {
        return { ...stop, stars: 0, unlocked: stop.requiredPreviousStop === null }
      }

      return {
        ...stop,
        stars: Math.max(0, Math.min(stop.maxStars, chapterProgress.highscore)),
        unlocked: chapterProgress.unlocked,
      }
    })
  } catch (error) {
    console.error('Unable to load pupil progress for map', error)
    progressError.value = 'Kunne ikke hente progresjon akkurat nå.'
  } finally {
    loadingProgress.value = false
  }
}

function findStopByName(name: string): Stop | undefined {
  return stops.value.find((stop) => stop.name === name)
}

function isUnlocked(stop: Stop): boolean {
  if (stop.isHome) return true
  if (stop.requiredPreviousStop === null) return true
  return stop.unlocked
}

function openHelpPopup(): void {
  showHelpPopup.value = true
}

function closeHelpPopup(): void {
  showHelpPopup.value = false
}

function openHintPopup(): void {
  showHintPopup.value = true
}

function closeHintPopup(): void {
  showHintPopup.value = false
}

function openStop(stop: Stop): void {
  if (loadingProgress.value) return

  if (stop.isHome) {
    goBack()
    return
  }

  if (!isUnlocked(stop)) {
    activeLockedStop.value = stop
    return
  }

  if (stop.routePath) {
    router.push(stop.routePath)
  }
}

function closeLockedPopup(): void {
  activeLockedStop.value = null
}

function goToRequiredStop(): void {
  if (!activeLockedStop.value?.requiredPreviousStop) return

  const previousStop = findStopByName(activeLockedStop.value.requiredPreviousStop)
  if (!previousStop?.routePath) return

  closeLockedPopup()
  router.push(previousStop.routePath)
}

function goBack(): void {
  router.push('/pupil')
}

onActivated(() => {
  void loadProgress()
})

onMounted(() => {
  void loadProgress()
})
</script>

<style scoped>
.stops-page {
  min-height: 100vh;
  margin: 0;
  padding: 0;
  box-sizing: border-box;

  background-image: url('@/assets/backgrounds/map-bg.png');
  background-size: cover;
  background-position: center top;
  background-repeat: no-repeat;
  background-attachment: scroll;
}

.stops-card {
  width: 100%;
  min-height: 100vh;
  background: rgba(234, 211, 180, 0.35);
  box-sizing: border-box;
  padding: 28px 36px 24px;
  overflow: visible;
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
  border: 4px solid var(--brown-dark, #7a5b43);
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
  color: var(--color-text, #111);
}

.back-btn {
  background: var(--red-danger, #ff433b);
  color: var(--color-text, #111);
  border: 5px solid var(--color-text, #111);
  border-radius: 26px;
  padding: 18px 38px;
  font-size: 2rem;
  font-weight: 800;
  cursor: pointer;
}

.homeNode {
  left: var(--road-start-x) !important;
}

.homeNode .path-point {
  top: 343px;
  display: none;
}

.homeNode .connector {
  top: 330px;
  height: 31px;
}

.homeNode .stop-content {
  top: 260px;
}

.endNode {
  left: var(--road-end-x) !important;
  right: auto !important;
}

.endNode .stop-content {
  top: 200px;
}

.endNode .path-point {
  display: none;
}

.map-wrapper {
  position: relative;
  padding: 20px 90px 0;
}

.map-area {
  position: relative;
  min-height: 900px;
  --stop-node-width: clamp(180px, 12vw, 240px);
  --stop-circle-size: clamp(108px, 9vw, 170px);
  --road-start-x: 8%;
  --road-one-third-x: 36%;
  --road-two-thirds-x: 64%;
  --road-end-x: 92%;
}

.path-line {
  position: absolute;
  left: var(--road-start-x);
  right: calc(100% - var(--road-end-x));
  top: 360px;
  transform: translateY(-50%);
  height: 40px;
  border-radius: 999px;
  background: rgba(245, 237, 224, 0.95);
  box-shadow: inset 0 0 0 2px rgba(122, 91, 67, 0.12);
}

.stop-node {
  position: absolute;
  width: var(--stop-node-width);
  left: 0;
  transform: translateX(-50%);
}

.stop-node .path-point {
  position: absolute;
  left: 50%;
  top: 343px;
  transform: translateX(-50%);
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: var(--brown-medium, #bf7b33);
  z-index: 3;
}

.stop-node .connector {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  width: 0;
  border-left: 5px dotted var(--brown-medium, #8a6546);
  z-index: 1;
  display: none;
}

.stop-node .stop-content {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  width: var(--stop-node-width);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 10px 0;
}

.stop-node.pos-1 {
  left: var(--road-one-third-x);
}

.stop-node.pos-2 {
  left: var(--road-two-thirds-x);
}

.stop-node.pos-3 {
  left: var(--road-two-thirds-x);
}

.stop-node.top .connector {
  top: 170px;
  height: 152px;
}

.stop-node.bottom .connector {
  top: 386px;
  height: 34px;
}

.stop-node.bottom .stop-content {
  top: 400px;
  flex-direction: column;
}

.stop-node.bottom .bottomStars {
  order: 0;
  margin-top: 0;
  margin-bottom: 12px;
}

.stop-node.bottom .stop-circle {
  order: 1;
}

.stop-node.bottom .stop-title {
  order: 2;
  margin-top: 14px;
}

.stars {
  display: flex;
  gap: 6px;
  margin-bottom: 12px;
  padding: 6px 12px;
  border-radius: 16px;
  background: rgba(245, 237, 224, 0.5);
  backdrop-filter: blur(2px);
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
  filter: drop-shadow(0 2px 3px rgba(0, 0, 0, 0.18));
}

.star.empty {
  color: rgba(245, 237, 224, 0.98);
  -webkit-text-stroke: 2.5px var(--brown-dark, #7a5b43);
}

.star.filled {
  color: var(--yellow-award, #f0c33a);
  -webkit-text-stroke: 1.5px var(--brown-dark, #7a5b43);
}

.stop-circle {
  width: var(--stop-circle-size);
  height: var(--stop-circle-size);
  border-radius: 50%;
  border: 4px solid rgba(146, 94, 44, 0.8);
  background: rgba(245, 237, 224, 0.96);
  display: flex;
  justify-content: center;
  align-items: center;
  box-shadow:
    0 8px 18px rgba(0, 0, 0, 0.12),
    inset 0 0 0 3px rgba(255, 255, 255, 0.3);
  overflow: hidden;
  padding: 0;
}

.stop-circle.unlocked {
  cursor: pointer;
}

.stop-circle.unlocked:hover {
  transform: scale(1.05);
  transition: 0.2s;
}

.stop-circle.locked {
  cursor: not-allowed;
  opacity: 0.75;
  filter: grayscale(30%);
}

.stop-circle.home {
  border: 4px solid var(--brown-medium, #bf7b33);
  background: rgba(245, 237, 224, 1);
}

.stop-image {
  width: 82%;
  height: 82%;
  object-fit: contain;
}

.stop-title {
  margin-top: 12px;
  font-size: 1.75rem;
  font-weight: 800;
  line-height: 1.1;
  color: var(--color-text, #111);
  text-align: center;
  max-width: 260px;

  background: rgba(245, 237, 224, 0.92);
  border: 3px solid rgba(122, 91, 67, 0.25);
  border-radius: 18px;
  padding: 10px 18px;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.06);

  display: inline-block;
  min-width: 170px;
}

.nav-arrow {
  position: absolute;
  top: 360px;
  transform: translateY(-50%);
  width: 66px;
  height: 66px;
  border-radius: 50%;
  border: 3px solid var(--brown-medium, #bf7b33);
  background: rgba(245, 237, 224, 0.95);
  color: var(--brown-medium, #bf7b33);
  font-size: 2.2rem;
  font-weight: 800;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.08);
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

.map-status {
  margin: 12px 0 0;
  text-align: center;
  font-size: 1rem;
  color: var(--color-text, #111);
}

.map-status.error {
  color: #a12a2a;
}

.pagination-dot {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  border: none;
  background: var(--brown-medium, #bf7b33);
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
    left: var(--road-start-x);
    right: calc(100% - var(--road-end-x));
  }

  .stop-node .stop-content {
    width: var(--stop-node-width);
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
    top: 360px;
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
    --road-start-x: 10%;
    --road-one-third-x: 36.667%;
    --road-two-thirds-x: 63.333%;
    --road-end-x: 90%;
  }

  .path-line {
    height: 22px;
  }

  .stop-node .stop-content {
    width: var(--stop-node-width);
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
    --road-start-x: 12%;
    --road-one-third-x: 37.333%;
    --road-two-thirds-x: 62.667%;
    --road-end-x: 88%;
  }

  .stop-node .path-point {
    width: 28px;
    height: 28px;
  }

  .stop-node .path-point {
    top: 328px;
  }

  .path-line {
    top: 328px;
  }

  .stop-node.top .connector {
    top: calc(var(--path-y) - 60px);
    height: 60px;
  }

  .stop-node.bottom .connector {
    top: calc(var(--path-y) + 6px);
    height: 60px;
  }

  .stop-node.bottom .stop-content {
    top: 395px;
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
  width: 52%;
  height: 52%;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: rgba(0, 0, 0, 0.35);
  border-radius: 50%;
  padding: 9%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.lock-overlay img {
  width: 100%;
  height: 100%;
  object-fit: contain;
}

.locked-popup {
  width: min(520px, calc(100vw - 32px));
  background: var(--orange-medium);
  border: var(--border-wide) solid var(--brown-dark);
  border-radius: 28px;
  box-shadow: 0 10px 24px var(--brown-dark-20);
  padding: 28px 24px 20px;
  text-align: center;
}

.popup-btn-back {
  background: var(--red-danger, #ef3b37);
}

.popup-btn-play {
  background: var(--green-confirm, #8ee22d);
}

.home-stars-spacer {
  display: none;
}

@media (max-width: 700px) {
  .locked-popup {
    padding: 22px 16px 18px;
    border-radius: 22px;
  }

  .popup-btn {
    min-width: 110px;
    font-size: 1rem;
    padding: 12px 18px;
  }
}

.hint-popup {
  width: min(560px, calc(100vw - 32px));
  background: rgba(234, 148, 71, 0.72);
  background: var(--orange-medium-80, rgba(234, 148, 71, 0.72));
  border: 4px solid var(--brown-dark, #6f4d33);
  border-radius: 28px;
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.2);
  padding: 28px 24px 20px;
  text-align: center;
  backdrop-filter: blur(2px);
}

.hint-text {
  margin-top: 10px;
  font-size: 1.1rem;
  line-height: 1.35;
  font-weight: 700;
  color: var(--color-text, #111);
  text-align: left;
}

.hint-text p {
  margin: 0 0 8px;
  font-size: 1.2rem;
}

.hint-text ul {
  margin: 0;
  padding-left: 24px;
}

.hint-text li {
  margin-bottom: 6px;
}
</style>
