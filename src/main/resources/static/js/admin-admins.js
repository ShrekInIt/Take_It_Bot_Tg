class AdminManager {
    static async loadAdmins() {
        console.log('Загрузка администраторов...');
        const tbody = document.getElementById('adminUsersTableBody');

        if (!tbody) return;

        tbody.innerHTML = '<tr><td colspan="6" class="text-center">Загрузка...</td></tr>';

        try {
            const response = await axios.get(`${new AuthManager().API_BASE}/admins`);
            const admins = response.data;

            tbody.innerHTML = '';

            if (!admins || admins.length === 0) {
                tbody.innerHTML =
                    '<tr><td colspan="6" class="text-center">Администраторы не найдены</td></tr>';
                return;
            }

            admins.forEach(admin => {
                const tr = AdminManager.createAdminRowElement(admin);
                tbody.appendChild(tr);
            });

        } catch (error) {
            console.error('Ошибка загрузки администраторов:', error);
            tbody.innerHTML =
                '<tr><td colspan="6" class="text-center text-danger">Ошибка загрузки</td></tr>';
        }
    }

    static createAdminRowElement(admin) {
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

    static async showCreateAdminUserModal() {
        const oldModal = document.getElementById('createAdminUserModal');
        if (oldModal) {
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

    static async createAdminUser() {
        const form = document.getElementById('createAdminUserForm');
        const errorBlock = document.getElementById('createAdminError');

        const payload = {
            username: form.querySelector('[name="username"]').value,
            password: form.querySelector('#adminPasswordInput').value,
            role: form.querySelector('[name="role"]').value,
            isActive: form.querySelector('[name="isActive"]').checked
        };

        console.log('Отправляемые данные:', payload);
        console.log('Пароль пустой?', !payload.password);

        try {
            const response = await axios.post(`${new AuthManager().API_BASE}/admins`, payload);
            const created = response.data;

            const modalEl = document.getElementById('createAdminUserModal');
            bootstrap.Modal.getInstance(modalEl).hide();

            if (created && created.id) {
                AdminManager.insertOrUpdateAdminRow(created);
                ToastManager.showToast('Администратор создан', 'success');
            } else {
                AdminManager.loadAdmins();
                ToastManager.showToast('Администратор создан', 'success');
            }
        } catch (error) {
            console.error('Ошибка создания администратора:', error);

            if (error.response) {
                console.error('Ответ сервера:', error.response.data);
                console.error('Статус:', error.response.status);
            }

            errorBlock.textContent = error.response?.data?.message ||
                error.response?.data?.error ||
                'Ошибка при создании администратора';
            errorBlock.classList.remove('d-none');

            ToastManager.showToast('Ошибка при создании администратора', 'danger');
        }
    }

    static async showEditAdminModal(adminId) {
        let admin;
        try {
            const response = await axios.get(`${new AuthManager().API_BASE}/admins/${adminId}`);
            admin = response.data;
        } catch (error) {
            console.error('Не удалось получить данные админа', error);
            ToastManager.showToast('Ошибка загрузки данных администратора', 'danger');
            return;
        }

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

    static async updateAdmin(adminId) {
        const form = document.getElementById('editAdminForm');
        const errorBlock = document.getElementById('editAdminError');

        const payload = {
            username: form.querySelector('[name="username"]').value,
            role: form.querySelector('[name="role"]').value,
            isActive: form.querySelector('[name="isActive"]').checked
        };

        const passwordInput = form.querySelector('#editAdminPasswordInput');
        if (passwordInput && passwordInput.value.trim()) {
            payload.password = passwordInput.value;
        }

        console.log('Обновление администратора', adminId, payload);

        try {
            const response = await axios.put(`${new AuthManager().API_BASE}/admins/${adminId}`, payload);
            const updatedAdmin = response.data;

            AdminManager.insertOrUpdateAdminRow(updatedAdmin);

            const modalEl = document.getElementById('editAdminModal');
            bootstrap.Modal.getInstance(modalEl).hide();

            ToastManager.showToast('Администратор обновлён', 'success');
        } catch (error) {
            console.error('Ошибка обновления администратора:', error);
            errorBlock.textContent = error.response?.data?.message || 'Ошибка при обновлении';
            errorBlock.classList.remove('d-none');
            ToastManager.showToast('Ошибка при обновлении администратора', 'danger');
        }
    }

    static async searchAdmin() {
        const q = document.getElementById('adminSearchInput').value.trim();
        const tbody = document.getElementById('adminUsersTableBody');
        if (!tbody) return;

        if (!q) {
            AdminManager.loadAdmins();
            return;
        }

        tbody.innerHTML = '<tr><td colspan="6" class="text-center">Поиск...</td></tr>';

        try {
            const response = await axios.get(`${new AuthManager().API_BASE}/admins/search`, {
                params: { username: q }
            });

            const result = response.data;

            tbody.innerHTML = '';

            if (!result) {
                tbody.innerHTML = '<tr><td colspan="6" class="text-center">Ничего не найдено</td></tr>';
                return;
            }

            if (Array.isArray(result)) {
                if (result.length === 0) {
                    tbody.innerHTML = '<tr><td colspan="6" class="text-center">Ничего не найдено</td></tr>';
                    return;
                }
                result.forEach(admin => AdminManager.insertOrUpdateAdminRow(admin));
            } else {
                AdminManager.insertOrUpdateAdminRow(result);
            }

            ToastManager.showToast('Результаты поиска загружены', 'info');
        } catch (error) {
            console.error('Ошибка поиска админа:', error);
            tbody.innerHTML = '<tr><td colspan="6" class="text-center text-danger">Ошибка при поиске</td></tr>';
            ToastManager.showToast('Ошибка при поиске', 'danger');
        }
    }

    static async deleteAdmin(id) {
        if (!confirm('Удалить администратора? Это действие нельзя отменить.')) return;

        try {
            await axios.delete(`${new AuthManager().API_BASE}/admins/${id}`);

            const tr = document.querySelector(`tr[data-admin-id="${id}"]`);
            if (tr) {
                tr.style.transition = 'opacity 250ms';
                tr.style.opacity = '0';
                setTimeout(() => tr.remove(), 260);
            } else {
                AdminManager.loadAdmins();
            }

            ToastManager.showToast('Администратор удалён', 'success');
        } catch (error) {
            console.error('Ошибка удаления администратора:', error);
            ToastManager.showToast('Не удалось удалить администратора', 'danger');
        }
    }

    static insertOrUpdateAdminRow(admin) {
        const tbody = document.getElementById('adminUsersTableBody');
        if (!tbody) return;

        const emptyRow = tbody.querySelector('tr td[colspan="6"]');
        if (emptyRow) {
            emptyRow.parentElement.remove();
        }

        const existingRow = tbody.querySelector(`tr[data-admin-id="${admin.id}"]`);

        const newRow = AdminManager.createAdminRowElement(admin);

        if (existingRow) {
            existingRow.replaceWith(newRow);
        } else {
            tbody.prepend(newRow);
        }
    }
}

// Глобальные функции для совместимости с HTML
function loadAdmins() {
    AdminManager.loadAdmins();
}

function showCreateAdminUserModal() {
    AdminManager.showCreateAdminUserModal();
}

function searchAdmin() {
    AdminManager.searchAdmin();
}

function showEditAdminModal(id) {
    AdminManager.showEditAdminModal(id);
}

function deleteAdmin(id) {
    AdminManager.deleteAdmin(id);
}

function createAdminUser() {
    AdminManager.createAdminUser();
}

function updateAdmin(id) {
    AdminManager.updateAdmin(id);
}