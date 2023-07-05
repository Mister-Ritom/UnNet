package com.unreelnet.unnet.utils.reusable

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.animation.AccelerateInterpolator

class ReusableAnimator {

    companion object {
        @JvmStatic
        fun animate(view: View) {
            val animationSet = AnimatorSet()
            val accelerateInterpolator = AccelerateInterpolator()
            view.scaleX = 0.1F
            view.scaleY = 0.1F
            val scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 0.1f, 1f)
            scaleDownY.duration = 300
            scaleDownY.interpolator = accelerateInterpolator
            val scaleDownX = ObjectAnimator.ofFloat(view, "scaleX", 0.1f, 1f)
            scaleDownX.duration = 300
            scaleDownX.interpolator = accelerateInterpolator
            animationSet.playTogether(scaleDownX,scaleDownY)
            animationSet.start()
        }
    }
}