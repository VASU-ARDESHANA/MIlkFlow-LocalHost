package vasu.apps.milkflow.Services

import android.util.Log
import io.appwrite.Client
import io.appwrite.ID
import io.appwrite.Query
import io.appwrite.exceptions.AppwriteException
import io.appwrite.models.Document
import io.appwrite.services.Databases
import vasu.apps.milkflow.Constant.Constant

class UserService(client: Client) {

    private val databases = Databases(client)
    private val databaseId = Constant.DATABASE_ID
    private val userCollectionId = Constant.USER_COLLECTION_ID
    private val customerCollectionId = Constant.CUSTOMER_COLLECTION_ID
    private var accountService = Appwrite.accountService

    suspend fun getUserDocument(userId: String): Map<String, Any>? {
        return try {
            val result = databases.listDocuments(databaseId, userCollectionId, queries = listOf(Query.equal("uid", userId)))
            val document = result.documents.firstOrNull()
            document?.data
        } catch (e: AppwriteException) {
            Log.e("LogUserServices", "getUserDocument: ${e.message}")
            throw e
        }
    }

    suspend fun updateUserDocument(userId: String, updateData: Map<String, Any>, currentData: Map<String, Any>) {
        try {
            val changedData = updateData.filter { (key, newValue) ->
                val currentValue = currentData[key]
                currentValue != newValue
            }

            if (changedData.containsKey("name")) {
                val newName = changedData["name"] as? String
                newName?.let {
                    accountService.updateName(it)
                }
            }

            if (changedData.isNotEmpty()) {
                val result = databases.listDocuments(databaseId, userCollectionId, queries = listOf(Query.equal("uid", userId)))
                val document = result.documents.firstOrNull()
                    ?: throw Exception("User document not found for uid: $userId")

                databases.updateDocument(databaseId, userCollectionId, document.id, data = changedData)
                Log.d("LogUserServices", "Updated fields: ${changedData.keys}")
            } else {
                Log.d("LogUserServices", "No changes detected. Skipping update.")
            }

        } catch (e: AppwriteException) {
            Log.e("LogUserServices", "updateUserDocument: ${e.message}")
            throw e
        }
    }

    suspend fun updateProductsDocument(userId: String, updateData: Map<String, Any>, currentData: Map<String, Any>) {
        try {
            val changedData = updateData.filter { (key, newValue) ->
                val currentValue = currentData[key]
                when {
                    currentValue is Number && newValue is Number -> {
                        currentValue.toDouble() != newValue.toDouble()
                    }

                    else -> currentValue != newValue
                }
            }


            if (changedData.containsKey("name")) {
                val newName = changedData["name"] as? String
                newName?.let {
                    Log.d("LogUserServices", "Name changed. Updating Appwrite account name.")
                }
            }

            if (changedData.isNotEmpty()) {
                val result = databases.listDocuments(databaseId, userCollectionId, queries = listOf(Query.equal("uid", userId)))
                val document = result.documents.firstOrNull()
                    ?: throw Exception("User document not found for uid: $userId")

                databases.updateDocument(databaseId, userCollectionId, document.id, data = changedData)
                Log.d("LogUserServices", "Updated fields: ${changedData.keys}")
            } else {
                Log.d("LogUserServices", "No changes detected. Skipping update.")
            }

        } catch (e: AppwriteException) {
            Log.e("LogUserServices", "updateUserDocument: ${e.message}")
            throw e
        }
    }

    suspend fun createNewUserDocument(data: Map<String, Any>) {
        try {
            databases.createDocument(databaseId, customerCollectionId, ID.unique(), data)
            Log.d("LogUserServices", "Document created successfully")
        } catch (e: AppwriteException) {
            Log.e("LogUserServices", "Error creating document: ${e.message}")
            throw e
        }
    }

    suspend fun getCustomerById(documentId: String): Document<Map<String, Any>> {
        return try {
            val document = databases.getDocument(databaseId, customerCollectionId, documentId)
            Log.e("LogUserServices", "getCustomerById: $document")
            document
        } catch (e: AppwriteException) {
            Log.e("LogUserServices", "getCustomerById: ${e.message}")
            throw e
        }
    }

    suspend fun updatedCustomerById(documentId: String, data: Map<String, Any>): Document<Map<String, Any>> {
        return try {
            val updateDocument = databases.updateDocument(databaseId, customerCollectionId, documentId, data)
            Log.e("LogUserService", updateDocument.toString())
            updateDocument
        } catch (e: AppwriteException) {
            Log.e("LogUserServices", "updatedCustomerById: ${e.message}")
            throw e
        }
    }

    suspend fun getCustomers(userId: String): List<Document<Map<String, Any>>> {
        return try {
            val result = databases.listDocuments(databaseId, customerCollectionId, queries = listOf(Query.equal("supplier_id", userId)))
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
