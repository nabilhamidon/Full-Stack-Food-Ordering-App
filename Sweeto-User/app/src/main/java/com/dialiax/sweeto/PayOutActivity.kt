package com.dialiax.sweeto

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.dialiax.sweeto.databinding.ActivityPayOutBinding
import com.dialiax.sweeto.model.OrderDetails
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class PayOutActivity : AppCompatActivity() {

    lateinit var binding: ActivityPayOutBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var name: String
    private lateinit var address: String
    private lateinit var phone: String
    private lateinit var email: String // Added email field
    private lateinit var totalAmount: String
    private lateinit var foodItemsName: ArrayList<String>
    private lateinit var foodItemsPrice: ArrayList<String>
    private lateinit var foodItemsIngredients: ArrayList<String>
    private lateinit var foodItemsDescription: ArrayList<String>
    private lateinit var foodItemsImage: ArrayList<String>
    private lateinit var foodItemsQuantity: ArrayList<Int>
    private lateinit var databaseReference: DatabaseReference
    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPayOutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Init firebase and user details
        auth = FirebaseAuth.getInstance()
        databaseReference = FirebaseDatabase.getInstance().getReference()

        // Set user data
        setUserData()

        // Get user details from firebase
        val intent = intent
        foodItemsName = intent.getStringArrayListExtra("FoodItemsName") as ArrayList<String>
        foodItemsPrice = intent.getStringArrayListExtra("FoodItemsPrice") as ArrayList<String>
        foodItemsIngredients = intent.getStringArrayListExtra("FoodItemsIngredients") as ArrayList<String>
        foodItemsDescription = intent.getStringArrayListExtra("FoodItemsDescription") as ArrayList<String>
        foodItemsImage = intent.getStringArrayListExtra("FoodItemsImage") as ArrayList<String>
        foodItemsQuantity = intent.getIntegerArrayListExtra("FoodItemsQuantity") as ArrayList<Int>

        totalAmount = '$' + calculateTotalAmount().toString()
        binding.amount.isEnabled = false
        binding.amount.setText(totalAmount)

        binding.backButton.setOnClickListener {
            finish()
        }

        binding.placeMyOrder.setOnClickListener {
            // Get data from textview
            name = binding.name.text.toString().trim()
            address = binding.address.text.toString().trim()
            phone = binding.phone.text.toString().trim()
            email = binding.email.text.toString().trim() // Get email

            if (name.isBlank() || address.isBlank() || phone.isBlank() || email.isBlank()) {
                Toast.makeText(this, "Please Enter All The Details", Toast.LENGTH_SHORT).show()
            } else {
                placeOrder()
            }
        }

        // Set click listener for address TextView
        binding.address.setOnClickListener {
            val mapIntent = Intent(this, MapActivity::class.java)
            startActivityForResult(mapIntent, LOCATION_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == LOCATION_REQUEST_CODE && resultCode == RESULT_OK) {
            val selectedLocation = data?.getStringExtra("location")
            binding.address.text = selectedLocation
        }
    }

    private fun placeOrder() {
        userId = auth.currentUser?.uid ?: ""
        val time = System.currentTimeMillis()
        val itemPushKey = databaseReference.child("OrderDetails").push().key
        val orderDetails = OrderDetails(
            userId,
            name,
            foodItemsName,
            foodItemsImage,
            foodItemsPrice,
            foodItemsQuantity,
            address,
            totalAmount,
            phone,
            email, // Added email field
            time,
            itemPushKey,
            false,
            false
        )
        val orderReference = databaseReference.child("OrderDetails").child(itemPushKey!!)
        orderReference.setValue(orderDetails).addOnSuccessListener {
            val bottomSheetDialog = CongratsBottomSheet()
            bottomSheetDialog.show(supportFragmentManager, "Test")
            removeItemFromCart()
            addOrderToHistory(orderDetails)
        }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to Order", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addOrderToHistory(orderDetails: OrderDetails) {
        databaseReference.child("user").child(userId).child("BuyHistory")
            .child(orderDetails.itemPushKey!!)
            .setValue(orderDetails).addOnSuccessListener {

            }
    }

    private fun removeItemFromCart() {
        val cartItemsReference = databaseReference.child("user").child(userId).child("CartItems")
        cartItemsReference.removeValue()
    }

    private fun calculateTotalAmount(): Int {
        var totalAmount = 0
        for (i in 0 until foodItemsPrice.size) {
            val price = foodItemsPrice[i]
            val lastChar = price.last()
            val priceIntValue = if (lastChar == '$') {
                price.dropLast(1).toInt()
            } else {
                price.toInt()
            }
            val quantity = foodItemsQuantity[i]
            totalAmount += priceIntValue * quantity
        }
        return totalAmount
    }

    private fun setUserData() {
        val user = auth.currentUser
        if (user != null) {
            val userId = user.uid
            val userReference = databaseReference.child("user").child(userId)

            userReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val names = snapshot.child("name").getValue(String::class.java) ?: ""
                        val addresses = snapshot.child("address").getValue(String::class.java) ?: ""
                        val phones = snapshot.child("phone").getValue(String::class.java) ?: ""
                        val emails = snapshot.child("email").getValue(String::class.java) ?: "" // Fetch email
                        binding.apply {
                            name.setText(names)
                            address.setText(addresses)
                            phone.setText(phones)
                            email.setText(emails) // Set email
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                }

            })
        }
    }

    companion object {
        private const val LOCATION_REQUEST_CODE = 1000
    }
}
