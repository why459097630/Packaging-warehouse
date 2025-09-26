// DetailFragment.java

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity

class DetailFragment : Fragment() {
    private lateinit var imageImageView: ImageView
    private lateinit var likeButton: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.detail_fragment, container, false)
        imageImageView = view.findViewById(R.id.image_image_view)
        likeButton = view.findViewById(R.id.like_button)

        likeButton.setOnClickListener {
            // 点赞逻辑
            Toast.makeText(activity, "点赞成功", Toast.LENGTH_SHORT).show()
        }

        return view
    }
}
