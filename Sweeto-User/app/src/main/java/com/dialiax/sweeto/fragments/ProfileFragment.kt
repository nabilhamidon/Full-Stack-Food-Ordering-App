package com.dialiax.sweeto.fragments

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.dialiax.sweeto.LoginActivity
import com.dialiax.sweeto.R
import com.dialiax.sweeto.databinding.FragmentProfileBinding
import com.dialiax.sweeto.model.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.bumptech.glide.Glide
import java.io.File
import java.io.FileOutputStream

class ProfileFragment : Fragment() {
    private lateinit var binding: FragmentProfileBinding

    private var auth = FirebaseAuth.getInstance()
    private var database = FirebaseDatabase.getInstance()
    private var storage = FirebaseStorage.getInstance()
    private lateinit var storageReference: StorageReference

    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    private var imageUri: Uri? = null



    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        storageReference = storage.reference.child("profile_pictures")


        binding.name.isEnabled = false
        binding.address.isEnabled = false
        binding.email.isEnabled = false
        binding.phoneNo.isEnabled = false

        setupActivityResultLaunchers()

        var isEnable = false

        binding.profileEdit.setOnClickListener {
            isEnable = !isEnable

            binding.name.isEnabled = isEnable
            binding.address.isEnabled = isEnable
            binding.email.isEnabled = isEnable
            binding.phoneNo.isEnabled = isEnable

            if (isEnable) {
                binding.name.requestFocus()
            }
        }

        binding.saveInfoButton.setOnClickListener {
            val name = binding.name.text.toString()
            val email = binding.email.text.toString()
            val address = binding.address.text.toString()
            val phone = binding.phoneNo.text.toString()

            if (imageUri != null) {
                // Upload image first and then update user data
                uploadImageToFirebase(imageUri!!, name, email, address, phone)
            } else {
                // Update user data only
                updateUserData(name, email, address, phone, null)
            }
        }

        binding.logOutButton.setOnClickListener {
            logoutUser()
        }

        binding.profilePPic.setOnClickListener {
            showImageSourceDialog()
        }

        setUserData()

        return binding.root
    }

    private fun setupActivityResultLaunchers() {
        cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val imageBitmap = result.data?.extras?.get("data") as Bitmap
                binding.profilePPic.setImageBitmap(imageBitmap)
                // Save image as a file and get URI
                imageUri = getImageUriFromBitmap(imageBitmap)
            }
        }

        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                showImageSourceDialog()
            } else {
                Toast.makeText(requireContext(), "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Take Photo")
        AlertDialog.Builder(requireContext())
            .setTitle("Select Action")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
                        } else {
                            openCamera()
                        }
                    }
                }
            }
            .show()
    }

    private fun openCamera() {
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent.resolveActivity(requireActivity().packageManager) != null) {
            cameraLauncher.launch(takePictureIntent)
        }
    }

    private fun getImageUriFromBitmap(bitmap: Bitmap): Uri {
        val file = File(requireContext().cacheDir, "profile_picture.jpg")
        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        outputStream.flush()
        outputStream.close()
        return Uri.fromFile(file)
    }

    private fun uploadImageToFirebase(uri: Uri, name: String, email: String, address: String, phone: String) {
        val userId = auth.currentUser?.uid
        val imageRef = storageReference.child("$userId.jpg")

        Log.d("ProfileFragment", "Uploading image to: ${imageRef.path}")

        val uploadTask = imageRef.putFile(uri)
        uploadTask.addOnSuccessListener {
            Log.d("ProfileFragment", "Image uploaded successfully")
            imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                Log.d("ProfileFragment", "Image download URL: $downloadUri")
                // Update user data including the image URL
                updateUserData(name, email, address, phone, downloadUri.toString())
            }.addOnFailureListener { exception ->
                Log.e("ProfileFragment", "Failed to get download URL: ${exception.message}")
                Toast.makeText(requireContext(), "Image Upload Failed", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { exception ->
            Log.e("ProfileFragment", "Image upload failed: ${exception.message}")
            Toast.makeText(requireContext(), "Image Upload Failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateUserData(name: String, email: String, address: String, phone: String, photoUrl: String?) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val userReference = database.getReference("user").child(userId)
            val userData = hashMapOf(
                "name" to name,
                "address" to address,
                "email" to email,
                "phone" to phone,
                "photoUrl" to photoUrl // Include the photo URL if it's not null
            )
            userReference.setValue(userData).addOnSuccessListener {
                Toast.makeText(requireContext(), "Profile Updated Successfully", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener {
                Toast.makeText(requireContext(), "Profile Update Failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun logoutUser() {
        auth.signOut()
        Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        activity?.finish()
    }

    private fun setUserData() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val userReference = database.getReference("user").child(userId)

            userReference.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val userProfile = snapshot.getValue(UserModel::class.java)
                        if (userProfile != null) {
                            binding.name.setText(userProfile.name)
                            binding.address.setText(userProfile.address)
                            binding.email.setText(userProfile.email)
                            binding.phoneNo.setText(userProfile.phone)

                            // Load profile picture if available
                            userProfile.photoUrl?.let { photoUrl ->
                                Glide.with(requireContext())
                                    .load(photoUrl)
                                    .placeholder(R.drawable.add) // Placeholder image
                                    .into(binding.profilePPic)
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }
}
