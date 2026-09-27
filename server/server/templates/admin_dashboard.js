// ============================================================================
// Fichier : admin-dashboard.js
// Version : 13/09/25
// Auteur  : Anthropic Claude
// Projet  : Serveur CodeLab - Interface d'administration
// ============================================================================

// Variables globales
let autoRefreshInterval;
let isAutoRefreshEnabled = false;
let allData = {
    clients: [],
    users: [],
    sessions: []
};
let serverStopped = false;
let consecutiveErrors = 0;

// État du tri pour chaque table
let sortState = {
    users: { column: null, direction: 'asc' },
    sessions: { column: null, direction: 'asc' },
    clients: { column: null, direction: 'asc' }
};

// Fonction de debug
function debugLog(message, type = 'info') {
    const debugDiv = document.getElementById('debugLog');
    const timestamp = new Date().toLocaleTimeString();
    const cssClass = type === 'error' ? 'debug-error' : 
                    type === 'success' ? 'debug-success' : 
                    type === 'warning' ? 'debug-warning' : '';
    
    const logLine = document.createElement('div');
    logLine.className = `debug-line ${cssClass}`;
    logLine.textContent = `[${timestamp}] ${message}`;
    
    debugDiv.appendChild(logLine);
    debugDiv.scrollTop = debugDiv.scrollHeight;
    
    // Limiter à 50 lignes
    while (debugDiv.children.length > 50) {
        debugDiv.removeChild(debugDiv.firstChild);
    }
}

// Gestion du tri des tableaux
function setupTableSorting() {
    setupSortingForTable('usersTable', 'users');
    setupSortingForTable('sessionsTable', 'sessions');
    setupSortingForTable('clientsTable', 'clients');
}

function setupSortingForTable(tableId, dataKey) {
    const table = document.getElementById(tableId);
    if (!table) return;
    
    const headers = table.querySelectorAll('th.sortable');
    headers.forEach(header => {
        header.addEventListener('click', function() {
            const column = this.dataset.column;
            if (!column) return;
            
            // Mettre à jour l'état du tri
            if (sortState[dataKey].column === column) {
                sortState[dataKey].direction = sortState[dataKey].direction === 'asc' ? 'desc' : 'asc';
            } else {
                sortState[dataKey].column = column;
                sortState[dataKey].direction = 'asc';
            }
            
            // Mettre à jour les styles des en-têtes
            headers.forEach(h => {
                h.classList.remove('sort-asc', 'sort-desc');
            });
            
            if (sortState[dataKey].direction === 'asc') {
                this.classList.add('sort-asc');
            } else {
                this.classList.add('sort-desc');
            }
            
            // Mettre à jour l'affichage avec le nouveau tri
            if (dataKey === 'users') updateUsersTable();
            else if (dataKey === 'sessions') updateSessionsTable();
            else if (dataKey === 'clients') updateClientsTable();
            
            debugLog(`Tri ${dataKey} par ${column} (${sortState[dataKey].direction})`, 'info');
        });
    });
}

// Fonction pour appliquer le tri à un tableau de données
function applySortToData(data, dataKey) {
    if (!sortState[dataKey].column || !data || !Array.isArray(data)) {
        return data;
    }
    
    const column = sortState[dataKey].column;
    const direction = sortState[dataKey].direction;
    
    return [...data].sort((a, b) => {
        let valueA = a[column];
        let valueB = b[column];
        
        // Gestion des valeurs nulles/undefined
        if (valueA == null) valueA = '';
        if (valueB == null) valueB = '';
        
        // Tri spécialisé pour certaines colonnes
        valueA = getSpecialSortValue(valueA, column);
        valueB = getSpecialSortValue(valueB, column);
        
        // Conversion en string pour comparaison si ce ne sont pas des nombres
        if (typeof valueA !== 'number') valueA = String(valueA).toLowerCase();
        if (typeof valueB !== 'number') valueB = String(valueB).toLowerCase();
        
        // Tri numérique si les valeurs sont des nombres
        if (!isNaN(valueA) && !isNaN(valueB) && valueA !== '' && valueB !== '') {
            valueA = parseFloat(valueA);
            valueB = parseFloat(valueB);
        }
        
        let comparison = 0;
        if (valueA > valueB) {
            comparison = 1;
        } else if (valueA < valueB) {
            comparison = -1;
        }
        
        return direction === 'desc' ? comparison * -1 : comparison;
    });
}

// Tri spécialisé pour certaines colonnes
function getSpecialSortValue(value, column) {
    switch (column) {
        case 'status':
            const statusOrder = { 'ADMIN': 0, 'TUTOR': 1, 'STUDENT': 2 };
            return statusOrder[value] || 999;
            
        case 'help_flag':
            if (value === true || value === 'true' || value === '🆘') return 0;
            return 1;
            
        case 'openned':
            return value === true ? 0 : 1;
            
        default:
            return value;
    }
}

// Test de toutes les API
async function testAllApis() {
    debugLog('=== DÉBUT DES TESTS API ===', 'info');
    
    try {
        const response = await fetch('/api/dashboard');
        debugLog(`Dashboard - Status: ${response.status} ${response.statusText}`, response.ok ? 'success' : 'error');
        if (response.ok) {
            const data = await response.json();
            debugLog(`Dashboard - Données reçues: ${Object.keys(data).join(', ')}`, 'success');
        }
    } catch (error) {
        debugLog(`Dashboard - Exception: ${error.message}`, 'error');
    }
    
    try {
        const response = await fetch('/api/users');
        debugLog(`Users - Status: ${response.status} ${response.statusText}`, response.ok ? 'success' : 'error');
        if (response.ok) {
            const data = await response.json();
            debugLog(`Users - ${data.length} utilisateurs trouvés`, 'success');
        }
    } catch (error) {
        debugLog(`Users - Exception: ${error.message}`, 'error');
    }
    
    try {
        const response = await fetch('/api/sessions');
        debugLog(`Sessions - Status: ${response.status} ${response.statusText}`, response.ok ? 'success' : 'error');
        if (response.ok) {
            const data = await response.json();
            debugLog(`Sessions - ${data.length} sessions trouvées`, 'success');
        }
    } catch (error) {
        debugLog(`Sessions - Exception: ${error.message}`, 'error');
    }
    
    debugLog('=== FIN DES TESTS API ===', 'info');
}

// Gestion des onglets
function setupTabs() {
    document.querySelectorAll('.nav-tab').forEach(tab => {
        tab.addEventListener('click', function() {
            const targetTab = this.dataset.tab;
            
            document.querySelectorAll('.nav-tab').forEach(t => t.classList.remove('active'));
            this.classList.add('active');
            
            document.querySelectorAll('.tab-content').forEach(content => content.classList.remove('active'));
            document.getElementById(targetTab).classList.add('active');
            
            debugLog(`Changement vers l'onglet: ${targetTab}`, 'info');
        });
    });
}

// Chargement des données utilisateurs
async function loadUsersData() {
    debugLog('Chargement des données utilisateurs...', 'info');
    const tbody = document.querySelector('#usersTable tbody');
    if (tbody) {
        tbody.innerHTML = '<tr><td colspan="8" style="text-align: center; padding: 20px;">🔄 Chargement...</td></tr>';
    }
    
    try {
        const response = await fetch('/api/users');
        debugLog(`API Users - Status: ${response.status}`, response.ok ? 'success' : 'error');
        
        if (response.ok) {
            allData.users = await response.json();
            debugLog(`${allData.users.length} utilisateurs chargés`, 'success');
            updateUsersTable();
        } else {
            const errorText = await response.text();
            debugLog(`Erreur API Users: ${errorText}`, 'error');
            if (tbody) {
                tbody.innerHTML = '<tr><td colspan="8" style="text-align: center; color: #e74c3c;">⌘ Erreur lors du chargement</td></tr>';
            }
        }
    } catch (error) {
        debugLog(`Exception Users: ${error.message}`, 'error');
        if (tbody) {
            tbody.innerHTML = '<tr><td colspan="8" style="text-align: center; color: #e74c3c;">⌘ Erreur de connexion</td></tr>';
        }
    }
}

// Chargement des données sessions
async function loadSessionsData() {
    debugLog('Chargement des données sessions...', 'info');
    const tbody = document.querySelector('#sessionsTable tbody');
    if (tbody) {
        tbody.innerHTML = '<tr><td colspan="6" style="text-align: center; padding: 20px;">🔄 Chargement...</td></tr>';
    }
    
    try {
        const response = await fetch('/api/sessions');
        debugLog(`API Sessions - Status: ${response.status}`, response.ok ? 'success' : 'error');
        
        if (response.ok) {
            allData.sessions = await response.json();
            debugLog(`${allData.sessions.length} sessions chargées`, 'success');
            updateSessionsTable();
        } else {
            const errorText = await response.text();
            debugLog(`Erreur API Sessions: ${errorText}`, 'error');
            if (tbody) {
                tbody.innerHTML = '<tr><td colspan="6" style="text-align: center; color: #e74c3c;">⌘ Erreur lors du chargement</td></tr>';
            }
        }
    } catch (error) {
        debugLog(`Exception Sessions: ${error.message}`, 'error');
        if (tbody) {
            tbody.innerHTML = '<tr><td colspan="6" style="text-align: center; color: #e74c3c;">⌘ Erreur de connexion</td></tr>';
        }
    }
}

// Mise à jour de la table des utilisateurs
function updateUsersTable() {
    const tbody = document.querySelector('#usersTable tbody');
    if (!tbody || allData.users.length === 0) {
        if (tbody) {
            tbody.innerHTML = '<tr><td colspan="8" style="text-align: center; color: #7f8c8d;">Aucun utilisateur trouvé</td></tr>';
        }
        return;
    }
    
    const filteredUsers = filterUsers();
    const sortedUsers = applySortToData(filteredUsers, 'users');
    debugLog(`Affichage de ${sortedUsers.length} utilisateurs`, 'info');
    
    tbody.innerHTML = sortedUsers.map(user => {
        const statusClass = {
            'STUDENT': 'status-student',
            'TUTOR': 'status-tutor',
            'ADMIN': 'status-admin'
        }[user.status] || '';
        
        const groupsBadges = user.groups && user.groups !== '--' && user.groups.trim()
            ? user.groups.split(',').map(g => g.trim()).filter(g => g)
                .map(group => `<span class="badge">${group}</span>`).join(' ')
            : '';
        
        return `<tr>
            <td><strong>${user.login}</strong></td>
            <td>${user.fullname || user.login}</td>
            <td><span class="${statusClass}">${user.status}</span></td>
            <td>${groupsBadges}</td>
            <td>${user.mail !== '--' ? user.mail : ''}</td>
            <td>${user.date !== '--' ? user.date : ''}</td>
            <td>${user.addr !== '--' ? user.addr : ''}</td>
            <td>${user.session_id !== '--' ? user.session_id : ''}</td>
        </tr>`;
    }).join('');
}

// Mise à jour de la table des sessions
function updateSessionsTable() {
    const tbody = document.querySelector('#sessionsTable tbody');
    if (!tbody || allData.sessions.length === 0) {
        if (tbody) {
            tbody.innerHTML = '<tr><td colspan="6" style="text-align: center; color: #7f8c8d;">Aucune session trouvée</td></tr>';
        }
        return;
    }
    
    const filteredSessions = filterSessions();
    const sortedSessions = applySortToData(filteredSessions, 'sessions');
    debugLog(`Affichage de ${sortedSessions.length} sessions`, 'info');
    
    tbody.innerHTML = sortedSessions.map(session => {
        const statusClass = session.openned ? 'session-open' : 'session-closed';
        const statusText = session.openned ? '✅ Ouverte' : '🔒 Fermée';
        
        const groupsBadges = session.groups && session.groups.trim()
            ? session.groups.split(' ').filter(g => g.trim())
                .map(group => `<span class="badge">${group}</span>`).join(' ')
            : '';
        
        const usersBadges = session.users && session.users.trim()
            ? session.users.split(' ').filter(u => u.trim())
                .map(user => `<span class="badge" style="background: #e67e22;">${user}</span>`).join(' ')
            : '';
        
        return `<tr>
            <td><strong>${session.id}</strong></td>
            <td><span class="${statusClass}">${statusText}</span></td>
            <td>${groupsBadges}</td>
            <td>${usersBadges}</td>
            <td>${session.total_users || 0}</td>
            <td>${session.connected_users || 0}</td>
        </tr>`;
    }).join('');
}

// Mise à jour de la table des clients connectés
function updateClientsTable() {
    const tbody = document.querySelector('#clientsTable tbody');
    if (!tbody) return;
    
    if (allData.clients.length === 0) {
        const message = serverStopped ? 
            '⚠️ Serveur arrêté - Aucun client connecté' : 
            'Aucun client connecté';
        const color = serverStopped ? '#e74c3c' : '#7f8c8d';
        tbody.innerHTML = `<tr><td colspan="9" style="text-align: center; color: ${color}; font-weight: ${serverStopped ? 'bold' : 'normal'};">${message}</td></tr>`;
        return;
    }
    
    const sortedClients = applySortToData(allData.clients, 'clients');
    debugLog(`Affichage de ${sortedClients.length} clients connectés`, 'info');
    
    tbody.innerHTML = sortedClients.map(client => {
        const statusClass = {
            'STUDENT': 'status-student',
            'TUTOR': 'status-tutor',
            'ADMIN': 'status-admin'
        }[client.status] || '';
        
        const helpClass = client.help_flag ? 'help-flag' : '';
        const helpText = client.help_flag ? '🆘' : 'Non';
        
        const escapedLogin = (client.login || '').replace(/'/g, "\\'");
        const escapedFullname = (client.fullname || '').replace(/'/g, "\\'");
        
        return `<tr>
            <td><strong>${client.login || 'N/A'}</strong></td>
            <td>${client.fullname || 'N/A'}</td>
            <td><span class="${statusClass}">${client.status || 'N/A'}</span></td>
            <td>${client.session || 'N/A'}</td>
            <td>${client.addr || 'N/A'}</td>
            <td>${client.duration || 'N/A'}</td>
            <td><span class="${helpClass}">${helpText}</span></td>
            <td>${client.edited_file || 'Aucun'}</td>
            <td class="action-cell">
                <button class="btn btn-disconnect" onclick="disconnectClient('${escapedLogin}', '${escapedFullname}')" ${serverStopped ? 'disabled' : ''}>
                    Eject
                </button>
            </td>
        </tr>`;
    }).join('');
}

// Filtrage des utilisateurs
function filterUsers() {
    let filtered = [...allData.users];
    const searchTerm = document.getElementById('userSearch').value.toLowerCase();
    
    if (searchTerm) {
        filtered = filtered.filter(user => 
            user.login.toLowerCase().includes(searchTerm) ||
            (user.fullname && user.fullname.toLowerCase().includes(searchTerm)) ||
            (user.mail && user.mail.toLowerCase().includes(searchTerm))
        );
    }
    return filtered;
}

// Filtrage des sessions
function filterSessions() {
    let filtered = [...allData.sessions];
    const searchTerm = document.getElementById('sessionSearch').value.toLowerCase();
    
    if (searchTerm) {
        filtered = filtered.filter(session => 
            session.id.toLowerCase().includes(searchTerm) ||
            (session.groups && session.groups.toLowerCase().includes(searchTerm))
        );
    }
    return filtered;
}

// Configuration des filtres
function setupFilters() {
    const userSearch = document.getElementById('userSearch');
    if (userSearch) {
        userSearch.addEventListener('input', updateUsersTable);
    }
    
    const sessionSearch = document.getElementById('sessionSearch');
    if (sessionSearch) {
        sessionSearch.addEventListener('input', updateSessionsTable);
    }
}

// Gestion de l'auto-refresh
function startAutoRefresh() {
    if (autoRefreshInterval) clearInterval(autoRefreshInterval);
    autoRefreshInterval = setInterval(updateDashboard, 5000);
    isAutoRefreshEnabled = true;
    updateAutoRefreshButton();
    debugLog('Auto-refresh activé (5s)', 'info');
}

function stopAutoRefresh() {
    if (autoRefreshInterval) clearInterval(autoRefreshInterval);
    isAutoRefreshEnabled = false;
    updateAutoRefreshButton();
    debugLog('Auto-refresh désactivé', 'warning');
}

function toggleAutoRefresh() {
    if (serverStopped) return;
    if (isAutoRefreshEnabled) {
        stopAutoRefresh();
    } else {
        startAutoRefresh();
    }
}

function updateAutoRefreshButton() {
    const btn = document.getElementById('autoRefreshBtn');
    if (btn) {
        if (serverStopped) {
            btn.textContent = 'Auto-refresh arrêté';
            btn.className = 'btn btn-warning';
            btn.disabled = true;
        } else {
            btn.textContent = isAutoRefreshEnabled ? 'Pause auto-refresh' : 'Démarrer auto-refresh';
            btn.className = isAutoRefreshEnabled ? 'btn btn-warning' : 'btn btn-success';
            btn.disabled = false;
        }
    }
}

// Mise à jour du dashboard principal
async function updateDashboard() {
    debugLog('Mise à jour du dashboard...', 'info');
    
    if (serverStopped) return;
    
    try {
        const response = await fetch('/api/dashboard');
        debugLog(`Dashboard API - Status: ${response.status}`, response.ok ? 'success' : 'error');
        
        if (response.ok) {
            const data = await response.json();
            debugLog('Dashboard - Données reçues avec succès', 'success');
            updateStats(data.stats);
            allData.clients = data.clients;
            updateClientsTable();
            updateLastRefresh();
            consecutiveErrors = 0;
        } else {
            const errorText = await response.text();
            debugLog(`Dashboard API Error: ${errorText}`, 'error');
            throw new Error(`HTTP ${response.status}`);
        }
    } catch (error) {
        debugLog(`Dashboard Exception: ${error.message}`, 'error');
        consecutiveErrors++;
        
        document.getElementById('connectionStatus').textContent = '⌘ Erreur de connexion';
        
        if (consecutiveErrors >= 3) {
            debugLog('Trop d\'erreurs consécutives - serveur considéré comme arrêté', 'error');
            handleServerStopped();
        }
    }
}

// Mise à jour des statistiques
function updateStats(stats) {
    if (stats) {
        document.getElementById('totalConnections').textContent = stats.total_connections || 0;
        document.getElementById('studentsCount').textContent = stats.students_count || 0;
        document.getElementById('tutorsCount').textContent = stats.tutors_count || 0;
        document.getElementById('activeSessions').textContent = stats.active_sessions || 0;
        debugLog(`Stats mises à jour: ${stats.total_connections} connexions`, 'success');
    } else {
        debugLog('Aucune statistique reçue', 'warning');
    }
}

// Déconnexion d'un client
async function disconnectClient(login, fullname) {
    if (serverStopped) {
        debugLog('Tentative de déconnexion impossible - serveur arrêté', 'error');
        alert('Impossible de déconnecter un client : le serveur est arrêté');
        return;
    }
    
    if (!confirm(`Êtes-vous sûr de vouloir déconnecter ${fullname} (${login}) ?`)) {
        return;
    }
    
    debugLog(`Tentative de déconnexion de ${login}`, 'info');
    
    try {
        const response = await fetch('/api/disconnect', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ login: login })
        });
        
        debugLog(`Disconnect API - Status: ${response.status}`, response.ok ? 'success' : 'error');
        
        if (response.ok) {
            const result = await response.json();
            if (result.status === 'success') {
                debugLog(`Client ${login} déconnecté avec succès`, 'success');
                alert(`Client ${fullname} (${login}) déconnecté avec succès`);
                updateDashboard();
            } else {
                debugLog(`Erreur lors de la déconnexion: ${result.message}`, 'error');
                alert(`Erreur lors de la déconnexion : ${result.message}`);
            }
        } else {
            const errorResult = await response.text();
            debugLog(`Erreur HTTP ${response.status}: ${errorResult}`, 'error');
            alert(`Erreur HTTP ${response.status} : ${errorResult}`);
        }
    } catch (error) {
        debugLog(`Exception lors de la déconnexion: ${error.message}`, 'error');
        alert(`Erreur lors de la déconnexion : ${error.message}`);
    }
}

// Réinitialisation du mot de passe d'un utilisateur
async function resetUserPassword(login, fullname) {
    if (serverStopped) {
        debugLog('Tentative de reset impossible - serveur arrêté', 'error');
        alert('Impossible de réinitialiser le mot de passe : le serveur est arrêté');
        return;
    }
    
    if (!confirm(`Êtes-vous sûr de vouloir réinitialiser le mot de passe de ${fullname} (${login}) ?\n\nLe nouveau mot de passe sera : ${login}`)) {
        return;
    }
    
    debugLog(`Tentative de reset du mot de passe pour ${login}`, 'info');
    
    try {
        const response = await fetch('/api/reset_password', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ login: login })
        });
        
        debugLog(`Reset Password API - Status: ${response.status}`, response.ok ? 'success' : 'error');
        
        if (response.ok) {
            const result = await response.json();
            if (result.status === 'success') {
                debugLog(`Mot de passe réinitialisé avec succès pour ${login}`, 'success');
                alert(`Mot de passe réinitialisé avec succès pour ${fullname} (${login})\nNouveau mot de passe : ${login}`);
            } else {
                debugLog(`Erreur lors de la réinitialisation: ${result.message}`, 'error');
                alert(`Erreur lors de la réinitialisation : ${result.message}`);
            }
        } else {
            const errorResult = await response.text();
            debugLog(`Erreur HTTP ${response.status}: ${errorResult}`, 'error');
            alert(`Erreur HTTP ${response.status} : ${errorResult}`);
        }
    } catch (error) {
        debugLog(`Exception lors de la réinitialisation: ${error.message}`, 'error');
        alert(`Erreur lors de la réinitialisation : ${error.message}`);
    }
}

// Modification de la fonction updateUsersTable pour inclure le bouton d'action
function updateUsersTable() {
    const tbody = document.querySelector('#usersTable tbody');
    if (!tbody || allData.users.length === 0) {
        if (tbody) {
            tbody.innerHTML = '<tr><td colspan="9" style="text-align: center; color: #7f8c8d;">Aucun utilisateur trouvé</td></tr>';
        }
        return;
    }
    
    const filteredUsers = filterUsers();
    const sortedUsers = applySortToData(filteredUsers, 'users');
    debugLog(`Affichage de ${sortedUsers.length} utilisateurs`, 'info');
    
    tbody.innerHTML = sortedUsers.map(user => {
        const statusClass = {
            'STUDENT': 'status-student',
            'TUTOR': 'status-tutor',
            'ADMIN': 'status-admin'
        }[user.status] || '';
        
        const groupsBadges = user.groups && user.groups !== '--' && user.groups.trim()
            ? user.groups.split(',').map(g => g.trim()).filter(g => g)
                .map(group => `<span class="badge">${group}</span>`).join(' ')
            : '';
        
        const escapedLogin = (user.login || '').replace(/'/g, "\\'");
        const escapedFullname = (user.fullname || user.login).replace(/'/g, "\\'");
        
        // Désactiver le bouton pour les admins et si le serveur est arrêté
        const isDisabled = user.status === 'ADMIN' || serverStopped;
        const disabledAttr = isDisabled ? 'disabled' : '';
        const title = user.status === 'ADMIN' ? 'title="Impossible de réinitialiser le mot de passe d\'un administrateur"' : '';
        
        return `<tr>
            <td><strong>${user.login}</strong></td>
            <td>${user.fullname || user.login}</td>
            <td><span class="${statusClass}">${user.status}</span></td>
            <td>${groupsBadges}</td>
            <td>${user.mail !== '--' ? user.mail : ''}</td>
            <td>${user.date !== '--' ? user.date : ''}</td>
            <td>${user.addr !== '--' ? user.addr : ''}</td>
            <td>${user.session_id !== '--' ? user.session_id : ''}</td>
            <td class="action-cell">
                <button class="btn btn-reset" 
                        onclick="resetUserPassword('${escapedLogin}', '${escapedFullname}')" 
                        ${disabledAttr} 
                        ${title}>
                    Reset
                </button>
            </td>
        </tr>`;
    }).join('');
}

// Gestion de l'arrêt du serveur
function handleServerStopped() {
    serverStopped = true;
    stopAutoRefresh();
    
    document.getElementById('totalConnections').textContent = '0';
    document.getElementById('studentsCount').textContent = '0';
    document.getElementById('tutorsCount').textContent = '0';
    document.getElementById('activeSessions').textContent = '0';
    
    updateClientsTable();
    
    document.getElementById('serverStoppedBanner').style.display = 'block';
    document.getElementById('connectionStatus').textContent = '🔴 Serveur arrêté';
    document.getElementById('lastRefresh').textContent = 'Serveur arrêté le: ' + new Date().toLocaleTimeString('fr-FR');
    
    document.getElementById('refreshBtn').disabled = true;
    document.getElementById('shutdownBtn').disabled = true;
    updateAutoRefreshButton();
    
    allData.clients = [];
    debugLog('Serveur marqué comme arrêté', 'error');
}

// Mise à jour du timestamp
function updateLastRefresh() {
    const now = new Date();
    const timeStr = now.toLocaleTimeString('fr-FR');
    document.getElementById('lastRefresh').textContent = `Dernière mise à jour: ${timeStr}`;
    document.getElementById('connectionStatus').textContent = '🟢 Serveur actif';
}

// Actions d'administration
function confirmShutdown() {
    if (serverStopped) return;
    
    if (confirm('Êtes-vous sûr de vouloir arrêter le serveur ?')) {
        debugLog('Envoi de la commande d\'arrêt du serveur', 'warning');
        fetch('/api/shutdown', { method: 'POST' })
        .then(response => {
            debugLog(`Shutdown API - Status: ${response.status}`, response.ok ? 'success' : 'error');
            if (response.ok) {
                alert('Commande d\'arrêt envoyée au serveur');
                setTimeout(() => handleServerStopped(), 2000);
            } else {
                throw new Error('Erreur HTTP');
            }
        })
        .catch(error => {
            debugLog(`Erreur lors de l\'arrêt: ${error.message}`, 'error');
            alert('Erreur lors de l\'arrêt du serveur: ' + error);
        });
    }
}

// Test Dashboard Live
function testDashboardLive() {
    debugLog('Test Dashboard Live démarré', 'info');
    updateDashboard();
}

// Initialisation
document.addEventListener('DOMContentLoaded', function() {
    debugLog('=== INITIALISATION DE L\'INTERFACE ===', 'info');
    debugLog('Configuration des onglets...', 'info');
    setupTabs();
    debugLog('Configuration des filtres...', 'info');
    setupFilters();
    debugLog('Configuration du tri des tableaux...', 'info');
    setupTableSorting();
    debugLog('Mise à jour du timestamp initial...', 'info');
    updateLastRefresh();
    
    debugLog('Démarrage de l\'auto-refresh...', 'info');
    startAutoRefresh();
    
    debugLog('Premier chargement du dashboard...', 'info');
    updateDashboard();
    
    debugLog('Interface initialisée avec auto-refresh activé', 'success');
});