// Active Navigation Highlighting Script
document.addEventListener('DOMContentLoaded', function() {
    // Get current path
    const currentPath = window.location.pathname;

    // Remove any existing active classes
    document.querySelectorAll('.navbar-nav .nav-link').forEach(link => {
        link.classList.remove('active');
    });

    // Define path mappings for navigation items
    const navMappings = [
        { paths: ['/'], selector: 'a[href="/"]' },
        { paths: ['/books', '/books/add', '/books/edit'], selector: 'a[href="/books"]' },
        { paths: ['/authors', '/authors/add', '/authors/edit'], selector: 'a[href="/authors"]' },
        { paths: ['/cart', '/checkout'], selector: 'a[href="/cart"]' },
        { paths: ['/login'], selector: 'a[href="/login"]' },
        { paths: ['/register', '/register-success'], selector: 'a[href="/register"]' }
    ];

    // Find and activate the appropriate navigation item
    navMappings.forEach(mapping => {
        const isCurrentPath = mapping.paths.some(path => {
            if (path === '/') {
                return currentPath === '/';
            }
            return currentPath.startsWith(path);
        });

        if (isCurrentPath) {
            const navLink = document.querySelector(`.navbar-nav ${mapping.selector}`);
            if (navLink) {
                navLink.classList.add('active');
            }
        }
    });
});

