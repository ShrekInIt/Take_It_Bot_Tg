/* global axios, bootstrap */

class CategoryManager {
    static API_BASE = `${new AuthManager().API_BASE}/categories`;
    static API_ROOT = new AuthManager().API_BASE;
    static _inited = false;

    static init() {
        if (this._inited) return;
        this._inited = true;

        // делегирование кликов по таблице категорий
        document.addEventListener('click', (e) => {
            const edit = e.target.closest('.js-edit-category');
            if (edit) {
                const id = Number(edit.dataset.id);
                if (id) CategoryManager.showCategoryModalById(id, 'edit');
                return;
            }
            const del = e.target.closest('.js-delete-category');
            if (del) {
                const id = Number(del.dataset.id);
                if (id) CategoryManager.deleteCategory(id);
                return;
            }
            const createOpen = e.target.closest('#openCreateCategoryModal') || e.target.closest('[data-action="open-create-category"]');
            if (createOpen) {
                CategoryManager.showCategoryModal({}, 'create');
                return;
            }
            const searchBtn = e.target.closest('#searchCategoryBtn') || e.target.closest('[data-action="search-categories"]');
            if (searchBtn) {
                CategoryManager.searchCategories();
                return;
            }
            const resetBtn = e.target.closest('#resetCategoryBtn') || e.target.closest('[data-action="reset-categories"]');
            if (resetBtn) {
                CategoryManager.loadCategories();

            }
        });

        // Enter для поиска
        const searchInput = document.getElementById('categorySearchInput');
        if (searchInput) {
            searchInput.addEventListener('keydown', (ev) => {
                if (ev.key === 'Enter') { ev.preventDefault(); CategoryManager.searchCategories(); }
            });
        }
    }

    static async loadCategories() {
        try {
            const res = await axios.get(this.API_BASE);
            const categories = res.data;
            const tbody = document.getElementById('categoriesTableBody');
            if (!tbody) return;
            tbody.innerHTML = '';
            if (!categories || categories.length === 0) {
                tbody.innerHTML = '<tr><td colspan="5" class="text-center">Нет категорий</td></tr>';
                return;
            }
            categories.forEach(c => tbody.appendChild(CategoryManager.createCategoryRowElement(c)));
        } catch (err) {
            console.error('Ошибка загрузки категорий:', err);
            const tbody = document.getElementById('categoriesTableBody');
            if (tbody) tbody.innerHTML = '<tr><td colspan="5" class="text-center text-danger">Ошибка загрузки данных</td></tr>';
            ToastManager.showToast('Ошибка загрузки категорий', 'danger');
        }
    }

    static createCategoryRowElement(category) {
        const tr = document.createElement('tr');
        tr.setAttribute('data-category-id', category.id);

        tr.innerHTML = `
      <td>${category.id}</td>
      <td>${CategoryManager.escapeHtml(category.name ?? 'Без названия')}</td>
      <td>${CategoryManager.escapeHtml(category.description ?? '')}</td>
      <td>${category.parentName ?? '—'}</td>
      <td>
          <span class="badge ${category.isActive ? 'bg-success' : 'bg-danger'} text-white">
          ${category.isActive ? 'Активна' : 'Выключена'}
          </span>
      </td>

      <td>${category.sortOrder ?? category.displayOrder ?? 0}</td>
      <td>
        <button class="btn btn-sm btn-outline-primary js-edit-category" data-id="${category.id}" title="Редактировать">
          <i class="bi bi-pencil"></i>
        </button>
        <button class="btn btn-sm btn-outline-danger ms-1 js-delete-category" data-id="${category.id}" title="Удалить">
          <i class="bi bi-trash"></i>
        </button>
      </td>
    `;
        return tr;
    }

    static updateCategoryRow(category) {
        const tbody = document.getElementById('categoriesTableBody');
        if (!tbody) return;
        const existing = tbody.querySelector(`tr[data-category-id="${category.id}"]`);
        const newRow = CategoryManager.createCategoryRowElement(category);
        if (existing) existing.replaceWith(newRow);
        else tbody.prepend(newRow);
    }

    static async searchCategories() {
        const input = document.getElementById('categorySearchInput');
        if (!input) return;
        const q = input.value.trim();
        const tbody = document.getElementById('categoriesTableBody');
        if (!tbody) return;
        if (!q) { CategoryManager.loadCategories(); return; }
        tbody.innerHTML = '<tr><td colspan="5" class="text-center">Поиск...</td></tr>';
        try {
            const res = await axios.get(`${this.API_BASE}/search`, { params: { name: q } });
            const result = res.data;
            tbody.innerHTML = '';
            if (!result || result.length === 0) {
                tbody.innerHTML = '<tr><td colspan="5" class="text-center">Ничего не найдено</td></tr>';
                return;
            }
            result.forEach(c => tbody.appendChild(CategoryManager.createCategoryRowElement(c)));
            ToastManager.showToast(`Найдено: ${result.length}`, 'info');
        } catch (err) {
            console.error('Ошибка поиска категорий:', err);
            tbody.innerHTML = '<tr><td colspan="5" class="text-center text-danger">Ошибка при поиске</td></tr>';
            ToastManager.showToast('Ошибка при поиске', 'danger');
        }
    }

    // Modal (create/edit/view)
    static async showCategoryModal(category = {}, mode = 'view') {
        // дождёмся удаления старой
        const old = document.getElementById('categoryModal');
        if (old) {
            await new Promise(resolve => {
                const inst = bootstrap.Modal.getInstance(old);
                if (inst) inst.hide();
                old.addEventListener('hidden.bs.modal', () => { try { old.remove(); } catch(_){} resolve(); }, { once: true });
                setTimeout(() => { if (!document.getElementById('categoryModal')) resolve(); }, 300);
            });
        }

        // загрузим возможных родителей (для select)
        let parents = [];
        try {
            const r = await axios.get(this.API_BASE);
            parents = Array.isArray(r.data) ? r.data : [];
        } catch (e) { console.warn('Не удалось загрузить список родителей', e); }

        // загрузим список типов категорий
        let types = [];
        try {
            const t = await axios.get(`${this.API_ROOT}/category-types`);
            types = Array.isArray(t.data) ? t.data : [];
        } catch (e) { console.warn('Не удалось загрузить список типов категорий', e); }

        if (mode === 'create') {
            category = { name: '', description: '', sortOrder: 0, parentId: null, isActive: true, categoryTypeId: null };
        }

        const parentOptions = parents.map(p => `<option value="${p.id}" ${((category.parentId === p.id) || (category.parent?.id === p.id)) ? 'selected' : ''}>${CategoryManager.escapeHtml(p.name)}</option>`).join('');

        const typeOptions = types.map(t => `<option value="${t.id}" ${((category.categoryTypeId === t.id) || (category.categoryType?.id === t.id) || (category.categoryTypeId && Number(category.categoryTypeId) === t.id)) ? 'selected' : ''}>${CategoryManager.escapeHtml(t.name)}</option>`).join('');

        const modalHtml = `
      <div class="modal fade" id="categoryModal" tabindex="-1" aria-hidden="true">
        <div class="modal-dialog">
          <div class="modal-content">
            <div class="modal-header">
              <h5 class="modal-title">${mode === 'create' ? 'Создать категорию' : mode === 'edit' ? 'Редактировать категорию' : 'Просмотр категории'} ${category.id ? '#'+category.id : ''}</h5>
              <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Закрыть"></button>
            </div>
            <div class="modal-body">
              <div id="categoryError" class="alert alert-danger d-none"></div>

              <div class="mb-3">
                <label class="form-label">Название</label>
                ${mode === 'view' ? `<p>${CategoryManager.escapeHtml(category.name || '—')}</p>` : `<input id="categoryName" class="form-control" value="${CategoryManager.escapeHtml(category.name || '')}">`}
              </div>

              <div class="mb-3">
                <label class="form-label">Описание</label>
                ${mode === 'view' ? `<p>${CategoryManager.escapeHtml(category.description || '—')}</p>` : `<textarea id="categoryDescription" class="form-control" rows="3">${CategoryManager.escapeHtml(category.description || '')}</textarea>`}
              </div>

              <div class="mb-3">
                <label class="form-label">Родитель</label>
                ${mode === 'view' ? `<p>${CategoryManager.escapeHtml(category.parentName ?? category.parent?.name ?? '—')}</p>` : `<select id="categoryParent" class="form-select"><option value="">-- Нет родителя --</option>${parentOptions}</select>`}
              </div>

              <div class="mb-3">
                <label class="form-label">Тип категории</label>
                ${mode === 'view' ? `<p>${CategoryManager.escapeHtml(category.categoryTypeName ?? category.categoryType?.name ?? '—')}</p>` : `<select id="categoryType" class="form-select"><option value="">-- Нет типа --</option>${typeOptions}</select>`}
              </div>

              <div class="mb-3">
                <label class="form-label">Порядок</label>
                ${mode === 'view' ? `<p>${category.sortOrder ?? category.displayOrder ?? 0}</p>` : `<input id="categorySortOrder" type="number" class="form-control" value="${category.sortOrder ?? category.displayOrder ?? 0}">`}
              </div>

              <div class="mb-3 form-check">
                ${mode === 'view' ? `<p>Активна: ${category.isActive ? 'Да' : 'Нет'}</p>` : `<input id="categoryIsActive" type="checkbox" class="form-check-input" ${category.isActive ? 'checked' : ''}><label class="form-check-label ms-2">Активна</label>`}
              </div>
            </div>

            <div class="modal-footer">
              <button class="btn btn-secondary" data-bs-dismiss="modal">Закрыть</button>
              ${mode === 'edit' ? `<button id="categorySaveBtn" class="btn btn-primary">Сохранить</button>` : ''}
              ${mode === 'create' ? `<button id="categoryCreateBtn" class="btn btn-primary">Создать</button>` : ''}
            </div>
          </div>
        </div>
      </div>
    `;
        document.body.insertAdjacentHTML('beforeend', modalHtml);
        const modalEl = document.getElementById('categoryModal');
        const bs = new bootstrap.Modal(modalEl);

        // handlers
        const createHandler = async () => { await CategoryManager.createCategory(); };
        const saveHandler = async () => { if (category.id) await CategoryManager.updateCategory(category.id); };

        if (mode === 'create') document.getElementById('categoryCreateBtn').addEventListener('click', createHandler);
        if (mode === 'edit') document.getElementById('categorySaveBtn').addEventListener('click', saveHandler);

        modalEl.addEventListener('hidden.bs.modal', () => {
            try {
                if (mode === 'create') document.getElementById('categoryCreateBtn')?.removeEventListener('click', createHandler);
                if (mode === 'edit') document.getElementById('categorySaveBtn')?.removeEventListener('click', saveHandler);
            } catch (e) {}
            try { modalEl.remove(); } catch (e) {}
        }, { once: true });

        modalEl.addEventListener('shown.bs.modal', () => {
            const first = modalEl.querySelector('input, textarea, select, button');
            if (first) first.focus();
        }, { once: true });

        bs.show();
    }

    static escapeHtml(s) { return String(s ?? '').replaceAll('&','&amp;').replaceAll('<','&lt;').replaceAll('>','&gt;').replaceAll('"','&quot;').replaceAll("'",'&#39;'); }

    static async createCategory() {
        const errBlock = document.getElementById('categoryError');
        if (errBlock) { errBlock.classList.add('d-none'); errBlock.textContent = ''; }

        const name = document.getElementById('categoryName')?.value?.trim();
        const description = document.getElementById('categoryDescription')?.value?.trim() || null;
        const sortOrder = Number(document.getElementById('categorySortOrder')?.value || 0);
        const parentVal = document.getElementById('categoryParent')?.value;
        const parentId = parentVal && !isNaN(Number(parentVal)) ? Number(parentVal) : null;
        const typeVal = document.getElementById('categoryType')?.value;
        const categoryTypeId = typeVal && !isNaN(Number(typeVal)) ? Number(typeVal) : null;
        const isActive = !!document.getElementById('categoryIsActive')?.checked;

        if (!name) {
            if (errBlock) { errBlock.textContent = 'Введите название'; errBlock.classList.remove('d-none'); }
            return;
        }

        const payload = { name, description, sortOrder, parentId, categoryTypeId, isActive };

        try {
            const res = await axios.post(this.API_BASE, payload);
            const created = res.data;
            document.activeElement?.blur();
            bootstrap.Modal.getInstance(document.getElementById('categoryModal'))?.hide();
            CategoryManager.updateCategoryRow(created);
            ToastManager.showToast('Категория создана', 'success');
        } catch (err) {
            console.error('Ошибка создания категории', err);
            const msg = err?.response?.data?.message || err.message || 'Ошибка';
            if (errBlock) { errBlock.textContent = msg; errBlock.classList.remove('d-none'); }
            ToastManager.showToast('Ошибка создания', 'danger');
        }
    }

    static async updateCategory(id) {
        const errBlock = document.getElementById('categoryError');
        if (errBlock) { errBlock.classList.add('d-none'); errBlock.textContent = ''; }

        const name = document.getElementById('categoryName')?.value?.trim();
        const description = document.getElementById('categoryDescription')?.value?.trim() || null;
        const sortOrder = Number(document.getElementById('categorySortOrder')?.value || 0);
        const parentVal = document.getElementById('categoryParent')?.value;
        const parentId = parentVal && !isNaN(Number(parentVal)) ? Number(parentVal) : null;
        const typeVal = document.getElementById('categoryType')?.value;
        const categoryTypeId = typeVal && !isNaN(Number(typeVal)) ? Number(typeVal) : null;
        const isActive = !!document.getElementById('categoryIsActive')?.checked;

        if (!name) {
            if (errBlock) { errBlock.textContent = 'Введите название'; errBlock.classList.remove('d-none'); }
            return;
        }

        const payload = { name, description, sortOrder, parentId, categoryTypeId, isActive };

        try {
            const res = await axios.put(`${this.API_BASE}/${id}`, payload);
            const updated = res.data;
            document.activeElement?.blur();
            bootstrap.Modal.getInstance(document.getElementById('categoryModal'))?.hide();
            CategoryManager.updateCategoryRow(updated);
            ToastManager.showToast('Категория обновлена', 'success');
        } catch (err) {
            console.error('Ошибка обновления категории', err);
            const msg = err?.response?.data?.message || err.message || 'Ошибка';
            if (errBlock) { errBlock.textContent = msg; errBlock.classList.remove('d-none'); }
            ToastManager.showToast('Ошибка обновления', 'danger');
        }
    }

    static async deleteCategory(id) {
        if (!confirm('Удалить категорию? (Товары не будут удалены)')) return;
        try {
            await axios.delete(`${this.API_BASE}/${id}`);
            document.querySelector(`tr[data-category-id="${id}"]`)?.remove();
            ToastManager.showToast('Категория удалена', 'success');
        } catch (err) {
            console.error('Ошибка удаления категории', err);
            ToastManager.showToast('Не удалось удалить категорию', 'danger');
        }
    }

    static async showCategoryModalById(id, mode='edit') {
        try {
            const res = await axios.get(`${this.API_BASE}/${id}`);
            await CategoryManager.showCategoryModal(res.data, mode);
        } catch (err) {
            console.error('Не удалось загрузить категорию', err);
            ToastManager.showToast('Не удалось загрузить категорию', 'danger');
        }
    }
}

// Глобальные совместимые функции (если где-то используются inline handlers)
function editCategory(id) { CategoryManager.showCategoryModalById(id, 'edit'); }
function deleteCategory(id) { CategoryManager.deleteCategory(id); }
function showCreateCategoryModal() { CategoryManager.showCategoryModal({}, 'create'); }

// Инициализация при загрузке страницы
document.addEventListener('DOMContentLoaded', () => {
    CategoryManager.init();
    const tbody = document.getElementById('categoriesTableBody');
    if (tbody) CategoryManager.loadCategories();
});
