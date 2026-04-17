<script setup lang="ts">
import Logo from '@/components/misc/Logo.vue';
</script>

<template>
    <div class="teacher-menu teacher-variant-2">
        <Logo />

        <button type="button" class="button-variant-1 teacher-menu-button">
            Hjem
        </button>

        <section class="teacher-card">
            <div class="teacher-card-title">Klasserom</div>
            <div class="teacher-card-body">
                <button type="button" class="button-variant-1 teacher-card-item">
                    Ekeberg 6C
                </button>
            </div>
        </section>

        <button type="button" class="button-variant-1 teacher-menu-button">
            Nytt klasserom
        </button>
    </div>
</template>

<style scoped>
.teacher-menu {
    border-radius: 0;
    border: none;
    outline-width: var(--border-width);
    outline-color: var(--accent);
    outline-style: solid;
    padding: var(--medium-padding);
    height: 100%;
}

.teacher-menu-button {
    width: 100%;
    padding: 0.85rem 1rem;
}

.teacher-card-body {
    padding: 0.8rem;
}

.teacher-card-item {
    width: 100%;
    text-align: left;
}

</style>