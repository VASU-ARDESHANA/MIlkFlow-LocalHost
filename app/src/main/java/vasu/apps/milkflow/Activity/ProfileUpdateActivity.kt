package vasu.apps.milkflow.Activity

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.textview.MaterialTextView
import com.pranavpandey.android.dynamic.toasts.DynamicToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vasu.apps.milkflow.R
import vasu.apps.milkflow.Services.Appwrite

class ProfileUpdateActivity : AppCompatActivity() {

    val accountService = Appwrite.accountService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_update)

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)


        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.profile_update_nav_host_fragment_content_main) as NavHostFragment

        val navController = navHostFragment.navController
        val navInflater = navController.navInflater
        val graph = navInflater.inflate(R.navigation.fragment_profile_update_navigation)
        navController.graph = graph

        lifecycleScope.launch {
            val user = withContext(Dispatchers.IO) {
                accountService.getLoggedIn()
            }

            val prefs = user?.prefs?.data ?: emptyMap<String, Any>()
            val isPasswordUpdated = prefs["passwordUpdated"]?.toString() ?: "no"
            val isProfileUpdated = prefs["profileUpdated"]?.toString() ?: "no"
            val isProductSet = prefs["productSet"]?.toString() ?: "no"

            val startDestination = when {
                isPasswordUpdated == "no" -> R.id.password_navigation
                isProfileUpdated == "no" -> R.id.update_profile_navigation
                isProductSet == "no" -> R.id.update_product_navigation

                else -> {
                    DynamicToast.makeSuccess(
                        this@ProfileUpdateActivity,
                        "Your profile is up to date.",
                        Toast.LENGTH_SHORT
                    ).show()
                    accountService.setUserPrefs(profileCreated = "yes")
                    val intent = Intent(this@ProfileUpdateActivity, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                    return@launch
                }
            }

            graph.setStartDestination(startDestination)
            navController.graph = graph
        }

    }

    private suspend fun handleLogout(message: String) {
        DynamicToast.makeWarning(this, message, Toast.LENGTH_SHORT).show()
        withContext(Dispatchers.IO) {
            accountService.logout()
        }
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }

    fun setToolbarTitle(title: String) {
        val topBarName = findViewById<MaterialTextView>(R.id.profile_update_top_bar_name)
        topBarName?.text = title
    }

}