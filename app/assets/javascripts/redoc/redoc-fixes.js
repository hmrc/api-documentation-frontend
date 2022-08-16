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

    makeLeftMenuLinksAccessibleViaKeyboard();
}