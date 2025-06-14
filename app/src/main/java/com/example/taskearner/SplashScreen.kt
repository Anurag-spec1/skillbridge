package com.example.taskearner


import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ReplacementSpan
import android.view.animation.OvershootInterpolator
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieListener
import com.example.taskearner.databinding.ActivitySplashScreenBinding


class SplashScreen : AppCompatActivity() {

    private lateinit var binding: ActivitySplashScreenBinding
    private val appName = "Skill Bridge"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        animateAppName()

        binding.lottie.addAnimatorListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                goToMainActivityWithDelay(500)
            }
        })
    }

    private fun animateAppName() {

        binding.appNameText.text = ""  // clear text initially
        val handler = Handler(Looper.getMainLooper())
        var delay = 0L

        for (char in appName) {
            handler.postDelayed({
                val letterView = TextView(this).apply {
                    text = char.toString()
                    textSize = 38f
                    setTextColor(Color.WHITE)
                    typeface = binding.appNameText.typeface
                    alpha = 0f
                    translationY = 20f
                }

                binding.appNameTextContainer.addView(letterView)

                letterView.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(300)
                    .setInterpolator(OvershootInterpolator())
                    .start()
            }, delay)

            delay += 150  // tighter timing for fluid effect
        }

        // After full name appears, glow effect on the whole container
        handler.postDelayed({
            val glow = ObjectAnimator.ofFloat(binding.appNameTextContainer, "alpha", 1f, 0.7f, 1f)
            glow.duration = 600
            glow.repeatCount = 1
            glow.start()
        }, delay + 300)

        // Ensure we transition even if Lottie fails
        handler.postDelayed({
            goToMainActivity()
        }, delay + 1200)
    }

    private fun goToMainActivityWithDelay(extraDelay: Long) {
        Handler(Looper.getMainLooper()).postDelayed({
            goToMainActivity()
        }, extraDelay)
    }

    private fun goToMainActivity() {
        startActivity(Intent(this, Login::class.java))
        finish()
    }
}
