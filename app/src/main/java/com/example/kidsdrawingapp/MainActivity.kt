package com.example.kidsdrawingapp

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.media.MediaScannerConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

class MainActivity : AppCompatActivity() {
  private var drawingView: DrawingView? = null
  private var brushButton: ImageButton? = null
  private var galleryButton: ImageButton? = null
  private var undoButton: ImageButton? = null
  private var saveButton: ImageButton? = null
  private var brushDialog: Dialog? = null
  private var customProgressDialog: Dialog? = null
  private var brushSizeButtons = ArrayList<ImageButton>()
  private var currentPaintButton: ImageButton? = null

  private val requestPermissionLauncher: ActivityResultLauncher<Array<String>> =
    requestStoragePermissionResultLauncher()
  private val openGalleryLauncher: ActivityResultLauncher<Intent> =
    openGalleryResultLauncher()

  private fun requestStoragePermissionResultLauncher() =
    registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permission ->
      permission.entries.forEach {
        val permissionName = it.key
        val isGranted = it.value
        if (isGranted) {
          onGrantedPermission()
        } else {
          checkPermissionAndDisplayDeniedToast(permissionName)
        }
      }
    }

  private fun onGrantedPermission() {
    displayToast("Permission granted now you can read the storage files.")
    val pickIntent = Intent(
      Intent.ACTION_PICK,
      MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    )
    openGalleryLauncher.launch(pickIntent)
  }

  private fun displayToast(text: String) {
    Toast.makeText(
      this,
      text,
      Toast.LENGTH_LONG
    ).show()
  }

  private fun openGalleryResultLauncher() =
    registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
      if (it.resultCode == RESULT_OK && it.data != null) {
        val imageBackground: ImageView = findViewById(R.id.image_background)
        imageBackground.setImageURI(it.data?.data)
      }
    }

  private fun checkPermissionAndDisplayDeniedToast(permissionName: String) {
    if (permissionName == Manifest.permission.READ_EXTERNAL_STORAGE) {
      displayToast("Oops you just denied the permission.")
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    initializeElementsOnCreate()
    handleButtonsOnCreate()
  }

  private fun initializeElementsOnCreate() {
    drawingView = findViewById(R.id.drawing_view)
    brushButton = findViewById(R.id.brush)
    galleryButton = findViewById(R.id.gallery)
    undoButton = findViewById(R.id.undo)
    saveButton = findViewById(R.id.save)
  }

  private fun handleButtonsOnCreate() {
    brushButton?.setOnClickListener {
      showBrushSizeChooserDialog()
    }
    galleryButton?.setOnClickListener {
      requestStoragePermission()
    }
    undoButton?.setOnClickListener {
      onClickUndo()
    }
    saveButton?.setOnClickListener {
      onClickSave()
    }
  }

  private fun onClickUndo() {
    drawingView?.undoLastDrawing()
  }

  private fun onClickSave() {
    if (isReadStorageAllowed()) {
      showProgressDialog()
      lifecycleScope.launch {
        val frameDrawingView: FrameLayout = findViewById(R.id.drawing_view_container)
        val frameBitmap: Bitmap = getBitmapFromView(frameDrawingView)
        saveBitmapFile(frameBitmap)
      }
    }
  }

  private fun showProgressDialog() {
    customProgressDialog = Dialog(this)
    customProgressDialog?.setContentView(R.layout.dialog_custom_progress)
    customProgressDialog?.show()
  }

  private fun cancelProgressDialog() {
    customProgressDialog?.dismiss()
    customProgressDialog = null
  }

  private fun showBrushSizeChooserDialog() {
    brushDialog = Dialog(this)
    brushDialog?.let {
      it.setContentView(R.layout.dialog_brush_size)
      it.setTitle("Brush size: ")
      setUpDialogButtons()
      it.show()
    }
  }

  private fun setUpDialogButtons() {
    appendBrushButtons()
    for (button in brushSizeButtons) {
      when (button.id) {
        R.id.brush_size_small -> handleBrushSizeButton(button, 10)
        R.id.brush_size_medium -> handleBrushSizeButton(button, 20)
        R.id.brush_size_large -> handleBrushSizeButton(button, 30)
        else -> handleBrushSizeButton(button, 20)
      }
    }
  }

  private fun appendBrushButtons() {
    appendButton(R.id.brush_size_small)
    appendButton(R.id.brush_size_medium)
    appendButton(R.id.brush_size_large)
  }

  private fun appendButton(id: Int) {
    brushDialog?.let {
      val button: ImageButton = it.findViewById(id)
      brushSizeButtons.add(button)
    }
  }

  private fun handleBrushSizeButton(button: ImageButton, brushSize: Int) {
    brushDialog?.let { dialog ->
      button.setOnClickListener {
        drawingView?.setBrushSize(brushSize)
        dialog.dismiss()
      }
    }
  }

  private fun isReadStorageAllowed(): Boolean {
    val result = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
    return result == PackageManager.PERMISSION_GRANTED
  }

  private fun requestStoragePermission() {
    if (ActivityCompat.shouldShowRequestPermissionRationale(
        this,
        Manifest.permission.READ_EXTERNAL_STORAGE
      )
    ) {
      showRationaleDialog("Kids Drawing App", "The app needs to access your external storage")
    } else {
      requestPermissionLauncher.launch(
        arrayOf(
          Manifest.permission.READ_EXTERNAL_STORAGE,
          Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
      )
    }
  }

  fun onPaintClick(view: View) {
    if (view !== currentPaintButton) {
      val selectedPaintButton = view as ImageButton
      val selectedColor = selectedPaintButton.tag.toString()
      updatePaintColor(selectedPaintButton, selectedColor)
    }
  }

  private fun updatePaintColor(selectedPaintButton: ImageButton, color: String) {
    drawingView?.setPaintColor(color)
    val paintButton = setSelectedStyleToPaintButton(selectedPaintButton)
    setRegularStyleToCurrentPaintButton()
    currentPaintButton = paintButton
  }

  private fun setSelectedStyleToPaintButton(button: ImageButton): ImageButton {
    button.setImageDrawable(
      ContextCompat.getDrawable(
        this,
        R.drawable.pallet_selected
      )
    )
    return button
  }

  private fun setRegularStyleToCurrentPaintButton() {
    currentPaintButton?.setImageDrawable(
      ContextCompat.getDrawable(
        this,
        R.drawable.pallet_regular
      )
    )
  }

  private fun showRationaleDialog(title: String, message: String) {
    val builder = AlertDialog.Builder(this)
    builder.setTitle(title)
      .setMessage(message)
      .setPositiveButton("Cancel") { dialog, _ ->
        dialog.dismiss()
      }
    builder.create().show()
  }

  private fun getBitmapFromView(view: View): Bitmap {
    val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val backgroundImage = view.background
    if (backgroundImage != null) {
      backgroundImage.draw(canvas)
    } else {
      canvas.drawColor(Color.WHITE)
    }
    view.draw(canvas)
    return bitmap
  }

  private suspend fun saveBitmapFile(bitmap: Bitmap?): String {
    var result = ""
    withContext(Dispatchers.IO) {
      bitmap?.let {
        try {
          result = createFileFromBitmapAndGetFilePath(it)
          handleUiThreadOnSaveFile(result)
        } catch (error: Exception) {
          result = handleExceptionOnSaveFile(error)
        }
      }
    }
    return result
  }

  private fun createFileFromBitmapAndGetFilePath(bitmap: Bitmap): String {
    val bytes = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 90, bytes)
    val pathName = getPathName()
    val file = File(pathName)
    val fileOutput = FileOutputStream(file)
    fileOutput.write(bytes.toByteArray())
    fileOutput.close()
    return file.absolutePath
  }

  private fun getPathName(): String {
    return externalCacheDir?.absoluteFile.toString() +
        File.separator +
        "KidsDrawingApp_" +
        System.currentTimeMillis() / 1000 +
        ".png"
  }

  private fun handleUiThreadOnSaveFile(result: String) {
    runOnUiThread {
      cancelProgressDialog()
      if (result.isNotEmpty()) {
        displayToast("File saved successfully: $result")
        shareImage(result)
      } else {
        displayToast("Ops error while saving the file")
      }
    }
  }


  private fun shareImage(pathName: String) {
    MediaScannerConnection.scanFile(this, arrayOf(pathName), null) { _, uri ->
      val shareIntent = Intent()
      shareIntent.action = Intent.ACTION_SEND
      shareIntent.putExtra(Intent.EXTRA_STREAM, uri)
      shareIntent.type = "image/png"
      startActivity(Intent.createChooser(shareIntent, "Share"))
    }
  }

  private fun handleExceptionOnSaveFile(error: Exception): String {
    error.printStackTrace()
    return ""
  }
}