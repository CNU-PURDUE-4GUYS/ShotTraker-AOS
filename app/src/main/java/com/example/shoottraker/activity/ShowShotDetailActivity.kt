package com.example.shoottraker.activity

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.shoottraker.database.BulletDatabase
import com.example.shoottraker.database.HistoryDatabase
import com.example.shoottraker.databinding.ActivityShowShotDetailBinding
import com.example.shoottraker.model.History
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.*
import kotlin.random.Random
import kotlin.system.exitProcess

class ShowShotDetailActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityShowShotDetailBinding.inflate(layoutInflater)
    }

    private val paint: Paint? = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.RED
        strokeWidth = STROKE_WIDTH
    }

    private val maxPaint: Paint? = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.BLUE
        strokeWidth = STROKE_LONG_WIDTH
    }

    private var totalBullet: Int? = null
    private var refUri: String? = null
    private var copyUri: String? = null
    private var bitmap: Bitmap? = null
    private var maxValues: MutableList<Float> = mutableListOf(0F, 0F, 0F, 0F)
    private var points: Array<Array<Float>>? = null
    private var distance: Float = 0F

    private var db: BulletDatabase? = null
    private var historyDb: HistoryDatabase? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        db = BulletDatabase.getInstance(applicationContext)
        historyDb = HistoryDatabase.getInstance(applicationContext)

        // Initialize variable from intent
        refUri = intent.getStringExtra("refUri")
        copyUri = intent.getStringExtra("copyUri")
        totalBullet = intent.getStringExtra("totalBullet")!!.toInt()
        points = intent.getSerializableExtra("points") as Array<Array<Float>>

        calculateDistance()
        drawBulletTraces()
        setResetButton()
        setSaveButton()
    }


    override fun onPause() {
        super.onPause()
        overridePendingTransition(0, 0)
    }

    // Calculate distance using points
    private fun calculateDistance() {
        for (i in points!!.indices) {
            for (j in i + 1 until points!!.size) {
                if (i == j) continue
                val temp = getDistance(
                    points!![i][0],
                    points!![i][1],
                    points!![j][0],
                    points!![j][1]
                )
                if (distance <= temp) {
                    distance = temp
                    maxValues[0] = points!![i][0]
                    maxValues[1] = points!![i][1]
                    maxValues[2] = points!![j][0]
                    maxValues[3] = points!![j][1]
                }
            }
        }
    }

    // Draw bullet traces using points array
    private fun drawBulletTraces() {
        bitmap = MediaStore.Images.Media.getBitmap(contentResolver, Uri.parse(copyUri))
        val copyBitmap = bitmap!!.copy(Bitmap.Config.ARGB_8888, true)
        val drawPoint = DrawDistance(binding.targetImageView.context)
        val canvas = Canvas(copyBitmap)
        drawPoint.draw(canvas)

        copyUri =
            MediaStore.Images.Media.insertImage(
                contentResolver,
                copyBitmap,
                Random(100).toString(),
                null
            )

        // Update targetImageView
        binding.targetImageView.setImageBitmap(copyBitmap)
    }

    // To draw shot group size, Declare inner class overriding onDraw
    private inner class DrawDistance(context: Context) : View(context) {
        override fun onDraw(canvas: Canvas?) {
            super.onDraw(canvas)

            canvas?.apply {
                for (i in points!!.indices) {
                    for (j in i + 1 until points!!.size) {
                        drawLine(
                            points!![i][0],
                            points!![i][1],
                            points!![j][0],
                            points!![j][1],
                            paint!!
                        )

                    }
                }

                // Draw longest distance
                drawLine(
                    maxValues[0],
                    maxValues[1],
                    maxValues[2],
                    maxValues[3],
                    maxPaint!!
                )
            }

            // Change the UI
            binding.showTotalBullet.text = points!!.size.toString()
            binding.showAverageSize.text = String.format("%.2f", distance * RATIO)
        }
    }

    // Calculate distance between two points
    private fun getDistance(src_x: Float, src_y: Float, dst_x: Float, dst_y: Float): Float {
        return sqrt(abs(src_x - dst_x).pow(2) + abs(src_y - dst_y).pow(2))
    }

    // Reset shot history
    private fun setResetButton() {
        binding.shotResetButton.setOnClickListener {
            val intent = Intent(this, StartShotActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            intent.putExtra("refUri", refUri)
            startActivity(intent)
            finish()
            exitProcess(0)
        }
    }

    // Save image in the Room DB
    private fun setSaveButton() {
        binding.shotSaveButton.setOnClickListener {
            // If finish the shot, save screenshot in gallery
//            val screenshotToView = window.decorView.rootView
//            screenshotToView.isDrawingCacheEnabled = true
//            val viewToBitmap = Bitmap.createBitmap(screenshotToView.drawingCache)
//            screenshotToView.isDrawingCacheEnabled = false
//
//            val bytes = ByteArrayOutputStream()
//            viewToBitmap!!.compress(Bitmap.CompressFormat.PNG, 100, bytes)
//
//            MediaStore.Images.Media.insertImage(
//                contentResolver,
//                viewToBitmap,
//                Random(100).toString(),
//                null
//            )

            // If finish the shot, save information in Room DB
            val current = LocalDateTime.now()
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val formatted = current.format(formatter)

            Thread {
                historyDb!!.HistoryDao().insertHistory(
                    History(
                        id = null,
                        imageUri = copyUri!!,
                        date = formatted,
                        totalBullet = totalBullet.toString(),
                        averageSize = String.format("%.2f", distance * RATIO)
                    )
                )
            }.start()

            finish()
        }
    }

    // Declare value using pixel to inch
    companion object {
        const val RATIO = 0.008594539939333
        const val STROKE_WIDTH = 12F
        const val STROKE_LONG_WIDTH = 15F
    }
}