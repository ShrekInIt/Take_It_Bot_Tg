function isPageAllowed(page, role) {
    return ROLE_PERMISSIONS[role]?.includes(page);
}

class NavigationManager {
    static redirectBasedOnPath(userRole) {
        const path = window.location.pathname;

        const pageMap = [
            { key: 'dashboard', match: '/admin/dashboard', action: showDashboard },
            { key: 'users', match: '/admin/users', action: showUsers },
            { key: 'categories', match: '/admin/categories', action: showCategories },
            { key: 'products', match: '/admin/products', action: showProducts },
            { key: 'orders', match: '/admin/orders', action: showOrders },
            { key: 'admins', match: '/admin/admins', action: showAdmins }
        ];

        for (const page of pageMap) {
            if (path.includes(page.match)) {
                if (isPageAllowed(page.key, userRole)) {
                    page.action();
                    return;
                } else {
                    break;
                }
            }
        }

        // ⛔ если страница запрещена → первая разрешённая
        const fallback = ROLE_PERMISSIONS[userRole]?.[0];
        if (fallback === 'categories') showCategories();
        else if (fallback === 'products') showProducts();
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

    static redirectByRole(role) {
        if (role === 'ADMIN') {
            window.location.hash = '#products';
            showProducts();
            return;
        }
        // SUPER_ADMIN и всё остальное
        window.location.hash = '#dashboard';
        showDashboard();
    }
}

document.addEventListener('DOMContentLoaded', () => {
    const hash = window.location.hash;

    if (hash === '#products') showProducts();
    else if (hash === '#categories') showCategories();
    else if (hash === '#orders') showOrders();
    else if (hash === '#admins') showAdmins();
    else showDashboard();
});


document.addEventListener('click', (e) => {
    if (e.target.closest('#logoutBtn')) {
        if (confirm('Вы уверены, что хотите выйти?')) {
            window.location.href = '/admin/logout';
        }
    }
});
