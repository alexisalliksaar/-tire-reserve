<template>
  <v-app>
    <AppHeader/>

    <v-main>
      <RouterView/>
    </v-main>

  </v-app>
</template>

<script setup lang="ts">
import { onMounted } from 'vue';
import { useStore } from './store';


const store = useStore();

onMounted(async () => {
  await store.fetchWorkshops();
})

</script>
<style>
.v-application {
  background-color: rgb(255, 252, 245) !important;
}
</style>