export enum ServiceableVehicle {
  CAR = "CAR",
  TRUCK = "TRUCK"
}

export function getServiceableVehicleName(serviceableVehicles: ServiceableVehicle): string{
  switch(serviceableVehicles) {
    case ServiceableVehicle.CAR:
      return "Car";
    case ServiceableVehicle.TRUCK:
      return "Truck"
  }
}