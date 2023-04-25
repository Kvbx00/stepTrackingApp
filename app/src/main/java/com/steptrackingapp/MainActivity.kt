package com.steptrackingapp

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.mikhaellopez.circularprogressbar.CircularProgressBar
import java.math.BigDecimal
import java.math.RoundingMode
import android.Manifest

class MainActivity : AppCompatActivity(), SensorEventListener,
    SharedPreferences.OnSharedPreferenceChangeListener {

    private var sensorManager: SensorManager? = null
    private var running = false
    private var allSteps = 0f
    private var allPreviousSteps = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val permission = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACTIVITY_RECOGNITION
        )

        if (permission != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                PERMISSION_REQUEST_ACTIVITY_RECOGNITION
            )
        }

        loadData()
        resetSteps()

        supportActionBar?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setDisplayShowTitleEnabled(false)
        }

        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        val circularProgressBar = findViewById<CircularProgressBar>(R.id.circularProgressBar)
        val progressMaxTextView = findViewById<TextView>(R.id.progressMaxTextView)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val progressMax = prefs.getString("progress_input", "100")?.toFloatOrNull() ?: 100f

        circularProgressBar.progressMax = progressMax
        progressMaxTextView.text = String.format(getString(R.string.main_goal), progressMax.toInt())

    }

    companion object {
        const val PERMISSION_REQUEST_ACTIVITY_RECOGNITION = 0
    }

    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences?, key: String?) {

    }

    override fun onDestroy() {
        super.onDestroy()
        PreferenceManager.getDefaultSharedPreferences(this)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                val intent = Intent(this, SettingsActivity::class.java)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        running = true
        val stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (stepSensor == null) {
            Toast.makeText(this, R.string.main_sensor, Toast.LENGTH_SHORT).show()
        } else {
            sensorManager?.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (running) {
            allSteps = event!!.values[0]
            val currentSteps = allSteps.toInt() - allPreviousSteps.toInt()

            val stepsTaken = findViewById<TextView>(R.id.stepsTakenTextView)
            stepsTaken.text = ("$currentSteps")

            val circularProgressBar = findViewById<CircularProgressBar>(R.id.circularProgressBar)
            circularProgressBar.apply {
                setProgressWithAnimation(currentSteps.toFloat())
            }

            val prefs = PreferenceManager.getDefaultSharedPreferences(this)
            PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this)

            val height = prefs.getString("height", "170")?.toFloatOrNull() ?: 170f
            val weight = prefs.getString("weight", "55")?.toFloatOrNull() ?: 55f

            val stride = (height * 0.01f) * 0.414f
            val distance = stride * currentSteps
            val distanceKm = distance * 0.001f
            val distanceRounded = BigDecimal("$distanceKm").setScale(2, RoundingMode.HALF_EVEN)
            val distanceTextView = findViewById<TextView>(R.id.distanceTextView)
            distanceTextView.text = "$distanceRounded"

            val pace = prefs.getString("pace", "1")?.toInt() ?: 1
            when (pace) {
                1 -> {
                    val speed = 0.9
                    val met = 2.8
                    val time = distance / speed
                    val kcal = time * met * 3.5 * weight / (200 * 60)
                    val kcalTextView = findViewById<TextView>(R.id.kcalTextView)
                    kcalTextView.text = "${kcal.toInt()}"
                }
                2 -> {
                    val speed = 1.34
                    val met = 3.5
                    val time = distance / speed
                    val kcal = time * met * 3.5 * weight / (200 * 60)
                    val kcalTextView = findViewById<TextView>(R.id.kcalTextView)
                    kcalTextView.text = "${kcal.toInt()}"
                }
                3 -> {
                    val speed = 1.79
                    val met = 5
                    val time = distance / speed
                    val kcal = time * met * 3.5 * weight / (200 * 60)
                    val kcalTextView = findViewById<TextView>(R.id.kcalTextView)
                    kcalTextView.text = "${kcal.toInt()}"
                }
            }
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {
    }

    private fun resetSteps() {
        val stepsTaken = findViewById<TextView>(R.id.stepsTakenTextView)
        stepsTaken.setOnClickListener {
            Toast.makeText(this, R.string.main_reset, Toast.LENGTH_SHORT).show()
        }

        val circularProgressBar = findViewById<CircularProgressBar>(R.id.circularProgressBar)
        val distanceTextView = findViewById<TextView>(R.id.distanceTextView)
        val kcalTextView = findViewById<TextView>(R.id.kcalTextView)
        stepsTaken.setOnLongClickListener {
            allPreviousSteps = allSteps
            circularProgressBar.progress = 0f
            stepsTaken.text = 0.toString()
            kcalTextView.text = 0.toString()
            distanceTextView.text = 0.00.toString()
            saveData()

            true
        }
    }

    private fun saveData() {
        val sharedPreferences = getSharedPreferences("preferences", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putFloat("number", allPreviousSteps)
        editor.apply()
    }

    private fun loadData() {
        val sharedPreferences = getSharedPreferences("preferences", Context.MODE_PRIVATE)
        val savedNumber = sharedPreferences.getFloat("number", 0f)
        Log.d("MainActivity", "$savedNumber")
        allPreviousSteps = savedNumber
    }

}