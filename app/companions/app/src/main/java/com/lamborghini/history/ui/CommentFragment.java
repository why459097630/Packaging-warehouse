class CommentFragment : Fragment() {
    private lateinit var commentAdapter: CommentAdapter
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_comment, container, false)
        commentAdapter = CommentAdapter(commentRepository.getCommentList())
        view.findViewById<RecyclerView>(R.id.comment_list).adapter = commentAdapter
        return view
    }
}
