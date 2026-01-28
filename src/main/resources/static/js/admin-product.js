class ProductManager {
    static API_BASE() { return `${new AuthManager().API_BASE}/products`; }

    // внутренний флаг, чтобы навесить делегированые обработчики только один раз
    static _inited = false;

    // ----------------- Init (вешаем делегирование один раз) -----------------
    static init() {
        if (this._inited) return;
        this._inited = true;

        // Делегирование кликов по таблице продуктов
        document.addEventListener('click', (e) => {
            const editBtn = e.target.closest('.js-edit-product');
            if (editBtn) {
                const id = editBtn.dataset.id;
                if (id) ProductManager.showProductModalById(Number(id), 'edit');
                return;
            }

            const deleteBtn = e.target.closest('.js-delete-product');
            if (deleteBtn) {
                const id = deleteBtn.dataset.id;
                if (id) ProductManager.deleteProduct(Number(id));
                return;
            }

            // кнопка создания (если у тебя есть кнопка с id openCreateProductModal)
            const createOpen = e.target.closest('#openCreateProductModal') || e.target.closest('[data-action="open-create-product"]');
            if (createOpen) {
                ProductManager.showProductModal({}, 'create');
                return;
            }

            // кнопка поиска (если есть id searchProductBtn)
            const searchBtn = e.target.closest('#searchProductBtn') || e.target.closest('[data-action="search-products"]');
            if (searchBtn) {
                ProductManager.searchProducts();
                return;
            }

            // кнопка ресета
            const resetBtn = e.target.closest('#resetProductBtn') || e.target.closest('[data-action="reset-products"]');
            if (resetBtn) {
                ProductManager.loadProducts();

            }
        });

        // если таблица уже присутствует, можно навесить клавишу Enter на поле поиска
        const searchInput = document.getElementById('productSearchInput');
        if (searchInput) {
            searchInput.addEventListener('keydown', (ev) => {
                if (ev.key === 'Enter') {
                    ev.preventDefault();
                    ProductManager.searchProducts();
                }
            });
        }
    }

    // ----------------- Load / render -----------------
    static async loadProducts() {
        try {
            const response = await axios.get(this.API_BASE());
            const products = response.data;
            const tbody = document.getElementById('productsTableBody');
            if (!tbody) return;
            tbody.innerHTML = '';
            if (!products || products.length === 0) {
                tbody.innerHTML = '<tr><td colspan="6" class="text-center">Нет продуктов</td></tr>';
                return;
            }
            products.forEach(product => tbody.appendChild(ProductManager.createProductRowElement(product)));
        } catch (e) {
            console.error('Ошибка загрузки продуктов:', e);
            const tbody = document.getElementById('productsTableBody');
            if (tbody) tbody.innerHTML = '<tr><td colspan="6" class="text-center text-danger">Ошибка загрузки</td></tr>';
            ToastManager.showToast('Ошибка загрузки продуктов', 'danger');
        }
    }

    static createProductRowElement(product) {
        const tr = document.createElement('tr');
        tr.setAttribute('data-product-id', product.id);

        // берём categoryName или вложенный объект category.name — на бэке может быть по-разному
        const categoryDisplay = product.categoryName ?? (product.category?.name) ?? '—';
        const amount = product.amount != null ? product.amount : (product.price != null ? product.price : 0);

        tr.innerHTML = `
      <td>${product.id}</td>
      <td>${ProductManager.escapeHtml(product.name ?? 'Без названия')}</td>
      <td>${amount} ₽</td>
      <td>${ProductManager.escapeHtml(categoryDisplay)}</td>
      <td>
        <span class="badge ${product.available ? 'bg-success' : 'bg-danger'}">
          ${product.available ? 'В наличии' : 'Нет в наличии'}
        </span>
      </td>
      <td>
        <button class="btn btn-sm btn-outline-primary js-edit-product" data-id="${product.id}" title="Редактировать">
          <i class="bi bi-pencil"></i>
        </button>
        <button class="btn btn-sm btn-outline-danger ms-1 js-delete-product" data-id="${product.id}" title="Удалить">
          <i class="bi bi-trash"></i>
        </button>
      </td>
    `;
        return tr;
    }

    static updateProductRow(product) {
        const tbody = document.getElementById('productsTableBody');
        if (!tbody) return;
        const existingRow = tbody.querySelector(`tr[data-product-id="${product.id}"]`);
        const newRow = ProductManager.createProductRowElement(product);
        if (existingRow) existingRow.replaceWith(newRow);
        else tbody.prepend(newRow);
    }

    // ----------------- Search -----------------
    static async searchProducts() {
        const searchInput = document.getElementById('productSearchInput');
        if (!searchInput) return;
        const query = searchInput.value.trim();
        const tbody = document.getElementById('productsTableBody');
        if (!tbody) return;
        if (!query) {
            await ProductManager.loadProducts();
            return;
        }
        tbody.innerHTML = '<tr><td colspan="6" class="text-center">Поиск...</td></tr>';
        try {
            const response = await axios.get(`${new AuthManager().API_BASE}/products/search`, { params: { name: query } });
            const result = response.data;
            tbody.innerHTML = '';
            if (!result || (Array.isArray(result) && result.length === 0)) {
                tbody.innerHTML = '<tr><td colspan="6" class="text-center">Ничего не найдено</td></tr>';
                return;
            }
            (Array.isArray(result) ? result : [result]).forEach(p => tbody.appendChild(ProductManager.createProductRowElement(p)));
            ToastManager.showToast(`Найдено: ${Array.isArray(result) ? result.length : 1}`, 'info');
        } catch (e) {
            console.error('Ошибка поиска продуктов:', e);
            tbody.innerHTML = '<tr><td colspan="6" class="text-center text-danger">Ошибка при поиске</td></tr>';
            ToastManager.showToast('Ошибка при поиске', 'danger');
        }
    }

    // ----------------- Modal (create / edit / view) -----------------
    // product: объект продукта или {} при create
    // mode: 'view' | 'edit' | 'create'
    static async showProductModal(product = {}, mode = 'view') {
        // Дождёмся удаления старой модалки, если есть
        const oldModal = document.getElementById('productModal');
        if (oldModal) {
            await new Promise(resolve => {
                const inst = bootstrap.Modal.getInstance(oldModal);
                if (inst) inst.hide();
                oldModal.addEventListener('hidden.bs.modal', () => {
                    try { oldModal.remove(); } catch (e) {}
                    resolve();
                }, { once: true });
                // На случай, если инстанса нет, все равно удаляем сразу
                setTimeout(() => {
                    if (!document.getElementById('productModal')) resolve();
                }, 300);
            });
        }

        // Получаем категории (если есть), но не падаем если нет
        let categories = [];
        try {
            const res = await axios.get(`${new AuthManager().API_BASE.replace('/products', '')}/categories`);
            categories = Array.isArray(res.data) ? res.data : [];
        } catch (e) {
            // просто логируем и продолжаем — категории необязательны
            console.warn('Не удалось загрузить категории:', e);
        }

        // При создании — гарантируем пустую модель
        if (mode === 'create') {
            product = {
                name: '',
                amount: 0,
                size: '',
                count: 0,
                categoryId: null,
                available: false,
                photo: '',
                description: ''
            };
        }

        const categoryOptions = categories.map(c =>
            `<option value="${c.id}" ${((product.categoryId === c.id) || (product.category?.id === c.id) || (product.categoryName === c.name)) ? 'selected' : ''}>${ProductManager.escapeHtml(c.name)}</option>`
        ).join('');

        const amountValue = (product.amount != null) ? product.amount : (product.price != null ? product.price : 0);
        const modalHtml = `
      <div class="modal fade" id="productModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog modal-lg">
          <div class="modal-content">
            <div class="modal-header">
              <h5 class="modal-title">${mode === 'create' ? 'Создать продукт' : mode === 'edit' ? 'Редактировать продукт' : 'Просмотр продукта'} ${product.id ? '#' + product.id : ''}</h5>
              <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
            </div>
            <div class="modal-body">
              <div id="productError" class="alert alert-danger d-none"></div>

              <div class="mb-3">
                <label class="form-label">Название</label>
                ${mode === 'view' ? `<p>${ProductManager.escapeHtml(product.name || '—')}</p>` : `<input id="productName" class="form-control" value="${ProductManager.escapeHtml(product.name || '')}">`}
              </div>

              <div class="row">
                <div class="col-md-4 mb-3">
                  <label class="form-label">Цена (amount, ₽)</label>
                  ${mode === 'view' ? `<p>${amountValue} ₽</p>` : `<input id="productAmount" type="number" class="form-control" value="${amountValue}">`}
                </div>

                <div class="col-md-4 mb-3">
                  <label class="form-label">Размер (size)</label>
                  ${mode === 'view' ? `<p>${ProductManager.escapeHtml(product.size || '—')}</p>` : `<input id="productSize" class="form-control" value="${ProductManager.escapeHtml(product.size || '')}">`}
                </div>

                <div class="col-md-4 mb-3">
                  <label class="form-label">Кол-во на складе (count)</label>
                  ${mode === 'view' ? `<p>${product.count ?? 0}</p>` : `<input id="productCount" type="number" class="form-control" value="${product.count ?? 0}">`}
                </div>
              </div>

              <div class="mb-3">
                <label class="form-label">Категория</label>
                ${mode === 'view' ? `<p>${ProductManager.escapeHtml(product.categoryName ?? product.category?.name ?? '—')}</p>` : `<select id="productCategory" class="form-select"><option value="">Без категории</option>${categoryOptions}</select>`}
              </div>

              <div class="mb-3 form-check">
                ${mode === 'view' ? `<p>В наличии: ${product.available ? 'Да' : 'Нет'}</p>` : `<input id="productAvailable" type="checkbox" class="form-check-input" ${product.available ? 'checked' : ''}><label class="form-check-label ms-2">В наличии</label>`}
              </div>

              <div class="mb-3">
                <label class="form-label">Фото (URL)</label>
                ${mode === 'view' ? `<p>${ProductManager.escapeHtml(product.photo || '—')}</p>` : `<input id="productPhoto" class="form-control" value="${ProductManager.escapeHtml(product.photo || '')}">`}
              </div>

              <div class="mb-3">
                <label class="form-label">Описание</label>
                ${mode === 'view' ? `<p>${ProductManager.escapeHtml(product.description || '—')}</p>` : `<textarea id="productDescription" class="form-control" rows="4">${ProductManager.escapeHtml(product.description || '')}</textarea>`}
              </div>
            </div>

            <div class="modal-footer">
              <button class="btn btn-secondary" data-bs-dismiss="modal">Закрыть</button>
              ${mode === 'edit' ? `<button class="btn btn-primary" id="productSaveBtn">Сохранить</button>` : ''}
              ${mode === 'create' ? `<button class="btn btn-primary" id="productCreateBtn">Создать</button>` : ''}
            </div>

          </div>
        </div>
      </div>
    `;

        document.body.insertAdjacentHTML('beforeend', modalHtml);

        const modalEl = document.getElementById('productModal');
        const bsModal = new bootstrap.Modal(modalEl);

        // Навешиваем обработчики для кнопок модалки (и снимаем после закрытия)
        const cleanup = () => {
            try {
                const createBtn = document.getElementById('productCreateBtn');
                if (createBtn) createBtn.removeEventListener('click', ProductManager._boundCreateHandler);
                const saveBtn = document.getElementById('productSaveBtn');
                if (saveBtn) saveBtn.removeEventListener('click', ProductManager._boundSaveHandler);
            } catch (e) {}
        };

        // Создаём и запоминаем функции-обработчики, чтобы можно было снять их позже
        ProductManager._boundCreateHandler = async () => {
            await ProductManager.createProduct();
        };
        ProductManager._boundSaveHandler = async () => {
            // product.id может быть undefined for create; for edit productId passed through showProductModalById
            const id = product.id;
            if (id) await ProductManager.updateProduct(id);
        };

        // Навешиваем только если кнопка существует
        const createBtnEl = document.getElementById('productCreateBtn');
        if (createBtnEl) createBtnEl.addEventListener('click', ProductManager._boundCreateHandler);

        const saveBtnEl = document.getElementById('productSaveBtn');
        if (saveBtnEl) saveBtnEl.addEventListener('click', ProductManager._boundSaveHandler);

        // Когда модалка скрыта — удаляем DOM и снимаем обработчики
        modalEl.addEventListener('hidden.bs.modal', () => {
            cleanup();
            try { modalEl.remove(); } catch (e) {}
        }, { once: true });

        // Ставим фокус на первый контрол и показываем модалку
        modalEl.addEventListener('shown.bs.modal', () => {
            const focusable = modalEl.querySelector('input, textarea, select, button');
            if (focusable) focusable.focus();
        }, { once: true });

        bsModal.show();
    }

    // ----------------- Helpers: create / update / delete -----------------
    static escapeHtml(str) {
        return String(str ?? '')
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#39;');
    }

    static async createProduct() {
        const errBlock = document.getElementById('productError');
        if (errBlock) { errBlock.classList.add('d-none'); errBlock.textContent = ''; }

        const name = document.getElementById('productName')?.value?.trim();
        let amount = Number(document.getElementById('productAmount')?.value);
        if (!Number.isFinite(amount)) amount = 0;
        const size = document.getElementById('productSize')?.value?.trim() || null;
        let count = Number(document.getElementById('productCount')?.value);
        if (!Number.isFinite(count)) count = 0;
        const catVal = document.getElementById('productCategory')?.value;
        const categoryId = (catVal && !isNaN(Number(catVal))) ? Number(catVal) : null;
        const available = !!document.getElementById('productAvailable')?.checked;
        const photo = document.getElementById('productPhoto')?.value?.trim() || null;
        const description = document.getElementById('productDescription')?.value?.trim() || null;

        if (!name) {
            if (errBlock) { errBlock.textContent = 'Введите название продукта'; errBlock.classList.remove('d-none'); }
            return;
        }

        // Формируем payload с единой схемой: amount, count, categoryId, available, photo, description
        const payload = { name, amount, size, count, categoryId, available, photo, description };

        try {
            const response = await axios.post(ProductManager.API_BASE(), payload);
            const created = response.data;

            // Снимаем фокус, чтобы не было aria-hidden warning
            document.activeElement?.blur();
            const modalEl = document.getElementById('productModal');
            bootstrap.Modal.getInstance(modalEl)?.hide();

            // Обновляем строку в таблице
            ProductManager.updateProductRow(created);
            ToastManager.showToast('Продукт создан', 'success');
        } catch (error) {
            console.error('Ошибка создания продукта:', error);
            const message = error?.response?.data?.message || error.message || 'Ошибка при создании продукта';
            if (errBlock) { errBlock.textContent = message; errBlock.classList.remove('d-none'); }
            ToastManager.showToast('Ошибка при создании', 'danger');
        }
    }

    static async updateProduct(productId) {
        const errBlock = document.getElementById('productError');
        if (errBlock) { errBlock.classList.add('d-none'); errBlock.textContent = ''; }

        const name = document.getElementById('productName')?.value?.trim();
        let amount = Number(document.getElementById('productAmount')?.value);
        if (!Number.isFinite(amount)) amount = 0;
        const size = document.getElementById('productSize')?.value?.trim() || null;
        let count = Number(document.getElementById('productCount')?.value);
        if (!Number.isFinite(count)) count = 0;
        const catVal = document.getElementById('productCategory')?.value;
        const categoryId = (catVal && !isNaN(Number(catVal))) ? Number(catVal) : null;
        const available = !!document.getElementById('productAvailable')?.checked;
        const photo = document.getElementById('productPhoto')?.value?.trim() || null;
        const description = document.getElementById('productDescription')?.value?.trim() || null;

        if (!name) {
            if (errBlock) { errBlock.textContent = 'Введите название продукта'; errBlock.classList.remove('d-none'); }
            return;
        }

        const payload = { name, amount, size, count, categoryId, available, photo, description };

        try {
            const response = await axios.put(`${ProductManager.API_BASE()}/${productId}`, payload);
            const updated = response.data;

            document.activeElement?.blur();
            const modalEl = document.getElementById('productModal');
            bootstrap.Modal.getInstance(modalEl)?.hide();

            ProductManager.updateProductRow(updated);
            ToastManager.showToast('Продукт обновлён', 'success');
        } catch (error) {
            console.error('Ошибка обновления продукта:', error);
            const message = error?.response?.data?.message || error.message || 'Ошибка при обновлении продукта';
            if (errBlock) { errBlock.textContent = message; errBlock.classList.remove('d-none'); }
            ToastManager.showToast('Ошибка при обновлении', 'danger');
        }
    }

    static async deleteProduct(productId) {
        if (!confirm('Удалить продукт?')) return;
        try {
            await axios.delete(`${ProductManager.API_BASE()}/${productId}`);
            document.querySelector(`tr[data-product-id="${productId}"]`)?.remove();
            ToastManager.showToast('Продукт удалён', 'success');
        } catch (error) {
            console.error('Ошибка удаления продукта:', error);
            ToastManager.showToast('Не удалось удалить продукт', 'danger');
        }
    }

    // ----------------- helper: load product by id and open modal -----------------
    static async showProductModalById(productId, mode = 'edit') {
        try {
            const response = await axios.get(`${ProductManager.API_BASE()}/${productId}`);
            const product = response.data;
            await ProductManager.showProductModal(product, mode);
        } catch (e) {
            console.error('Не удалось загрузить продукт:', e);
            ToastManager.showToast('Не удалось загрузить продукт', 'danger');
        }
    }
}

// ----------------- expose small compatibility globals (если где-то они используются) -----------------
function editProduct(id) { ProductManager.showProductModalById(id, 'edit'); }
function deleteProduct(id) { ProductManager.deleteProduct(id); }

// ----------------- Автоинициализация (если DOM уже содержит таблицу) -----------------
document.addEventListener('DOMContentLoaded', () => {
    ProductManager.init();
    // если страница сразу показывает продукты — можно инициировать загрузку:
    const tbody = document.getElementById('productsTableBody');
    if (tbody) ProductManager.loadProducts();
});
