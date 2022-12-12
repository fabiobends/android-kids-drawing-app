package com.example.kidsdrawingapp

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
  private var drawingView: DrawingView? = null
  private var brushButton: ImageButton? = null
  private var galleryButton: ImageButton? = null
  private var undoButton: ImageButton? = null
  private var brushDialog: Dialog? = null
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
    drawingView = findViewById(R.id.drawing_view)
    brushButton = findViewById(R.id.brush)
    galleryButton = findViewById(R.id.gallery)
    undoButton = findViewById(R.id.undo)
    brushButton?.setOnClickListener {
      showBrushSizeChooserDialog()
    }
    galleryButton?.setOnClickListener {
      requestStoragePermission()
    }
    undoButton?.setOnClickListener {
      onClickUndo()
    }
  }

  private fun onClickUndo() {
    drawingView?.undoLastDrawing()
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

  private fun requestStoragePermission() {
    if (ActivityCompat.shouldShowRequestPermissionRationale(
        this,
        Manifest.permission.READ_EXTERNAL_STORAGE
      )
    ) {
      showRationaleDialog("Kids Drawing App", "The app needs to access your external storage")
    } else {
      requestPermissionLauncher.launch(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE))
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
}