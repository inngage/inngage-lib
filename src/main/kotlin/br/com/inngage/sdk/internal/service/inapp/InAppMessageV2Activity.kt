package br.com.inngage.sdk.internal.service.inapp

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import br.com.inngage.sdk.R
import br.com.inngage.sdk.internal.core.config.InngageConfig
import br.com.inngage.sdk.internal.core.util.DeepLinkHandler
import br.com.inngage.sdk.internal.service.inapp.application.InAppCallbackHolder
import br.com.inngage.sdk.internal.service.inapp.domain.InAppActionMapper
import br.com.inngage.sdk.internal.service.inapp.domain.InAppMessageV2
import br.com.inngage.sdk.internal.service.inapp.domain.InAppV2Action
import br.com.inngage.sdk.internal.service.inapp.domain.InAppV2ActionType
import br.com.inngage.sdk.internal.service.inapp.domain.InAppV2Style


/**
 * Renders an In-App Message v2 fetched from the Inngage backend.
 *
 * The message is received as a [InAppMessageV2] domain object via [EXTRA_MESSAGE]
 * (no intermediate JSON parsing). A [ViewPager2] displays the slides:
 * a **single** slide renders as a banner, **multiple** slides as a carousel with a
 * dot indicator below the card.
 *
 * Each slide is rendered by [InAppSlideAdapter].
 */
internal class InAppMessageV2Activity : AppCompatActivity() {

    /** When false, the host app handles actions via the callback and the SDK does not navigate. */
    private var handledBySdk: Boolean = true

    companion object {
        /** Intent extra key for the [InAppMessageV2] payload. */
        const val EXTRA_MESSAGE = "inAppV2Message"
        /** Intent extra key for the "SDK handles actions" flag (defensive copy of the holder). */
        const val EXTRA_HANDLED_BY_SDK = "inAppV2HandledBySdk"
        private const val TAG = InngageConfig.TAG_INAPP

        /** Guard: prevents launching a second instance while one is already visible. */
        private val isShowing = java.util.concurrent.atomic.AtomicBoolean(false)

        /**
         * Returns true and marks as showing if no instance is currently visible.
         * Returns false (and does NOT mark) if one is already showing.
         */
        internal fun tryAcquire(): Boolean = isShowing.compareAndSet(false, true)

        /** Releases the guard — call when a launch acquired but failed to start. */
        internal fun release() {
            isShowing.set(false)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isShowing.set(false)
        // Drop the (possibly Activity-capturing) app callback so it is not retained.
        InAppCallbackHolder.clear()
        Log.d(TAG, "InAppMessageV2Activity: onDestroy — isShowing reset, callback cleared")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "InAppMessageV2Activity: onCreate")
        setContentView(R.layout.activity_inapp_v2)

        // Prefer the holder (set on launch); fall back to the Intent extra defensively.
        handledBySdk = InAppCallbackHolder.handledBySdk &&
            intent.getBooleanExtra(EXTRA_HANDLED_BY_SDK, true)

        val message = readMessage() ?: run {
            Log.e(TAG, "InAppMessageV2Activity: EXTRA_MESSAGE missing/invalid — finishing")
            finish(); return
        }
        Log.d(TAG, "InAppMessageV2Activity: message received → type=${message.type}, items=${message.media.carousel.items.size}, handledBySdk=$handledBySdk")

        render(message)

        // App-driven mode: deliver every action once, on display. The SDK will not navigate.
        if (!handledBySdk) {
            val actions = InAppActionMapper.map(message)
            Log.d(TAG, "InAppMessageV2Activity: delivering ${actions.size} action(s) to host app")
            InAppCallbackHolder.onActions?.invoke(actions)
        }
    }

    @Suppress("DEPRECATION")
    private fun readMessage(): InAppMessageV2? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_MESSAGE, InAppMessageV2::class.java)
        } else {
            intent.getSerializableExtra(EXTRA_MESSAGE) as? InAppMessageV2
        }
    }.getOrNull()

    // ── Main renderer ──────────────────────────────────────────────────────────

    private fun render(message: InAppMessageV2) {
        val style = message.style
        val items = message.media.carousel.items

        Log.d(TAG, "InAppMessageV2Activity: render() items=${items.size}")

        if (items.isEmpty()) {
            Log.e(TAG, "InAppMessageV2Activity: items is empty — finishing")
            finish(); return
        }

        val card          = findViewById<CardView>(R.id.cardInAppV2)
        val viewPager     = findViewById<ViewPager2>(R.id.inAppViewPager)
        val dotsContainer = findViewById<LinearLayout>(R.id.dotsContainer)

        applyCardBorder(style, card)
        findViewById<ImageButton>(R.id.closeButtonV2).setOnClickListener { finish() }

        // Adapter — all pages rendered up-front so we can measure them
        val adapter = InAppSlideAdapter(
            items     = items,
            style     = style,
            onAction  = { data, action ->
                if (handledBySdk) {
                    resolveAction(action)
                } else {
                    // App-driven: hand the tapped action (with its url) to the host app.
                    Log.d(TAG, "InAppMessageV2Activity: click → slide=${data.slideIndex}, button=${data.buttonIndex}, url=${data.url}")
                    InAppCallbackHolder.onActionClick?.invoke(data)
                }
            },
            onDismiss = { finish() }
        )
        viewPager.offscreenPageLimit = items.size
        viewPager.adapter = adapter

        // ── Dots ──────────────────────────────────────────────────────────────
        // A single slide is a banner (no dots); multiple slides form a carousel.
        val dots = if (items.size > 1) {
            dotsContainer.visibility = View.VISIBLE
            buildDots(items.size).also { updateDots(it, 0) }
        } else emptyList()

        // ── Smooth dynamic height ─────────────────────────────────────────────
        // pageHeights[i] = measured height of slide i (populated after first layout)
        val pageHeights = IntArray(items.size)
        var heightAnimator: ValueAnimator? = null

        /** Measures slide [index] from the RecyclerView's ViewHolder pool. */
        fun measurePageHeight(index: Int): Int {
            val recycler = viewPager.getChildAt(0) as? RecyclerView ?: return 0
            val vh = recycler.findViewHolderForAdapterPosition(index) ?: return 0
            val view = vh.itemView
            view.measure(
                View.MeasureSpec.makeMeasureSpec(viewPager.width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            )
            return view.measuredHeight
        }

        /** Sets ViewPager2 height instantly (no animation). */
        fun setViewPagerHeight(px: Int) {
            if (px <= 0 || viewPager.layoutParams.height == px) return
            viewPager.layoutParams = viewPager.layoutParams.also { it.height = px }
        }

        /** Animates ViewPager2 from its current height to [targetPx]. */
        fun animateToHeight(targetPx: Int) {
            if (targetPx <= 0) return
            val from = viewPager.layoutParams.height
            if (from == targetPx) return
            heightAnimator?.cancel()
            heightAnimator = ValueAnimator.ofInt(from, targetPx).apply {
                duration = 280
                interpolator = DecelerateInterpolator()
                addUpdateListener { setViewPagerHeight(it.animatedValue as Int) }
                start()
            }
        }

        // After the first layout pass all ViewHolders are attached — measure them all
        viewPager.post {
            for (i in 0 until items.size) {
                pageHeights[i] = measurePageHeight(i)
            }
            Log.d(TAG, "pageHeights = ${pageHeights.toList()}")
            // Set initial height immediately (no animation on first show)
            if (pageHeights[0] > 0) setViewPagerHeight(pageHeights[0])
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {

            /**
             * Called continuously during a drag — linearly interpolates height
             * between [position] and [position+1] so the card grows/shrinks
             * in perfect sync with the user's finger.
             */
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                val h0 = pageHeights.getOrElse(position) { 0 }
                val h1 = pageHeights.getOrElse(position + 1) { h0 }
                if (h0 <= 0) return
                val interpolated = (h0 + (h1 - h0) * positionOffset).toInt()
                // Cancel any running animator — the finger drives the height now
                heightAnimator?.cancel()
                heightAnimator = null
                setViewPagerHeight(interpolated)
            }

            /**
             * Called when a page settles (fling / programmatic scroll).
             * Animates to the exact target height for a polished snap feel.
             */
            override fun onPageSelected(position: Int) {
                if (dots.isNotEmpty()) updateDots(dots, position)
                val target = pageHeights.getOrElse(position) { 0 }
                if (target > 0) animateToHeight(target)
            }
        })
    }

    // ── Card border helper ─────────────────────────────────────────────────────

    private fun applyCardBorder(style: InAppV2Style, card: CardView) {
        val borderColor = style.borderColor.takeIf { it.isNotBlank() } ?: return
        val density = resources.displayMetrics.density
        val borderDrawable = android.graphics.drawable.GradientDrawable().apply {
            shape        = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = 16f * density
            setStroke((3 * density).toInt(), parseColor(borderColor))
            setColor(Color.TRANSPARENT)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            card.foreground = borderDrawable
        }
        card.cardElevation = if (style.hasShadow) 16f * density else 12f
    }

    // ── Dots ───────────────────────────────────────────────────────────────────

    private fun buildDots(count: Int): List<View> {
        val container = findViewById<LinearLayout>(R.id.dotsContainer)
        container.removeAllViews()
        val density = resources.displayMetrics.density
        val dotSize  = (8 * density).toInt()
        val margin   = (4 * density).toInt()
        return (0 until count).map {
            View(this).apply {
                layoutParams = LinearLayout.LayoutParams(dotSize, dotSize).also { lp ->
                    lp.setMargins(margin, 0, margin, 0)
                }
                background = dotDrawable(Color.WHITE)
                container.addView(this)
            }
        }
    }

    private fun updateDots(dots: List<View>, selected: Int) {
        dots.forEachIndexed { index, dot ->
            dot.background = dotDrawable(
                if (index == selected) Color.WHITE else Color.argb(128, 255, 255, 255)
            )
        }
    }

    private fun dotDrawable(color: Int) = android.graphics.drawable.GradientDrawable().apply {
        shape = android.graphics.drawable.GradientDrawable.OVAL
        setColor(color)
    }

    // ── Action resolution ──────────────────────────────────────────────────────

    /** SDK-side navigation for [handledBySdk] = true. */
    private fun resolveAction(action: InAppV2Action) {
        if (action.type == InAppV2ActionType.DISMISS) return
        val url = action.url
        when (action.type) {
            InAppV2ActionType.DEEP_LINK  -> { if (url.isNotBlank()) DeepLinkHandler.openDeepLink(this, url) }
            InAppV2ActionType.WEBLINK    -> { if (url.isNotBlank()) DeepLinkHandler.openInBrowser(this, url) }
            InAppV2ActionType.IN_APP_URL -> { if (url.isNotBlank()) DeepLinkHandler.openInBrowser(this, url) }
            InAppV2ActionType.METADATA   -> { /* metadata delivered via onAction callback — no navigation */ }
            InAppV2ActionType.DISMISS    -> { /* handled above */ }
        }
    }

    private fun parseColor(hex: String): Int =
        runCatching { Color.parseColor(hex) }.getOrDefault(Color.BLACK)
}
