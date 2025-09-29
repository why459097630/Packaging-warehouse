class LikeButton : Button() {
    private var isLiked = false
    fun like() {
        isLiked = true
        text = "Unlike"
    }
    fun unlike() {
        isLiked = false
        text = "Like"
    }
    fun isLiked(): Boolean {
        return isLiked
    }
}
