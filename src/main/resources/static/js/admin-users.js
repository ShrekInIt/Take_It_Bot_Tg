class UserManager {
    static async loadUsers() {
        try {
            console.log('Загрузка пользователей...');
            const response = await axios.get(`${new AuthManager().API_BASE}/users`);
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
}