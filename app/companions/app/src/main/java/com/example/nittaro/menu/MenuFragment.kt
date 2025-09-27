import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.example.nittaro.menu.R

class MenuFragment : Fragment() {
    private lateinit var menuName: TextView
    private lateinit var menuPrice: TextView
    private lateinit var menuDescription: TextView
    private lateinit var menuImage: ImageView
    private lateinit var likeButton: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_menu, container, false)

        menuName = view.findViewById(R.id.menu_name)
        menuPrice = view.findViewById(R.id.menu_price)
        menuDescription = view.findViewById(R.id.menu_description)
        menuImage = view.findViewById(R.id.menu_image)
        likeButton = view.findViewById(R.id.like_button)

        likeButton.setOnClickListener {
            // 点赞逻辑
        }

        return view
    }
}
