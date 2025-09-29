class CommentRepository(private val commentDao: CommentDao) {
    fun getCommentList(): List<Comment> {
        return commentDao.getCommentList()
    }
}
