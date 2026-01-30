class DashboardManager {
    // Базовый URL для всех запросов дашборда
    static API_BASE = `${new AuthManager().API_BASE}/dashboard`;

    // Флаг, чтобы инициализация выполнялась один раз
    static _inited = false;

    // Инициализация дашборда
    static async init() {
        if (this._inited) return;
        this._inited = true;

        await this.loadStats();
        await this.loadRecentOrders();
        await this.loadRecentUsers();
    }

    // Загрузка статистики
    static async loadStats() {
        try {
            const response = await axios.get(`${this.API_BASE}/stats`);
            const stats = response.data;

            document.getElementById('totalUsers').textContent = stats.totalUsers || 0;
            document.getElementById('newUsersToday').textContent = `+${stats.newUsersToday || 0} сегодня`;
            document.getElementById('totalOrders').textContent = stats.activeOrders || 0;
            document.getElementById('totalProducts').textContent = stats.totalProducts || 0;
            document.getElementById('todayRevenue').textContent = (stats.todayRevenue || 0) + ' ₽';

            document.getElementById('ordersToday').innerText = stats.ordersToday;
        } catch (error) {
            console.error('Ошибка загрузки статистики:', error);
        }
    }

    // Загрузка последних заказов
    static async loadRecentOrders() {
        try {
            const response = await axios.get(`${this.API_BASE}/orders/recent`);
            const orders = response.data;
            const ordersBody = document.getElementById('recentOrdersBody');

            ordersBody.innerHTML = '';
            if (orders && orders.length > 0) {
                orders.forEach(order => {
                    const row = document.createElement('tr');
                    row.innerHTML = `
                        <td>#${order.id}</td>
                        <td>${order.userName || 'Неизвестно'}</td>
                        <td>${order.totalAmount || 0} ₽</td>
                        <td>
                            <span class="badge bg-${DashboardManager.getStatusColor(order.status)}">
                                ${DashboardManager.getStatusText(order.status)}
                            </span>
                        </td>
                    `;
                    ordersBody.appendChild(row);
                });
            } else {
                ordersBody.innerHTML = `
                    <tr>
                        <td colspan="4" class="text-center text-muted">Нет заказов</td>
                    </tr>
                `;
            }
        } catch (error) {
            console.error('Ошибка загрузки последних заказов:', error);
        }
    }

    // Загрузка последних пользователей
    static async loadRecentUsers() {
        try {
            const response = await axios.get(`${this.API_BASE}/users/recent`);
            const users = response.data;
            const usersBody = document.getElementById('recentUsersBody');

            usersBody.innerHTML = '';
            if (users && users.length > 0) {
                users.forEach(user => {
                    const row = document.createElement('tr');
                    row.innerHTML = `
                        <td>${user.id}</td>
                        <td>${user.name || 'Без имени'}</td>
                        <td>${user.telegramId ? `@${user.telegramId}` : '-'}</td>
                        <td>${new Date(user.createdAt).toLocaleDateString()}</td>
                    `;
                    usersBody.appendChild(row);
                });
            } else {
                usersBody.innerHTML = `
                    <tr>
                        <td colspan="4" class="text-center text-muted">Нет пользователей</td>
                    </tr>
                `;
            }
        } catch (error) {
            console.error('Ошибка загрузки новых пользователей:', error);
        }
    }

    // Получение цвета статуса заказа
    static getStatusColor(status) {
        if (!status) return 'secondary';
        switch(status.toLowerCase()) {
            case 'pending': return 'warning';
            case 'confirmed': return 'primary';
            case 'preparing': return 'info';
            case 'ready': return 'success';
            case 'completed': return 'success';
            case 'cancelled': return 'danger';
            default: return 'secondary';
        }
    }

    // Текст статуса заказа
    static getStatusText(status) {
        if (!status) return 'Неизвестно';
        switch(status.toLowerCase()) {
            case 'pending': return 'Ожидает';
            case 'confirmed': return 'Подтвержден';
            case 'preparing': return 'Готовится';
            case 'ready': return 'Готов';
            case 'completed': return 'Завершен';
            case 'cancelled': return 'Отменен';
            default: return status;
        }
    }
}

// Автоинициализация при загрузке страницы
document.addEventListener('DOMContentLoaded', () => {
    DashboardManager.init();
});
