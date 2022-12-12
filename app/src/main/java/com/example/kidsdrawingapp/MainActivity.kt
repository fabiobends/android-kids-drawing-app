package com.example.kidsdrawingapp

import android.app.Dialog
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {
  private var drawingView: DrawingView? = null
  private var brushButton: ImageButton? = null
  private var brushDialog: Dialog? = null
  private var brushSizeButtons = ArrayList<ImageButton>()
  private var currentPaintButton: ImageButton? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    drawingView = findViewById(R.id.drawing_view)
    brushButton = findViewById(R.id.brush)
    brushButton?.setOnClickListener {
      showBrushSizeChooserDialog()
    }
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
}