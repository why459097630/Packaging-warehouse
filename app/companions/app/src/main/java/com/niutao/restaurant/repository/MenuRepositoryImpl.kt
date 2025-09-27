class MenuRepositoryImpl : MenuRepository {
    override fun getMenu(): List<Menu> {
        // 从数据库或网络获取菜单列表
        return listOf(Menu(1, "牛肉排", 15.99, "牛肉排", "menu_image"), Menu(2, "鸡肉排", 12.99, "鸡肉排", "menu_image"))
    }

    override fun saveMenu(menu: Menu) {
        // 保存菜单到数据库或网络
    }
}
