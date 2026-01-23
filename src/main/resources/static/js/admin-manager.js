class NavigationManager {
    static redirectBasedOnPath() {
        const path = window.location.pathname;
        if (path.includes('/admin/users')) {
            console.log('Открываем страницу пользователей');
            showUsers();
        } else if (path.includes('/admin/categories')) {
            console.log('Открываем страницу категорий');
            showCategories();
        } else if (path.includes('/admin/products')) {
            console.log('Открываем страницу продуктов');
            showProducts();
        } else if (path.includes('/admin/orders')) {
            console.log('Открываем страницу заказов');
            showOrders();
        } else if (path.includes('/admin/addons')) {
            console.log('Открываем страницу добавок');
            showAddons();
        } else if (path.includes('/admin/admins')) {
            console.log('Открываем страницу администраторов');
            showAdmins();
        } else {
            console.log('Открываем дашборд по умолчанию');
            showDashboard();
        }
    }

    async loadContent(url) {
        console.log('Загрузка контента:', url);

        document.getElementById('contentArea').innerHTML = `
            <div class="text-center my-5">
                <div class="spinner-border text-primary" role="status">
                    <span class="visually-hidden">Загрузка...</span>
                </div>
                <p class="mt-3">Загрузка...</p>
            </div>
        `;

        try {
            const response = await axios.get(url);
            console.log('HTML контент получен успешно');
            document.getElementById('contentArea').innerHTML = response.data;
            PageScriptExecutor.execute();
        } catch (error) {
            console.error('Ошибка загрузки контента:', error);
            if (error.response && error.response.status === 403) {
                new AuthManager().showLoginMessage();
            } else {
                document.getElementById('contentArea').innerHTML =
                    `<div class="alert alert-danger">
                        <h5>Ошибка загрузки контента</h5>
                        <p>${error.message}</p>
                        <p>Попробуйте войти заново:</p>
                        <a href="/admin/login" class="btn btn-primary">Войти</a>
                    </div>`;
            }
        }
    }

    static setActiveNav(navItem) {
        console.log('Активирован пункт меню:', navItem);

        document.querySelectorAll('.nav-link').forEach(link => {
            link.classList.remove('active');
        });

        const navLinks = {
            'dashboard': document.querySelector('.nav-link[onclick="showDashboard()"]'),
            'users': document.querySelector('.nav-link[onclick="showUsers()"]'),
            'categories': document.querySelector('.nav-link[onclick="showCategories()"]'),
            'products': document.querySelector('.nav-link[onclick="showProducts()"]'),
            'orders': document.querySelector('.nav-link[onclick="showOrders()"]'),
            'addons': document.querySelector('.nav-link[onclick="showAddons()"]'),
            'admins': document.querySelector('.nav-link[onclick="showAdmins()"]')
        };

        if (navLinks[navItem]) {
            navLinks[navItem].classList.add('active');
        }
    }
}