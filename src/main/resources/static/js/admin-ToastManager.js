class ToastManager {
    static showToast(message, type = 'info', timeout = 3000) {
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

        toastEl.addEventListener('hidden.bs.toast', () => toastEl.remove());
    }
}

// Глобальные функции для совместимости с HTML
function togglePasswordVisibility(inputId) {
    const input = document.getElementById(inputId);
    if (!input) return;

    if (input.type === 'password') {
        input.type = 'text';
    } else {
        input.type = 'password';
    }
}

// Навигационные функции
function showDashboard() {
    console.log('Показываем дашборд');
    new NavigationManager().loadContent('/admin/dashboard');
    NavigationManager.setActiveNav('dashboard');
}

function showUsers() {
    console.log('Показываем пользователей');
    new NavigationManager().loadContent('/admin/users');
    NavigationManager.setActiveNav('users');
}

function showCategories() {
    console.log('Показываем категории');
    new NavigationManager().loadContent('/admin/categories');
    NavigationManager.setActiveNav('categories');
}

function showProducts() {
    console.log('Показываем продукты');
    new NavigationManager().loadContent('/admin/products');
    NavigationManager.setActiveNav('products');
}

function showOrders() {
    console.log('Показываем заказы');
    new NavigationManager().loadContent('/admin/orders');
    NavigationManager.setActiveNav('orders');
}

function showAddons() {
    console.log('Показываем добавки');
    new NavigationManager().loadContent('/admin/addons');
    NavigationManager.setActiveNav('addons');
}

function showAdmins() {
    console.log('Показываем администраторов');
    new NavigationManager().loadContent('/admin/admins');
    NavigationManager.setActiveNav('admins');
}

// Инициализация при загрузке страницы
document.addEventListener('DOMContentLoaded', function() {
    console.log('DOM загружен, вызываем checkAuth');
    new AuthManager().checkAuth();
});