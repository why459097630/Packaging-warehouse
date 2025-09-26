// MenuFragment.java

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

class MenuFragment : Fragment() {
    private lateinit var nameEditText: EditText
    private lateinit var descriptionEditText: EditText
    private lateinit var priceEditText: EditText
    private lateinit var imageImageView: ImageView
    private lateinit var addButton: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.menu_fragment, container, false)
        nameEditText = view.findViewById(R.id.name_edit_text)
        descriptionEditText = view.findViewById(R.id.description_edit_text)
        priceEditText = view.findViewById(R.id.price_edit_text)
        imageImageView = view.findViewById(R.id.image_image_view)
        addButton = view.findViewById(R.id.add_button)

        addButton.setOnClickListener {
            val name = nameEditText.text.toString()
            val description = descriptionEditText.text.toString()
            val price = priceEditText.text.toString().toDouble()
            val image = imageImageView.drawable.toString()

            val menu = Menu(name, description, price, image)
            // 上传菜品数据
            Toast.makeText(activity, "菜品添加成功", Toast.LENGTH_SHORT).show()
        }

        return view
    }
}
