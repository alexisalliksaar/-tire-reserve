<template>
  <VContainer class="fill-height align-content-start pa-2">

    <div class="d-flex flex-column mt-8 mb-4 ga-2">
      <div>
        <span class="text-h4">Welcome</span>
      </div>
      <div>
        <span class="text-body-1">Please search for available tire change times using the filters below</span>
      </div>
    </div>

    <VCard :loading="store.isLoading" class="w-100">

      <template v-slot:title>
        <span class="text-h5">Filters</span>
      </template>
      <template v-slot:text>
        <div>

          <div class="d-flex flex-row align-center mb-2">
            <div>
              <div class="text-h6">City:</div>
            </div>
            <div v-for="workshop in store.workshops" :key="workshop.workshopId" class="mx-2">
              <VCheckboxBtn
                :label="workshop.city"
                color="primary"
                v-model="selectedWorkshopIds"
                :value="workshop.workshopId"
                ></VCheckboxBtn>
            </div>
          </div>

          <div class="d-flex flex-row align-center ga-2">
            <div class="text-h6">Date Range:</div>
            <VLocaleProvider locale="en-GB">
            <VDateInput
              label="Date Range"
              multiple="range"
              max-width="368"
              show-adjacent-months
              density="compact"
              color="primary"
              hide-details
              :min="yesterDay"
              :max="yearFromNow"
              v-model="dateRange"

              ></VDateInput></VLocaleProvider>

          </div>

          <div class="d-flex flex-row align-center my-2">
            <div>
              <div class="text-h6">Vehicle Type:</div>
            </div>
            <div v-for="serviceableVehicle in store.allServiceableVehicles" :key="serviceableVehicle" class="mx-2">
              <VCheckboxBtn
                :label="getServiceableVehicleName(serviceableVehicle)"
                color="primary"
                v-model="selectedServiceableVehicles"
                :value="serviceableVehicle"
                ></VCheckboxBtn>
            </div>
          </div>

          <div v-if="formErrors.length > 0" class="mb-4">
            <div class="d-flex flex-row ga-2 align-center" v-for="formError in formErrors">
              <VIcon color="error" icon="mdi-alert-circle"></VIcon>
              <span class="text-error text-body-1">{{ formError }}</span>
            </div>
          </div>

          <VBtn @click="queryTireChangeTimes" color="primary">
            Search
            <VIcon end icon="mdi-magnify"></VIcon>
          </VBtn>
        </div>
      </template>
    </VCard>

    <VCard class="mt-4 w-100" v-if="isLoadingTimes || availableTireChangeTimes || errorMessage">
    <template v-slot:text>
      <VSkeletonLoader  :loading="isLoadingTimes" type="paragraph">

        <div class="d-flex flex-column">
          <div v-if="errorMessage" class="mb-2">
            <div class="d-flex flex-row ga-2 align-center">
              <VIcon color="error" icon="mdi-alert-circle"></VIcon>
              <span class="text-error text-body-1">{{ errorMessage }}</span>
            </div>
            <VDivider :thickness="2" class="my-2"></VDivider>
          </div>
          <div class="text-h6 mb-4 ">
            Available Tire Change Times:
          </div>

          <VExpansionPanels v-if="availableTireChangeTimes?.tireChangeTimes">
            <VExpansionPanel
              v-for="tireChangeTime in availableTireChangeTimes?.tireChangeTimes"
              :key="`${tireChangeTime.workshopId}.${tireChangeTime.id}`"
              >

              <VExpansionPanelTitle>
                <VRow class="d-flex flex-row ga-2" no-gutters justify="start">
                  <VCol class="font-weight-medium" cols="3">
                    {{ workShopMap.get(tireChangeTime.workshopId)?.city }}
                  </VCol>
                  <VCol cols="3">
                    <span class="font-weight-medium"> Date: </span>
                    <span> {{ getDatePartFromDateTime(tireChangeTime.time) }}</span>
                  </VCol>
                  <VCol cols="3">
                    <span class="font-weight-medium"> Time: </span>
                    <span> {{ getTimePartFromDateTime(tireChangeTime.time) }}</span>
                  </VCol>
                </VRow>
              </VExpansionPanelTitle>

              <VExpansionPanelText>
                <div class="d-flex flex-row justify-space-between">
                  <div class="d-flex flex-column">
                    <div>
                      <span class="font-weight-medium">Address: </span>
                      <span> {{ workShopMap.get(tireChangeTime.workshopId)?.address }}</span>
                    </div>
                    <div class="d-flex flex-row">
                      <div>
                        <span class="font-weight-medium">Serviceable Vehicle Types: </span>
                        <span> {{
                          workShopMap.get(tireChangeTime.workshopId)?.serviceableVehicles
                            .map(getServiceableVehicleName).join(", ")
                          }}
                        </span>
                      </div>
                    </div>
                  </div>
                  <VBtn color="orange-darken-2" class="mr-8" @click="navigateToBookingView(tireChangeTime)">
                    Book
                    <VIcon end icon="mdi-arrow-right-bold"></VIcon>
                  </VBtn>
                </div>
              </VExpansionPanelText>
            </VExpansionPanel>
          </VExpansionPanels>

          <div v-else class="text-body-1">
            No available tire change times matching the filter were found
          </div>

        </div>
      </VSkeletonLoader>
    </template>
    </VCard>
  </VContainer>
</template>

<script setup lang="ts">
import axios from '@/axios';
import { useStore } from '@/store';
import { ServiceableVehicle, getServiceableVehicleName } from '@/types/enums/ServiceableVehicle';
import { AvailableTireChangeTimes, TireChangeTime, Workshop } from '@/types/Workshop';
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';

function formattedDate(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

const store = useStore();

const selectedWorkshopIds = ref([] as string[]);

const now = new Date()

const yesterDay: Date = new Date(now);
yesterDay.setDate(now.getDate() - 1)

const yearFromNow: Date = new Date(now);
yearFromNow.setFullYear(now.getFullYear() + 1)

const weekFromNow = new Date(now);
weekFromNow.setDate(now.getDate() + 7)

const dateRange = ref([now, weekFromNow] as Date[]);

const selectedServiceableVehicles = ref([] as ServiceableVehicle[]);

const isLoadingTimes = ref(false);

const availableTireChangeTimes = ref<AvailableTireChangeTimes | null>(null);

const errorMessage = ref<string | null>(null);

const workShopMap = computed(() => {
  const result: Map<string, Workshop> = new Map();
  store.workshops?.forEach((ws) => result.set(ws.workshopId, ws));
  return result;
})

const formErrors = ref([] as string[]);

function validateForm(): boolean {
  formErrors.value = [];

  selectedServiceableVehicles.value.forEach((vehicleType) => {
    selectedWorkshopIds.value.forEach((wsId) => {
      if (! workShopMap.value.get(wsId)?.serviceableVehicles.includes(vehicleType)){
        const workShopName = workShopMap.value.get(wsId)?.city;
        const vehicleName = getServiceableVehicleName(vehicleType);
        formErrors.value.push(`${workShopName} workshop does not serve vehicles of type: ${vehicleName}`);
      }
    })
  });

  if (formErrors.value.length > 0){
    return false;
  }

  return true;
}

async function queryTireChangeTimes() {

  if (validateForm() === false){
    return;
  }

  isLoadingTimes.value = true;
  errorMessage.value = null;
  availableTireChangeTimes.value = null;

  const fromDateFormatted = formattedDate(dateRange.value[0]);

  // To date in dateRange has value of last date at time 23:59:59
  const toDate = dateRange.value.findLast((_) => true);
  let toDateFormatted = null;
  if (toDate) {
    toDate.setDate(toDate.getDate() + 1)
    toDateFormatted = formattedDate(toDate);
  }

  const body = {
    selectedWorkshops: selectedWorkshopIds.value,
    fromDate: fromDateFormatted,
    toDate: toDateFormatted,
    serviceableVehicles: selectedServiceableVehicles.value
  }

  const responseData = await axios.post("/tire-change-times/available", body)
        .then((response) => {
          return response.data as AvailableTireChangeTimes;
        })
        .catch((error) => {
          if (error.response) {
            console.error("Server responded:", error.response.data.message, error.response.status);
          } else if (error.request) {
            console.error("The request was made but no response was received, Request: ", error.request);
          } else {
            console.error('Something happened in setting up the request that triggered an Error, Error', error.message);
          }

          errorMessage.value = "Something wen't wrong when querying available tire change times";

          console.error(error.config);
          return null;
        });

  availableTireChangeTimes.value = responseData;

  if (responseData?.failedWorkshopIds) {
    const failedIds = responseData.failedWorkshopIds;
    const failedWsNames = store.workshops
      ?.filter((ws) => failedIds.includes(ws.workshopId))
      .map((ws) => ws.city) ?? [];

    if (failedWsNames.length >= 2){
      errorMessage.value = `Failed fetching available times for workshops: ${failedWsNames.join(", ")}`;
    } else if (failedWsNames.length == 1) {
      errorMessage.value = `Failed fetching available times for workshop: ${failedWsNames[0]}`;
    }
  }

  isLoadingTimes.value = false;
};

function getDatePartFromDateTime(dateTime: string): string {
  return dateTime.split("T")[0];
}
function getTimePartFromDateTime(dateTime: string): string {
  return dateTime.split("T")[1].substring(0, 5);
}

const router = useRouter();

function navigateToBookingView(tireChangeTime: TireChangeTime) {
  const bookingInfo = {
    workshopId: tireChangeTime.workshopId,
    id: tireChangeTime.id,
    dateDisplay: getDatePartFromDateTime(tireChangeTime.time),
    timeDisplay: getTimePartFromDateTime(tireChangeTime.time),
    workshopNameDisplay: workShopMap.value.get(tireChangeTime.workshopId)?.city,
    serviceableVehiclesDisplay: workShopMap.value.get(tireChangeTime.workshopId)?.serviceableVehicles
      .map(getServiceableVehicleName).join(", "),
    addressDisplay: workShopMap.value.get(tireChangeTime.workshopId)?.address
  };

  store.$patch({
    bookingInfo: bookingInfo
  })

  router.push({ name: "Book" });
}

</script>

<style>
.v-card-title {
  font-size: 2rem;
}
</style>
