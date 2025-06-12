package vasu.apps.milkflow.Model

data class DailySell(
    val idSupplier: String,
    val id: String,
    val name: String,
    val deliveryTime: String,
    val vacationMode: Boolean,
    val milkType: String,
    var morningCowMilkQty: Double?,
    var eveningCowMilkQty: Double?,
    var morningBuffaloMilkQty: Double?,
    var eveningBuffaloMilkQty: Double?,
    val priceCowMilk: Double?,
    val priceBuffaloMilk: Double?
)
