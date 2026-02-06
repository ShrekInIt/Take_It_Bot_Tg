class PageScriptExecutor {
    static async execute() {
        const contentEl = document.getElementById('contentArea');
        if (!contentEl) return;

        const content = contentEl.innerHTML;

        try {
            if (content.includes('id="totalUsers"')) {
                await DashboardManager.loadStats();
            }

            if (content.includes('id="usersTableBody"')) {
                await UserManager.loadUsers();
            }

            if (content.includes('id="categoriesTableBody"')) {
                await CategoryManager.loadCategories();
            }

            if (content.includes('id="productsTableBody"')) {
                await ProductManager.loadProducts();
            }

            if (content.includes('id="ordersTableBody"')) {
                await OrderManager.loadOrders();
            }

            if (content.includes('id="adminUsersTableBody"')) {
                await AdminManager.loadAdmins();
            }
        } catch (e) {
            if (typeof ToastManager !== 'undefined') {
                ToastManager.showToast('Ошибка загрузки данных страницы', 'danger');
            }
            console.error(e);
        }
    }
}
