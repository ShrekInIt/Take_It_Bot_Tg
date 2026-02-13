/* global axios, bootstrap, Toast */

class ToastManager {
    static showToast(message, type = 'info', timeout = 3000) {
        try {
            const container = document.getElementById('toastContainer');
            if (!container) {
                console.warn('toastContainer not found:', message);
                return;
            }

            const id = `toast-${Date.now()}`;
            const bgClass =
                type === 'success' ? 'bg-success text-white' :
                    type === 'danger'  ? 'bg-danger text-white'  :
                        type === 'warning' ? 'bg-warning text-dark'  : 'bg-secondary text-white';

            const toastHtml = `
        <div id="${id}" class="toast align-items-center ${bgClass}" role="alert" aria-live="assertive" aria-atomic="true">
          <div class="d-flex">
            <div class="toast-body">${message}</div>
            <button type="button" class="btn-close btn-close-white me-2 m-auto" data-bs-dismiss="toast" aria-label="Закрыть"></button>
          </div>
        </div>
      `;

            container.insertAdjacentHTML('beforeend', toastHtml);

            const toastEl = document.getElementById(id);

            // Bootstrap 5: Toast лежит в bootstrap.Toast
            if (!window.bootstrap?.Toast) {
                console.warn('bootstrap.Toast is not available:', message);
                toastEl?.remove();
                return;
            }

            const bsToast = new bootstrap.Toast(toastEl, { delay: timeout });
            bsToast.show();

            toastEl.addEventListener('hidden.bs.toast', () => toastEl.remove(), { once: true });
        } catch (e) {
            console.warn('ToastManager failed:', e);
        }
    }
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

function showDashboard() {
    console.log('Показываем дашборд');
    new NavigationManager().loadContent('/admin/dashboard')
        .catch(() => ToastManager.showToast('Ошибка загрузки', 'danger'));
    NavigationManager.setActiveNav('dashboard');
}

function showUsers() {
    console.log('Показываем пользователей');
    new NavigationManager().loadContent('/admin/users')
        .catch(() => ToastManager.showToast('Ошибка загрузки', 'danger'));
    NavigationManager.setActiveNav('users');
}

function showCategories() {
    console.log('Показываем категории');
    new NavigationManager().loadContent('/admin/categories')
        .catch(() => ToastManager.showToast('Ошибка загрузки', 'danger'));
    NavigationManager.setActiveNav('categories');
}

function showProducts() {
    console.log('Показываем продукты');
    new NavigationManager().loadContent('/admin/products')
        .catch(() => ToastManager.showToast('Ошибка загрузки', 'danger'));
    NavigationManager.setActiveNav('products');
}

function showOrders() {
    console.log('Показываем заказы');
    new NavigationManager().loadContent('/admin/orders')
        .catch(() => ToastManager.showToast('Ошибка загрузки', 'danger'));
    NavigationManager.setActiveNav('orders');
}

function showAdmins() {
    console.log('Показываем администраторов');
    new NavigationManager().loadContent('/admin/admins')
        .catch(() => ToastManager.showToast('Ошибка загрузки', 'danger'));
    NavigationManager.setActiveNav('admins');
}

document.addEventListener('DOMContentLoaded', function() {
    console.log('DOM загружен, вызываем checkAuth');
    new AuthManager().checkAuth()
        .catch(() => ToastManager.showToast('Ошибка авторизации', 'danger'));
});