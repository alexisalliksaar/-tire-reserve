<template>
  <v-app-bar app class="px-4">

    <v-img src="@/assets/tire_logo.png" alt="Logo" max-width="42"></v-img>
    <v-toolbar-title class="ml-2">
      <span class="header-title">Tire Reserve</span>
    </v-toolbar-title>

    <v-spacer></v-spacer>

    <nav class="d-flex flex-row">
      <RouterLink to="/home">
        <span class="text-cyan-darken-2">Home</span>
      </RouterLink>

      <p class="mx-2">|</p>

      <RouterLink to="/about">
        <span class="text-cyan-darken-2">About</span>
      </RouterLink>
    </nav>
  </v-app-bar>
</template>

<script setup lang="ts">

</script>

<style scoped lang="scss">
@import "@/styles/variables.scss";

.v-app-bar {
  background-color: darken($tr-background, 5%) !important;
}
</style>
