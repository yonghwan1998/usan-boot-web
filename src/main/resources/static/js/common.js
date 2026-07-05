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

/**
 * @date    2026-01-31
 * @author  yongss
 * @param   {string} phone
 * @return  {Promise<{exists: boolean}>}
 */
async function __checkPhoneExists(phone) {
    const qs = new URLSearchParams({ phone });

    const res = await fetch(`/join/api/phone-exists?${qs.toString()}`, {
        method: 'GET',
        headers: { 'Accept': 'application/json' }
    });

    if (!res.ok) throw new Error('phone check failed');

    return await res.json();
}

/**
 * @date    2026-01-31
 * @author  yongss
 * @param   {string} pw
 * @return  {boolean}
 *
 * 처리 과정:
 *  - 전달받은 값이 영문 + 숫자 + 특수문자, 8자 이상인지 검증
 */
function __isValidPassword(pw) {
    return /^(?=.*[A-Za-z])(?=.*\d)(?=.*[^A-Za-z\d]).{8,}$/.test(pw);
}

/**
 * @date    2026-01-31
 * @author  yongss
 * @param   {string} phone
 * @return  {boolean}
 *
 * 처리 과정:
 *  - 전달받은 값이 하이픈/공백 제외 후 01[016789] + 7~8자리 인지 확인
 */
function __isValidPhone(phone) {
    const v = String(phone ?? '').trim();

    const digits = v.replace(/[^\d]/g, '');

    return /^01[016789]\d{7,8}$/.test(digits);
}

/**
 * @date    2026-01-31
 * @author  yongss
 * @param   {string} phone
 * @return  {string}
 *
 * 처리 과정:
 *  - 입력 받은 값을 전화번호 형식으로 하이픈 처리
 */
function __formatPhone(phone) {
    const digits = String(phone ?? '').replace(/[^\d]/g, '');

    // 010 / 011~019 기준으로 하이픈 포맷
    if (digits.length <= 3) return digits;
    if (digits.length <= 7) return digits.replace(/(\d{3})(\d+)/, '$1-$2');
    if (digits.length <= 11) {
        // 010 4자리 + 4자리 (11) / 010 3자리 + 4자리 (10)
        if (digits.length === 10) return digits.replace(/(\d{3})(\d{3})(\d{4})/, '$1-$2-$3');
        return digits.replace(/(\d{3})(\d{4})(\d{4})/, '$1-$2-$3');
    }
    return digits.slice(0, 11).replace(/(\d{3})(\d{4})(\d{4})/, '$1-$2-$3');
}

/**
 * @date    2026-02-16
 * @author  yongss
 * @param   {string} input
 * @return  {string}
 *
 * 처리 과정:
 *  - 입력 받은 HTML의 특수문자를 HTML 엔티티로 치환하여 XSS 위험 줄임
 */
function __escapeHtml(input) {
    return String(input)
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}

/**
 * @date    2026-06-22
 * @author  yongss
 * @param   {string} inputEl
 * @return  {string}
 *
 * 처리 과정:
 *  - 입력 받은 inputEl의 만원 단위를 원 단위로 변환
 *  - 변환 한 hint를 표기
 */
function __showManwonHint(inputEl) {
    const wrapper = inputEl.closest('.common-input-wrapper');
    if (!wrapper) return;

    let hint = wrapper.nextElementSibling;
    if (!hint || !hint.classList.contains('manwon-hint')) {
        hint = document.createElement('div');
        hint.className = 'manwon-hint';
        wrapper.insertAdjacentElement('afterend', hint);
    }

    const val = parseInt(inputEl.value, 10);
    if (!inputEl.value || isNaN(val) || val <= 0) {
        hint.textContent = '';
        return;
    }

    hint.textContent = (val * 10000).toLocaleString('ko-KR') + '원';
}

/**
 * @date    2026-07-02
 * @author  yongss
 * @param   {string} email
 * @return  {string}
 *
 * 처리 과정:
 *  - 입력 받은 email을 앞 두자리와 도메인을 제외하고 '*'로 변환
 *  - ex. na***@gmail.com
 */
function __maskEmail(email) {
    const atIdx = email.indexOf('@');
    if (atIdx <= 0) return email;

    const local = email.slice(0, atIdx);
    const domain = email.slice(atIdx);
    const visible = local.slice(0, 2);
    const masked = '*'.repeat(Math.max(local.length - 2, 0));

    return visible + masked + domain;
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

(function () {
    let el = null;
    let isOpen = false;
    let popstateHandlerBound = false;
    let backdropHandler = null;

    function mount() {
        const div = document.createElement('div');
        div.className = 'common-popup';
        div.setAttribute('aria-hidden', 'true');
        div.innerHTML =
            '<div class="common-popup__backdrop" data-popup-close></div>' +
            '<div class="common-popup__card" role="dialog" aria-modal="true">' +
                '<div class="common-popup__content" id="common-popup-content"></div>' +
                '<div class="common-popup__actions" id="common-popup-actions"></div>' +
            '</div>';

        const shell = document.querySelector('.mobile-shell');
        (shell || document.body).appendChild(div);
        return div;
    }

    function close() {
        if (!isOpen || !el) return;

        el.classList.remove('is-open');
        el.setAttribute('aria-hidden', 'true');
        document.body.classList.remove('is-modal-open');

        el.querySelector('#common-popup-content').innerHTML = '';
        el.querySelector('#common-popup-actions').innerHTML = '';

        if (backdropHandler) {
            el.removeEventListener('click', backdropHandler);
            backdropHandler = null;
        }

        isOpen = false;

        if (history.state && history.state.__COMMON_POPUP__ === true) {
            history.back();
        }
    }

    function open(opts) {
        const {
            contentHtml = '',
            actionsHtml = '',
            closeOnBackdrop = true,
            closeOnEsc = true,
            useBackButtonClose = true
        } = (opts || {});

        if (!el) el = mount();

        el.querySelector('#common-popup-content').innerHTML = contentHtml;
        el.querySelector('#common-popup-actions').innerHTML = actionsHtml;

        el.classList.add('is-open');
        el.setAttribute('aria-hidden', 'false');
        document.body.classList.add('is-modal-open');
        isOpen = true;

        if (useBackButtonClose) {
            history.pushState({ __COMMON_POPUP__: true }, '');

            if (!popstateHandlerBound) {
                window.addEventListener('popstate', function () {
                    if (!isOpen) return;
                    el.classList.remove('is-open');
                    el.setAttribute('aria-hidden', 'true');
                    document.body.classList.remove('is-modal-open');
                    el.querySelector('#common-popup-content').innerHTML = '';
                    el.querySelector('#common-popup-actions').innerHTML = '';
                    if (backdropHandler) {
                        el.removeEventListener('click', backdropHandler);
                        backdropHandler = null;
                    }
                    isOpen = false;
                });
                popstateHandlerBound = true;
            }
        }

        if (closeOnEsc) {
            window.addEventListener('keydown', function onKey(e) {
                if (!isOpen) return;
                if (e.key === 'Escape') close();
                window.removeEventListener('keydown', onKey);
            });
        }

        if (closeOnBackdrop) {
            backdropHandler = function (e) {
                if (!isOpen) return;
                if (e.target && e.target.matches('[data-popup-close]')) close();
            };
            el.addEventListener('click', backdropHandler);
        }
    }

    window.CommonPopup = { open, close };
})();