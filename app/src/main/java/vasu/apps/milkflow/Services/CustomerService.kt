package vasu.apps.milkflow.Services

import android.util.Log
import io.appwrite.Client
import io.appwrite.ID
import io.appwrite.Query
import io.appwrite.exceptions.AppwriteException
import io.appwrite.models.Document
import io.appwrite.services.Databases
import vasu.apps.milkflow.Constant.Constant

class CustomerService(client: Client) {
    private val databases = Databases(client)
    private val databaseId = Constant.DATABASE_ID
    private val customerCollectionId = Constant.CUSTOMER_COLLECTION_ID

    suspend fun createNewUserDocument(data: Map<String, Any>) {
        try {
            databases.createDocument(databaseId, customerCollectionId, ID.unique(), data)
            Log.d("LogUserServices", "Document created successfully")
        } catch (e: AppwriteException) {
            Log.e("LogUserServices", "Error creating document: ${e.message}")
            throw e
        }
    }

    suspend fun updatedCustomerById(
        documentId: String, data: Map<String, Any>
    ): Document<Map<String, Any>> {
        return try {
            val updateDocument =
                databases.updateDocument(databaseId, customerCollectionId, documentId, data)
            Log.e("LogUserService", updateDocument.toString())
            updateDocument
        } catch (e: AppwriteException) {
            Log.e("LogUserServices", "updatedCustomerById: ${e.message}")
            throw e
        }
    }

    suspend fun getCustomers(userId: String): List<Document<Map<String, Any>>> {
        return try {
            val result = databases.listDocuments(
                databaseId, customerCollectionId, queries = listOf(
                    Query.equal("supplier_id", userId)
                )
            )
            result.documents
        } catch (e: AppwriteException) {
            Log.e("LogUserServices", "getCustomers: ${e.message}")
            throw e
        }
    }

    suspend fun deleteCustomer(userId: String): Any {
        return try {
            databases.deleteDocument(databaseId, customerCollectionId, userId)
        } catch (e: AppwriteException) {
            Log.e("LogUserServices", "deleteCustomer: ${e.message}")
            throw e
        }
    }
}