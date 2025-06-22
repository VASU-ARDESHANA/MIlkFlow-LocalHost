package vasu.apps.milkflow.Activity

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputLayout
import com.pranavpandey.android.dynamic.toasts.DynamicToast
import io.appwrite.exceptions.AppwriteException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vasu.apps.milkflow.R
import vasu.apps.milkflow.Services.Appwrite

class LoginActivity : AppCompatActivity() {

    private lateinit var emailInput: TextInputLayout
    private lateinit var passwordInput: TextInputLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var error: TextView
    private lateinit var loginButton: AppCompatButton
    private lateinit var loginCard: CardView
    private lateinit var checkLogin: LinearLayout

    val accountService = Appwrite.accountService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)

        error = findViewById(R.id.login_errors)

        emailInput = findViewById(R.id.login_email_til)
        passwordInput = findViewById(R.id.login_password_til)
        progressBar = findViewById(R.id.login_progressBar)
        loginButton = findViewById(R.id.login_button)
        loginCard = findViewById(R.id.login_card)
        checkLogin = findViewById(R.id.checkLogin)


        emailInput.editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                emailInput.error = null
                emailInput.isErrorEnabled = false
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        passwordInput.editText?.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val currentPassword = s.toString().trim()

                if (currentPassword.isNotEmpty()) {
                    if (isValidPassword(currentPassword)) {
                        passwordInput.setStartIconDrawable(R.drawable.password_lock)
                        passwordInput.error = null
                        passwordInput.isErrorEnabled = false
                    } else {
                        passwordInput.setStartIconDrawable(R.drawable.password_unlocked)
                    }
                } else {
                    passwordInput.setStartIconDrawable(R.drawable.password_unlocked)
                    passwordInput.error = null
                    passwordInput.isErrorEnabled = false
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        loginButton.setOnClickListener {
            loginUser()
        }
    }

    private fun loginUser() {
        val email = emailInput.editText?.text.toString().trim()
        val password = passwordInput.editText?.text.toString().trim()

        emailInput.error = null
        passwordInput.error = null

        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInput.error = "Enter a valid email address"
            emailInput.requestFocus()
            return
        } else if (password.isEmpty()) {
            passwordInput.error = "Enter your password"
            passwordInput.requestFocus()
            return
        } else {
            emailInput.error = null
            passwordInput.error = null
            error.visibility = View.GONE
            progressBar.visibility = View.VISIBLE
            closeKeyboard()
            disabled()

            CoroutineScope(Dispatchers.Main).launch {
                loginWithAppwrite(email, password)
            }
        }
    }

    private suspend fun loginWithAppwrite(email: String, password: String) {
        try {
            val user = withContext(Dispatchers.IO) {
                accountService.login(email, password)
            }

            if (user != null) {
                val prefs = user.prefs.data
                val isProfileUpdated = prefs["profileCreated"]?.toString() ?: ""

                when (isProfileUpdated) {
                    "no" -> {
                        activityChange(ProfileUpdateActivity::class.java) //                    notification()
                    }

                    "yes" -> {
                        activityChange(MainActivity::class.java) //                    notification()
                    }

                    else -> {
                        handleLogout("Contact to ADMIN")
                    }
                }
            }

        } catch (e: Exception) {
            toastMsg(e.message ?: "An unknown error occurred", "error")
            progressBar.visibility = View.GONE
            enable()
            reset()
        }
    }

    private fun activityChange(activityClass: Class<out Activity>) {
        val intent = Intent(this@LoginActivity, activityClass)
        startActivity(intent)
        finish()
    }

    //    private suspend fun notification() {
    //        val target = withContext(Dispatchers.IO) {
    //            accountService.registerPushTarget()
    //        }
    //    }

    private suspend fun handleLogout(message: String) {
        withContext(Dispatchers.IO) {
            accountService.logout()
        }
        toastMsg(message, "warning")
        progressBar.visibility = View.GONE
        enable()
        reset()
    }

    private fun isValidPassword(password: String): Boolean {
        val minLength = 8
        return password.length >= minLength
    }

    private fun closeKeyboard() {
        val view = this.currentFocus
        if (view != null) {
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun toastMsg(message: String, toastType: String) {
        when (toastType) {
            "warning" -> {
                DynamicToast.makeWarning(this, message, Toast.LENGTH_SHORT).show()
            }

            "error" -> {
                DynamicToast.makeError(this, message, Toast.LENGTH_SHORT).show()
            }

            else -> {
                DynamicToast.make(
                    this,
                    message,
                    getColor(R.color.white),
                    getColor(R.color.success),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun enable() {
        emailInput.isEnabled = true
        passwordInput.isEnabled = true
        loginButton.isEnabled = true
    }

    private fun disabled() {
        emailInput.isEnabled = false
        passwordInput.isEnabled = false
        loginButton.isEnabled = false
    }

    private fun reset() {
        emailInput.editText?.setText("")
        passwordInput.editText?.setText("")
        error.visibility = View.GONE
        progressBar.visibility = View.GONE
    }

    override fun onStart() {
        super.onStart()

        checkAndRequestStoragePermission()

        CoroutineScope(Dispatchers.Main).launch {
            try {
                val user = withContext(Dispatchers.IO) {
                    accountService.getLoggedIn()
                }

                if (user != null) {
                    loginCard.visibility = View.GONE
                    checkLogin.visibility = View.VISIBLE
                    val prefs = user.prefs.data
                    val isProfileUpdated = prefs["profileCreated"]?.toString() ?: ""

                    when (isProfileUpdated) {
                        "no" -> {
                            activityChange(ProfileUpdateActivity::class.java)
                        }

                        "yes" -> {
                            activityChange(MainActivity::class.java)
                        }

                        else -> {
                            handleLogout("Contact to ADMIN")
                        }
                    }
                }

            } catch (_: AppwriteException) {
                loginCard.visibility = View.VISIBLE
                checkLogin.visibility = View.GONE
            }
        }
    }

    private fun checkAndRequestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissions = mutableListOf<String>()

            //            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
            //                permissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            //            }
            //
            //            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
            //                permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            //            }

            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }

            if (permissions.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 101)
            }

        } else {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.READ_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 101
                )
            }
        }
    }

    //    DynamicToast.make(
    //    this,
    //    "Profile updated successfully!",
    //    ContextCompat.getDrawable(this, R.drawable.email),
    //    getColor(R.color.white),
    //    getColor(R.color.success),
    //    Toast.LENGTH_SHORT
    //    ).show()

}