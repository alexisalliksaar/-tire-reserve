import { ServiceableVehicle } from "./enums/ServiceableVehicle"

export type Workshop = {
  workshopId: string,
  city: string,
  address: string,
  serviceableVehicles: ServiceableVehicle[],
  email: string,
  phoneNumber: string,
}

export type TireChangeTime = {
  workshopId: string,
  time: string,
  id: string
}

export type AvailableTireChangeTimes = {
  tireChangeTimes: TireChangeTime[],
  failedWorkshopIds: string[]
}

