class UploadFragment : Fragment() {
    private lateinit var binding: FragmentUploadBinding
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentUploadBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.uploadButton.setOnClickListener {
            // 上传照片逻辑
        }
    }
}