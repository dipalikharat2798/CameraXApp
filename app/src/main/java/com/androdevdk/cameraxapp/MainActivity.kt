package com.androdevdk.cameraxapp

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.androdevdk.cameraxapp.Utility.Zoom
import com.androdevdk.cameraxapp.adapter.CustomAdapter
import com.androdevdk.cameraxapp.model.Image
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.item.*
import java.io.File
import java.nio.ByteBuffer
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

typealias LumaListener = (luma: Double) -> Unit

class MainActivity : AppCompatActivity(), CustomAdapter.RecyclerImageClick {
    private var imageCapture: ImageCapture? = null
    private var count: Int = 0
    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService
    val data1 = ArrayList<String>()
    var title = arrayOf<String>()
    var adapter: CustomAdapter? = null
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    lateinit var viewHolder: RecyclerView.ViewHolder
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        title = getResources().getStringArray(R.array.heading);

        val zoom: Zoom = Zoom(this)

        // this creates a vertical layout Manager
        recycler.layoutManager = LinearLayoutManager(this)

        // ArrayList of class Image
        val data = ArrayList<Image>()

        // This loop will create 20 Views containing
        // the image with the count of view
        for (i in 1..5) {
            data.add(Image(R.drawable.overlayimg))
            data1.add("")
        }

        // This will pass the ArrayList to our Adapter
        adapter = CustomAdapter(applicationContext, data, data1, this)

        // Setting the Adapter with the recyclerview
        recycler.adapter = adapter
        // Set up the listener for take photo button
        //  camera_capture_button.setOnClickListener { takePhoto() }
        mainHeading_tv.setText(title.get(count))
        // Log.d(TAG+"count", "onCreate: "+count)
        viewFinder.setOnClickListener {
            if (count < 5) {
                if (recycler.getChildAt(count) != null) {
                    viewHolder = recycler.getChildViewHolder(recycler.getChildAt(count))
                    setFadeAnimation(viewHolder.itemView.findViewById(R.id.animlayout))
                }
                takePhoto()
            }
        }

        outputDirectory = getOutputDirectory()

        cameraExecutor = Executors.newSingleThreadExecutor()

        retake_btn.setOnClickListener() {
            Log.d(TAG + "Retake", "onCreate: " + count)

            for (i in count..4)
                data1.add(count, "")
            viewFinder.setVisibility(View.VISIBLE)
            tapToCapture_tv.setVisibility(View.VISIBLE)
            saveAndNext_btn.setVisibility(View.GONE)
            retake_btn.setVisibility(View.GONE)
            mainimageView1.setVisibility(View.GONE)
            mainimageView.setVisibility(View.VISIBLE)
            mainimageView.setImageDrawable(resources.getDrawable(R.drawable.overlayimg))
        }
        saveAndNext_btn.setOnClickListener() {
            count++
            tapToCapture_tv.setVisibility(View.VISIBLE)
            viewFinder.setVisibility(View.VISIBLE)
            mainimageView1.setVisibility(View.GONE)
            saveAndNext_btn.setVisibility(View.GONE)
            mainimageView.setVisibility(View.VISIBLE)
            retake_btn.setVisibility(View.GONE)
            finish_btn.setVisibility(View.GONE)
            mainimageView.setImageDrawable(resources.getDrawable(R.drawable.overlayimg))
            if (count < 5) {
                mainHeading_tv.setText(title.get(count))
            } else {
                finish_btn.setVisibility(View.VISIBLE)
                tapToCapture_tv.setVisibility(View.GONE)
                mainimageView.setVisibility(View.GONE)
                viewFinder.setVisibility(View.GONE)
                mainimageView1.setVisibility(View.GONE)
                mainHeading_tv.setText("MISC Photo Captures Completed")
                saveAndNext_btn.setVisibility(View.GONE)
            }
        }
        finish_btn.setOnClickListener() {
            viewFinder.setVisibility(View.GONE)
            retake_btn.setVisibility(View.GONE)
            mainimageView1.setVisibility(View.GONE)
            finish_btn.setVisibility(View.GONE)
            mainimageView.setVisibility(View.GONE)
            mainHeading_tv.setText("MISC Photo Captures Completed")
            saveAndNext_btn.setVisibility(View.GONE)
            done.setVisibility(View.VISIBLE)
        }
    }

    override fun onResume() {
        super.onResume()
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time-stamped output file to hold the image
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(
                FILENAME_FORMAT
            ).format(System.currentTimeMillis()) + title[count] + ".jpg"
        )

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    retake_btn.setVisibility(View.VISIBLE)
                    saveAndNext_btn.setVisibility(View.VISIBLE)
                    tapToCapture_tv.setVisibility(View.GONE)
                    viewFinder.setVisibility(View.GONE)

                    val savedUri1 = output.savedUri ?: Uri.fromFile(photoFile)
                    mainimageView.setImageURI(savedUri1)
                    Log.d(TAG, output.savedUri.toString())
                    val num: Int = count
                    if (num < 5) {
                        data1.add(savedUri1.toString())
                        Log.d(TAG, "onImageSaved: " + num)
                        data1.add(count, savedUri1.toString())
                        adapter?.notifyDataSetChanged()
                        if (count < 5) {

                        } else {
                            finish_btn.setVisibility(View.VISIBLE)
                            retake_btn.setVisibility(View.GONE)
                            saveAndNext_btn.setVisibility(View.GONE)
                            Toast.makeText(
                                baseContext,
                                "Five captures completed-",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        finish_btn.setVisibility(View.VISIBLE)
                        retake_btn.setVisibility(View.GONE)
                        saveAndNext_btn.setVisibility(View.GONE)
                        Toast.makeText(baseContext, "Five captures completed", Toast.LENGTH_SHORT)
                            .show()
                    }
                }

            })
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .build()

            val imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, LuminosityAnalyzer { luma ->
                        // Log.d(TAG, "Average luminosity: $luma")
                    })
                }

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA


            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, imageAnalyzer
                )

            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() }
        }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    private class LuminosityAnalyzer(private val listener: LumaListener) : ImageAnalysis.Analyzer {

        private fun ByteBuffer.toByteArray(): ByteArray {
            rewind()    // Rewind the buffer to zero
            val data = ByteArray(remaining())
            get(data)   // Copy the buffer into a byte array
            return data // Return the byte array
        }

        override fun analyze(image: ImageProxy) {

            val buffer = image.planes[0].buffer
            val data = buffer.toByteArray()
            val pixels = data.map { it.toInt() and 0xFF }
            val luma = pixels.average()

            listener(luma)

            image.close()
        }
    }

    private fun setFadeAnimation(view: View) {
        val anim = ScaleAnimation(
            1.3f,
            1.3f,
            1.3f,
            1.3f,
            Animation.RELATIVE_TO_SELF,
            0.5f,
            Animation.RELATIVE_TO_SELF,
            0.5f
        )
        anim.duration = 9000
        view.startAnimation(anim)
    }

    override fun onCenterImageChange(imagePath: String?, position: Int) {
        Log.d(TAG, "onCenterImageChange: " + imagePath)

        mainimageView1.setImageURI(Uri.parse(imagePath))
        viewFinder.setVisibility(View.GONE)
        mainimageView1.setVisibility(View.VISIBLE)
        mainimageView.setVisibility(View.GONE)
        tapToCapture_tv.setVisibility(View.GONE)
        done.setVisibility(View.GONE)
        mainHeading_tv.setText(title.get(position))
    }

}