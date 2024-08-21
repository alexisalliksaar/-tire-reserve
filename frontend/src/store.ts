import { defineStore } from 'pinia'
import { Workshop } from './types/Workshop'

import axios from "./axios";

export type StoreError = {
  message: string
};

export type BookingInfo = {
  workshopId: string,
  id: string,
  dateDisplay: string,
  timeDisplay: string,
  workshopNameDisplay: string,
  serviceableVehiclesDisplay: string,
  addressDisplay: string
}

export const useStore = defineStore('workshops', {
  // arrow function recommended for full type inference
  state: () => {
    return {
      workshops: null as Workshop[] | null,
      isLoading: true,
      bookingInfo: null as BookingInfo | null,
    }
  },

  getters: {
    allServiceableVehicles: (state) => {
      const serviceableVehicles = state.workshops?.map((workshop) => workshop.serviceableVehicles).flat() ?? [];
      const set = new Set();
      return serviceableVehicles.filter((vehicleType) => set.has(vehicleType) ? false : set.add(vehicleType));
    }
  },

  actions: {
    async fetchWorkshops(): Promise<void> {
      await axios.get("/workshops")
        .then((response) => {
          this.workshops = response.data.workshops
          console.log("Set store workshops to:", response.data.workshops)
        })
        .catch((error) => {
          if (error.response) {
            console.error("Server responded:", error.response.data.message, error.response.status);
          } else if (error.request) {
            console.error("The request was made but no response was received, Request: ", error.request);
          } else {
            console.error('Something happened in setting up the request that triggered an Error, Error', error.message);
          }
          console.error(error.config);
        })
        this.isLoading = false;
    }
  }
});