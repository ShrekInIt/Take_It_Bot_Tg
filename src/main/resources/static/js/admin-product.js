class ProductManager {
    static async loadProducts() {
        console.log('Загрузка продуктов...');
        try {
            const response = await axios.get(`${new AuthManager().API_BASE}/products`);
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
}