function renderPagination({ container, totalPages, currentPage, onPageClick }) {
    container.innerHTML = '';
    if (totalPages <= 1) return;

    const makeBtn = (label, page, options = {}) => {
        const { disabled = false, active = false } = options;
        const li = document.createElement('li');
        const btn = document.createElement('button');
        btn.type = 'button';
        btn.className = 'page-btn';
        btn.textContent = label;

        if (disabled) btn.classList.add('disabled');
        if (active) btn.classList.add('active');

        btn.addEventListener('click', () => {
            if (!disabled && !active) onPageClick(page);
        });

        li.appendChild(btn);
        return li;
    };

    const addEllipsis = () => {
        const li = document.createElement('li');
        li.className = 'ellipsis';
        li.textContent = '…';
        container.appendChild(li);
    };

    const maxBtns = 7;
    let start = Math.max(1, currentPage - Math.floor(maxBtns / 2));
    let end = Math.min(totalPages, start + maxBtns - 1);
    start = Math.max(1, end - maxBtns + 1);

    container.appendChild(makeBtn('‹ 이전', currentPage - 1, { disabled: currentPage === 1 }));

    if (start > 1) {
        container.appendChild(makeBtn('1', 1));
        if (start > 2) addEllipsis();
    }

    for (let i = start; i <= end; i++) {
        container.appendChild(makeBtn(`${i}`, i, { active: i === currentPage }));
    }

    if (end < totalPages) {
        if (end < totalPages - 1) addEllipsis();
        container.appendChild(makeBtn(`${totalPages}`, totalPages));
    }

    container.appendChild(makeBtn('다음 ›', currentPage + 1, { disabled: currentPage === totalPages }));
}