class CommentFragment : Fragment() {
    private lateinit var binding: FragmentCommentBinding
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentCommentBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.likeButton.setOnClickListener {
            // 点赞逻辑
        }
    }
}