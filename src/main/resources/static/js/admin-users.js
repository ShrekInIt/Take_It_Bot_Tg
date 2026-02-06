/* global axios, bootstrap */
/**
 * @typedef {{ hide(): void, show(): void }} BsModalInstance
 * @typedef {{ getInstance(el: Element): (BsModalInstance|null), new(el: Element): BsModalInstance }} BsModalCtor
 * @typedef {{ Modal: BsModalCtor }} BootstrapNs
 */

/** @type {BootstrapNs} */

class UserManager {
    static API_BASE = `${new AuthManager().API_BASE}/users`;
    static _inited = false;

    static init() {
        if (this._inited) return;
        this._inited = true;

        document.addEventListener('click', (e) => {
            const edit = e.target.closest('.js-edit-user');
            if (edit) {
                const id = Number(edit.dataset.id);
                if (id) void UserManager.showUserModalById(id, 'edit');
                return;
            }

            const del = e.target.closest('.js-delete-user');
            if (del) {
                const id = Number(del.dataset.id);
                if (id) void UserManager.deleteUser(id);
                return;
            }

            const createOpen =
                e.target.closest('#openCreateUserModal') ||
                e.target.closest('[data-action="open-create-user"]');

            if (createOpen) {
                void UserManager.showUserModal({}, 'create');
                return;
            }

            const searchBtn =
                e.target.closest('#searchUserBtn') ||
                e.target.closest('[data-action="search-users"]');

            if (searchBtn) {
                void UserManager.searchUsers();
                return;
            }

            const resetBtn =
                e.target.closest('#resetUserBtn') ||
                e.target.closest('[data-action="reset-users"]');

            if (resetBtn) {
                void UserManager.loadUsers();
            }
        });

        document.addEventListener('click', (e) => {

            const searchBtn =
                e.target.closest('#searchUserBtn') ||
                e.target.closest('[data-action="search-users"]');

            if (searchBtn) {
                void UserManager.searchUsers();
                return;
            }

            const resetBtn =
                e.target.closest('#resetUserBtn') ||
                e.target.closest('[data-action="reset-users"]');

            if (resetBtn) {
                void UserManager.loadUsers();
            }

        });


        const searchInput = document.getElementById('userSearchInput');
        if (searchInput) {
            searchInput.addEventListener('keydown', (e) => {
                if (e.key === 'Enter') {
                    e.preventDefault();
                    void UserManager.searchUsers();
                }
            });
        }
    }

    static async loadUsers() {
        const tbody = document.getElementById('usersTableBody');
        if (!tbody) return;

        try {
            const res = await axios.get(this.API_BASE);
            const users = res.data;

            tbody.innerHTML = '';

            if (!users || users.length === 0) {
                tbody.innerHTML =
                    '<tr><td colspan="8" class="text-center">Нет пользователей</td></tr>';
                return;
            }

            users.forEach(u => tbody.appendChild(UserManager.createUserRow(u)));
        } catch (e) {
            console.error('Ошибка загрузки пользователей', e);
            tbody.innerHTML =
                '<tr><td colspan="8" class="text-center text-danger">Ошибка загрузки</td></tr>';
            ToastManager.showToast('Ошибка загрузки пользователей', 'danger');
        }
    }

    /**
     * @typedef {{id:number, name?:string, telegramId?:string, phoneNumber?:string,
     *  isActive?:boolean, isAdmin?:boolean, createdAt?:string}} UserDto
     */

    /** @param {UserDto} user */
    static createUserRow(user) {
        const tr = document.createElement('tr');
        tr.dataset.userId = `${user.id}`;

        tr.innerHTML = `
            <td>${user.id}</td>
            <td>${UserManager.escapeHtml(user.name ?? '—')}</td>
            <td>${UserManager.escapeHtml(user.telegramId ?? '—')}</td>
            <td>${UserManager.escapeHtml(user.phoneNumber ?? '—')}</td>
            <td>
                <span class="badge ${user.isActive ? 'bg-success' : 'bg-danger'}">
                    ${user.isActive ? 'Активен' : 'Неактивен'}
                </span>
            </td>
            <td>
                <span class="badge ${user.isAdmin ? 'bg-primary' : 'bg-secondary'}">
                    ${user.isAdmin ? 'Админ' : 'Пользователь'}
                </span>
            </td>
            <td>${UserManager.formatDate(user.createdAt)}</td>
            <td>
                <button class="btn btn-sm btn-outline-primary js-edit-user" data-id="${user.id}">
                    <i class="bi bi-pencil"></i>
                </button>
                <button class="btn btn-sm btn-outline-danger ms-1 js-delete-user" data-id="${user.id}">
                    <i class="bi bi-trash"></i>
                </button>
            </td>
        `;
        return tr;
    }

    static updateUserRow(user) {
        const tbody = document.getElementById('usersTableBody');
        if (!tbody) return;

        const existing = tbody.querySelector(`tr[data-user-id="${user.id}"]`);
        const row = UserManager.createUserRow(user);

        existing ? existing.replaceWith(row) : tbody.prepend(row);
    }

    static async searchUsers() {
        const input = document.getElementById('userSearchInput');
        const tbody = document.getElementById('usersTableBody');
        if (!input || !tbody) return;

        const q = input.value.trim();
        if (!q) return UserManager.loadUsers();

        tbody.innerHTML =
            '<tr><td colspan="8" class="text-center">Поиск...</td></tr>';

        try {
            const res = await axios.get(`${this.API_BASE}/search`, {
                params: { name: q }
            });

            const result = res.data;
            tbody.innerHTML = '';

            if (!result || result.length === 0) {
                tbody.innerHTML =
                    '<tr><td colspan="8" class="text-center">Ничего не найдено</td></tr>';
                return;
            }

            result.forEach(u => tbody.appendChild(UserManager.createUserRow(u)));
            ToastManager.showToast(`Найдено: ${result.length}`, 'info');
        } catch (e) {
            console.error('Ошибка поиска', e);
            tbody.innerHTML =
                '<tr><td colspan="8" class="text-center text-danger">Ошибка поиска</td></tr>';
            ToastManager.showToast('Ошибка поиска пользователей', 'danger');
        }
    }

    /**
     * @typedef {{id:number, name?:string, telegramId?:string, phoneNumber?:string,
     *  isActive?:boolean, isAdmin?:boolean, createdAt?:string}} UserDto
     */

    /** @param {{}} user
     * @param mode
     */
    static async showUserModal(user = {}, mode = 'view') {
        const old = document.getElementById('userModal');
        if (old) {
            const inst = bootstrap.Modal.getInstance(old);
            inst?.hide();
            old.remove();
        }

        if (mode === 'create') {
            user = {
                name: '',
                telegramId: '',
                phoneNumber: '',
                isActive: true,
                isAdmin: false
            };
        }

        const modalHtml = `
        <div class="modal fade" id="userModal" tabindex="-1">
            <div class="modal-dialog">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title">
                            ${mode === 'create' ? 'Создать пользователя' :
            mode === 'edit' ? 'Редактировать пользователя' :
                'Просмотр пользователя'}
                        </h5>
                        <button class="btn-close" data-bs-dismiss="modal"></button>
                    </div>
                    <div class="modal-body">
                        <div id="userError" class="alert alert-danger d-none"></div>

                        ${UserManager.input('Имя', 'userName', user.name, mode)}
                        ${UserManager.input('Telegram ID', 'userTelegramId', user.telegramId, mode)}
                        ${UserManager.input('Телефон', 'userPhoneNumber', user.phoneNumber, mode)}

                        ${UserManager.checkbox('Активен', 'userIsActive', user.isActive, mode)}
                        ${UserManager.checkbox('Администратор', 'userIsAdmin', user.isAdmin, mode)}

                        ${mode === 'view' && user.createdAt
            ? `<p class="text-muted">Создан: ${UserManager.formatDate(user.createdAt)}</p>`
            : ''}
                    </div>
                    <div class="modal-footer">
                        <button class="btn btn-secondary" data-bs-dismiss="modal">Закрыть</button>
                        ${mode === 'create' ? `<button id="userCreateBtn" class="btn btn-primary">Создать</button>` : ''}
                        ${mode === 'edit' ? `<button id="userSaveBtn" class="btn btn-primary">Сохранить</button>` : ''}
                    </div>
                </div>
            </div>
        </div>`;

        document.body.insertAdjacentHTML('beforeend', modalHtml);
        const modalEl = document.getElementById('userModal');
        const modal = new bootstrap.Modal(modalEl);

        if (mode === 'create') {
            document.getElementById('userCreateBtn').onclick = () => UserManager.createUser();
        }
        if (mode === 'edit') {
            document.getElementById('userSaveBtn').onclick = () => UserManager.updateUser(user.id);
        }

        modal.show();
    }

    static async createUser() {
        const data = UserManager.collectForm();
        if (!data) return;

        try {
            const res = await axios.post(this.API_BASE, data);
            bootstrap.Modal.getInstance(document.getElementById('userModal'))?.hide();
            UserManager.updateUserRow(res.data);
            ToastManager.showToast('Пользователь создан', 'success');
        } catch (e) {
            UserManager.showError(e);
        }
    }

    static async updateUser(id) {
        const data = UserManager.collectForm();
        if (!data) return;

        try {
            const res = await axios.put(`${this.API_BASE}/${id}`, data);
            bootstrap.Modal.getInstance(document.getElementById('userModal'))?.hide();
            UserManager.updateUserRow(res.data);
            ToastManager.showToast('Пользователь обновлён', 'success');
        } catch (e) {
            UserManager.showError(e);
        }
    }

    static async deleteUser(id) {
        if (!confirm('Удалить пользователя?')) return;
        try {
            await axios.delete(`${this.API_BASE}/${id}`);
            document.querySelector(`tr[data-user-id="${id}"]`)?.remove();
            ToastManager.showToast('Пользователь удалён', 'success');
        } catch {
            ToastManager.showToast('Ошибка удаления', 'danger');
        }
    }

    static async showUserModalById(id, mode) {
        try {
            const res = await axios.get(`${this.API_BASE}/${id}`);
            void UserManager.showUserModal(res.data, mode);
        } catch {
            ToastManager.showToast('Не удалось загрузить пользователя', 'danger');
        }
    }

    static collectForm() {
        const err = document.getElementById('userError');
        err.classList.add('d-none');

        const name = document.getElementById('userName')?.value?.trim();
        if (!name) {
            err.textContent = 'Введите имя';
            err.classList.remove('d-none');
            return null;
        }

        return {
            name,
            telegramId: document.getElementById('userTelegramId')?.value?.trim() || null,
            phoneNumber: document.getElementById('userPhoneNumber')?.value?.trim() || null,
            isActive: !!document.getElementById('userIsActive')?.checked,
            isAdmin: !!document.getElementById('userIsAdmin')?.checked
        };
    }

    static showError(e) {
        const err = document.getElementById('userError');
        err.textContent = e?.response?.data?.message || 'Ошибка';
        err.classList.remove('d-none');
    }

    static formatDate(value) {
        if (!value) return '—';
        const normalized = typeof value === 'string'
            ? value.replace(/\.\d+$/, '')
            : value;
        const d = new Date(normalized);
        return isNaN(d.getTime()) ? '—' : d.toLocaleDateString();
    }

    static escapeHtml(s) {
        return String(s ?? '')
            .replaceAll('&','&amp;')
            .replaceAll('<','&lt;')
            .replaceAll('>','&gt;')
            .replaceAll('"','&quot;')
            .replaceAll("'",'&#39;');
    }

    static input(label, id, value, mode) {
        return `
            <div class="mb-3">
                <label class="form-label">${label}</label>
                ${mode === 'view'
            ? `<p>${UserManager.escapeHtml(value || '—')}</p>`
            : `<input id="${id}" class="form-control" value="${UserManager.escapeHtml(value || '')}">`}
            </div>`;
    }

    static checkbox(label, id, value, mode) {
        return `
            <div class="mb-3 form-check">
                ${mode === 'view'
            ? `<p>${label}: ${value ? 'Да' : 'Нет'}</p>`
            : `<input id="${id}" type="checkbox" class="form-check-input" ${value ? 'checked' : ''}>
                       <label class="form-check-label ms-2">${label}</label>`}
            </div>`;
    }
}

document.addEventListener('DOMContentLoaded', () => {
    UserManager.init();
    const tbody = document.getElementById('usersTableBody');
    if (tbody) void UserManager.loadUsers();
});
