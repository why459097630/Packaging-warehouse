// CommentFragment.java

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

class CommentFragment : Fragment() {
    private lateinit var contentEditText: EditText
    private lateinit var addButton: Button

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.comment_fragment, container, false)
        contentEditText = view.findViewById(R.id.content_edit_text)
        addButton = view.findViewById(R.id.add_button)

        addButton.setOnClickListener {
            val content = contentEditText.text.toString()

            // 上传评论数据
            Toast.makeText(activity, "评论添加成功", Toast.LENGTH_SHORT).show()
        }

        return view
    }
}
