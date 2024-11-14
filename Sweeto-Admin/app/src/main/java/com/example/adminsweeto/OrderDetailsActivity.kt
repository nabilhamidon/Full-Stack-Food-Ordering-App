package com.example.adminsweeto

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import android.widget.Toast
import com.example.adminsweeto.adapter.OrderDetailsAdapter
import com.example.adminsweeto.databinding.ActivityOrderDetailsBinding
import com.example.adminsweeto.model.OrderDetails

class OrderDetailsActivity : AppCompatActivity() {
    private val binding: ActivityOrderDetailsBinding by lazy {
        ActivityOrderDetailsBinding.inflate(layoutInflater)
    }

    private var userName: String? = null
    private var address: String? = null
    private var phoneNumber: String? = null
    private var totalPrice: String? = null
    private var foodName: ArrayList<String> = arrayListOf()
    private var foodImages: ArrayList<String> = arrayListOf()
    private var foodQuantity: ArrayList<Int> = arrayListOf()
    private var foodPrices: ArrayList<String> = arrayListOf()
    private var email: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Handle back button click
        binding.backButton.setOnClickListener {
            finish()
        }

        // Handle send email button click
        binding.sendEmailButton.setOnClickListener {
            showEmailOptionsDialog()
        }

        // Get data from intent and populate the UI
        getDataFromIntent()
    }

    // Function to get data from intent
    private fun getDataFromIntent() {
        val receivedOrderDetails = intent.getParcelableExtra<OrderDetails>("UserOrderDetails")
        receivedOrderDetails?.let { orderDetails ->
            userName = receivedOrderDetails.userName
            foodName = receivedOrderDetails.foodNames as ArrayList<String>
            foodImages = receivedOrderDetails.foodImages as ArrayList<String>
            foodQuantity = receivedOrderDetails.foodQuantities as ArrayList<Int>
            address = receivedOrderDetails.address
            phoneNumber = receivedOrderDetails.phoneNumber
            foodPrices = receivedOrderDetails.foodPrices as ArrayList<String>
            totalPrice = receivedOrderDetails.totalPrice
            email = receivedOrderDetails.email // Ensure this field is available in OrderDetails

            Log.d("OrderDetailsActivity", "User Email: $email")
            if (email.isNullOrEmpty()) {
                Log.d("OrderDetailsActivity", "User Email is null or empty")
            }

            setUserDetails()
            setAdapter()
        }
    }

    // Function to set user details in the UI
    private fun setUserDetails() {
        binding.name.text = userName
        binding.address.text = address
        binding.phoneNo.text = phoneNumber
        binding.totalAmount.text = totalPrice
        binding.email.text = email
    }

    // Function to set the adapter for the RecyclerView
    private fun setAdapter() {
        binding.orderDetailsRecyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = OrderDetailsAdapter(this, foodName, foodImages, foodQuantity, foodPrices)
        binding.orderDetailsRecyclerView.adapter = adapter
    }

    // Function to show an AlertDialog for email options
    private fun showEmailOptionsDialog() {
        val options = arrayOf("Send order confirmation email", "Send order dispatch email")
        AlertDialog.Builder(this)
            .setTitle("Choose Email Type")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> sendOrderConfirmationEmail()
                    1 -> sendOrderDispatchEmail()
                }
            }
            .show()
    }

    // Function to send order confirmation email using implicit intent
    private fun sendOrderConfirmationEmail() {
        val subject = "Order Confirmation"
        val body = """
        Dear $userName,
        
        Your order has been confirmed. \nHere are the details:
        
        Address: $address
        Phone Number: $phoneNumber
        Total Price: $totalPrice
        
        Items Ordered:
        ${foodName.joinToString("\n")}
        
        Thank you for your purchase!
    """.trimIndent()

        sendEmail(subject, body)
    }

    // Function to send order dispatch email using implicit intent
    private fun sendOrderDispatchEmail() {
        val subject = "Order Dispatched"
        val body = """
        Dear $userName,
        
        Your order has been dispatched. \nHere are the details:
        
        Address: $address
        Phone Number: $phoneNumber
        Total Price: $totalPrice
        
        Items Ordered:
        ${foodName.joinToString("\n")}
        
        Thank you for your patience!
    """.trimIndent()

        sendEmail(subject, body)
    }

    // Function to send email using implicit intent
    private fun sendEmail(subject: String, body: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822" // MIME type for email
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }

        try {
            // Start an activity to send the email
            startActivity(Intent.createChooser(intent, "Send email via:"))
            Toast.makeText(this, "Email intent launched.", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to launch email client: ${e.message}", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }
}
