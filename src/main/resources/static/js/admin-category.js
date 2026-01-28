class CategoryManager {
    static async loadCategories() {
        try {
            console.log('Загрузка категорий...');
            const response = await axios.get(`${new AuthManager().API_BASE}/categories`);
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
}