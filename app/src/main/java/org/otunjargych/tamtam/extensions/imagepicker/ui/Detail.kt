package org.otunjargych.tamtam.extensions.imagepicker.ui

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import org.otunjargych.tamtam.databinding.DetailBinding
import org.otunjargych.tamtam.extensions.EXTRA_IMAGE
import org.otunjargych.tamtam.extensions.imagepicker.model.Image

internal class Detail : AppCompatActivity() {

    private lateinit var binding: DetailBinding

    companion object {

        fun starterIntent(context: Context, image: Image): Intent {
            return Intent(context, Detail::class.java).apply {
                putExtra(EXTRA_IMAGE, image)
            }
        }
    }

    private val image by lazy {
        intent.getParcelableExtra<Image>(
            EXTRA_IMAGE
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (image == null) {
            throw IllegalArgumentException("Missing Image")
        }
        postponeEnterTransition()

        image?.let {
            binding.detailImage.transitionName = image!!.id.toString()
            Glide.with(this)
                .load(it.path)
                .apply(RequestOptions().dontTransform())
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>?,
                        isFirstResource: Boolean
                    ): Boolean {
                        supportPostponeEnterTransition()
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable?,
                        model: Any?,
                        target: Target<Drawable>?,
                        dataSource: com.bumptech.glide.load.DataSource?,
                        isFirstResource: Boolean
                    ): Boolean {
                        startPostponedEnterTransition()
                        return false
                    }
                })
                .into(binding.detailImage)
        }
    }
}