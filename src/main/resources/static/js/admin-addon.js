class AddonManager {
    static async loadAddons() {
        console.log('Загрузка добавок...');
        const tbody = document.getElementById('addonsTableBody');
        if (tbody) {
            tbody.innerHTML = '<tr><td colspan="5" class="text-center">Страница в разработке</td></tr>';
        }
    }
}