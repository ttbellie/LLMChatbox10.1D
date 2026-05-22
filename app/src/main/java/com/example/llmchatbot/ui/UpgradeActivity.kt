package com.example.llmchatbot.ui

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.llmchatbot.data.AppDatabase
import com.example.llmchatbot.data.UserProfile
import com.example.llmchatbot.databinding.ActivityUpgradeBinding
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.IsReadyToPayRequest
import com.google.android.gms.wallet.PaymentData
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.WalletConstants
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

/**
 * Upgrade Account screen with three subscription tiers.
 * Uses Google Pay in TEST mode and falls back to a mock payment dialog for emulator/demo use.
 */
class UpgradeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUpgradeBinding
    private lateinit var username: String
    private lateinit var paymentsClient: PaymentsClient
    private var selectedTier: String = ""
    private var selectedPrice: String = ""

    companion object {
        private const val LOAD_PAYMENT_DATA_REQUEST_CODE = 991
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUpgradeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        username = intent.getStringExtra(LoginActivity.EXTRA_USERNAME) ?: "User"

        binding.btnBack.setOnClickListener { finish() }

        paymentsClient = Wallet.getPaymentsClient(
            this,
            Wallet.WalletOptions.Builder()
                .setEnvironment(WalletConstants.ENVIRONMENT_TEST)
                .build()
        )

        checkGooglePayAvailability()

        binding.btnPurchaseStarter.setOnClickListener {
            selectedTier = "Starter"
            selectedPrice = "4.99"
            requestPayment("4.99", "Starter Plan")
        }

        binding.btnPurchaseIntermediate.setOnClickListener {
            selectedTier = "Intermediate"
            selectedPrice = "9.99"
            requestPayment("9.99", "Intermediate Plan")
        }

        binding.btnPurchaseAdvanced.setOnClickListener {
            selectedTier = "Advanced"
            selectedPrice = "19.99"
            requestPayment("19.99", "Advanced Plan")
        }

        ensureProfileExists()
        loadCurrentTier()
    }

    private fun ensureProfileExists() {
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(this@UpgradeActivity)
            val profileDao = db.userProfileDao()
            if (profileDao.getProfile(username) == null) {
                profileDao.insert(UserProfile(username = username))
            }
        }
    }

    private fun loadCurrentTier() {
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(this@UpgradeActivity)
            val profile = db.userProfileDao().getProfile(username)
            if (profile != null && profile.tier != "Free") {
                binding.tvCurrentPlan.text = "Current Plan: ${profile.tier}"
                binding.tvCurrentPlan.visibility = View.VISIBLE
            } else {
                binding.tvCurrentPlan.visibility = View.GONE
            }
        }
    }

    private fun checkGooglePayAvailability() {
        val isReadyToPayRequest = IsReadyToPayRequest.fromJson(getIsReadyToPayRequest().toString())
        paymentsClient.isReadyToPay(isReadyToPayRequest).addOnCompleteListener { completedTask ->
            try {
                val result = completedTask.getResult(ApiException::class.java)
                if (result != true) {
                    Toast.makeText(this, "Google Pay unavailable. Demo payment will be used.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: ApiException) {
                Toast.makeText(this, "Google Pay unavailable. Demo payment will be used.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun requestPayment(price: String, itemName: String) {
        try {
            val paymentDataRequest = PaymentDataRequest.fromJson(
                getPaymentDataRequest(price, itemName).toString()
            )
            AutoResolveHelper.resolveTask(
                paymentsClient.loadPaymentData(paymentDataRequest),
                this,
                LOAD_PAYMENT_DATA_REQUEST_CODE
            )
        } catch (e: Exception) {
            showMockPaymentDialog()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LOAD_PAYMENT_DATA_REQUEST_CODE) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    val paymentData = data?.let { PaymentData.getFromIntent(it) }
                    handlePaymentSuccess(paymentData)
                }
                Activity.RESULT_CANCELED -> {
                    Toast.makeText(this, "Payment cancelled", Toast.LENGTH_SHORT).show()
                }
                AutoResolveHelper.RESULT_ERROR -> {
                    showMockPaymentDialog()
                }
            }
        }
    }

    private fun handlePaymentSuccess(paymentData: PaymentData?) {
        saveTierUpgrade()
    }

    private fun showMockPaymentDialog() {
        AlertDialog.Builder(this)
            .setTitle("Confirm Purchase")
            .setMessage("Purchase $selectedTier plan for \$$selectedPrice?\n\nDemo mode: no real payment will be charged.")
            .setPositiveButton("Confirm") { _, _ -> saveTierUpgrade() }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveTierUpgrade() {
        lifecycleScope.launch {
            val db = AppDatabase.getInstance(this@UpgradeActivity)
            val profileDao = db.userProfileDao()
            if (profileDao.getProfile(username) == null) {
                profileDao.insert(UserProfile(username = username))
            }
            profileDao.updateTier(username, selectedTier)

            Toast.makeText(
                this@UpgradeActivity,
                "Upgraded to $selectedTier plan!",
                Toast.LENGTH_LONG
            ).show()
            loadCurrentTier()
        }
    }

    private fun getBaseCardPaymentMethod(): JSONObject {
        return JSONObject().apply {
            put("type", "CARD")
            put("parameters", JSONObject().apply {
                put("allowedAuthMethods", JSONArray(listOf("PAN_ONLY", "CRYPTOGRAM_3DS")))
                put("allowedCardNetworks", JSONArray(listOf("AMEX", "DISCOVER", "MASTERCARD", "VISA")))
            })
        }
    }

    private fun getIsReadyToPayRequest(): JSONObject {
        return JSONObject().apply {
            put("apiVersion", 2)
            put("apiVersionMinor", 0)
            put("allowedPaymentMethods", JSONArray().put(getBaseCardPaymentMethod()))
        }
    }

    private fun getPaymentDataRequest(price: String, itemName: String): JSONObject {
        val cardPaymentMethod = getBaseCardPaymentMethod().apply {
            put("tokenizationSpecification", JSONObject().apply {
                put("type", "PAYMENT_GATEWAY")
                put("parameters", JSONObject().apply {
                    put("gateway", "example")
                    put("gatewayMerchantId", "exampleGatewayMerchantId")
                })
            })
        }

        return JSONObject().apply {
            put("apiVersion", 2)
            put("apiVersionMinor", 0)
            put("allowedPaymentMethods", JSONArray().put(cardPaymentMethod))
            put("transactionInfo", JSONObject().apply {
                put("totalPrice", price)
                put("totalPriceStatus", "FINAL")
                put("currencyCode", "AUD")
                put("countryCode", "AU")
            })
            put("merchantInfo", JSONObject().apply {
                put("merchantName", "LLM ChatBot")
            })
        }
    }
}
