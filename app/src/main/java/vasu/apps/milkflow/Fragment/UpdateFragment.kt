package vasu.apps.milkflow.Fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.cardview.widget.CardView
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.pranavpandey.android.dynamic.toasts.DynamicToast
import kotlinx.coroutines.launch
import vasu.apps.milkflow.Activity.MainActivity
import vasu.apps.milkflow.R
import vasu.apps.milkflow.Services.Appwrite

class UpdateFragment : Fragment() {

    private var userService = Appwrite.userService

    private lateinit var rootView: View
    private lateinit var currentVersion: TextView
    private lateinit var newVersion: TextView
    private lateinit var description: TextView
    private lateinit var descriptionCard: CardView
    private lateinit var checkUpdate: MaterialButton
    private lateinit var downloadButton: ImageButton
    private lateinit var progressBar: ProgressBar

    @SuppressLint("SetJavaScriptEnabled", "SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.fragment_update, container, false)

        val title = getString(R.string.app_name) + " " + getString(R.string.update)
        (requireActivity() as? MainActivity)?.setMainToolbarTitle(title)

        currentVersion = rootView.findViewById(R.id.update_app_current_version)
        newVersion = rootView.findViewById(R.id.update_app_new_version)
        description = rootView.findViewById(R.id.update_app_description)
        descriptionCard = rootView.findViewById(R.id.update_app_description_card)
        checkUpdate = rootView.findViewById(R.id.update_app_button)
        downloadButton = rootView.findViewById(R.id.update_app_download)
        progressBar = rootView.findViewById(R.id.update_app_progressBar)

        val versionName = requireContext().packageManager.getPackageInfo(
            requireContext().packageName, 0
        ).versionName

        currentVersion.text = getString(R.string.current_version) + " " + versionName

        checkUpdate.setOnClickListener {
            progressBar.visibility = View.VISIBLE
            lifecycleScope.launch {
                try {
                    val updateDocs = userService.getUpdate()
                    progressBar.visibility = View.GONE

                    if (updateDocs.isNotEmpty()) {
                        val versionName = requireContext().packageManager.getPackageInfo(
                            requireContext().packageName, 0
                        ).versionName

                        val newerVersions = updateDocs.filter {
                            val latestVersionName = it.data["version"] as? String
                            latestVersionName != null && isVersionNewer(
                                versionName, latestVersionName
                            )
                        }

                        if (newerVersions.isNotEmpty()) {
                            val latest = newerVersions.maxByOrNull {
                                it.data["version"] as? String ?: "0.0.0"
                            }?.let { latestDoc ->
                                newerVersions.firstOrNull { it.data["version"] == latestDoc.data["version"] }
                            }

                            val newVersionName = latest?.data["version"] as? String ?: "N/A"
                            val apkUrl = latest?.data["apk_url"] as? String ?: ""
                            val changeLog = latest?.data["change_log"] as? String ?: ""

                            newVersion.visibility = View.VISIBLE
                            descriptionCard.visibility = View.VISIBLE
                            downloadButton.visibility = View.VISIBLE

                            newVersion.text = getString(R.string.new_version) + " $newVersionName"
                            description.text = getString(R.string.change_log) + "\n \n $changeLog"

                            downloadButton.setOnClickListener {
                                val browserIntent = Intent(Intent.ACTION_VIEW, apkUrl.toUri())
                                startActivity(browserIntent)
                            }

                        } else {
                            newVersion.visibility = View.GONE
                            downloadButton.visibility = View.GONE
                            descriptionCard.visibility = View.GONE
                            toastMsg("There is no newer version available.", "warning")
                        }
                    } else {
                        toastMsg("No update info found.", "warning")
                    }
                } catch (e: Exception) {
                    progressBar.visibility = View.GONE
                    toastMsg("Error checking updates: ${e.message}", "error")
                } finally {
                    progressBar.visibility = View.GONE
                }
            }
        }

        return rootView
    }

    private fun isVersionNewer(current: String?, latest: String): Boolean {
        val currentParts = current?.split(".")
        val latestParts = latest.split(".")
        val maxLength = maxOf(currentParts?.size ?: 0, latestParts.size)

        for (i in 0 until maxLength) {
            val curr = currentParts?.getOrNull(i)?.toIntOrNull() ?: 0
            val latest = latestParts.getOrNull(i)?.toIntOrNull() ?: 0

            if (latest > curr) return true
            if (latest < curr) return false
        }
        return false
    }

    private fun toastMsg(message: String, toastType: String) {
        when (toastType) {
            "warning" -> {
                DynamicToast.makeWarning(requireActivity(), message, Toast.LENGTH_SHORT).show()
            }

            "error" -> {
                DynamicToast.makeError(requireActivity(), message, Toast.LENGTH_SHORT).show()
            }

            "success" -> {
                DynamicToast.make(
                    requireContext(),
                    message,
                    requireContext().getColor(R.color.white),
                    requireContext().getColor(R.color.success),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }
}