class DashboardManager {
    static async loadStats() {
        try {
            console.log('Загрузка статистики...');
            const response = await axios.get(`${new AuthManager().API_BASE}/stats`);
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
}