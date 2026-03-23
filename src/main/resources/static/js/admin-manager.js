/* global axios, bootstrap */

class NavigationManager {

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
            await PageScriptExecutor.execute();
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
        window.location.hash = '#dashboard';
        showDashboard();
    }
}

function getCookie(name) {
    const value = `; ${document.cookie}`;
    const parts = value.split(`; ${name}=`);
    if (parts.length === 2) {
        return parts.pop().split(';').shift();
    }
    return null;
}

document.addEventListener('DOMContentLoaded', () => {
    const hash = window.location.hash;

    if (hash === '#products') showProducts();
    else if (hash === '#categories') showCategories();
    else if (hash === '#orders') showOrders();
    else if (hash === '#admins') showAdmins();
    else showDashboard();
});

document.addEventListener('click', async (e) => {
    if (e.target.closest('#logoutBtn')) {
        if (!confirm('Вы уверены, что хотите выйти?')) {
            return;
        }

        try {
            const csrfToken = getCookie('XSRF-TOKEN');

            const response = await fetch('/admin/logout', {
                method: 'POST',
                headers: {
                    'X-XSRF-TOKEN': csrfToken
                }
            });

            if (response.redirected) {
                window.location.href = response.url;
            } else {
                window.location.href = '/admin/login?logout=true';
            }
        } catch (error) {
            console.error('Ошибка при выходе:', error);
            alert('Не удалось выйти из системы');
        }
    }
});
