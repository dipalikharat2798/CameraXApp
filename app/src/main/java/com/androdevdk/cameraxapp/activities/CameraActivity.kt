package com.androdevdk.cameraxapp.activities

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.media.MediaScannerConnection
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.androdevdk.cameraxapp.R
import com.androdevdk.cameraxapp.adapter.CustomAdapter
import com.androdevdk.cameraxapp.model.Image
import kotlinx.android.synthetic.main.activity_camera.*
import kotlinx.android.synthetic.main.activity_main1.*
import kotlinx.android.synthetic.main.activity_main1.cameraSwitchButton1
import kotlinx.android.synthetic.main.activity_main1.donem1
import kotlinx.android.synthetic.main.activity_main1.finish_btnm1
import kotlinx.android.synthetic.main.activity_main1.mainHeading_tvm1
import kotlinx.android.synthetic.main.activity_main1.mainimageView1m1
import kotlinx.android.synthetic.main.activity_main1.mainimageViewm1
import kotlinx.android.synthetic.main.activity_main1.recyclerm1
import kotlinx.android.synthetic.main.activity_main1.retake_btnm1
import kotlinx.android.synthetic.main.activity_main1.saveAndNext_btnm1
import kotlinx.android.synthetic.main.activity_main1.tapToCapture_tvm1
import kotlinx.android.synthetic.main.activity_main1.viewFinder
import java.io.File
import java.text.SimpleDateFormat
import java.util.ArrayList
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraActivity : AppCompatActivity(), CustomAdapter.RecyclerImageClick {
    private var imageCapture: ImageCapture? = null
    private lateinit var outputDirectory: File
    val data1 = ArrayList<String>()
    var title = arrayOf<String>()
    private var count: Int = 0
    var adapter: CustomAdapter? = null
    lateinit var viewHolder: RecyclerView.ViewHolder
    private var displayId: Int = -1
    private var lensFacing: Int = CameraSelector.LENS_FACING_BACK
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null


    /** Blocking camera operations are performed using this executor */
    private lateinit var cameraExecutor: ExecutorService
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Request camera permissions
        if (allPermissionsGranted()) {
            setUpCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        title = getResources().getStringArray(R.array.heading);

        // this creates a vertical layout Manager
        recyclerm1.layoutManager = LinearLayoutManager(this)

        // ArrayList of class Image
        val data = ArrayList<Image>()

        addBtn.setVisibility(View.GONE)
        data.add(Image(R.drawable.overlayimg))
        data1.add("")


        // This will pass the ArrayList to our Adapter
        adapter = CustomAdapter(this, data, data1, this)

        // Setting the Adapter with the recyclerm1view
        recyclerm1.adapter = adapter

        viewFinder.setOnClickListener { takePhoto() }

        outputDirectory = getOutputDirectory()

        cameraExecutor = Executors.newSingleThreadExecutor()

        retake_btnm1.setOnClickListener() {
            Log.d(TAG + "Retake", "onCreate: " + count)

            for (i in count..4)
                data1.add(count, "")
            viewFinder.setVisibility(View.VISIBLE)
            tapToCapture_tvm1.setVisibility(View.VISIBLE)
            saveAndNext_btnm1.setVisibility(View.GONE)
            retake_btnm1.setVisibility(View.GONE)
            mainimageView1m1.setVisibility(View.GONE)
            mainimageViewm1.setVisibility(View.VISIBLE)
            mainimageViewm1.setImageDrawable(resources.getDrawable(R.drawable.overlayimg))
        }
        addBtn.setOnClickListener {
            data.add(Image(R.drawable.overlayimg))
            adapter?.notifyDataSetChanged()
            viewFinder.setVisibility(View.VISIBLE)
            mainimageViewm1.setVisibility(View.VISIBLE)
            mainimageViewm1.setImageDrawable(resources.getDrawable(R.drawable.overlayimg))
            addBtn.setVisibility(View.GONE)
        }
        saveAndNext_btnm1.setOnClickListener() {
            count++
            tapToCapture_tvm1.setVisibility(View.VISIBLE)
            addBtn.setVisibility(View.VISIBLE)
            mainimageView1m1.setVisibility(View.GONE)
            saveAndNext_btnm1.setVisibility(View.GONE)
            retake_btnm1.setVisibility(View.GONE)
            finish_btnm1.setVisibility(View.GONE)
            tapToCapture_tvm1.setVisibility(View.GONE)
        }
        finish_btnm1.setOnClickListener() {
            viewFinder.setVisibility(View.GONE)
            retake_btnm1.setVisibility(View.GONE)
            mainimageView1m1.setVisibility(View.GONE)
            finish_btnm1.setVisibility(View.GONE)
            mainimageViewm1.setVisibility(View.GONE)
            //    mainHeading_tvm1.setText("MISC Photo Captures Completed")
            saveAndNext_btnm1.setVisibility(View.GONE)
            donem1.setVisibility(View.VISIBLE)
        }
        viewFinder.post {
            // Keep track of the display in which this view is attached
            displayId = viewFinder.display.displayId

            // Build UI controls
            takePhoto()

            // Set up the camera and its use cases
            setUpCamera()
        }
    }

    private fun takePhoto() {
        // Get a stable reference of the modifiable image capture use case
        viewFinder?.setOnClickListener {

            // Get a stable reference of the modifiable image capture use case
            imageCapture?.let { imageCapture ->

                // Create output file to hold the image
                val photoFile = File(
                    outputDirectory,
                    SimpleDateFormat(
                        CameraActivity.FILENAME_FORMAT
                    ).format(System.currentTimeMillis()) + title[count] + ".jpg"
                )

                // Setup image capture metadata
                val metadata = ImageCapture.Metadata().apply {

                    // Mirror image when using the front camera
                    isReversedHorizontal = lensFacing == CameraSelector.LENS_FACING_FRONT
                }

                // Create output options object which contains file + metadata
                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile)
                    .setMetadata(metadata)
                    .build()

                // Setup image capture listener which is triggered after photo has been taken
                imageCapture.takePicture(
                    outputOptions, cameraExecutor, object : ImageCapture.OnImageSavedCallback {
                        override fun onError(exc: ImageCaptureException) {
                            Log.e(CameraActivity.TAG, "Photo capture failed: ${exc.message}", exc)
                        }

                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                            runOnUiThread {
                                // Stuff that updates the UI
                                retake_btnm1.setVisibility(View.VISIBLE)
                                saveAndNext_btnm1.setVisibility(View.VISIBLE)
                                tapToCapture_tvm1.setVisibility(View.GONE)
                                viewFinder.setVisibility(View.GONE)

                                val savedUri1 = output.savedUri ?: Uri.fromFile(photoFile)
                                mainimageViewm1.setImageURI(savedUri1)
                                Log.d(CameraActivity.TAG, output.savedUri.toString())

                                data1.add(count, savedUri1.toString())
                                adapter?.notifyDataSetChanged()

                                // If the folder selected is an external media directory, this is
                                // unnecessary but otherwise other apps will not be able to access our
                                // images unless we scan them using [MediaScannerConnection]
                                val mimeType = MimeTypeMap.getSingleton()
                                    .getMimeTypeFromExtension(savedUri1.toFile().extension)
                                MediaScannerConnection.scanFile(
                                    applicationContext,
                                    arrayOf(savedUri1.toFile().absolutePath),
                                    arrayOf(mimeType)
                                ) { _, uri ->
                                    Log.d(
                                        CameraActivity.TAG,
                                        "Image capture scanned into media store: $uri"
                                    )
                                }
                            }
                        }
                    })

            }
        }
        // Setup for button used to switch cameras
        cameraSwitchButton1?.let {

            // Disable the button until the camera is set up
            it.isEnabled = false

            // Listener for button used to switch cameras. Only called if the button is enabled
            it.setOnClickListener {
                lensFacing = if (CameraSelector.LENS_FACING_FRONT == lensFacing) {
                    CameraSelector.LENS_FACING_BACK
                } else {
                    CameraSelector.LENS_FACING_FRONT
                }
                // Re-bind use cases to update selected camera
                bindCameraUseCases()
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)

        // Rebind the camera with the updated display metrics
        bindCameraUseCases()

        // Enable or disable switching between cameras
        updateCameraSwitchButton()
    }

    private fun bindCameraUseCases() {


        // CameraProvider
        val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        // CameraSelector
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        // Preview
        preview = Preview.Builder()
            // We request aspect ratio but no resolution

            .build()

        // ImageCapture
        imageCapture = ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            // We request aspect ratio but no resolution to match preview config, but letting
            // CameraX optimize for whatever specific resolution best fits our use cases
            .build()

        // ImageAnalysis
        imageAnalyzer = ImageAnalysis.Builder()
            // We request aspect ratio but no resolution
            .build()
            // The analyzer can then be assigned to the instance
            .also {
                it.setAnalyzer(cameraExecutor, MainActivity3.LuminosityAnalyzer { luma ->
                    // Values returned from our analyzer are passed to the attached listener
                    // We log image analysis results here - you should do something useful
                    // instead!
                    //  Log.d(TAG, "Average luminosity: $luma")
                })
            }

        // Must unbind the use-cases before rebinding them
        cameraProvider.unbindAll()

        try {
            // A variable number of use-cases can be passed here -
            // camera provides access to CameraControl & CameraInfo
            camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageCapture, imageAnalyzer
            )

            // Attach the viewfinder's surface provider to preview use case
            preview?.setSurfaceProvider(viewFinder.surfaceProvider)
        } catch (exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

    private fun setUpCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {

            // CameraProvider
            cameraProvider = cameraProviderFuture.get()

            // Select lensFacing depending on the available cameras
            lensFacing = when {
                hasBackCamera() -> CameraSelector.LENS_FACING_BACK
                hasFrontCamera() -> CameraSelector.LENS_FACING_FRONT
                else -> throw IllegalStateException("Back and front camera are unavailable")
            }

            // Enable or disable switching between cameras
            updateCameraSwitchButton()

            // Build and bind the camera use cases
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(applicationContext))
    }

    private fun updateCameraSwitchButton() {
        try {
            cameraSwitchButton1?.isEnabled = hasBackCamera() && hasFrontCamera()
        } catch (exception: CameraInfoUnavailableException) {
            cameraSwitchButton1?.isEnabled = false
        }
    }

    /** Returns true if the device has an available back camera. False otherwise */
    private fun hasBackCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) ?: false
    }

    /** Returns true if the device has an available front camera. False otherwise */
    private fun hasFrontCamera(): Boolean {
        return cameraProvider?.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) ?: false
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

            // Select back camera as a default
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
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
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                //  startCamera()
                setUpCamera()
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

    override fun onCenterImageChange(imagePath: String?, position: Int) {
        Log.d(TAG, "onCenterImageChange: " + imagePath)
        mainimageView1m1.setImageURI(Uri.parse(imagePath))
        viewFinder.setVisibility(View.GONE)
        mainimageView1m1.setVisibility(View.VISIBLE)
        mainimageViewm1.setVisibility(View.GONE)
        tapToCapture_tvm1.setVisibility(View.GONE)
        donem1.setVisibility(View.GONE)
        //    mainHeading_tvm1.setText(title.get(position))
    }
}