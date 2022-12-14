package com.example.kidsdrawingapp

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View

/**
 * The reference link to create this class is
 * https://medium.com/@ssaurel/learn-to-create-a-paint-application-for-android-5b16968063f8
 */

private const val INITIAL_BRUSH_SIZE = 20.toFloat()
private const val INITIAL_COLOR = Color.BLACK
private val initialCoordinates = object {
  val X = 0f
  val Y = 0f
}

class DrawingView(context: Context, attrs: AttributeSet) : View(context, attrs) {
  private var drawingPath: DrawingPath? = null
  private var drawingPaths = ArrayList<DrawingPath>()
  private var drawingPaint: Paint? = null
  private var canvasBitmap: Bitmap? = null
  private var canvasPaint: Paint? = null
  private var canvas: Canvas? = null
  private var brushSize: Float = INITIAL_BRUSH_SIZE
  private var color = INITIAL_COLOR

  init {
    setUpDrawing()
  }

  private fun setUpDrawing() {
    setUpDrawingPaint()
    setUpDrawingPath()
    setUpCanvasPaint()
  }

  private fun setUpDrawingPaint() {
    drawingPaint = Paint()
    drawingPaint?.let {
      it.color = color
      it.style = Paint.Style.STROKE
      it.strokeJoin = Paint.Join.ROUND
      it.strokeCap = Paint.Cap.ROUND
    }
  }

  private fun setUpDrawingPath() {
    drawingPath = DrawingPath(color, brushSize)
  }

  private fun setUpCanvasPaint() {
    canvasPaint = Paint(Paint.DITHER_FLAG)
  }

  override fun onSizeChanged(width: Int, height: Int, oldwidth: Int, oldHeight: Int) {
    super.onSizeChanged(width, height, oldwidth, oldHeight)
    setUpCanvasBitmap(width, height)
  }

  private fun setUpCanvasBitmap(weight: Int, height: Int) {
    canvasBitmap = Bitmap.createBitmap(weight, height, Bitmap.Config.ARGB_8888)
    canvasBitmap?.let {
      canvas = Canvas(it)
    }
  }

  override fun onDraw(canvas: Canvas?) {
    super.onDraw(canvas)
    updateCanvasOnDraw(canvas)
  }

  private fun updateCanvasOnDraw(canvas: Canvas?) {
    canvas?.let {
      canvasBitmap?.let { bitmap ->
        val canvasWithBitmap = drawBitmapToCanvas(bitmap, it)
        val canvasWithPaths = appendPathsToCanvas(canvasWithBitmap)
        drawPathAndPaintWithBitmapToCanvas(canvasWithPaths)
      }
    }
  }

  private fun drawBitmapToCanvas(bitmap: Bitmap, canvas: Canvas): Canvas {
    canvas.drawBitmap(bitmap, initialCoordinates.X, initialCoordinates.Y, canvasPaint)
    return canvas
  }

  private fun appendPathsToCanvas(canvas: Canvas): Canvas {
    var updatedCanvas = canvas
    for (path in drawingPaths) {
      updatedCanvas = checkDrawingPathAndDrawPathToCanvas(path, canvas)
    }
    return updatedCanvas
  }

  private fun checkDrawingPathAndDrawPathToCanvas(
    drawingPath: DrawingPath,
    canvas: Canvas
  ): Canvas {
    drawingPaint?.let { paint ->
      paint.strokeWidth = drawingPath.brushThickness
      paint.color = drawingPath.color
      canvas.drawPath(drawingPath, paint)
    }
    return canvas
  }

  private fun drawPathAndPaintWithBitmapToCanvas(canvas: Canvas) {
    drawingPath?.let { path ->
      checkDrawingPathAndDrawPathToCanvas(path, canvas)
    }
  }

  override fun onTouchEvent(event: MotionEvent?): Boolean {
    return checkMotionEventAndSetResponse(event)
  }

  private fun checkMotionEventAndSetResponse(event: MotionEvent?): Boolean {
    event?.let {
      val xPosition = it.x
      val yPosition = it.y
      when (event.action) {
        MotionEvent.ACTION_DOWN -> {
          onActionDown(xPosition, yPosition)
        }
        MotionEvent.ACTION_MOVE -> {
          onActionMove(xPosition, yPosition)
        }
        MotionEvent.ACTION_UP -> {
          onActionUp()
        }
        else -> return false
      }
    }
    invalidate()
    return true
  }

  private fun onActionDown(
    xPosition: Float,
    yPosition: Float
  ) {
    drawingPath?.color = color
    drawingPath?.brushThickness = brushSize
    drawingPath?.reset()
    drawingPath?.moveTo(xPosition, yPosition)
  }

  private fun onActionMove(
    xPosition: Float,
    yPosition: Float
  ) {
    drawingPath?.lineTo(xPosition, yPosition)
  }

  private fun onActionUp() {
    drawingPath?.let {
      drawingPaths.add(it)
    }
    setUpDrawingPath()
  }

  fun setBrushSize(newSize: Int) {
    val size = newSize.toFloat()
    brushSize =
      TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, size, resources.displayMetrics)
    drawingPaint?.strokeWidth = brushSize
  }

  fun setPaintColor(newColor: String) {
    color = Color.parseColor(newColor)
    drawingPaint?.color = color
  }

  fun undoLastDrawing() {
    if (drawingPaths.size > 0) {
      drawingPaths.removeLast()
      invalidate()
    }
  }

  internal inner class DrawingPath(var color: Int, var brushThickness: Float) : Path()
}