<script setup lang="ts">
</script>

<template>
  <div class="teacher-edit-tasks-page-container">
    <div class="stop-navigation-container">
      <button class="go-to-stop-btn">
        Nyhetskvartalet
      </button>
      <button class="go-to-stop-btn">
        Fotografen
      </button>
      <button class="go-to-stop-btn active-go-to-stop-btn">
        Passordbanken
      </button>
      <button class="go-to-stop-btn">
        Postkontoret
      </button>
    </div>
    <div>
      <div class="task-difficulty-container">
        Medium
        <div class="tasks-container">
          <div class="task-container">
            <label>
              Rediger oppgavebeskrivelsen
              <br />
              <textarea placeholder="Skriv oppgavebeskrivelse her..."></textarea>
            </label>
            <!-- Her burde det være custom kode for hver oppgavetype -->
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.teacher-edit-tasks-page-container {
  display: flex;
  gap: 1rem;
}
.stop-navigation-container {
  display: flex;
  flex-direction: column;
  gap: 1rem;
}
.go-to-stop-btn {
  background-color: var(--beige-light);
  border: var(--border-thin) solid var(--brown-dark);
  color: var(--brown-dark);
  padding: 1rem .5rem;
  border-radius: 1rem;
  font-weight: 600;
}
.go-to-stop-btn.active-go-to-stop-btn, .go-to-stop-btn:hover {
  background-color: var(--background-sec);
}
.task-difficulty-container {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  border: var(--border-thin) solid var(--brown-dark);
  background-color: var(--beige-light);
  padding: 1rem;
  border-radius: 1rem;
  
  .tasks-container {
    display: flex;
    flex-direction: column;
    gap: 1rem;
    .task-container {
      background-color: var(--background-sec);
      padding: 1rem .5rem;
      border-radius: .5rem;
    }
  }
}
</style>
