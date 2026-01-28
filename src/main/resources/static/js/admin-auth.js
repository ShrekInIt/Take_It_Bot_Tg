class AuthManager {
    constructor() {
        this.currentUser = null;
        this.API_BASE = '/api/admin';
    }

    async checkAuth() {
        console.log('checkAuth вызван');
        try {
            const response = await axios.get(`${this.API_BASE}/auth/check`);
            console.log('Ответ от /auth/check:', response.data);

            if (response.data.authenticated) {
                this.currentUser = response.data.user;
                document.getElementById('currentUser').textContent = this.currentUser.username;
                console.log('Пользователь авторизован:', this.currentUser.username);

                NavigationManager.redirectBasedOnPath();
            } else {
                console.log('Пользователь не авторизован');
                this.showLoginMessage();
            }
        } catch (error) {
            console.error('Ошибка проверки авторизации:', error);
            this.showLoginMessage();
        }
    }

    showLoginMessage() {
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

    logout() {
        if (confirm('Вы уверены, что хотите выйти?')) {
            window.location.href = '/admin/logout';
        }
    }
}