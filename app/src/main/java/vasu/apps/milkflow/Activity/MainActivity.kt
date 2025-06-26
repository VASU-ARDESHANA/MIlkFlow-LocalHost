package vasu.apps.milkflow.Activity

import android.content.Intent
import android.content.pm.ActivityInfo
import android.graphics.Outline
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textview.MaterialTextView
import com.pranavpandey.android.dynamic.toasts.DynamicToast
import io.appwrite.exceptions.AppwriteException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vasu.apps.milkflow.R
import vasu.apps.milkflow.Services.Appwrite
import kotlin.reflect.KMutableProperty0

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    val accountService = Appwrite.accountService

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navController: NavController
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var toolbarHeader: TextView
    private lateinit var toolbar: Toolbar
    private lateinit var navigationView: NavigationView
    private lateinit var headerView: View
    private lateinit var closeButton: AppCompatImageButton

    private var isCustomerMenuExpanded = false
    private var isAccountMenuExpanded = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)

        toolbar = findViewById(R.id.toolbar)
        toolbarHeader = findViewById(R.id.main_toolbar_text)
        setSupportActionBar(toolbar)

        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)
        headerView = navigationView.getHeaderView(0)
        closeButton = headerView.findViewById(R.id.nh_close)

        closeButton.setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.START)
        }

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        navController = navHostFragment.navController

        toolbarHeader.text = R.string.app_name.toString()

        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home,
                R.id.nav_customer_list,
                R.id.nav_register_customer,
                R.id.nav_supplier_account,
                R.id.nav_supplier_products,
                R.id.nav_daily_sell,
                R.id.nav_update
            ), drawerLayout
        )

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)
        NavigationUI.setupWithNavController(navigationView, navController)
        navigationView.setNavigationItemSelectedListener(this)

        drawerLayout.addDrawerListener(object : DrawerLayout.DrawerListener {
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                val content = findViewById<View>(R.id.content_view)
                val scaleFactor = 0.85f
                val radius = 50f * slideOffset

                val slideX = drawerView.width * slideOffset
                content.translationX = slideX
                content.scaleX = 1 - (slideOffset * (1 - scaleFactor))
                content.scaleY = 1 - (slideOffset * (1 - scaleFactor))
                content.elevation = slideOffset * 25f

                content.outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(view: View?, outline: Outline?) {
                        view?.let {
                            outline?.setRoundRect(0, 0, it.width, it.height, radius)
                        }
                    }
                }
                content.clipToOutline = true
            }

            override fun onDrawerOpened(drawerView: View) {}

            override fun onDrawerClosed(drawerView: View) {
                val content = findViewById<View>(R.id.content_view)

                collapseMenuItems(
                    listOf(R.id.nav_customer_list, R.id.nav_register_customer),
                    ::isCustomerMenuExpanded,
                    R.id.nav_customer
                )

                collapseMenuItems(
                    listOf(R.id.nav_supplier_account, R.id.nav_supplier_products),
                    ::isAccountMenuExpanded,
                    R.id.nav_account
                )

                content.scaleX = 1f
                content.scaleY = 1f
                content.translationX = 0f
                content.clipToOutline = false
            }

            override fun onDrawerStateChanged(newState: Int) {}
        })

        setMenuArrow(R.id.nav_customer, isCustomerMenuExpanded)
        setMenuArrow(R.id.nav_account, isAccountMenuExpanded)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val menu = navigationView.menu

        when (item.itemId) {
            R.id.nav_home -> navController.navigate(R.id.action_to_dash_board)

            R.id.nav_customer -> {
                isCustomerMenuExpanded = !isCustomerMenuExpanded
                menu.findItem(R.id.nav_customer_list).isVisible = isCustomerMenuExpanded
                menu.findItem(R.id.nav_register_customer).isVisible = isCustomerMenuExpanded
                setMenuArrow(R.id.nav_customer, isCustomerMenuExpanded)
                return true
            }

            R.id.nav_account -> {
                isAccountMenuExpanded = !isAccountMenuExpanded
                menu.findItem(R.id.nav_supplier_account).isVisible = isAccountMenuExpanded
                menu.findItem(R.id.nav_supplier_products).isVisible = isAccountMenuExpanded
                setMenuArrow(R.id.nav_account, isAccountMenuExpanded)
                return true
            }

            R.id.nav_customer_list, R.id.nav_register_customer, R.id.nav_daily_sell, R.id.nav_update -> {
                NavigationUI.onNavDestinationSelected(item, navController)
            }

            R.id.nav_supplier_products -> {
                val args = Bundle().apply {
                    putString("products", "MainActivity")
                }
                navController.navigate(R.id.nav_supplier_products, args)
            }

            R.id.nav_supplier_account -> {
                val args = Bundle().apply {
                    putString("products", "MainActivity")
                }
                navController.navigate(R.id.nav_supplier_account, args)
            }

            R.id.action_logout -> {
                toastMsg("Logging out", "success")
                navigateToLogin()
            }

            else -> {
                toastMsg("Contact Developer, because it's under construction", "warning")
            }
        }

        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun setMenuArrow(menuItemId: Int, isExpanded: Boolean) {
        val menu = navigationView.menu
        val navItem = menu.findItem(menuItemId)
        val arrowImageView = navItem.actionView as? ImageView
        val arrowDrawable = if (isExpanded) R.drawable.ic_arrow_down else R.drawable.ic_arrow_right
        arrowImageView?.setImageDrawable(ContextCompat.getDrawable(this, arrowDrawable))
    }

    private fun collapseMenuItems(
        menuItemIds: List<Int>, isExpandedFlag: KMutableProperty0<Boolean>, parentItemId: Int
    ) {
        val menu = navigationView.menu
        menuItemIds.forEach { menu.findItem(it).isVisible = false }
        isExpandedFlag.set(false)
        setMenuArrow(parentItemId, false)
    }

    private fun navigateToLogin() {
        lifecycleScope.launch {
            accountService.logout()
        }
        val intent = Intent(this, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        startActivity(intent)
        finish()
    }

    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(
            navController, appBarConfiguration
        ) || super.onSupportNavigateUp()
    }

    override fun onStart() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val user = withContext(Dispatchers.IO) {
                    accountService.getLoggedIn()
                }
                if (user == null) {
                    accountService.logout()
                }
            } catch (e: AppwriteException) {
                toastMsg(e.message ?: "An unknown error occurred", "error")
            }
        }
        super.onStart()
    }

    fun setMainToolbarTitle(title: String) {
        val topBarName = findViewById<MaterialTextView>(R.id.main_toolbar_text)
        topBarName?.text = title
    }

    private fun toastMsg(message: String, toastType: String) {
        when (toastType) {
            "warning" -> DynamicToast.makeWarning(this, message, Toast.LENGTH_SHORT).show()
            "error" -> DynamicToast.makeError(this, message, Toast.LENGTH_SHORT).show()
            else -> DynamicToast.make(
                this,
                message,
                ContextCompat.getColor(this, R.color.white),
                ContextCompat.getColor(this, R.color.success),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
