<template>
    <v-container class="fill-height align-start justify-center pa-2">
      <v-card class="pa-4" >
        <VRow class="pa-2 justify-center">
          <div class="text-h4">
            About us
          </div>
        </VRow>
        <VRow class="mx-0 mb-0">
          <div>
            Lorem ipsum odor amet, consectetuer adipiscing elit. Efficitur mollis quam duis sagittis himenaeos in accumsan convallis. Finibus vehicula sapien cubilia gravida hendrerit dolor. Erat nisl nibh aptent sagittis lacinia orci magnis? Est efficitur molestie duis interdum accumsan scelerisque lectus. Augue nisl justo duis phasellus praesent hendrerit rhoncus. Suspendisse turpis penatibus hendrerit orci imperdiet penatibus. Feugiat magna placerat tellus congue ultricies aenean class condimentum etiam.
            <br/>
            <br/>
            Tellus enim phasellus molestie dolor fermentum. Sapien ridiculus auctor ac primis accumsan netus maecenas. Facilisis nisi gravida erat habitant finibus sit porta. Turpis vitae convallis imperdiet ipsum vulputate non rutrum. Id risus condimentum metus; ex arcu pulvinar ridiculus. Suscipit rutrum diam non; lacinia habitasse fames mauris sem aptent. Platea accumsan justo non imperdiet nibh nisl. Pulvinar faucibus hac tortor faucibus; ornare vel eu malesuada.
          </div>
        </VRow>

        <VDivider :thickness="2" class="my-4"></VDivider>

        <VRow class="pa-2 mt-2 justify-center">
          <div class="text-h4">
            Workshops
          </div>
        </VRow>
        <VSkeletonLoader :loading="store.isLoading" type="paragraph">
          <div>
            <VRow v-for="workshop in store.workshops" :key="workshop.workshopId" class="pa-4 mt-2">
              <div>
                  <div class="text-h5 mb-1">{{ workshop.city }}</div>
                  <div class="text-body-1"><span class="font-weight-medium">Address:</span> {{ workshop.address }}</div>
                  <div class="text-body-1"><span class="font-weight-medium">Email:</span> {{ workshop.email }}</div>
                  <div class="text-body-1"><span class="font-weight-medium">Phone Number:</span> {{ workshop.phoneNumber }}</div>
                  <div class="text-body-1"><span class="font-weight-medium">Serviceable Vehicle Types:</span> {{ workshop.serviceableVehicles.map(getServiceableVehicleName).join(", ") }}</div>
              </div>

            </VRow>
          </div>
        </VSkeletonLoader>

      </v-card>
    </v-container>
</template>

<script setup lang="ts">
import { useStore } from '@/store';
import { getServiceableVehicleName } from '@/types/enums/ServiceableVehicle';

const store = useStore();

</script>

<style scoped>

</style>