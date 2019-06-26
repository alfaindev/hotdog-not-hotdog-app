package com.azk.seefood

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import com.bumptech.glide.Glide
import com.google.firebase.ml.common.modeldownload.FirebaseLocalModel
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler
import com.google.firebase.ml.vision.label.FirebaseVisionOnDeviceAutoMLImageLabelerOptions
import kotlinx.android.synthetic.main.activity_process.*
import java.io.IOException

class ProcessActivity : AppCompatActivity() {
    private lateinit var imageUri: Uri
    private lateinit var image: FirebaseVisionImage
    private lateinit var detector: FirebaseVisionImageLabeler
    private lateinit var label: String

    companion object {
        private val TAG = ProcessActivity::class.java.simpleName
        private val LOCAL_MODEL_NAME = "hotdog_nothotdog_model"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_process)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN

        imageUri = intent.getParcelableExtra("img_uri")
        Glide.with(this)
            .load(imageUri)
            .centerCrop()
            .into(img_result)

        prepareModel()
        processImage(imageUri)
    }

    private fun processImage(uri: Uri) {
        try {
            image = FirebaseVisionImage.fromFilePath(this, uri)
        } catch (e: IOException) {
            e.printStackTrace()
        }

        detector.processImage(image)
            .addOnSuccessListener { results ->
                for (result in results) {
                    Log.d(TAG, "onSuccessLabeling: label.text -> " + result.text)
                    Log.d(TAG, "onSuccessLabeling: label.entityId -> " + result.entityId)
                    Log.d(TAG, "onSuccessLabeling: label.confidence -> " + result.confidence)
                    label = result.text
                    break
                }
                if (results.isEmpty())
                    label = ""
                Handler().postDelayed({
                    hideLoadingView()
                    when(label) {
                        "hotdog" -> showHotdogView()
                        "not_hot_dog" -> showNotHotdogView()
                        else -> showConfusedView()
                    }
                }, 1000)
            }
            .addOnFailureListener { e ->
                Log.d(TAG, "onFailureLabeling: " + e.localizedMessage)
            }
    }

    private fun hideLoadingView() {
        view_dim_evaluating.visibility = View.GONE
        progressBar.visibility = View.GONE
        tv_evaluating.visibility = View.GONE
    }

    private fun showConfusedView() {
        view_confused.visibility = View.VISIBLE
    }

    private fun showNotHotdogView() {
        view_not_hotdog.visibility = View.VISIBLE
    }

    private fun showHotdogView() {
        view_hotdog.visibility = View.VISIBLE
    }

    private fun prepareModel() {
        Log.d(TAG, "prepareModel: preparing local model...")
        FirebaseModelManager.getInstance()
            .registerLocalModel(
                FirebaseLocalModel.Builder(LOCAL_MODEL_NAME)
                    .setAssetFilePath("automl/manifest.json")
                    .build()
            )
        val optionsBuilder = FirebaseVisionOnDeviceAutoMLImageLabelerOptions.Builder()
            .setConfidenceThreshold(0.5f)
            .setLocalModelName(LOCAL_MODEL_NAME)

        detector = FirebaseVision.getInstance().getOnDeviceAutoMLImageLabeler(optionsBuilder.build())
    }

    override fun onDestroy() {
        try {
            detector.close()
        } catch (e: IOException) {
            Log.d(TAG, "Exception thrown while trying to close the image labeler: $e")
        }
        super.onDestroy()
    }
}
