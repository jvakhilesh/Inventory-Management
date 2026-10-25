/*
 * =========================================================
 * SecurePortal - Authentication Frontend Logic
 * Vanilla JavaScript + Bootstrap 5.3
 * Connected to Spring Boot Auth Service
 * =========================================================
 */


/* =========================================================
   CONFIGURATION
   ========================================================= */

const AUTH_CONFIG = {

    endpoints: {

        csrf: '/auth/csrf',

        login: '/auth/login',

        signup: '/auth/signup',

        verifyOtp: '/auth/verify-otp',

        resendOtp: '/auth/resend-otp',

        refresh: '/auth/refresh',

        logout: '/auth/logout',

        me: '/auth/me',

        googleOAuth: '/oauth2/authorization/google',

        githubOAuth: '/oauth2/authorization/github',

        facebookOAuth: '/oauth2/authorization/facebook'
    },

    simulateBackend: false
};


/*
 * Store CSRF token in memory.
 *
 * IMPORTANT:
 * JWT tokens are NOT stored here.
 * Access/refresh tokens remain HttpOnly cookies.
 */

let csrfToken = null;


/* =========================================================
   PAGE INITIALIZATION
   ========================================================= */

document.addEventListener('DOMContentLoaded', () => {

    initPasswordToggles();

    initPasswordStrengthMeter();

    initLoginForm();

    initSignupForm();

    initEmailOtpHandlers();

    initForgotPasswordModal();

    initOAuthHandlers();

});


/* =========================================================
   1. PASSWORD VISIBILITY TOGGLE
   ========================================================= */

function initPasswordToggles() {

    const toggleButtons =
        document.querySelectorAll('.btn-password-toggle');

    toggleButtons.forEach((btn) => {

        btn.addEventListener('click', () => {

            const targetInputId =
                btn.getAttribute('data-target');

            const input =
                document.getElementById(targetInputId) ||
                btn.closest('.input-group')?.querySelector('input');

            const icon =
                btn.querySelector('i');

            if (!input) {
                return;
            }

            const isPassword =
                input.type === 'password';

            input.type =
                isPassword ? 'text' : 'password';

            if (icon) {

                icon.classList.toggle(
                    'bi-eye',
                    !isPassword
                );

                icon.classList.toggle(
                    'bi-eye-slash',
                    isPassword
                );
            }

            btn.setAttribute(
                'aria-label',
                isPassword
                    ? 'Hide password'
                    : 'Show password'
            );

            btn.setAttribute(
                'aria-pressed',
                isPassword ? 'true' : 'false'
            );

        });

    });

}


/* =========================================================
   2. PASSWORD STRENGTH METER
   ========================================================= */

function initPasswordStrengthMeter() {

    const passwordInput =
        document.getElementById('signupPassword');

    const confirmPasswordInput =
        document.getElementById('confirmPassword');

    const strengthBar =
        document.getElementById('strengthBar');

    const strengthText =
        document.getElementById('strengthLabel');

    if (!passwordInput || !strengthBar) {
        return;
    }

    const criteria = {

        length:
            document.getElementById('crit-length'),

        lower:
            document.getElementById('crit-lower'),

        upper:
            document.getElementById('crit-upper'),

        number:
            document.getElementById('crit-number'),

        special:
            document.getElementById('crit-special')
    };


    const evaluateCriteria = (value) => {

        const checks = {

            length:
                value.length >= 8,

            lower:
                /[a-z]/.test(value),

            upper:
                /[A-Z]/.test(value),

            number:
                /[0-9]/.test(value),

            special:
                /[^A-Za-z0-9]/.test(value)
        };


        let passedCount = 0;


        for (const [key, passed] of Object.entries(checks)) {

            if (criteria[key]) {

                criteria[key].classList.toggle(
                    'met',
                    passed
                );

                const icon =
                    criteria[key].querySelector('i');

                if (icon) {

                    icon.className =
                        passed
                            ? 'bi bi-check-circle-fill'
                            : 'bi bi-circle';
                }
            }


            if (passed) {
                passedCount++;
            }

        }


        let width = 0;

        let label = '';

        let colorClass = '#ef4444';


        if (value.length === 0) {

            width = 0;

            label = '';

        } else if (passedCount <= 2) {

            width = 25;

            label = 'Weak';

            colorClass = '#ef4444';

        } else if (passedCount === 3) {

            width = 50;

            label = 'Fair';

            colorClass = '#f59e0b';

        } else if (passedCount === 4) {

            width = 75;

            label = 'Good';

            colorClass = '#0ea5e9';

        } else {

            width = 100;

            label = 'Strong';

            colorClass = '#10b981';
        }


        strengthBar.style.width =
            `${width}%`;

        strengthBar.style.backgroundColor =
            colorClass;


        if (strengthText) {

            strengthText.textContent =
                label;

            strengthText.style.color =
                colorClass;
        }


        return passedCount >= 3;
    };


    passwordInput.addEventListener(
        'input',
        () => {

            evaluateCriteria(
                passwordInput.value
            );


            if (
                confirmPasswordInput &&
                confirmPasswordInput.value.length > 0
            ) {

                validatePasswordMatch(
                    passwordInput,
                    confirmPasswordInput
                );
            }

        }
    );


    if (confirmPasswordInput) {

        confirmPasswordInput.addEventListener(
            'input',
            () => {

                validatePasswordMatch(
                    passwordInput,
                    confirmPasswordInput
                );

            }
        );
    }

}


/* =========================================================
   PASSWORD MATCH VALIDATION
   ========================================================= */

function validatePasswordMatch(
    passwordElement,
    confirmElement
) {

    if (
        confirmElement.value !==
        passwordElement.value
    ) {

        confirmElement.setCustomValidity(
            'Passwords do not match'
        );

        confirmElement.classList.add(
            'is-invalid'
        );

        confirmElement.classList.remove(
            'is-valid'
        );

    } else {

        confirmElement.setCustomValidity('');

        confirmElement.classList.remove(
            'is-invalid'
        );

        if (confirmElement.value.length > 0) {

            confirmElement.classList.add(
                'is-valid'
            );
        }
    }

}


/* =========================================================
   3. CSRF TOKEN
   ========================================================= */

/*
 * Spring Security endpoint:
 *
 * GET /auth/csrf
 *
 * Backend returns:
 *
 * {
 *     "headerName": "X-XSRF-TOKEN",
 *     "parameterName": "_csrf",
 *     "token": "..."
 * }
 *
 * The token is also stored by Spring Security
 * inside the XSRF-TOKEN cookie.
 */

async function getCsrfToken() {

    const response =
        await fetch(
            AUTH_CONFIG.endpoints.csrf,
            {
                method: 'GET',

                credentials: 'include',

                cache: 'no-store'
            }
        );


    if (!response.ok) {

        throw new Error(
            'Unable to get CSRF token'
        );
    }


    const data =
        await response.json();


    if (!data.token) {

        throw new Error(
            'CSRF token was not returned by the server'
        );
    }


    /*
     * Save token in memory.
     */

    csrfToken =
        data.token;


    return csrfToken;
}


/* =========================================================
   CSRF-PROTECTED POST HELPER
   ========================================================= */

async function csrfPost(
    url,
    body = null
) {

    /*
     * Always get a fresh CSRF token.
     */

    const token =
        await getCsrfToken();


    const headers = {

        'X-XSRF-TOKEN':
            token
    };


    /*
     * Add JSON content type only
     * when we actually have a body.
     */

    if (body !== null) {

        headers['Content-Type'] =
            'application/json';
    }


    const options = {

        method: 'POST',

        credentials: 'include',

        headers: headers,

        cache: 'no-store'
    };


    if (body !== null) {

        options.body =
            JSON.stringify(body);
    }


    return await fetch(
        url,
        options
    );
}


/* =========================================================
   4. LOGIN FORM
   ========================================================= */

function initLoginForm() {

    const loginForm =
        document.getElementById('loginForm');

    if (!loginForm) {
        return;
    }


    loginForm.addEventListener(
        'submit',
        async (event) => {

            event.preventDefault();


            /*
             * Browser validation
             */

            if (!loginForm.checkValidity()) {

                event.stopPropagation();

                loginForm.classList.add(
                    'was-validated'
                );

                return;
            }


            /*
             * Get values
             */

            const email =
                document
                    .getElementById('loginIdentifier')
                    .value
                    .trim();

            const password =
                document
                    .getElementById('loginPassword')
                    .value;


            const submitButton =
                loginForm.querySelector(
                    'button[type="submit"]'
                );


            /*
             * Backend expects:
             *
             * {
             *     "email": "...",
             *     "password": "..."
             * }
             */

            const payload = {

                email: email,

                password: password
            };


            setButtonLoading(
                submitButton,
                true,
                'Signing in...'
            );


            clearAlert(
                'loginAlertArea'
            );


            try {

                /*
                 * POST /auth/login
                 *
                 * csrfPost() automatically:
                 *
                 * 1. Gets CSRF token
                 * 2. Sends X-XSRF-TOKEN header
                 * 3. Sends browser cookies
                 */

                const response =
                    await csrfPost(
                        AUTH_CONFIG.endpoints.login,
                        payload
                    );


                const data =
                    await parseResponse(response);


                /*
                 * Successful login
                 */

                if (response.ok) {

                    showAlert(
                        'loginAlertArea',
                        'success',
                        'Login successful! Redirecting...'
                    );


                    setButtonLoading(
                        submitButton,
                        false,
                        'Sign In'
                    );


                    /*
                     * Backend has now created:
                     *
                     * accessToken cookie
                     * refreshToken cookie
                     *
                     * Both are HttpOnly.
                     */


                    window.location.href =
                        'http://localhost:8081/';

                    return;
                }


                /*
                 * Login failed
                 */

                throw new Error(
                    getErrorMessage(
                        data,
                        'Login failed. Please check your credentials.'
                    )
                );

            } catch (error) {

                console.error(
                    'Login error:',
                    error
                );


                setButtonLoading(
                    submitButton,
                    false,
                    'Sign In'
                );


                showAlert(
                    'loginAlertArea',
                    'danger',
                    error.message ||
                    'Unable to connect to backend.'
                );

            }

        }
    );

}


/* =========================================================
   5. SIGNUP FORM
   ========================================================= */

function initSignupForm() {

    const signupForm =
        document.getElementById('signupForm');

    if (!signupForm) {
        return;
    }


    const passwordInput =
        document.getElementById('signupPassword');

    const confirmPasswordInput =
        document.getElementById('confirmPassword');


    signupForm.addEventListener(
        'submit',
        async (event) => {

            event.preventDefault();


            /*
             * Password confirmation
             */

            if (
                confirmPasswordInput &&
                passwordInput.value !==
                confirmPasswordInput.value
            ) {

                confirmPasswordInput.setCustomValidity(
                    'Passwords do not match'
                );

            } else if (confirmPasswordInput) {

                confirmPasswordInput.setCustomValidity('');
            }


            /*
             * Browser validation
             */

            if (!signupForm.checkValidity()) {

                event.stopPropagation();

                signupForm.classList.add(
                    'was-validated'
                );

                return;
            }


            /*
             * Get values
             */

            const name =
                document
                    .getElementById('signupName')
                    .value
                    .trim();


            const email =
                document
                    .getElementById('signupEmail')
                    .value
                    .trim();


            const password =
                passwordInput.value;


            const terms =
                document
                    .getElementById('termsCheckbox')
                    ?.checked;


            const submitButton =
                signupForm.querySelector(
                    'button[type="submit"]'
                );


            /*
             * Terms are frontend-only.
             *
             * Backend does not expect a terms field.
             */

            if (!terms) {

                showAlert(
                    'signupAlertArea',
                    'warning',
                    'Please accept the Terms of Service & Privacy Policy to proceed.'
                );

                return;
            }


            /*
             * Backend expects:
             *
             * {
             *     "name": "...",
             *     "email": "...",
             *     "password": "..."
             * }
             */

            const payload = {

                name: name,

                email: email,

                password: password,

                otpChannel: 'EMAIL',

                whatsappNumber: null
            };


            setButtonLoading(
                submitButton,
                true,
                'Creating Account...'
            );


            clearAlert(
                'signupAlertArea'
            );


            try {

                /*
                 * POST /auth/signup
                 */

                const response =
                    await csrfPost(
                        AUTH_CONFIG.endpoints.signup,
                        payload
                    );


                const data =
                    await parseResponse(response);


                if (!response.ok) {

                    throw new Error(
                        getErrorMessage(
                            data,
                            'Registration failed.'
                        )
                    );
                }


                setButtonLoading(
                    submitButton,
                    false,
                    'Create Account'
                );

                // Signup creates an inactive account and sends an email OTP.
                // Keep the email in memory until verification finishes.
                pendingOtpEmail = email.trim().toLowerCase();
                showEmailOtpPanel(pendingOtpEmail);
                startOtpResendCountdown(60);

                showAlert(
                    'otpAlertArea',
                    'success',
                    'If the account is eligible, a verification code has been sent to your email.'
                );


            } catch (error) {

                console.error(
                    'Signup error:',
                    error
                );


                setButtonLoading(
                    submitButton,
                    false,
                    'Create Account'
                );


                showAlert(
                    'signupAlertArea',
                    'danger',
                    error.message ||
                    'Unable to connect to backend.'
                );

            }

        }
    );

}


/* =========================================================
   EMAIL OTP VERIFICATION (manual signup only)
   ========================================================= */

let pendingOtpEmail = '';
let otpResendTimer = null;
let otpResendAllowedAt = 0;

const MAX_OTP_ATTEMPTS = 5;
const MAX_OTP_RESENDS = 3;

let otpAttemptsRemaining = MAX_OTP_ATTEMPTS;
let successfulOtpResends = 0;

function updateOtpAttemptsDisplay() {
    const counter = document.getElementById('otpAttemptsRemaining');
    if (counter) {
        counter.textContent = `Verification attempts remaining: ${otpAttemptsRemaining}`;
        counter.classList.toggle('text-danger', otpAttemptsRemaining <= 2);
        counter.classList.toggle('text-muted', otpAttemptsRemaining > 2);
    }
}

function redirectToSignupForOtpLimit(reason) {
    if (otpResendTimer) {
        clearInterval(otpResendTimer);
        otpResendTimer = null;
    }

    try {
        sessionStorage.setItem('signupOtpLimitReason', reason);
    } catch (error) {
        console.warn('Could not store OTP limit reason:', error);
    }

    window.location.replace('/signup.html?otpLimit=1');
}

function showOtpLimitReasonIfPresent() {
    const params = new URLSearchParams(window.location.search);
    if (params.get('otpLimit') !== '1') return;

    let reason = 'You reached the OTP limit. Please sign up again.';
    try {
        reason = sessionStorage.getItem('signupOtpLimitReason') || reason;
        sessionStorage.removeItem('signupOtpLimitReason');
    } catch (error) {
        console.warn('Could not read OTP limit reason:', error);
    }

    showAlert('signupAlertArea', 'warning', reason);
}

function showEmailOtpPanel(email) {
    const signupForm = document.getElementById('signupForm');
    const otpPanel = document.getElementById('otpVerificationPanel');
    const authHeader = document.querySelector('.auth-card > .auth-header');
    const oauthGroup = document.querySelector('.oauth-group');
    const divider = document.querySelector('.auth-divider');
    const footer = document.querySelector('.auth-footer');
    const emailLabel = document.getElementById('otpEmailLabel');
    const otpInput = document.getElementById('signupOtp');

    if (signupForm) signupForm.classList.add('d-none');
    if (otpPanel) otpPanel.classList.remove('d-none');
    if (authHeader) authHeader.classList.add('d-none');
    if (oauthGroup) oauthGroup.classList.add('d-none');
    if (divider) divider.classList.add('d-none');
    if (footer) footer.classList.add('d-none');
    if (emailLabel) emailLabel.textContent = email;

    otpAttemptsRemaining = MAX_OTP_ATTEMPTS;
    successfulOtpResends = 0;
    updateOtpAttemptsDisplay();

    if (otpInput) {
        otpInput.value = '';
        otpInput.focus();
    }

    clearAlert('signupAlertArea');
}

function startOtpResendCountdown(seconds) {
    if (otpResendTimer) clearInterval(otpResendTimer);

    const resendButton = document.getElementById('resendOtpButton');
    const countdownText = document.getElementById('resendCountdownText');
    otpResendAllowedAt = Date.now() + seconds * 1000;

    const update = () => {
        const remaining = Math.max(0, Math.ceil((otpResendAllowedAt - Date.now()) / 1000));

        if (countdownText) {
            if (successfulOtpResends >= MAX_OTP_RESENDS && remaining <= 0) {
                countdownText.textContent = 'You have used all 3 resend attempts. Requesting another will restart signup.';
            } else {
                countdownText.textContent = remaining > 0
                    ? `You can request another code in ${remaining} seconds.`
                    : 'You can request another code now.';
            }
        }

        if (resendButton) resendButton.disabled = remaining > 0;

        if (remaining <= 0 && otpResendTimer) {
            clearInterval(otpResendTimer);
            otpResendTimer = null;
        }
    };

    if (resendButton) resendButton.disabled = true;
    update();
    otpResendTimer = setInterval(update, 1000);
}

function initEmailOtpHandlers() {
    const verifyForm = document.getElementById('verifyOtpForm');
    const otpInput = document.getElementById('signupOtp');
    const resendButton = document.getElementById('resendOtpButton');

    showOtpLimitReasonIfPresent();
    updateOtpAttemptsDisplay();

    if (otpInput) {
        otpInput.addEventListener('input', () => {
            otpInput.value = otpInput.value.replace(/\D/g, '').slice(0, 6);
            otpInput.classList.remove('is-invalid');
        });
    }

    if (verifyForm) {
        verifyForm.addEventListener('submit', async (event) => {
            event.preventDefault();
            clearAlert('otpAlertArea');

            const otp = otpInput ? otpInput.value.trim() : '';

            if (!pendingOtpEmail) {
                showAlert('otpAlertArea', 'warning', 'Please start signup again.');
                return;
            }

            if (!/^\d{6}$/.test(otp)) {
                if (otpInput) otpInput.classList.add('is-invalid');
                showAlert('otpAlertArea', 'warning', 'Please enter the 6-digit OTP.');
                return;
            }

            const verifyButton = document.getElementById('verifyOtpButton');
            setButtonLoading(verifyButton, true, 'Verifying...');

            try {
                const response = await csrfPost(AUTH_CONFIG.endpoints.verifyOtp, {
                    email: pendingOtpEmail,
                    otp: otp
                });
                const data = await parseResponse(response);

                if (!response.ok) {
                    const error = new Error(getErrorMessage(data, 'OTP verification failed.'));
                    error.countsAsOtpFailure = true;
                    throw error;
                }

                if (otpResendTimer) clearInterval(otpResendTimer);
                showAlert('otpAlertArea', 'success', 'Email verified successfully! Redirecting...');
                window.location.href = 'http://localhost:8081/';
            } catch (error) {
                console.error('OTP verification error:', error);

                // Decrement only when the backend actually rejects the submitted OTP.
                // Network/CSRF failures should not consume a verification attempt.
                if (error.countsAsOtpFailure) {
                    otpAttemptsRemaining = Math.max(0, otpAttemptsRemaining - 1);
                    updateOtpAttemptsDisplay();

                    if (otpAttemptsRemaining === 0) {
                        showAlert(
                            'otpAlertArea',
                            'danger',
                            'You used all 5 verification attempts. Redirecting to signup...'
                        );

                        if (verifyButton) verifyButton.disabled = true;

                        setTimeout(() => {
                            redirectToSignupForOtpLimit(
                                'You reached the maximum of 5 invalid OTP attempts. Please sign up again and request a new verification code.'
                            );
                        }, 1200);
                    } else {
                        showAlert(
                            'otpAlertArea',
                            'danger',
                            `${error.message || 'OTP verification failed.'} You have ${otpAttemptsRemaining} attempt${otpAttemptsRemaining === 1 ? '' : 's'} remaining.`
                        );
                    }
                } else {
                    showAlert(
                        'otpAlertArea',
                        'danger',
                        error.message || 'Could not contact the server. Your attempt was not counted.'
                    );
                }
            } finally {
                if (verifyButton && otpAttemptsRemaining > 0) {
                    setButtonLoading(verifyButton, false, 'Verify Email');
                }
            }
        });
    }

    if (resendButton) {
        resendButton.addEventListener('click', async () => {
            if (!pendingOtpEmail || Date.now() < otpResendAllowedAt) return;

            // Three successful resends have already been used. The next click redirects.
            if (successfulOtpResends >= MAX_OTP_RESENDS) {
                showAlert(
                    'otpAlertArea',
                    'danger',
                    'You used all 3 resend attempts. Redirecting to signup...'
                );

                resendButton.disabled = true;

                setTimeout(() => {
                    redirectToSignupForOtpLimit(
                        'You reached the maximum of 3 OTP resend requests. Please sign up again to restart email verification.'
                    );
                }, 1200);
                return;
            }

            clearAlert('otpAlertArea');
            setButtonLoading(resendButton, true, 'Sending...');

            try {
                const response = await csrfPost(AUTH_CONFIG.endpoints.resendOtp, {
                    email: pendingOtpEmail
                });
                const data = await parseResponse(response);

                if (!response.ok) {
                    throw new Error(getErrorMessage(data, 'Unable to resend OTP.'));
                }

                successfulOtpResends += 1;

                showAlert(
                    'otpAlertArea',
                    'success',
                    `A new OTP request was accepted. Resends used: ${successfulOtpResends} of ${MAX_OTP_RESENDS}.`
                );

                startOtpResendCountdown(60);
            } catch (error) {
                console.error('Resend OTP error:', error);
                showAlert('otpAlertArea', 'danger', error.message || 'Unable to resend OTP.');

                if (/wait|cooldown|another otp|try again later/i.test(error.message || '')) {
                    startOtpResendCountdown(60);
                }
            } finally {
                const remaining = Math.max(0, Math.ceil((otpResendAllowedAt - Date.now()) / 1000));
                resendButton.innerHTML = 'Resend OTP';
                resendButton.disabled = remaining > 0;
            }
        });
    }
}


/* =========================================================
   6. FORGOT PASSWORD
   ========================================================= */

/*
 * Backend does not currently contain:
 *
 * POST /auth/forgot-password
 *
 * Therefore we don't call a fake endpoint.
 */

function initForgotPasswordModal() {

    const form =
        document.getElementById(
            'forgotPasswordForm'
        );

    if (!form) {
        return;
    }


    form.addEventListener(
        'submit',
        (event) => {

            event.preventDefault();


            showAlert(
                'forgotPasswordAlert',
                'warning',
                'Password reset is not available yet.'
            );

        }
    );

}


/* =========================================================
   7. OAUTH
   ========================================================= */

function initOAuthHandlers() {

    const oauthButtons =
        document.querySelectorAll(
            '[data-oauth-provider]'
        );


    oauthButtons.forEach((button) => {

        button.addEventListener(
            'click',
            (event) => {

                const provider =
                    button.getAttribute(
                        'data-oauth-provider'
                    );


                const targetEndpoint =
                    AUTH_CONFIG.endpoints[
                        `${provider}OAuth`
                    ];


                if (!targetEndpoint) {

                    event.preventDefault();

                    console.error(
                        `OAuth provider not configured: ${provider}`
                    );

                    return;
                }


                /*
                 * Spring Security handles OAuth.
                 *
                 * We only redirect the browser.
                 */

                window.location.href =
                    targetEndpoint;

            }
        );

    });

}


/* =========================================================
   8. REFRESH ACCESS TOKEN
   ========================================================= */

async function refreshAccessToken() {

    const response =
        await csrfPost(
            AUTH_CONFIG.endpoints.refresh
        );


    const data =
        await parseResponse(response);


    if (!response.ok) {

        throw new Error(
            getErrorMessage(
                data,
                'Unable to refresh access token.'
            )
        );
    }


    return data;
}


/* =========================================================
   9. LOGOUT
   ========================================================= */

async function logoutUser() {

    try {

        const response =
            await csrfPost(
                AUTH_CONFIG.endpoints.logout
            );


        const data =
            await parseResponse(response);


        if (!response.ok) {

            throw new Error(
                getErrorMessage(
                    data,
                    'Logout failed.'
                )
            );
        }


        /*
         * Backend clears:
         *
         * accessToken
         * refreshToken
         */

        window.location.href =
            '/login.html';

    } catch (error) {

        console.error(
            'Logout error:',
            error
        );


        alert(
            error.message ||
            'Logout failed. Please try again.'
        );
    }

}


/* =========================================================
   10. GET CURRENT USER
   ========================================================= */

/*
 * Used by home.html.
 *
 * JWT is NOT read by JavaScript.
 *
 * Browser automatically sends the HttpOnly
 * accessToken cookie.
 */

async function getCurrentUser() {

    const response =
        await fetch(
            AUTH_CONFIG.endpoints.me,
            {
                method: 'GET',

                credentials: 'same-origin',

                cache: 'no-store'
            }
        );


    const data =
        await parseResponse(response);


    if (!response.ok) {

        throw new Error(
            getErrorMessage(
                data,
                'User is not authenticated.'
            )
        );
    }


    return data;
}


/* =========================================================
   11. RESPONSE HELPER
   ========================================================= */

async function parseResponse(response) {

    const contentType =
        response.headers.get(
            'content-type'
        );


    if (
        contentType &&
        contentType.includes(
            'application/json'
        )
    ) {

        return await response.json();
    }


    const text =
        await response.text();


    return text
        ? { message: text }
        : {};
}


/* =========================================================
   12. ERROR MESSAGE HELPER
   ========================================================= */

function getErrorMessage(
    data,
    defaultMessage
) {

    if (!data) {
        return defaultMessage;
    }


    if (typeof data === 'string') {
        return data;
    }


    if (Array.isArray(data.errors) && data.errors.length > 0) {
        return data.errors.join(', ');
    }

    return (
        data.message ||
        data.error ||
        defaultMessage
    );
}


/* =========================================================
   13. BUTTON LOADING
   ========================================================= */

function setButtonLoading(
    button,
    isLoading,
    text
) {

    if (!button) {
        return;
    }


    button.disabled =
        isLoading;


    if (isLoading) {

        button.dataset.originalText =
            button.innerHTML;


        button.innerHTML =
            `<span
                class="spinner-border spinner-border-sm me-2"
                role="status"
                aria-hidden="true">
             </span>${text}`;

    } else {

        button.innerHTML =
            button.dataset.originalText ||
            text;
    }

}


/* =========================================================
   14. SHOW ALERT
   ========================================================= */

function showAlert(
    containerId,
    type,
    message
) {

    const container =
        document.getElementById(
            containerId
        );


    if (!container) {
        return;
    }


    const icon =
        type === 'success'
            ? 'bi-check-circle-fill'
            : type === 'warning'
                ? 'bi-exclamation-triangle-fill'
                : 'bi-x-circle-fill';


    container.innerHTML = `

        <div
            class="alert alert-${type} alert-dismissible fade show auth-alert"
            role="alert">

            <i
                class="bi ${icon} me-2 fs-5">
            </i>

            <div>
                ${escapeHtml(message)}
            </div>

            <button
                type="button"
                class="btn-close"
                data-bs-dismiss="alert"
                aria-label="Close">
            </button>

        </div>

    `;
}


/* =========================================================
   15. CLEAR ALERT
   ========================================================= */

function clearAlert(containerId) {

    const container =
        document.getElementById(
            containerId
        );


    if (container) {

        container.innerHTML =
            '';
    }

}


/* =========================================================
   16. ESCAPE HTML
   ========================================================= */

function escapeHtml(str) {

    return String(str).replace(
        /[&<>'"]/g,

        (tag) => ({
            '&': '&amp;',
            '<': '&lt;',
            '>': '&gt;',
            "'": '&#39;',
            '"': '&quot;'
        }[tag] || tag)
    );

}