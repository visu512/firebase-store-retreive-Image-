package com.music.musicify

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import java.io.ByteArrayOutputStream

class MainActivity : AppCompatActivity() {

    private lateinit var btnSelect: Button
    private lateinit var btnUpload: Button
    private lateinit var imageViewPreview: ImageView
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ImageAdapter
    private val imageList = mutableListOf<String>()

    private var bitmap: Bitmap? = null
    private val databaseRef: DatabaseReference = FirebaseDatabase.getInstance().getReference("images")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnSelect = findViewById(R.id.btnSelect)
        btnUpload = findViewById(R.id.btnUpload)
        imageViewPreview = findViewById(R.id.imageView)
        recyclerView = findViewById(R.id.recyclerView)

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ImageAdapter(imageList)
        recyclerView.adapter = adapter

        btnSelect.setOnClickListener { selectImage() }
        btnUpload.setOnClickListener { uploadImage() }

        retrieveImages() // Retrieve all images
    }

    private val imagePicker =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri = result.data?.data
                val inputStream = contentResolver.openInputStream(uri!!)
                bitmap = BitmapFactory.decodeStream(inputStream)

                // Show preview
                imageViewPreview.setImageBitmap(bitmap)
            }
        }

    private fun selectImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        imagePicker.launch(intent)
    }

    private fun uploadImage() {
        bitmap?.let {
            val base64Image = encodeImage(it)
            val imageId = databaseRef.push().key ?: return
            databaseRef.child(imageId).setValue(base64Image)
                .addOnSuccessListener {
                    Toast.makeText(this, "Image Uploaded!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Upload Failed", Toast.LENGTH_SHORT).show()
                }
        } ?: Toast.makeText(this, "No Image Selected", Toast.LENGTH_SHORT).show()
    }

    private fun encodeImage(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun retrieveImages() {
        databaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                imageList.clear()
                for (child in snapshot.children) {
                    val image = child.getValue(String::class.java)
                    image?.let { imageList.add(it) }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@MainActivity, "Failed to load images", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
