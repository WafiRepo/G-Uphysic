// Common Navigation Component for Admin Portal
// This ensures consistent navigation across all pages

function renderNavigation(currentPage) {
    const navItems = [
        { href: 'dashboard.html', icon: 'bi-house', text: 'Dashboard' },
        { href: 'users.html', icon: 'bi-people', text: 'Users' },
        { href: 'user_questions.html', icon: 'bi-question-circle', text: 'User Questions' }
    ];

    // Render top navbar
    const navbar = `
        <nav class="navbar navbar-expand-lg navbar-light bg-white fixed-top">
            <div class="container-fluid">
                <a class="navbar-brand" href="dashboard.html">P-MAGIC Physics Admin</a>
                <div class="navbar-nav ms-auto">
                    ${navItems.map(item => `
                        <a class="nav-link ${currentPage === item.href ? 'active' : ''}" href="${item.href}">
                            <i class="bi ${item.icon}"></i> ${item.text}
                        </a>
                    `).join('')}
                    <a class="nav-link" href="#" onclick="logout(); return false;">
                        <i class="bi bi-box-arrow-right"></i> Logout
                    </a>
                </div>
            </div>
        </nav>
    `;

    // Render sidebar
    const sidebar = `
        <nav class="col-md-3 col-lg-2 d-md-block sidebar">
            <div class="sidebar-sticky">
                <ul class="nav flex-column">
                    ${navItems.map(item => `
                        <li class="nav-item">
                            <a class="nav-link ${currentPage === item.href ? 'active' : ''}" href="${item.href}">
                                <i class="bi ${item.icon}"></i> ${item.text}
                            </a>
                        </li>
                    `).join('')}
                </ul>
            </div>
        </nav>
    `;

    // Insert into page
    document.body.insertAdjacentHTML('afterbegin', navbar);
    document.body.insertAdjacentHTML('afterbegin', sidebar);
}

// Common logout function
function logout() {
    if (typeof firebase !== 'undefined' && firebase.auth) {
        firebase.auth().signOut().then(() => {
            window.location.href = 'index.html';
        }).catch((error) => {
            console.error('Error signing out:', error);
            alert('Error signing out. Please try again.');
        });
    } else {
        window.location.href = 'index.html';
    }
}

// Common Firebase initialization
function initFirebase() {
    if (typeof firebase === 'undefined') {
        console.error('Firebase SDK not loaded');
        return null;
    }

    const firebaseConfig = {
        apiKey: "AIzaSyBF1Oy7vcWztqtcMAQoG_zx2eqQwevXvRg",
        authDomain: "gphysolve.firebaseapp.com",
        projectId: "gphysolve",
        storageBucket: "gphysolve.firebasestorage.app",
        messagingSenderId: "794871328792",
        appId: "1:794871328792:web:2a37ca02c792fc6b83536d",
        measurementId: "G-MNGEX8W1X4"
    };

    if (!firebase.apps.length) {
        firebase.initializeApp(firebaseConfig);
    }

    return firebase.firestore();
}

// Check authentication and admin status
function checkAuth() {
    return new Promise((resolve, reject) => {
        if (typeof firebase === 'undefined' || !firebase.auth) {
            window.location.href = 'index.html';
            reject('Firebase not loaded');
            return;
        }

        firebase.auth().onAuthStateChanged(async (user) => {
            if (!user) {
                window.location.href = 'index.html';
                reject('Not authenticated');
                return;
            }

            try {
                const db = firebase.firestore();
                const userDoc = await db.collection('user').doc(user.uid).get();
                
                if (!userDoc.exists || userDoc.data().role !== 'admin') {
                    await firebase.auth().signOut();
                    window.location.href = 'index.html';
                    reject('Not an admin');
                    return;
                }

                resolve(user);
            } catch (error) {
                console.error('Error checking admin status:', error);
                reject(error);
            }
        });
    });
}

