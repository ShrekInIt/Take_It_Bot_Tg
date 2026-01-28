class PageScriptExecutor {
    static execute() {
        console.log('Выполнение скриптов для загруженной страницы');
        const content = document.getElementById('contentArea').innerHTML;

        if (content.includes('id="totalUsers"')) {
            console.log('Обнаружена страница дашборда');
            DashboardManager.loadStats();
        }

        if (content.includes('id="usersTableBody"')) {
            console.log('Обнаружена страница пользователей');
            UserManager.loadUsers();
        }

        if (content.includes('id="categoriesTableBody"')) {
            console.log('Обнаружена страница категорий');
            CategoryManager.loadCategories();
        }

        if (content.includes('id="productsTableBody"')) {
            console.log('Обнаружена страница продуктов');
            ProductManager.loadProducts();
        }

        if (content.includes('id="ordersTableBody"')) {
            console.log('Обнаружена страница заказов');
            OrderManager.loadOrders();
        }

        if (content.includes('id="addonsTableBody"')) {
            console.log('Обнаружена страница добавок');
            AddonManager.loadAddons();
        }

        if (content.includes('id="adminUsersTableBody"')) {
            console.log('Обнаружена страница администраторов');
            AdminManager.loadAdmins();
        }
    }
}