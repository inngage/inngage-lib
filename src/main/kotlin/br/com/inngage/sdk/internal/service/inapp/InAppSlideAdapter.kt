package br.com.inngage.sdk.internal.service.inapp

import android.graphics.Color
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import br.com.inngage.sdk.R
import br.com.inngage.sdk.internal.service.inapp.domain.InAppActionData
import br.com.inngage.sdk.internal.service.inapp.domain.InAppActionMapper
import br.com.inngage.sdk.internal.service.inapp.domain.InAppV2Action
import br.com.inngage.sdk.internal.service.inapp.domain.InAppV2Button
import br.com.inngage.sdk.internal.service.inapp.domain.InAppV2CarouselItem
import br.com.inngage.sdk.internal.service.inapp.domain.InAppV2Style
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

/**
 * ViewPager2 adapter that renders each [InAppV2CarouselItem] as a full slide page.
 *
 * Each page shows:
 * - An optional image (CENTER_CROP, 220 dp tall) at the top of the card.
 * - A content area with the global [style] background color, title, body, and buttons.
 *
 * @param items         Slides to display.
 * @param style         Global style (background color, title/body text colors).
 * @param onAction      Callback invoked when a button or background is tapped, carrying
 *                      both the public [InAppActionData] (slide/button context + url) and
 *                      the internal [InAppV2Action] used for SDK-side navigation.
 * @param onDismiss     Callback invoked when the activity should be dismissed.
 */
internal class InAppSlideAdapter(
    private val items: List<InAppV2CarouselItem>,
    private val style: InAppV2Style,
    private val onAction: (InAppActionData, InAppV2Action) -> Unit,
    private val onDismiss: () -> Unit
) : RecyclerView.Adapter<InAppSlideAdapter.SlideViewHolder>() {

    inner class SlideViewHolder(root: View) : RecyclerView.ViewHolder(root) {
        val image: ImageView        = root.findViewById(R.id.slideImage)
        val contentArea: LinearLayout = root.findViewById(R.id.slideContentArea)
        val title: TextView          = root.findViewById(R.id.slideTitle)
        val body: TextView           = root.findViewById(R.id.slideBody)
        val buttonsContainer: LinearLayout = root.findViewById(R.id.slideButtonsContainer)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlideViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_inapp_slide, parent, false)
        return SlideViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: SlideViewHolder, position: Int) {
        val item    = items[position]
        val context = holder.itemView.context
        val density = context.resources.displayMetrics.density

        // ── Image ──────────────────────────────────────────────────────────────
        // No image, or a broken/unreachable URL → render nothing (collapse the slot).
        if (item.image.isNotBlank()) {
            holder.image.visibility = View.VISIBLE
            Glide.with(context)
                .load(item.image)
                .centerCrop()
                .addListener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        holder.image.visibility = View.GONE
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean = false
                })
                .into(holder.image)
        } else {
            holder.image.visibility = View.GONE
        }

        // ── Content area background ────────────────────────────────────────────
        val bgColor = style.backgroundColor.takeIf { it.isNotBlank() }
        holder.contentArea.setBackgroundColor(
            bgColor?.let { parseColor(it) } ?: Color.WHITE
        )

        // ── Title ──────────────────────────────────────────────────────────────
        val title = item.content.title.takeIf { it.isNotBlank() }
        if (title != null) {
            holder.title.visibility = View.VISIBLE
            holder.title.text = title
            holder.title.setTextColor(parseColor(style.titleColor.takeIf { it.isNotBlank() } ?: "#000000"))
        } else {
            holder.title.visibility = View.GONE
        }

        // ── Body ───────────────────────────────────────────────────────────────
        val body = item.content.body.takeIf { it.isNotBlank() }
        if (body != null) {
            holder.body.visibility = View.VISIBLE
            holder.body.text = body
            holder.body.setTextColor(parseColor(style.bodyColor.takeIf { it.isNotBlank() } ?: "#000000"))
        } else {
            holder.body.visibility = View.GONE
        }

        // ── Buttons ────────────────────────────────────────────────────────────
        holder.buttonsContainer.removeAllViews()
        item.actions.buttons.forEachIndexed { buttonIndex, btn ->
            holder.buttonsContainer.addView(
                createButton(holder, position, buttonIndex, btn, density, item.actions.backgroundClick)
            )
        }

        // ── Background click ───────────────────────────────────────────────────
        val bgAction = item.actions.backgroundClick
        if (bgAction != null) {
            holder.itemView.setOnClickListener {
                val data = InAppActionMapper.toData(position, -1, "background", null, bgAction)
                onAction(data, bgAction)
                onDismiss()
            }
        }
    }

    // ── Button factory ─────────────────────────────────────────────────────────

    private fun createButton(
        holder: SlideViewHolder,
        slideIndex: Int,
        buttonIndex: Int,
        btn: InAppV2Button,
        density: Float,
        fallback: InAppV2Action?
    ): Button {
        val bgColor  = parseColor(btn.style.backgroundColor.takeIf { it.isNotBlank() } ?: "#000000")
        val txtColor = parseColor(btn.style.textColor.takeIf { it.isNotBlank() } ?: "#FFFFFF")

        val bgDrawable = android.graphics.drawable.GradientDrawable().apply {
            shape        = android.graphics.drawable.GradientDrawable.RECTANGLE
            cornerRadius = 50f * density
            setColor(bgColor)
        }

        return Button(holder.itemView.context).apply {
            text       = btn.text
            setTextColor(txtColor)
            background = bgDrawable
            isAllCaps  = false
            textSize   = 15f

            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                (48 * density).toInt()
            ).apply { setMargins(0, (8 * density).toInt(), 0, 0) }

            setOnClickListener {
                val action = btn.action ?: fallback ?: InAppV2Action()
                val data = InAppActionMapper.toData(slideIndex, buttonIndex, "button", btn.text, action)
                onAction(data, action)
                onDismiss()
            }
        }
    }

    // ── Utils ──────────────────────────────────────────────────────────────────

    private fun parseColor(hex: String): Int =
        runCatching { Color.parseColor(hex) }.getOrDefault(Color.BLACK)
}

