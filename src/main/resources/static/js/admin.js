let currentUser = null;
const API_BASE = '/api/admin';

// Проверка авторизации
async function checkAuth() {
    console.log('checkAuth вызван');
    try {
        const response = await axios.get(`${API_BASE}/auth/check`);
        console.log('Ответ от /auth/check:', response.data);

        if (response.data.authenticated) {
            currentUser = response.data.user;
            document.getElementById('currentUser').textContent = currentUser.username;
            console.log('Пользователь авторизован:', currentUser.username);

            // Определяем, какую страницу показать на основе URL
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
        } else {
            console.log('Пользователь не авторизован');
            showLoginMessage();
        }
    } catch (error) {
        console.error('Ошибка проверки авторизации:', error);
        showLoginMessage();
    }
}

// Показать сообщение о необходимости входа
function showLoginMessage() {
    console.log('Показываем сообщение о необходимости входа');
    document.getElementById('contentArea').innerHTML = `
        <div class="row justify-content-center">
            <div class="col-md-6 col-lg-4">
                <div class="card shadow">
                    <div class="card-body text-center">
                        <h3 class="card-title mb-4">Требуется вход</h3>
                        <div class="alert alert-warning">
                            <p>Для доступа к админ-панели необходимо войти в систему.</p>
                            <a href="/admin/login" class="btn btn-primary w-100 mt-3">
                                <i class="bi bi-box-arrow-in-right"></i> Перейти к входу
                            </a>
                        </div>
                        <p class="mt-3 text-muted">Используйте логин: <strong>admin</strong>, пароль: <strong>admin123</strong></p>
                    </div>
                </div>
            </div>
        </div>
    `;
}

// Выход
function logout() {
    if (confirm('Вы уверены, что хотите выйти?')) {
        window.location.href = '/admin/logout';
    }
}

// Загрузка контента
async function loadContent(url) {
    console.log('Загрузка контента:', url);

    // Показываем индикатор загрузки
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

        // Запустить специфичные для страницы скрипты
        executePageScripts();
    } catch (error) {
        console.error('Ошибка загрузки контента:', error);
        if (error.response && error.response.status === 403) {
            // Если доступ запрещен, показываем сообщение о необходимости входа
            showLoginMessage();
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

// Функции навигации
function showDashboard() {
    console.log('Показываем дашборд');
    loadContent('/admin/dashboard');
    setActiveNav('dashboard');
}

function showUsers() {
    console.log('Показываем пользователей');
    loadContent('/admin/users');
    setActiveNav('users');
}

function showCategories() {
    console.log('Показываем категории');
    loadContent('/admin/categories');
    setActiveNav('categories');
}

function showProducts() {
    console.log('Показываем продукты');
    loadContent('/admin/products');
    setActiveNav('products');
}

function showOrders() {
    console.log('Показываем заказы');
    loadContent('/admin/orders');
    setActiveNav('orders');
}

function showAddons() {
    console.log('Показываем добавки');
    loadContent('/admin/addons');
    setActiveNav('addons');
}

function showAdmins() {
    console.log('Показываем администраторов');
    loadContent('/admin/admins');
    setActiveNav('admins');
}

// Выполнение скриптов для загруженной страницы
function executePageScripts() {
    console.log('Выполнение скриптов для загруженной страницы');
    const content = document.getElementById('contentArea').innerHTML;

    // Дашборд
    if (content.includes('id="totalUsers"')) {
        console.log('Обнаружена страница дашборда');
        loadDashboardStats();
    }

    // Пользователи
    if (content.includes('id="usersTableBody"')) {
        console.log('Обнаружена страница пользователей');
        loadUsers();
    }

    // Категории
    if (content.includes('id="categoriesTableBody"')) {
        console.log('Обнаружена страница категорий');
        loadCategories();
    }

    // Продукты
    if (content.includes('id="productsTableBody"')) {
        console.log('Обнаружена страница продуктов');
        loadProducts();
    }

    // Заказы
    if (content.includes('id="ordersTableBody"')) {
        console.log('Обнаружена страница заказов');
        loadOrders();
    }

    // Добавки
    if (content.includes('id="addonsTableBody"')) {
        console.log('Обнаружена страница добавок');
        loadAddons();
    }

    // Администраторы
    if (content.includes('id="adminsTableBody"')) {
        console.log('Обнаружена страница администраторов');
        loadAdmins();
    }
}

// Функция для загрузки статистики на dashboard
async function loadDashboardStats() {
    try {
        console.log('Загрузка статистики...');
        const response = await axios.get(`${API_BASE}/stats`);
        const stats = response.data;

        console.log('Статистика загружена:', stats);

        if (document.getElementById('totalUsers')) {
            document.getElementById('totalUsers').textContent = stats.totalUsers || 0;
        }
        if (document.getElementById('activeOrders')) {
            document.getElementById('activeOrders').textContent = stats.activeOrders || 0;
        }
        if (document.getElementById('totalProducts')) {
            document.getElementById('totalProducts').textContent = stats.totalProducts || 0;
        }
        if (document.getElementById('todayRevenue')) {
            document.getElementById('todayRevenue').textContent = (stats.todayRevenue || 0) + ' ₽';
        }
    } catch (error) {
        console.error('Ошибка загрузки статистики:', error);
    }
}

// Функция для загрузки пользователей
async function loadUsers() {
    try {
        console.log('Загрузка пользователей...');
        const response = await axios.get(`${API_BASE}/users`);
        const users = response.data;
        const tbody = document.getElementById('usersTableBody');

        if (tbody) {
            tbody.innerHTML = '';

            if (!users || users.length === 0) {
                tbody.innerHTML = '<tr><td colspan="7" class="text-center">Нет пользователей</td></tr>';
                return;
            }

            users.forEach(user => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td>${user.id}</td>
                    <td>${user.username || user.firstName || 'Не указано'}</td>
                    <td>${user.telegramId || 'Не указан'}</td>
                    <td>${user.phoneNumber || '-'}</td>
                    <td>
                        <span class="badge ${user.active ? 'bg-success' : 'bg-danger'}">
                            ${user.active ? 'Активен' : 'Неактивен'}
                        </span>
                    </td>
                    <td>${user.createdAt ? new Date(user.createdAt).toLocaleDateString() : '—'}</td>
                    <td>
                        <button class="btn btn-sm btn-outline-primary" onclick="editUser(${user.id})">
                            <i class="bi bi-pencil"></i>
                        </button>
                        <button class="btn btn-sm btn-outline-danger ms-1" onclick="deleteUser(${user.id})">
                            <i class="bi bi-trash"></i>
                        </button>
                    </td>
                `;
                tbody.appendChild(tr);
            });
        }
    } catch (error) {
        console.error('Ошибка загрузки пользователей:', error);
        const tbody = document.getElementById('usersTableBody');
        if (tbody) {
            tbody.innerHTML = '<tr><td colspan="7" class="text-center text-danger">Ошибка загрузки данных</td></tr>';
        }
    }
}

// Функция для загрузки категорий
async function loadCategories() {
    try {
        console.log('Загрузка категорий...');
        const response = await axios.get(`${API_BASE}/categories`);
        const categories = response.data;
        const tbody = document.getElementById('categoriesTableBody');

        if (tbody) {
            tbody.innerHTML = '';

            if (!categories || categories.length === 0) {
                tbody.innerHTML = '<tr><td colspan="5" class="text-center">Нет категорий</td></tr>';
                return;
            }

            categories.forEach(category => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td>${category.id}</td>
                    <td>${category.name || 'Без названия'}</td>
                    <td>${category.description || '-'}</td>
                    <td>${category.displayOrder || 0}</td>
                    <td>
                        <button class="btn btn-sm btn-outline-primary" onclick="editCategory(${category.id})">
                            <i class="bi bi-pencil"></i>
                        </button>
                        <button class="btn btn-sm btn-outline-danger ms-1" onclick="deleteCategory(${category.id})">
                            <i class="bi bi-trash"></i>
                        </button>
                    </td>
                `;
                tbody.appendChild(tr);
            });
        }
    } catch (error) {
        console.error('Ошибка загрузки категорий:', error);
        const tbody = document.getElementById('categoriesTableBody');
        if (tbody) {
            tbody.innerHTML = '<tr><td colspan="5" class="text-center text-danger">Ошибка загрузки данных</td></tr>';
        }
    }
}

// Функция для загрузки продуктов
async function loadProducts() {
    console.log('Загрузка продуктов...');
    try {
        const response = await axios.get(`${API_BASE}/products`);
        const products = response.data;
        const tbody = document.getElementById('productsTableBody');

        if (tbody) {
            tbody.innerHTML = '';

            if (!products || products.length === 0) {
                tbody.innerHTML = '<tr><td colspan="5" class="text-center">Нет продуктов</td></tr>';
                return;
            }

            products.forEach(product => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td>${product.id}</td>
                    <td>${product.name || 'Без названия'}</td>
                    <td>${product.price || 0} ₽</td>
                    <td>${product.category?.name || 'Без категории'}</td>
                    <td>
                        <span class="badge ${product.available ? 'bg-success' : 'bg-danger'}">
                            ${product.available ? 'В наличии' : 'Нет в наличии'}
                        </span>
                    </td>
                    <td>
                        <button class="btn btn-sm btn-outline-primary" onclick="editProduct(${product.id})">
                            <i class="bi bi-pencil"></i>
                        </button>
                        <button class="btn btn-sm btn-outline-danger ms-1" onclick="deleteProduct(${product.id})">
                            <i class="bi bi-trash"></i>
                        </button>
                    </td>
                `;
                tbody.appendChild(tr);
            });
        }
    } catch (error) {
        console.error('Ошибка загрузки продуктов:', error);
    }
}

// Функция для загрузки заказов
async function loadOrders() {
    try {
        console.log('Загрузка заказов...');
        const response = await axios.get(`${API_BASE}/orders`);
        const orders = response.data;
        const tbody = document.getElementById('ordersTableBody');

        if (tbody) {
            tbody.innerHTML = '';

            if (!orders || orders.length === 0) {
                tbody.innerHTML = '<tr><td colspan="6" class="text-center">Нет заказов</td></tr>';
                return;
            }

            orders.forEach(order => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td>${order.id}</td>
                    <td>${order.user?.username || 'Неизвестный пользователь'}</td>
                    <td>${order.totalAmount || 0} ₽</td>
                    <td>
                        <span class="badge ${getStatusBadgeClass(order.status)}">
                            ${getStatusText(order.status)}
                        </span>
                    </td>
                    <td>${order.createdAt ? new Date(order.createdAt).toLocaleDateString() : '—'}</td>
                    <td>
                        <button class="btn btn-sm btn-outline-primary" onclick="viewOrder(${order.id})">
                            <i class="bi bi-eye"></i>
                        </button>
                    </td>
                `;
                tbody.appendChild(tr);
            });
        }
    } catch (error) {
        console.error('Ошибка загрузки заказов:', error);
    }
}

// Функция для загрузки добавок
async function loadAddons() {
    console.log('Загрузка добавок...');
    // Реализовать позже
    const tbody = document.getElementById('addonsTableBody');
    if (tbody) {
        tbody.innerHTML = '<tr><td colspan="5" class="text-center">Страница в разработке</td></tr>';
    }
}

// Функция для загрузки администраторов
async function loadAdmins() {
    console.log('Загрузка администраторов...');
    // Реализовать позже
    const tbody = document.getElementById('adminsTableBody');
    if (tbody) {
        tbody.innerHTML = '<tr><td colspan="5" class="text-center">Страница в разработке</td></tr>';
    }
}

// Вспомогательные функции
function getStatusBadgeClass(status) {
    if (!status) return 'bg-secondary';
    switch(status.toUpperCase()) {
        case 'NEW': return 'bg-info';
        case 'PROCESSING': return 'bg-warning';
        case 'COMPLETED': return 'bg-success';
        case 'CANCELLED': return 'bg-danger';
        default: return 'bg-secondary';
    }
}

function getStatusText(status) {
    if (!status) return 'Неизвестно';
    switch(status.toUpperCase()) {
        case 'NEW': return 'Новый';
        case 'PROCESSING': return 'В обработке';
        case 'COMPLETED': return 'Завершен';
        case 'CANCELLED': return 'Отменен';
        default: return status;
    }
}

// Установка активного пункта меню
function setActiveNav(navItem) {
    console.log('Активирован пункт меню:', navItem);

    // Убрать активный класс у всех элементов навигации
    document.querySelectorAll('.nav-link').forEach(link => {
        link.classList.remove('active');
    });

    // Добавить активный класс к выбранному элементу
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

// Инициализация при загрузке страницы
document.addEventListener('DOMContentLoaded', function() {
    console.log('DOM загружен, вызываем checkAuth');
    checkAuth();
});