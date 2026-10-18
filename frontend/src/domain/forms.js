const optionalNumber = (value) => (value === '' ? null : Number(value))
const optionalText = (value) => value.trim() || null

export function blankMaintenanceForm(vehicleId = '') {
  return {
    vehicleId: String(vehicleId), category: 'ENGINE_OIL', productName: '', purchaseUrl: '',
    lastReplacedOn: '', lastReplacedOdometerKm: '', intervalKm: '', intervalMonths: '',
  }
}

// Form inputs are strings. Keep each API's numeric/null contract explicit.
export function vehiclePayload(form) {
  return {
    manufacturer: form.manufacturer.trim(),
    model: form.model.trim(),
    modelYear: optionalNumber(form.modelYear),
    currentOdometerKm: Number(form.currentOdometerKm),
    monthlyAverageKm: optionalNumber(form.monthlyAverageKm),
  }
}

export function maintenancePayload(form) {
  return {
    vehicleId: Number(form.vehicleId),
    category: form.category,
    productName: optionalText(form.productName),
    purchaseUrl: optionalText(form.purchaseUrl ?? ''),
    lastReplacedOn: optionalText(form.lastReplacedOn),
    lastReplacedOdometerKm: Number(form.lastReplacedOdometerKm),
    intervalKm: optionalNumber(form.intervalKm),
    intervalMonths: optionalNumber(form.intervalMonths),
  }
}

export function localDate(today = new Date()) {
  return `${today.getFullYear()}-${String(today.getMonth() + 1).padStart(2, '0')}-${String(today.getDate()).padStart(2, '0')}`
}
