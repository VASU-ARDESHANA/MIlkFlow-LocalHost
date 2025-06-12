package vasu.apps.milkflow.Model

data class Customer(
    val id: String,
    val name: String,
    val phone: String,
    val vacationMode: Boolean,
    val deliveryTime: String,
    val milkType: String,
    val isActive: Boolean,
    val startDate: String,
    val paddingAmount: String
)
