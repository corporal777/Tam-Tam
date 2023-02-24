package org.otunjargych.tamtam.ui.views

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatAutoCompleteTextView
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import org.koin.android.compat.ViewModelCompat
import org.otunjargych.tamtam.R
import org.otunjargych.tamtam.model.request.LocationResponseModel
import org.otunjargych.tamtam.model.request.SpecialityModel
import org.otunjargych.tamtam.ui.town.CitiesViewModel
import org.otunjargych.tamtam.ui.views.adapters.NoFilterArrayAdapter

class SpecialitiesAutoCompleteTextView : AppCompatAutoCompleteTextView {

    var onDataSelectedListener: OnDataSelectedListener? = null
    var onDataChangedListener: OnDataSelectedListener? = null

    private lateinit var mViewModel: CitiesViewModel

    private val adapter =
        NoFilterArrayAdapter<String>(context, R.layout.layout_town, android.R.id.text1)
            .apply {
                setOnItemClickListener { _, _, position, _ ->
                    performOnItemSelected(position)
                }
            }

    private val simpleTextWatcher = SimpleTextWatcher().setAfterTextChangeRunnable {
        performOnItemChanged(it.toString())
    }

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        obtainAttributes(attrs)
    }

    init {
        setPadding(35, 0, 20, 0)
        setAdapter(adapter)
        addTextChangedListener(simpleTextWatcher)
    }

    private fun setData(list: List<SpecialityModel>) {
        adapter.apply {
            clear()
            addAll(list.map { it.name })
            notifyDataSetChanged()
        }
    }

    fun setTextWithoutSearch(text: String?) {
        removeTextChangedListener(simpleTextWatcher)
        setText(text)
        addTextChangedListener(simpleTextWatcher)
    }

    private fun performOnItemSelected(item: Int) {
        val data = adapter.getItem(item)
        onDataSelectedListener?.invoke(data ?: "")
    }

    private fun performOnItemChanged(item: String) {
        mViewModel.findSpecialities(item)
        onDataChangedListener?.invoke(item)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!this::mViewModel.isInitialized){
            mViewModel = ViewModelCompat.getViewModel(
                findViewTreeViewModelStoreOwner()!!,
                CitiesViewModel::class.java
            )
        }
        //mViewModel = getViewModel(findViewTreeViewModelStoreOwner()!!, CitiesViewModel::class.java)
        mViewModel.specialities.observe(findViewTreeLifecycleOwner()!!) {
            setData(it)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
    }

    private fun obtainAttributes(attrs: AttributeSet?) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.CustomInputEditText)
        val hint = a.getText(R.styleable.CustomInputEditText_customHint)
        a.recycle()
        setCustomHint(hint)
    }

    private fun setCustomHint(hint: CharSequence) {
        val spannableHint = SpannableString(hint)
        val font = Typeface.createFromAsset(context.assets, "noto_sans_regular.ttf")
        spannableHint.setSpan(
            CustomTypefaceSpan("", font),
            0,
            spannableHint.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        val textSize = resources.getDimensionPixelSize(R.dimen.text_input_hint_size)
        spannableHint.setSpan(
            AbsoluteSizeSpan(textSize),
            0,
            spannableHint.length,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        super.setHint(spannableHint)
    }
}