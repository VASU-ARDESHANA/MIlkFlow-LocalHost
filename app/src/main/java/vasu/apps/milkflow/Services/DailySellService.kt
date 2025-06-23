package vasu.apps.milkflow.Services

import io.appwrite.Client
import io.appwrite.services.Databases
import vasu.apps.milkflow.Constant.Constant

class DailySellService(client: Client) {
    private val databases = Databases(client)
    private val databaseId = Constant.DATABASE_ID
    private val deliveryRecordsCollectionId = Constant.DELIVERY_RECORDS_COLLECTION

    suspend fun createDeliveryRecord(data: Map<String, Any>) {
        databases.createDocument(
            databaseId = databaseId,
            collectionId = deliveryRecordsCollectionId,
            documentId = "unique()",
            data = data
        )
    }

}