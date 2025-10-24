document.addEventListener('DOMContentLoaded', () => {
    const root = document.querySelector('main[data-api-series][data-api-page]');
    const apiSeries = root.dataset.apiSeries; // 전체 시계열(차트용)
    const apiPage   = root.dataset.apiPage;   // 페이지네이션(테이블/합계용)
    const $ = (id) => document.getElementById(id);
    const fmt = (n) => (n || 0).toLocaleString('ko-KR');

    // 기본 기간: 최근 30일
    const today = new Date();
    const to = today.toISOString().slice(0,10);
    const fromD = new Date(today); fromD.setDate(today.getDate() - 29);
    const from = fromD.toISOString().slice(0,10);
    $('fromDate').value = from;
    $('toDate').value = to;

    // 상태
    let chart;
    let page = 1;
    const size = 40;

    function renderChart(labels, netSales, orders) {
        const canvas = document.getElementById('salesChart');
        if (!window.Chart || !canvas) return;

        const ctx = canvas.getContext('2d');
        if (chart) chart.destroy();
        chart = new Chart(ctx, {
            type: 'line',
            data: {
                labels,
                datasets: [
                    { label: '순매출', data: netSales },
                    { label: '주문수', data: orders }
                ]
            },
            options: {
                responsive: true,
                interaction: { mode: 'index', intersect: false },
                scales: { y: { beginAtZero: true } }
            }
        });
    }

    function renderPager(curr, total) {
        const el = $('pager');
        if (!el) return;
        const mk = (p, label, disabled=false, active=false) =>
            `<li class="page-item ${disabled ? 'disabled':''} ${active ? 'active':''}">
         <a class="page-link" href="#" data-p="${p}">${label}</a>
       </li>`;
        const items = [];

        const prev = Math.max(1, curr - 1);
        items.push(mk(prev, '«', curr === 1));

        const windowSize = 5; // 현재 기준 5개만
        let start = Math.max(1, curr - Math.floor(windowSize/2));
        let end   = Math.min(total, start + windowSize - 1);
        start     = Math.max(1, Math.min(start, Math.max(1, end - windowSize + 1)));

        if (start > 1) items.push(mk(1, '1'), mk(Math.max(1, curr - 10), '...'));

        for (let i = start; i <= end; i++) items.push(mk(i, String(i), false, i === curr));

        if (end < total) items.push(mk(Math.min(total, curr + 10), '...'), mk(total, String(total)));

        const next = Math.min(total, curr + 1);
        items.push(mk(next, '»', curr === total));

        el.innerHTML = items.join('');
        el.querySelectorAll('a[data-p]').forEach(a => {
            a.addEventListener('click', (e) => {
                e.preventDefault();
                const p = Number(a.dataset.p);
                if (!isNaN(p)) { page = p; loadPage(); }
            });
        });
    }

    async function loadChart() {
        const f = $('fromDate').value;
        const t = $('toDate').value;
        const res = await fetch(`${apiSeries}?from=${f}&to=${t}`);
        if (!res.ok) throw new Error(`series HTTP ${res.status}`);
        const list = await res.json();

        // 차트는 데이터 있는 일자만 사용 (빈 날짜 보간 불필요)
        const labels = list.map(r => r.date);
        const orders = list.map(r => r.orders);
        const net    = list.map(r => r.netSales);
        renderChart(labels, net, orders);
    }

    async function loadPage() {
        const f = $('fromDate').value;
        const t = $('toDate').value;

        const res = await fetch(`${apiPage}?from=${f}&to=${t}&page=${page}&size=${size}`);
        if (!res.ok) throw new Error(`page HTTP ${res.status}`);
        const data = await res.json();

        // 합계
        $('sumOrders').textContent = fmt(data.sumOrders);
        $('sumNet').textContent    = fmt(data.sumNetSales);
        $('sumAov').textContent    = fmt(data.aov);

        // 표
        const tbody = $('salesTbody');
        tbody.innerHTML = data.items.map(r => `
      <tr>
        <td>${r.date}</td>
        <td class="text-end">${fmt(r.orders)}</td>
        <td class="text-end">${fmt(r.netSales)}</td>
        <td class="text-end">${fmt(r.aov)}</td>
      </tr>
    `).join('');

        // 페이지네이션
        renderPager(data.page, data.totalPages);
    }

    async function loadAll() {
        try {
            page = 1;        // 기간 바꾸면 1페이지로
            await loadChart();
            await loadPage();
        } catch (e) {
            console.error('[report-sales] 로딩 실패:', e);
            $('salesTbody').innerHTML =
                `<tr><td colspan="4" class="text-danger">데이터 로딩 실패: ${String(e)}</td></tr>`;
        }
    }

    $('btnApply').addEventListener('click', loadAll);
    loadAll();
});
