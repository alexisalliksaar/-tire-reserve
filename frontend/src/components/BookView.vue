<template>
  <VContainer class="fill-height align-content-start pa-2">
    <VCard class="w-100">

      <template v-slot:title>
        <div class="my-2">
          <span class="text-h5">Booking Info</span>

        </div>
      </template>

      <template v-slot:text>
        <div class="d-flex flex-row justify-space-between">
          <div class="d-flex flex-column ga-1">
            <div class="text-body-1">
              <span class="font-weight-medium"> Workshop: </span>
              <span>{{ store.bookingInfo?.workshopNameDisplay }} </span>
            </div>
            <div class="text-body-1">
              <span class="font-weight-medium"> Address: </span>
              <span>{{ store.bookingInfo?.addressDisplay }} </span>
            </div>
            <div class="text-body-1">
              <span class="font-weight-medium"> Date: </span>
              <span>{{ store.bookingInfo?.dateDisplay }} </span>
            </div>
            <div class="text-body-1">
              <span class="font-weight-medium"> Time: </span>
              <span>{{ store.bookingInfo?.timeDisplay }} </span>
            </div>
            <div class="text-body-1">
              <span class="font-weight-medium">Serviceable Vehicle Types: </span>
              <span>{{ store.bookingInfo?.serviceableVehiclesDisplay }} </span>
            </div>
          </div>
          <div class="mr-2 d-flex flex-column justify-end mb-4">
            <VBtn @click="navigateHome" style="width: min-content;" color="grey-lighten-2">
              <VIcon icon="mdi-arrow-left-bold" start></VIcon>
              Home
            </VBtn>
          </div>
        </div>
        <VDivider thickness="2" class="my-2"></VDivider>

        <div class="d-flex flex-column ga-1">
          <div class="text-h5 mb-2">
            Your Contact Information
          </div>
          <div class="d-flex flex-row justify-space-between">
            <div class="d-flex flex-row ga-2">
              <VTextField
                label="Email address*"
                density="comfortable"
                :rules="required"
                min-width="250"
                v-model="email"
                ></VTextField>

              <VTextField
                label="Phone number*"
                density="comfortable"
                :rules="required"
                min-width="250"
                v-model="phoneNumber"
                ></VTextField>
            </div>
            <div class="d-flex flex-column justify-end ml-2">
              <VBtn @click="bookTime" color="primary" class="mb-6" :disabled="isBooked ?? false">
                Book
                <VIcon icon="mdi-car" end></VIcon>
              </VBtn>
            </div>
          </div>
          <div v-if="success || errorMessage">
            <div v-if="success" class="d-flex flex-row ga-2 align-center">
              <VIcon color="success" icon="mdi-check-circle"></VIcon>
              <span class="text-success text-body-1">Tire change time successfully booked</span>
            </div>
            <div v-if="errorMessage" class="d-flex flex-row ga-2 align-center">
              <VIcon color="error" icon="mdi-alert-circle"></VIcon>
              <span class="text-error text-body-1">{{ errorMessage }}</span>
            </div>
          </div>
        </div>
      </template>
    </VCard>
  </VContainer>
</template>

<script setup lang="ts">
import axios from '@/axios';
import { useStore } from '@/store';
import { onBeforeMount, ref } from 'vue';
import { useRouter } from 'vue-router';


const store = useStore();
const router = useRouter();

const email = ref("");
const phoneNumber = ref("");

const success = ref(null as null | boolean);
const errorMessage = ref(null as null | string);
const isBooked = ref(null as null | boolean)

onBeforeMount(() => {
  if (! store.bookingInfo) {
    navigateHome()
  }
})

function navigateHome() {
  router.push({ name: "Home" })
}

async function bookTime() {
  success.value = null;
  errorMessage.value = null;

  const body = {
    contactInformation: JSON.stringify({
      email: email.value,
      phoneNumber: phoneNumber.value
    }),
    id: store.bookingInfo?.id,
    workshopId: store.bookingInfo?.workshopId
  }

  await axios.post("/tire-change-times/available/book", body)
    .then((_response) => {
      success.value = true;
      isBooked.value = true;
    }).catch((error) => {
      success.value = false;
      if (error.response) {
        console.error("Server responded:", error.response.data.message, error.response.status);

        if (error.response?.status === 422) {
          errorMessage.value = "Someone has already booked this tire change time, please choose another time";
          isBooked.value = true;
          return;
        }
      } else if (error.request) {
        console.error("The request was made but no response was received, Request: ", error.request);
      } else {
        console.error('Something happened in setting up the request that triggered an Error, Error', error.message);
      }

      errorMessage.value = "Something wen't wrong when trying to book the desired tire change time";

      console.error(error.config);
    });
}

const required = [
  (value: string) => !!value || "Required"
];
</script>

<style scoped>

</style>