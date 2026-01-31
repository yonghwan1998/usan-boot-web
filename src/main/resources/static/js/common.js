function __toggleClearBtn(inputEl) {
    const wrapper = inputEl.closest(".target__clear-wrapper");
    const btn = wrapper.querySelector(".target--clear-btn");

    btn.style.display = inputEl.value.trim().length > 0 ? "block" : "none";
}

function __clearInput(btnEl) {
    const wrapper = btnEl.closest(".target__clear-wrapper");
    const input = wrapper.querySelector(".target--clear-input");

    input.value = "";
    btnEl.style.display = "none";
    input.focus();
}

function __toggleViewBtn(btn) {
    const wrapper = btn.closest('.target__view-wrapper');
    if (!wrapper) return;

    const input = wrapper.querySelector('.target--view-input');
    if (!input) return;

    const isHidden = input.type === 'password';

    input.type = isHidden ? 'text' : 'password';

    if (isHidden) {
        btn.classList.remove('common-input__eye-slash-btn');
        btn.classList.add('common-input__eye-btn');
    } else {
        btn.classList.remove('common-input__eye-btn');
        btn.classList.add('common-input__eye-slash-btn');
    }
}

/**
 * @date    2026-01-26
 * @author  yongss
 * @param   {string} name
 * @return  {string|null}
 *
 * 처리 과정:
 *  - 현재 페이지 URL의 쿼리 스트링(location.search)을 가져옴
 *  - URLSearchParams 객체로 파싱
 *  - 전달받은 name에 해당하는 값을 조회
 *
 * 예외/주의:
 *  - 반환값은 항상 문자열(string) 또는 null
 *  - 숫자 좌표(lat, lng 등)는 parseFloat 또는 parseInt로 변환 필요
 *  - 동일한 이름의 파라미터가 여러 개일 경우 첫 번째 값만 반환
 */
function __getParam(name) {
    return new URLSearchParams(location.search).get(name);
}

/**
 * @date    2026-01-31
 * @author  yongss
 * @param   {string} value
 * @return  {boolean}
 *
 * 처리 과정:
 *  - 전달받은 값이 공백인지 검증
 */
function __isBlank(value) {
    return value.trim().length === 0;
}

/**
 * @date    2026-01-31
 * @author  yongss
 * @param   {string} email
 * @return  {boolean}
 *
 * 처리 과정:
 *  - 전달받은 값이 이메일 형식인지 검증
 */
function __isValidEmail(email) {
    const EMAIL_REGEX =
        /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

    return EMAIL_REGEX.test(email);
}

/**
 * @date    2026-01-31
 * @author  yongss
 * @param   {string} email
 * @return  {Promise<{exists: boolean}>}
 *
 * 처리 과정:
 *  - email을 전달 받아 DB에 존재하는지 AJAX 검증
 */
async function __checkEmailExists(email) {
    const qs = new URLSearchParams({ email });
    const res = await fetch(`/join/api/email-exists?${qs.toString()}`, {
        method: 'GET',
        headers: { 'Accept': 'application/json' }
    });

    if (!res.ok) throw new Error('email check failed');
    return await res.json();
}

(function () {
    const modal = document.getElementById('common-modal');
    const titleEl = document.getElementById('common-modal-title');
    const bodyEl = document.getElementById('common-modal-body');
    const actionsEl = document.getElementById('common-modal-actions');

    if (!modal || !titleEl || !bodyEl || !actionsEl) return;

    let isOpen = false;
    let popstateHandlerBound = false;

    function close() {
        if (!isOpen) return;

        modal.classList.remove('is-open');
        modal.setAttribute('aria-hidden', 'true');
        document.body.classList.remove('is-modal-open');

        actionsEl.innerHTML = '';
        bodyEl.innerHTML = '';
        titleEl.textContent = '';

        isOpen = false;

        // 모달 열 때 pushState를 넣었으면 뒤로가기 history 한 칸 되돌림
        // (사용자가 직접 뒤로가기를 누른 경우엔 이미 popstate가 실행되므로 안전)
        if (history.state && history.state.__COMMON_MODAL__ === true) {
            history.back();
        }
    }

    function open(opts) {
        const {
            title = '',
            bodyHtml = '',
            buttons = [
                { text: '확인', variant: 'primary', onClick: close }
            ],
            closeOnBackdrop = true,
            closeOnEsc = true,
            useBackButtonClose = true
        } = (opts || {});

        titleEl.textContent = title;
        bodyEl.innerHTML = bodyHtml;

        actionsEl.innerHTML = '';
        buttons.forEach(btn => {
            const el = document.createElement('button');
            el.type = 'button';
            el.className = `common-modal__btn ${
                btn.variant === 'outline' ? 'common-modal__btn--outline' : 'common-modal__btn--primary'
            }`;
            el.textContent = btn.text;

            el.addEventListener('click', () => {
                if (typeof btn.onClick === 'function') btn.onClick(close);
                else close();
            });

            actionsEl.appendChild(el);
        });

        modal.classList.add('is-open');
        modal.setAttribute('aria-hidden', 'false');
        document.body.classList.add('is-modal-open');
        isOpen = true;

        // ✅ 모바일 "뒤로가기"로 모달 닫기
        if (useBackButtonClose) {
            // 모달이 열릴 때 history를 한 칸 쌓아둠
            history.pushState({ __COMMON_MODAL__: true }, '');

            // popstate는 한 번만 바인딩
            if (!popstateHandlerBound) {
                window.addEventListener('popstate', (e) => {
                    // 모달이 열려있으면 popstate로 닫기
                    if (isOpen) {
                        modal.classList.remove('is-open');
                        modal.setAttribute('aria-hidden', 'true');
                        document.body.classList.remove('is-modal-open');
                        actionsEl.innerHTML = '';
                        bodyEl.innerHTML = '';
                        titleEl.textContent = '';
                        isOpen = false;
                    }
                });
                popstateHandlerBound = true;
            }
        }

        // ESC 닫기
        if (closeOnEsc) {
            const onKey = (e) => {
                if (!isOpen) return;
                if (e.key === 'Escape') close();
            };
            window.addEventListener('keydown', onKey, { once: true });
        }

        // backdrop 클릭 닫기
        if (closeOnBackdrop) {
            const onBackdrop = (e) => {
                if (!isOpen) return;
                if (e.target && e.target.matches('[data-modal-close]')) close();
            };
            modal.addEventListener('click', onBackdrop, { once: true });
        }
    }

    // 전역으로 노출
    window.CommonModal = { open, close };
})();