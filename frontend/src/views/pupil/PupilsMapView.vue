<template>
  <div class="stops-page">
    <div class="stops-card">
      <TopBar title="Kart" @help="openHelpPopup" @hint="openHintPopup" />

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
            :class="[positions[index], stop.side, { homeNode: stop.isHome }]"
          >
            <div class="path-point"></div>
            <div class="connector"></div>

            <div class="stop-content">
              <div
                v-if="!stop.isHome"
                class="stars"
                :class="{ bottomStars: stop.side === 'bottom' }"
              >
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
                :class="{ locked: !isUnlocked(stop), unlocked: isUnlocked(stop) }"
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
      <div v-if="activeLockedStop" class="popup-overlay" @click="closeLockedPopup">
        <div class="locked-popup" @click.stop>
          <h2 class="popup-title">{{ activeLockedStop.name }} er låst.</h2>

          <p class="popup-text">
            Få 3 stjerner på
            {{ activeLockedStop.requiredPreviousStop }}
            for å låse det opp!
          </p>

          <div class="popup-actions">
            <button class="popup-btn popup-btn-back" @click="closeLockedPopup">Tilbake</button>

            <button class="popup-btn popup-btn-play" @click="goToRequiredStop">Spill</button>
          </div>
        </div>
      </div>
      <div v-if="showHelpPopup" class="popup-overlay" @click="closeHelpPopup">
        <div class="hint-popup" @click.stop>
          <h2 class="popup-title">Trenger du hjelp?</h2>

          <div class="hint-text">
            <p>
              Inne på denne siden kan du trykke deg inn på de ulike stoppestedene og gjøre ulike
              oppgaver knyttet til de.
            </p>
            <ul>
              <li>Begynn med Nyhetskvartalet.</li>
              <li>Fullfør oppgavene, oppnå 3 stjerner, og lås opp neste stoppested.</li>
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
            <p>
              Tenk på kartet som en rekkefølge du må følge.
            </p>
            <ul>
              <li>Stoppesteder med lås kan ikke åpnes før du har 3 stjerner på forrige sted.</li>
              <li>Hvis du står fast, gå tilbake og fullfør tidligere oppgaver.</li>
              <li>Se på stjernene under hvert stoppested for å følge progresjonen din.</li>
            </ul>
          </div>

          <div class="popup-actions">
            <button class="popup-btn popup-btn-play" @click="closeHintPopup">Ok</button>
          </div>
        </div>
      </div>
    </div>
  </div>
  <router-view />
</template>

<script setup lang="ts">
import { computed, onActivated, ref } from 'vue'
import { useRouter } from 'vue-router'
import TopBar from '@/components/TopBar.vue'

import detektivkontoretImage from '@/assets/stoppesteder/detektivkontoret.png'
import lockImage from '@/assets/icons/lock.png'
import nyhetskvartaletImage from '@/assets/stoppesteder/nyhetskvartalet.png'
import fotografenImage from '@/assets/stoppesteder/fotografen.png'
import postkontoretImage from '@/assets/stoppesteder/postkontoret.png'
import passordbankenImage from '@/assets/stoppesteder/passordbanken.png'
import markedsplassenImage from '@/assets/stoppesteder/markedsplassen.png'
import sosialMoteplassImage from '@/assets/stoppesteder/den-sosiale-moteplassen.png'
import datasenteretImage from '@/assets/stoppesteder/datasenteret.png'

type StopSide = 'top' | 'bottom'

type Stop = {
  id: number
  name: string
  stars: number
  image: string
  side: StopSide
  isHome?: boolean
  routePath: string | null
  requiredPreviousStop: string | null
}

const router = useRouter()

const currentPage = ref<number>(0)
const itemsPerPage = 4
const activeLockedStop = ref<Stop | null>(null)
const showHelpPopup = ref<boolean>(false)
const showHintPopup = ref<boolean>(false)

const stops = ref<Stop[]>([
  {
    id: 0,
    name: 'Hjem',
    stars: 0,
    image: detektivkontoretImage,
    side: 'top',
    isHome: true,
    routePath: '/pupil',
    requiredPreviousStop: null,
  },
  {
    id: 1,
    name: 'Nyhetskvartalet',
    stars: 0,
    image: nyhetskvartaletImage,
    side: 'top',
    routePath: '/pupil/classroom/map/news',
    requiredPreviousStop: null,
  },
  {
    id: 2,
    name: 'Fotografen',
    stars: 0,
    image: fotografenImage,
    side: 'bottom',
    routePath: '/pupil/classroom/map/photographer',
    requiredPreviousStop: 'Nyhetskvartalet',
  },
  {
    id: 3,
    name: 'Postkontoret',
    stars: 0,
    image: postkontoretImage,
    side: 'top',
    routePath: '/pupil/classroom/map/postoffice',
    requiredPreviousStop: 'Fotografen',
  },
  {
    id: 4,
    name: 'Passordbanken',
    stars: 0,
    image: passordbankenImage,
    side: 'bottom',
    routePath: '/pupil/classroom/map/passwordbank',
    requiredPreviousStop: 'Postkontoret',
  },
  {
    id: 5,
    name: 'Markedsplassen',
    stars: 0,
    image: markedsplassenImage,
    side: 'top',
    routePath: '/pupil/classroom/map/marketplace',
    requiredPreviousStop: 'Passordbanken',
  },
  {
    id: 6,
    name: 'Den sosiale møteplassen',
    stars: 0,
    image: sosialMoteplassImage,
    side: 'bottom',
    routePath: '/pupil/classroom/map/meetingplace',
    requiredPreviousStop: 'Markedsplassen',
  },
  {
    id: 7,
    name: 'Datasenteret',
    stars: 0,
    image: datasenteretImage,
    side: 'top',
    routePath: '/pupil/classroom/map/datacenter',
    requiredPreviousStop: 'Den sosiale møteplassen',
  },
])

const positions = ['pos-1', 'pos-2', 'pos-3', 'pos-4'] as const

const totalPages = computed<number>(() => Math.ceil(stops.value.length / itemsPerPage))

const pages = computed<number[]>(() =>
  Array.from({ length: totalPages.value }, (_, index) => index),
)

const paginatedStops = computed<Stop[]>(() => {
  const start = currentPage.value * itemsPerPage
  return stops.value.slice(start, start + itemsPerPage)
})

function loadStars(): void {
  const starKeys: Record<string, string> = {
    Nyhetskvartalet: 'nyhetskvartaletStars',
    Fotografen: 'fotografenStars',
    Postkontoret: 'postkontoretStars',
    Passordbanken: 'passordbankenStars',
    Markedsplassen: 'markedsplassenStars',
    'Den sosiale møteplassen': 'denSosialeMoteplassenStars',
    Datasenteret: 'datasenteretStars',
  }

  stops.value.forEach((stop) => {
    const storageKey = starKeys[stop.name]
    if (!storageKey) return

    const savedStars = localStorage.getItem(storageKey)
    stop.stars = savedStars ? JSON.parse(savedStars) : 0
  })
}

function findStopByName(name: string): Stop | undefined {
  return stops.value.find((stop) => stop.name === name)
}

function isUnlocked(stop: Stop): boolean {
  if (stop.isHome) return true
  if (!stop.requiredPreviousStop) return true

  const previousStop = findStopByName(stop.requiredPreviousStop)
  return previousStop?.stars === 3
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

function nextPage(): void {
  if (currentPage.value < totalPages.value - 1) {
    currentPage.value++
  }
}

function prevPage(): void {
  if (currentPage.value > 0) {
    currentPage.value--
  }
}

function goToPage(index: number): void {
  currentPage.value = index
}

function openStop(stop: Stop): void {
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
  loadStars()
})
</script>

<style scoped>
.stops-page {
  min-height: 100vh;
  background: var(--beige-medium);
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

.stops-card {
  width: 100%;
  min-height: 100vh;
  background: var(--beige-medium);
  box-sizing: border-box;
  padding: 28px 36px 24px;
  overflow: hidden;
}

.homeNode {
  left: 0 !important;
}

.homeNode .path-point {
  left: 50%;
  transform: translateX(-50%);
  top: 343px;
}

.homeNode .connector {
  top: 290px;
  height: 48px;
}

.homeNode .stop-content {
  top: 60px;
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
  background: var(--beige-medium);
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
  background: var(--brown-medium);
  z-index: 3;
}

.stop-node .connector {
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  width: 0;
  border-left: 5px dotted var(--brown-medium);
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
  top: 286px;
  height: 52px;
}

.stop-node.bottom .connector {
  top: 378px;
  height: 42px;
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
  -webkit-text-stroke: 2px var(--brown-dark);
}

.star.filled {
  color: var(--yellow-award);
  -webkit-text-stroke: 0;
}

.stop-circle {
  width: 170px;
  height: 170px;
  border-radius: 50%;
  border: none;
  background: rgb(255 255 255 / 72%);
  display: flex;
  justify-content: center;
  align-items: center;
  box-shadow: inset 0 0 0 2px var(--brown-dark-10);
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

.stop-circle.home {
  border: 3px solid var(--brown-medium);
  background: rgb(255 255 255 / 90%);
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
  color: var(--color-text);
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
  border: 3px solid var(--orange-medium);
  background: transparent;
  color: var(--orange-medium);
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
  background: var(--brown-medium);
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
  background: rgb(0 0 0 / 35%);
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

.popup-overlay {
  position: fixed;
  inset: 0;
  background: rgb(0 0 0 / 18%);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.locked-popup {
  width: min(520px, calc(100vw - 32px));
  background: var(--orange-medium);
  border: 4px solid var(--brown-dark);
  border-radius: 28px;
  box-shadow: 0 10px 24px rgb(0 0 0 / 20%);
  padding: 28px 24px 20px;
  text-align: center;
}

.popup-title {
  margin: 0 0 10px;
  font-size: 2rem;
  line-height: 1.15;
  font-weight: 800;
  color: var(--color-text);
}

.popup-text {
  margin: 0;
  font-size: 1.8rem;
  line-height: 1.2;
  font-weight: 700;
  color: var(--color-text);
}

.popup-actions {
  margin-top: 26px;
  display: flex;
  justify-content: center;
  gap: 22px;
  flex-wrap: wrap;
}

.popup-btn {
  min-width: 130px;
  padding: 14px 24px;
  border-radius: 22px;
  border: 4px solid var(--brown-dark);
  font-size: 1.15rem;
  font-weight: 800;
  color: var(--color-text);
  cursor: pointer;
  box-shadow: 0 6px 12px rgb(0 0 0 / 18%);
}

.popup-btn-back {
  background: var(--red-danger);
}

.popup-btn-play {
  background: var(--green-confirm);
}

@media (max-width: 700px) {
  .locked-popup {
    padding: 22px 16px 18px;
    border-radius: 22px;
  }

  .popup-title {
    font-size: 1.45rem;
  }

  .popup-text {
    font-size: 1.3rem;
  }

  .popup-btn {
    min-width: 110px;
    font-size: 1rem;
    padding: 12px 18px;
  }
}

.hint-popup {
  width: min(560px, calc(100vw - 32px));
  background: var(--orange-medium-80);
  border: 4px solid var(--brown-dark);
  border-radius: 28px;
  box-shadow: 0 10px 24px rgb(0 0 0 / 20%);
  padding: 28px 24px 20px;
  text-align: center;
  backdrop-filter: blur(2px);
}

.hint-text {
  margin-top: 10px;
  font-size: 1.1rem;
  line-height: 1.35;
  font-weight: 700;
  color: var(--color-text);
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
