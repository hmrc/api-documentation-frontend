function applyRedocFixes() {
    function makeLeftMenuLinksAccessibleViaKeyboard() {
        document.querySelectorAll('ul[role=navigation] li').forEach(li => {
            if (!li.hasAttribute('tabindex')) {
                li.setAttribute('tabindex', 0);
                const navLocation = li.getAttribute('data-item-id');
                if (navLocation) {
                    li.addEventListener('keydown', e => {
                        if (e.code === 'Enter') {
                            window.location.hash = navLocation;
                            li.focus();
                        }
                    }, true);
                }
            }
        });
    }

    function retainFocusAfterClickingCopyButtons() {
        [...document.querySelectorAll('div[role=tabpanel] button')].filter(btn => btn.innerText === 'Copy').forEach(copyBtn => {
            copyBtn.addEventListener('click', () => {
                setTimeout(() => copyBtn.focus(), 0);
            });
        })
    }

    function removeMenuItemRoleFromLeftHandMenu() {
        document.querySelectorAll('ul[role=navigation] label[role=menuitem]').forEach(label => {
            label.removeAttribute('role');
        });
    }

    function forEachActiveResponseCodeButton(fn) {
        [...document.querySelectorAll('h3')].filter(h3 => h3.innerText === 'Responses').forEach(h3 => {
            const buttonContainer = h3.nextElementSibling;
            [...buttonContainer.querySelectorAll('button:not([disabled])')].forEach(fn);
        });
    }

    function addAriaExpandedAttributeToResponsesButtons() {
        forEachActiveResponseCodeButton(activeButton => activeButton.setAttribute('aria-expanded', 'false'));
    }

    function makeMenuButtonAccessibleViaKeyboard() {
        const menuButton = document.querySelector('.menu-content').nextSibling;
        menuButton.classList.add('menuButton');
        if (!menuButton.hasAttribute('tabindex')) {
            menuButton.setAttribute('tabindex', 1);
        }
        menuButton.addEventListener('keydown', e => {
            if (e.code === 'Enter') {
                menuButton.click();
            }
        }, true);
    }

    function addClassesForWindowsHighContrastMode() {
        // see forced-colors media query in redoc-fixes.scss
        forEachActiveResponseCodeButton(activeButton => activeButton.classList.add('responseCodeButton'));
    }

    makeLeftMenuLinksAccessibleViaKeyboard();
    retainFocusAfterClickingCopyButtons();
    removeMenuItemRoleFromLeftHandMenu();
    addAriaExpandedAttributeToResponsesButtons();
    makeMenuButtonAccessibleViaKeyboard();
    addClassesForWindowsHighContrastMode();
}