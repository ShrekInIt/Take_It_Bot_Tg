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

async function loadAdmins() {
    console.log('Загрузка администраторов...');
    const tbody = document.getElementById('adminUsersTableBody');

    if (!tbody) return;

    tbody.innerHTML = '<tr><td colspan="6" class="text-center">Загрузка...</td></tr>';

    try {
        const response = await axios.get(`${API_BASE}/admins`);
        const admins = response.data;

        tbody.innerHTML = '';

        if (!admins || admins.length === 0) {
            tbody.innerHTML =
                '<tr><td colspan="6" class="text-center">Администраторы не найдены</td></tr>';
            return;
        }

        // Используем createAdminRowElement для создания всех строк
        admins.forEach(admin => {
            const tr = createAdminRowElement(admin);
            tbody.appendChild(tr);
        });

    } catch (error) {
        console.error('Ошибка загрузки администраторов:', error);
        tbody.innerHTML =
            '<tr><td colspan="6" class="text-center text-danger">Ошибка загрузки</td></tr>';
    }
}


function showCreateAdminUserModal() {
    // Удаляем старую модалку, если она есть
    const oldModal = document.getElementById('createAdminUserModal');
    if (oldModal) {
        const instance = bootstrap.Modal.getInstance(oldModal);
        if (instance) {
            instance.dispose();
        }
        oldModal.remove();
    }

    const modalHtml = `
    <div class="modal fade" id="createAdminUserModal" tabindex="-1">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">Добавить администратора</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>

                <div class="modal-body">
                    <form id="createAdminUserForm">
                        <div class="mb-3">
                            <label class="form-label">Логин</label>
                            <input type="text" class="form-control" name="username" required>
                        </div>

                        <div class="mb-3">
                            <label class="form-label">Пароль</label>
                            <div class="input-group">
                                <input type="password" class="form-control" name="password" required id="adminPasswordInput">
                                <button class="btn btn-outline-secondary" type="button" onclick="togglePasswordVisibility('adminPasswordInput')">
                                    <i class="bi bi-eye"></i>
                                </button>
                            </div>
                        </div>

                        <div class="mb-3">
                            <label class="form-label">Роль</label>
                            <select class="form-select" name="role">
                                <option value="ADMIN">ADMIN</option>
                                <option value="SUPER_ADMIN">SUPER_ADMIN</option>
                            </select>
                        </div>

                        <div class="form-check mb-3">
                            <input class="form-check-input" type="checkbox" name="isActive" checked>
                            <label class="form-check-label">
                                Активен
                            </label>
                        </div>
                    </form>

                    <div id="createAdminError" class="alert alert-danger d-none mt-3"></div>
                </div>

                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">
                        Отмена
                    </button>
                    <button type="button" class="btn btn-primary" onclick="createAdminUser()">
                        Создать
                    </button>
                </div>
            </div>
        </div>
    </div>
    `;

    document.body.insertAdjacentHTML('beforeend', modalHtml);

    const modal = new bootstrap.Modal(document.getElementById('createAdminUserModal'));
    modal.show();
}

function togglePasswordVisibility(inputId) {
    const input = document.getElementById(inputId);
    if (!input) return;

    if (input.type === 'password') {
        input.type = 'text';
    } else {
        input.type = 'password';
    }
}


async function createAdminUser() {
    const form = document.getElementById('createAdminUserForm');
    const errorBlock = document.getElementById('createAdminError');

    // Получаем значения напрямую из элементов
    const payload = {
        username: form.querySelector('[name="username"]').value,
        password: form.querySelector('#adminPasswordInput').value, // Важно: берем по id!
        role: form.querySelector('[name="role"]').value,
        isActive: form.querySelector('[name="isActive"]').checked
    };

    // Логирование для отладки
    console.log('Отправляемые данные:', payload);
    console.log('Пароль пустой?', !payload.password);

    try {
        const response = await axios.post(`${API_BASE}/admins`, payload);
        const created = response.data;

        // Закрываем модалку
        const modalEl = document.getElementById('createAdminUserModal');
        bootstrap.Modal.getInstance(modalEl).hide();

        if (created && created.id) {
            insertOrUpdateAdminRow(created);
            showToast('Администратор создан', 'success');
        } else {
            loadAdmins();
            showToast('Администратор создан', 'success');
        }
    } catch (error) {
        console.error('Ошибка создания администратора:', error);

        // Подробный вывод ошибки
        if (error.response) {
            console.error('Ответ сервера:', error.response.data);
            console.error('Статус:', error.response.status);
        }

        errorBlock.textContent = error.response?.data?.message ||
            error.response?.data?.error ||
            'Ошибка при создании администратора';
        errorBlock.classList.remove('d-none');

        showToast('Ошибка при создании администратора', 'danger');
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

    if (content.includes('id="adminUsersTableBody"')) {
        console.log('Обнаружена страница администраторов');
        loadAdmins(); // теперь точно вызывается
    }
}

async function showEditAdminModal(adminId) {
    let admin;
    try {
        const response = await axios.get(`${API_BASE}/admins/${adminId}`);
        admin = response.data;
    } catch (error) {
        console.error('Не удалось получить данные админа', error);
        showToast('Ошибка загрузки данных администратора', 'danger');
        return;
    }

    // Удаляем старое модальное окно
    const oldModal = document.getElementById('editAdminModal');
    if (oldModal) oldModal.remove();

    const modalHtml = `
    <div class="modal fade" id="editAdminModal" tabindex="-1">
        <div class="modal-dialog">
            <div class="modal-content">
                <div class="modal-header">
                    <h5 class="modal-title">Редактировать администратора</h5>
                    <button type="button" class="btn-close" data-bs-dismiss="modal"></button>
                </div>
                <div class="modal-body">
                    <form id="editAdminForm">
                        <input type="hidden" name="id" value="${admin.id}" />
                        <div class="mb-3">
                            <label class="form-label">Логин</label>
                            <input type="text" class="form-control" name="username" 
                                   value="${admin.username || ''}" required>
                        </div>
                        <div class="mb-3">
                            <label class="form-label">Новый пароль (оставьте пустым, чтобы не менять)</label>
                            <div class="input-group">
                                <input type="password" class="form-control" 
                                       id="editAdminPasswordInput" placeholder="Новый пароль">
                                <button class="btn btn-outline-secondary" type="button" 
                                        onclick="togglePasswordVisibility('editAdminPasswordInput')">
                                    <i class="bi bi-eye"></i>
                                </button>
                            </div>
                        </div>
                        <div class="mb-3">
                            <label class="form-label">Роль</label>
                            <select class="form-select" name="role">
                                <option value="ADMIN" ${admin.role === 'ADMIN' ? 'selected' : ''}>ADMIN</option>
                                <option value="SUPER_ADMIN" ${admin.role === 'SUPER_ADMIN' ? 'selected' : ''}>SUPER_ADMIN</option>
                            </select>
                        </div>
                        <div class="form-check mb-3">
                            <input class="form-check-input" type="checkbox" name="isActive" 
                                   id="editAdminIsActive" ${admin.isActive ? 'checked' : ''}>
                            <label class="form-check-label" for="editAdminIsActive">
                                Активен
                            </label>
                        </div>
                        <div id="editAdminError" class="alert alert-danger d-none"></div>
                    </form>
                </div>
                <div class="modal-footer">
                    <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Отмена</button>
                    <button type="button" class="btn btn-primary" onclick="updateAdmin(${admin.id})">Сохранить</button>
                </div>
            </div>
        </div>
    </div>
    `;

    document.body.insertAdjacentHTML('beforeend', modalHtml);

    const modal = new bootstrap.Modal(document.getElementById('editAdminModal'));
    modal.show();
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

function createAdminRowElement(admin) {
    const tr = document.createElement('tr');
    tr.setAttribute('data-admin-id', admin.id);
    const isActive = admin.isActive !== undefined ? admin.isActive : admin.active;
    const created = admin.createdAt ? new Date(admin.createdAt).toLocaleString() : '—';

    tr.innerHTML = `
        <td>${admin.id}</td>
        <td>${admin.username || ''}</td>
        <td>${admin.role || ''}</td>
        <td>
            <span class="badge ${isActive ? 'bg-success' : 'bg-danger'}">
                ${isActive ? 'Да' : 'Нет'}
            </span>
        </td>
        <td>${created}</td>
        <td>
            <button class="btn btn-sm btn-outline-primary me-1" onclick="showEditAdminModal(${admin.id})">
                <i class="bi bi-pencil"></i>
            </button>
            <button class="btn btn-sm btn-outline-danger" onclick="deleteAdmin(${admin.id})">
                <i class="bi bi-trash"></i>
            </button>
        </td>
    `;
    return tr;
}

async function searchAdmin() {
    const q = document.getElementById('adminSearchInput').value.trim();
    const tbody = document.getElementById('adminUsersTableBody');
    if (!tbody) return;

    if (!q) {
        loadAdmins();
        return;
    }

    tbody.innerHTML = '<tr><td colspan="6" class="text-center">Поиск...</td></tr>';

    try {
        const response = await axios.get(`${API_BASE}/admins/search`, {
            params: { username: q }
        });

        const result = response.data;

        tbody.innerHTML = '';

        // backend может вернуть один объект или массив
        if (!result) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center">Ничего не найдено</td></tr>';
            return;
        }

        if (Array.isArray(result)) {
            if (result.length === 0) {
                tbody.innerHTML = '<tr><td colspan="6" class="text-center">Ничего не найдено</td></tr>';
                return;
            }
            result.forEach(admin => insertOrUpdateAdminRow(admin));
        } else {
            insertOrUpdateAdminRow(result);
        }

        showToast('Результаты поиска загружены', 'info');
    } catch (error) {
        console.error('Ошибка поиска админа:', error);
        tbody.innerHTML = '<tr><td colspan="6" class="text-center text-danger">Ошибка при поиске</td></tr>';
        showToast('Ошибка при поиске', 'danger');
    }
}


async function deleteAdmin(id) {
    if (!confirm('Удалить администратора? Это действие нельзя отменить.')) return;

    try {
        await axios.delete(`${API_BASE}/admins/${id}`);

        // Удаляем строку из DOM с анимацией (плавное исчезание)
        const tr = document.querySelector(`tr[data-admin-id="${id}"]`);
        if (tr) {
            tr.style.transition = 'opacity 250ms';
            tr.style.opacity = '0';
            setTimeout(() => tr.remove(), 260);
        } else {
            // если не нашли строку — просто перезагрузим таблицу
            loadAdmins();
        }

        showToast('Администратор удалён', 'success');
    } catch (error) {
        console.error('Ошибка удаления администратора:', error);
        showToast('Не удалось удалить администратора', 'danger');
    }
}


function insertOrUpdateAdminRow(admin) {
    const tbody = document.getElementById('adminUsersTableBody');
    if (!tbody) return;

    // Удаляем строку "Загрузка..." или "Нет администраторов"
    const emptyRow = tbody.querySelector('tr td[colspan="6"]');
    if (emptyRow) {
        emptyRow.parentElement.remove();
    }

    // Ищем существующую строку
    const existingRow = tbody.querySelector(`tr[data-admin-id="${admin.id}"]`);

    const newRow = createAdminRowElement(admin);

    if (existingRow) {
        // Заменяем существующую строку
        existingRow.replaceWith(newRow);
    } else {
        // Добавляем новую строку в начало
        tbody.prepend(newRow);
    }
}

async function updateAdmin(adminId) {
    const form = document.getElementById('editAdminForm');
    const errorBlock = document.getElementById('editAdminError');

    // Получаем значения напрямую
    const payload = {
        username: form.querySelector('[name="username"]').value,
        role: form.querySelector('[name="role"]').value,
        isActive: form.querySelector('[name="isActive"]').checked
    };

    // Если пароль введен - добавляем его, если нет - не отправляем
    const passwordInput = form.querySelector('#editAdminPasswordInput');
    if (passwordInput && passwordInput.value.trim()) {
        payload.password = passwordInput.value;
    }

    console.log('Обновление администратора', adminId, payload);

    try {
        const response = await axios.put(`${API_BASE}/admins/${adminId}`, payload);
        const updatedAdmin = response.data;

        // Обновляем строку в таблице
        insertOrUpdateAdminRow(updatedAdmin);

        // Закрываем модалку
        const modalEl = document.getElementById('editAdminModal');
        bootstrap.Modal.getInstance(modalEl).hide();

        showToast('Администратор обновлён', 'success');
    } catch (error) {
        console.error('Ошибка обновления администратора:', error);
        errorBlock.textContent = error.response?.data?.message || 'Ошибка при обновлении';
        errorBlock.classList.remove('d-none');
        showToast('Ошибка при обновлении администратора', 'danger');
    }
}


function showToast(message, type = 'info', timeout = 3000) {
    // type: 'success'|'danger'|'info'|'warning'
    const id = `toast-${Date.now()}`;
    const bgClass = (type === 'success') ? 'bg-success text-white' :
        (type === 'danger') ? 'bg-danger text-white' :
            (type === 'warning') ? 'bg-warning text-dark' : 'bg-secondary text-white';

    const toastHtml = `
      <div id="${id}" class="toast align-items-center ${bgClass}" role="alert" aria-live="assertive" aria-atomic="true">
        <div class="d-flex">
          <div class="toast-body">
            ${message}
          </div>
          <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Закрыть"></button>
        </div>
      </div>
    `;

    const container = document.getElementById('toastContainer');
    container.insertAdjacentHTML('beforeend', toastHtml);

    const toastEl = document.getElementById(id);
    const bsToast = new bootstrap.Toast(toastEl, { delay: timeout });
    bsToast.show();

    // автоматически удалить DOM-элемент после скрытия
    toastEl.addEventListener('hidden.bs.toast', () => toastEl.remove());
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