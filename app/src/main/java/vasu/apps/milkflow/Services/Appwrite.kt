package vasu.apps.milkflow.Services

import android.content.Context
import io.appwrite.Client
import vasu.apps.milkflow.Constant.Constant

object Appwrite {

    private const val ENDPOINT = Constant.END_POINT
    private const val PROJECT_ID = Constant.PROJECT_ID

    private lateinit var client: Client
    val accountService: AccountService by lazy { AccountService(client) }
    val userService: UserService by lazy { UserService(client) }
    val customerService: CustomerService by lazy { CustomerService(client) }
    val dailySellService: DailySellService by lazy { DailySellService(client) }

    fun init(context: Context) {
        client = Client(context).setEndpoint(ENDPOINT).setProject(PROJECT_ID).setSelfSigned(true)
    }
}
