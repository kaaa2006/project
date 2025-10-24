// static/js/main.js

// ===========================
// 1) 홈 배너(스와이퍼) 초기화
// ===========================
document.addEventListener('DOMContentLoaded', () => {
    try {
        const banners = document.querySelectorAll('.banner-swiper');
        banners.forEach((el, idx) => {
            const nextEl = el.querySelector('.swiper-button-next');
            const prevEl = el.querySelector('.swiper-button-prev');

            if (typeof Swiper !== 'function') return;

            const swiper = new Swiper(el, {
                loop: true,
                speed: 600,
                slidesPerView: 1,
                autoplay: { delay: 3500, disableOnInteraction: false },
                navigation: { nextEl, prevEl },
                observer: true,
                observeParents: true,
                preloadImages: false,
                lazy: true,
                watchSlidesProgress: true,
                watchOverflow: false
            });

            window.__banner = window.__banner || [];
            window.__banner[idx] = swiper;
        });
    } catch (err) {
        console.error('banner init error:', err);
    }
});

// ================================================
// 2) 전체 카테고리 → 메가 패널 토글 (슬라이드)
// ================================================
document.addEventListener('DOMContentLoaded', () => {
    const btn   = document.getElementById('megaToggle');
    const panel = document.getElementById('megaPanel');
    if (!btn || !panel) return;

    // 중복 바인딩 방지
    if (panel.dataset.megaBound === '1') return;
    panel.dataset.megaBound = '1';

    const open  = () => { panel.classList.add('open');  btn.setAttribute('aria-expanded', 'true');  };
    const close = () => { panel.classList.remove('open');btn.setAttribute('aria-expanded', 'false'); };
    const toggle = () => (panel.classList.contains('open') ? close() : open());

    btn.addEventListener('click', (e) => { e.preventDefault(); toggle(); });
    btn.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); toggle(); }
    });

    document.addEventListener('click', (e) => {
        if (!panel.classList.contains('open')) return;
        if (!panel.contains(e.target) && !btn.contains(e.target)) close();
    });

    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && panel.classList.contains('open')) close();
    });
});

// ================================================
// 3) 카테고리 바: Sticky 복원 + Fixed Fallback
//    - CSS sticky가 깨질 상황(부모 overflow/특정 레이아웃)에서도
//      확실히 붙도록 .fixed 클래스를 토글
// ================================================
document.addEventListener('DOMContentLoaded', () => {
    const catBar = document.querySelector('.category-bar');
    if (!catBar) return;

    let startY = 0;
    let ticking = false;

    const measure = () => {
        // 문서 기준 시작 위치 (lazy 이미지 등으로 레이아웃 변할 수 있어 재계산)
        startY = catBar.getBoundingClientRect().top + window.scrollY;
    };

    const apply = () => {
        const y = window.scrollY || window.pageYOffset || 0;
        if (y >= startY) {
            if (!catBar.classList.contains('fixed')) {
                catBar.classList.add('fixed');
                document.body.classList.add('has-fixed-cat');
                // 점프 방지: 실제 높이만큼 padding-top 동적 부여
                document.body.style.paddingTop = catBar.offsetHeight + 'px';
            }
        } else {
            if (catBar.classList.contains('fixed')) {
                catBar.classList.remove('fixed');
                document.body.classList.remove('has-fixed-cat');
                document.body.style.paddingTop = '';
            }
        }
    };

    const onScroll = () => {
        if (ticking) return;
        ticking = true;
        requestAnimationFrame(() => {
            apply();
            ticking = false;
        });
    };

    // 초기화 & 이벤트
    measure();
    apply();

    window.addEventListener('scroll', onScroll, { passive: true });
    window.addEventListener('resize', () => { measure(); apply(); });
    window.addEventListener('orientationchange', () => { measure(); apply(); });
    window.addEventListener('load', () => { measure(); apply(); });

    // 글꼴/이미지 로딩으로 높이가 달라질 수 있음 → 약간 지연 재측정
    setTimeout(() => { measure(); apply(); }, 250);
});
