package com.example.myapplication

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.content.pm.PackageManager
import com.arthenica.mobileffmpeg.FFmpeg
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Base64
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.webkit.WebView
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.myapplication.ml.SsdMobilenetV11Metadata1
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp



class MainActivity : AppCompatActivity() {

    lateinit var labels:List<String>
    var colors = listOf<Int>(
        Color.BLUE, Color.GREEN, Color.RED, Color.CYAN, Color.GRAY, Color.BLACK,
        Color.DKGRAY, Color.MAGENTA, Color.YELLOW, Color.RED)
    val paint = Paint()
//    val mediaPlayer = MediaPlayer()
    lateinit var textToSpeech: TextToSpeech
    lateinit var imageProcessor: ImageProcessor
    private lateinit var context: Context
    lateinit var bitmap:Bitmap
    lateinit var imageView: ImageView
 //   lateinit var cameraDevice: CameraDevice
    lateinit var handler: Handler
   // lateinit var cameraManager: CameraManager
    lateinit var textureView: TextureView
    private lateinit var database:DatabaseReference
    lateinit var model:SsdMobilenetV11Metadata1
    private var isSpeaking = false


    private fun readData() {
        database = FirebaseDatabase.getInstance().getReference("esp32-cam")
        database.child("Photo").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val pic64 = dataSnapshot.value.toString()
                    val viewWidth = textureView.width
                    val viewHeight = textureView.height
                    val decodedBytes = Base64.decode(pic64, Base64.DEFAULT)
                    val originalBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    val scaledBitmap = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(scaledBitmap)
                    val scaleFactorX = viewWidth.toFloat() / originalBitmap.width.toFloat()
                    val scaleFactorY = viewHeight.toFloat() / originalBitmap.height.toFloat()
                    val scaleFactor = Math.max(scaleFactorX, scaleFactorY)
                    val scaledWidth = originalBitmap.width * scaleFactor
                    val scaledHeight = originalBitmap.height * scaleFactor
                    val x = (viewWidth - scaledWidth) / 2
                    val y = (viewHeight - scaledHeight) / 2
                    val rect = RectF(x, y, x + scaledWidth, y + scaledHeight)
                    canvas.drawBitmap(originalBitmap, null, rect, null)

                      // Draw the scaled bitmap on the TextureView surface
                    val surfaceTexture = textureView.surfaceTexture
                    val surface = Surface(surfaceTexture)
                    val canvas2 = surface.lockCanvas(null)
                    canvas2.drawBitmap(scaledBitmap, 0f, 0f, null)
                    surface.unlockCanvasAndPost(canvas2)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.i("Firebase Error", "failed")
            }
        })
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //get_permission()
        context = this
        val utteranceId = "myUtteranceId"

        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        // Called when the TTS engine starts speaking
                    }

                    override fun onDone(utteranceId: String?) {
                        // Called when the TTS engine finishes speaking
                        if (utteranceId == "myUtteranceId") {
                            isSpeaking = false
                        }
                    }

                    override fun onError(utteranceId: String?) {
                        // Called if there is an error with the TTS engine
                    }
                })
            }
        }

        labels = FileUtil.loadLabels(this, "labels.txt")
        imageProcessor = ImageProcessor.Builder().add(ResizeOp(300, 300, ResizeOp.ResizeMethod.BILINEAR)).build()
        model = SsdMobilenetV11Metadata1.newInstance(this)
        val handlerThread = HandlerThread("videoThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)

        imageView = findViewById(R.id.imageView)

        textureView = findViewById(R.id.textureView)
        // Assuming you have a TextureView component in your layout with ID "textureView"


        textureView.surfaceTextureListener = object:TextureView.SurfaceTextureListener{
            override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
                //open_camera();
                readData();

            }
            override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {
            }

            override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
                return true
            }

            override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {
                bitmap = textureView.bitmap!!
                var image = TensorImage.fromBitmap(bitmap)
                image = imageProcessor.process(image)

                val outputs = model.process(image)
                val locations = outputs.locationsAsTensorBuffer.floatArray
                val classes = outputs.classesAsTensorBuffer.floatArray
                val scores = outputs.scoresAsTensorBuffer.floatArray
                val numberOfDetections = outputs.numberOfDetectionsAsTensorBuffer.floatArray
                var mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true)
                val canvas = Canvas(mutable)
                var item = "";
                val h = mutable.height
                val w = mutable.width

                paint.textSize = h/15f
                paint.strokeWidth = h/85f
                var x = 0
                scores.forEachIndexed { index, fl ->
                    x = index
                    x *= 4
                    if(fl > 0.65){
                        paint.setColor(colors.get(index))
                        paint.style = Paint.Style.STROKE
                        canvas.drawRect(RectF(locations.get(x+1)*w, locations.get(x)*h, locations.get(x+3)*w, locations.get(x+2)*h), paint)
                        paint.style = Paint.Style.FILL
                        canvas.drawText(labels.get(classes.get(index).toInt()), locations.get(x+1)*w, locations.get(x)*h, paint)
                        if(!isSpeaking){
                            // If the text-to-speech function is not currently speaking, call it and set isSpeaking to true

                            textToSpeech.speak(labels.get(classes.get(index).toInt())+" Detected", TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                            isSpeaking = true

                        }
                    }
                }

                imageView.setImageBitmap(mutable)
            }
        }

        //cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager

    }


    override fun onDestroy() {
        super.onDestroy()
//        mediaPlayer.setOnCompletionListener(object:MediaPlayer.OnCompletionListener{
//            override fun onCompletion(p0: MediaPlayer?) {
//                mediaPlayer.release()
//            }
//        })
        model.close()
    }



    fun get_permission(){
        if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA), 101)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(grantResults[0] != PackageManager.PERMISSION_GRANTED){
            get_permission()
        }
    }
}


