<script setup lang="ts">
import ProfileButton from "@/components/ProfileButton.vue";
</script>

<template>
  <h1> Nyhetskvartalet </h1>
  <h2>Aktivitet 1</h2>
  <router-link to="/pupil/profile">
    <ProfileButton />
  </router-link>
  <p>To avisforsider ligger foran deg.</p>
  <p>Én er ekte og én er falsk.</p>
  <p>Kan du finne ut hvilken som er falsk?</p>
  <div>BILDE1</div>
  <div>BILDE2</div>
  <button>Dette er den falske</button>
  <button>Dette er den falske</button>
</template>
