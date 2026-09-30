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
    sessions: [],
    groups: []
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
    if (!debugDiv) return;
    const timestamp = new Date().toLocaleTimeString();
    const cssClass = type === 'error' ? 'debug-error' : 
                    type === 'success' ? 'debug-success' : 
                    type === 'warning' ? 'debug-warning' : '';
    
    const logLine = document.createElement('div');
    logLine.className = `debug-line ${cssClass}`;
    logLine.textContent = `[${timestamp}] ${message}`;
    
    debugDiv.appendChild(logLine);
    const container = document.getElementById('debugContent') || debugDiv;
    container.scrollTop = container.scrollHeight;
    
    // Limiter à 50 lignes
    while (debugDiv.children.length > 50) {
        debugDiv.removeChild(debugDiv.firstChild);
    }
}

// Gestion de la console de debug (refermable)
function toggleDebugConsole(event) {
    if (event) {
        event.stopPropagation();
    }
    const debugInfo = document.getElementById('debugInfo');
    const toggleIcon = document.getElementById('debugToggleIcon');
    const toggleText = document.getElementById('debugToggleText');
    if (!debugInfo) return;

    const isCollapsed = debugInfo.classList.toggle('collapsed');
    if (toggleIcon) toggleIcon.textContent = isCollapsed ? '▶' : '▼';
    if (toggleText) toggleText.textContent = isCollapsed ? 'Afficher' : 'Réduire';

    try {
        localStorage.setItem('codelab_admin_debug_collapsed', isCollapsed ? 'true' : 'false');
    } catch (_) {}
}

function clearDebugLog(event) {
    if (event) {
        event.stopPropagation();
    }
    const debugDiv = document.getElementById('debugLog');
    if (debugDiv) {
        debugDiv.innerHTML = '';
    }
}

function initDebugConsole() {
    try {
        const isCollapsed = localStorage.getItem('codelab_admin_debug_collapsed') === 'true';
        if (isCollapsed) {
            const debugInfo = document.getElementById('debugInfo');
            const toggleIcon = document.getElementById('debugToggleIcon');
            const toggleText = document.getElementById('debugToggleText');
            if (debugInfo) debugInfo.classList.add('collapsed');
            if (toggleIcon) toggleIcon.textContent = '▶';
            if (toggleText) toggleText.textContent = 'Afficher';
        }
    } catch (_) {}
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

            // Rechargement automatique de la liste selon l'onglet cliqué
            if (targetTab === 'users') {
                loadUsersData();
            } else if (targetTab === 'sessions') {
                loadSessionsData();
            } else if (targetTab === 'clients') {
                updateDashboard();
            } else if (targetTab === 'dashboard') {
                updateDashboard();
            }
        });
    });
}

// Chargement des données utilisateurs
async function loadUsersData() {
    debugLog('Chargement des données utilisateurs...', 'info');
    const tbody = document.querySelector('#usersTable tbody');
    if (tbody && (!allData.users || allData.users.length === 0)) {
        tbody.innerHTML = '<tr><td colspan="9" style="text-align: center; padding: 20px;">🔄 Chargement...</td></tr>';
    }
    
    // Charger également les groupes en arrière-plan
    loadGroupsData();

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
                tbody.innerHTML = '<tr><td colspan="9" style="text-align: center; color: #e74c3c;">⌘ Erreur lors du chargement</td></tr>';
            }
        }
    } catch (error) {
        debugLog(`Exception Users: ${error.message}`, 'error');
        if (tbody) {
            tbody.innerHTML = '<tr><td colspan="9" style="text-align: center; color: #e74c3c;">⌘ Erreur de connexion</td></tr>';
        }
    }
}

// Chargement des données sessions
async function loadSessionsData() {
    debugLog('Chargement des données sessions...', 'info');
    const tbody = document.querySelector('#sessionsTable tbody');
    if (tbody && (!allData.sessions || allData.sessions.length === 0)) {
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
        const statusText = session.openned ? 'Ouverte' : 'Fermée';
        
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
        
        const isStudent = user.status === 'STUDENT';
        const groupBtnHtml = isStudent ? `
            <button class="btn btn-group-change" 
                    onclick="openChangeGroupModal('${escapedLogin}')" 
                    ${serverStopped ? 'disabled' : ''} 
                    title="Changer le groupe de cet étudiant">
                Groupe
            </button>` : '';

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
                ${groupBtnHtml}
            </td>
        </tr>`;
    }).join('');
}

// ============================================================================
// Gestion de la modale de changement de groupe d'un étudiant (Option A)
// ============================================================================

let currentModalStudent = null;
let currentSimulation = null;

// Chargement de la liste des groupes
async function loadGroupsData() {
    try {
        const response = await fetch('/api/groups');
        if (response.ok) {
            allData.groups = await response.json();
            debugLog(`${allData.groups.length} groupes disponibles chargés`, 'info');
        } else {
            debugLog(`Erreur lors du chargement des groupes: HTTP ${response.status}`, 'error');
        }
    } catch (e) {
        debugLog(`Exception lors du chargement des groupes: ${e.message}`, 'error');
    }
}

// Ouvre la boîte de dialogue modale pour un étudiant
async function openChangeGroupModal(login) {
    debugLog(`Ouverture de la modale de changement de groupe pour: ${login}`, 'info');
    
    // Rechercher l'étudiant dans les données chargées
    const student = allData.users.find(u => u.login === login);
    if (!student) {
        alert(`Étudiant avec le login '${login}' introuvable.`);
        return;
    }
    currentModalStudent = student;
    currentSimulation = null;

    // S'assurer que les groupes sont chargés
    if (!allData.groups || allData.groups.length === 0) {
        await loadGroupsData();
    }

    // Mettre à jour les informations textuelles de l'étudiant
    document.getElementById('modalStudentName').textContent = student.fullname || student.login;
    document.getElementById('modalStudentLogin').textContent = student.login;

    // Déterminer la liste des groupes actuels de l'étudiant
    let studentGroups = [];
    if (student.groups && student.groups !== '--') {
        studentGroups = student.groups.split(',').map(g => g.trim()).filter(g => g);
    }
    
    // Badges de groupes actuels
    const groupsBadges = studentGroups.length > 0 
        ? studentGroups.map(g => `<span class="badge">${g}</span>`).join(' ')
        : '<em>Aucun groupe assigné</em>';
    document.getElementById('modalCurrentGroups').innerHTML = groupsBadges;

    // Remplir le sélecteur du groupe d'origine à remplacer
    const oldGroupSelect = document.getElementById('oldGroupSelect');
    oldGroupSelect.innerHTML = '';
    studentGroups.forEach(g => {
        const opt = document.createElement('option');
        opt.value = g;
        opt.textContent = g;
        oldGroupSelect.appendChild(opt);
    });

    // Remplir le sélecteur du nouveau groupe cible
    const newGroupSelect = document.getElementById('newGroupSelect');
    newGroupSelect.innerHTML = '';
    (allData.groups || []).forEach(g => {
        const opt = document.createElement('option');
        opt.value = g;
        opt.textContent = g;
        newGroupSelect.appendChild(opt);
    });

    // Sélectionner par défaut le premier groupe cible différent du groupe actuel
    if (oldGroupSelect.value && newGroupSelect.options.length > 0) {
        for (let i = 0; i < newGroupSelect.options.length; i++) {
            if (newGroupSelect.options[i].value !== oldGroupSelect.value) {
                newGroupSelect.selectedIndex = i;
                break;
            }
        }
    }

    // Réinitialiser la zone de simulation
    document.getElementById('simulationContainer').style.display = 'none';
    document.getElementById('applyGroupChangeBtn').disabled = true;

    // Afficher la modale
    document.getElementById('groupChangeModal').style.display = 'flex';

    // Déclencher la simulation immédiate
    await onGroupSelectionChange();
}

// Ferme la modale
function closeChangeGroupModal() {
    document.getElementById('groupChangeModal').style.display = 'none';
    currentModalStudent = null;
    currentSimulation = null;
}

// Appelé lors d'un changement de sélection dans les listes déroulantes de groupes
async function onGroupSelectionChange() {
    if (!currentModalStudent) return;

    const oldGroup = document.getElementById('oldGroupSelect').value;
    const newGroup = document.getElementById('newGroupSelect').value;
    const alertBox = document.getElementById('simulationAlert');
    const applyBtn = document.getElementById('applyGroupChangeBtn');
    const container = document.getElementById('simulationContainer');
    const loading = document.getElementById('simulationLoading');

    if (!oldGroup || !newGroup) {
        container.style.display = 'none';
        applyBtn.disabled = true;
        return;
    }

    if (oldGroup === newGroup) {
        container.style.display = 'block';
        alertBox.className = 'alert-box alert-warning';
        alertBox.textContent = '⚠️ Le nouveau groupe doit être différent du groupe d\'origine.';
        alertBox.style.display = 'block';
        document.getElementById('unchangedSessionsSection').style.display = 'none';
        document.getElementById('movesSection').style.display = 'none';
        document.getElementById('unmatchedArrivedSection').style.display = 'none';
        applyBtn.disabled = true;
        return;
    }

    loading.style.display = 'block';
    container.style.display = 'none';
    applyBtn.disabled = true;

    try {
        const response = await fetch('/api/students/simulate_group_change', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                login: currentModalStudent.login,
                old_group: oldGroup,
                new_group: newGroup
            })
        });

        loading.style.display = 'none';
        container.style.display = 'block';

        if (response.ok) {
            const sim = await response.json();
            currentSimulation = sim;
            renderSimulationResults(sim);
        } else {
            const err = await response.text();
            alertBox.className = 'alert-box alert-danger';
            alertBox.textContent = `Erreur lors de la simulation : ${err}`;
            alertBox.style.display = 'block';
            document.getElementById('unchangedSessionsSection').style.display = 'none';
            document.getElementById('movesSection').style.display = 'none';
            document.getElementById('unmatchedArrivedSection').style.display = 'none';
        }
    } catch (e) {
        loading.style.display = 'none';
        container.style.display = 'block';
        alertBox.className = 'alert-box alert-danger';
        alertBox.textContent = `Exception réseau : ${e.message}`;
        alertBox.style.display = 'block';
    }
}

// Affiche les résultats de la simulation
function renderSimulationResults(sim) {
    const alertBox = document.getElementById('simulationAlert');
    const applyBtn = document.getElementById('applyGroupChangeBtn');

    // 1. Sessions inchangées
    const unchangedSection = document.getElementById('unchangedSessionsSection');
    const unchangedList = document.getElementById('unchangedSessionsList');
    if (sim.unchanged_sessions && sim.unchanged_sessions.length > 0) {
        unchangedList.innerHTML = sim.unchanged_sessions
            .map(s => `<span class="badge" style="background: #27ae60;">${s}</span>`)
            .join(' ');
        unchangedSection.style.display = 'block';
    } else {
        unchangedList.innerHTML = '<em>Aucune session partagée entre l\'ancien et le nouveau profil.</em>';
        unchangedSection.style.display = 'block';
    }

    // 2. Mouvements de dossiers
    const movesSection = document.getElementById('movesSection');
    const tbody = document.getElementById('movesTableBody');
    tbody.innerHTML = '';

    if (!sim.moves || sim.moves.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4" style="text-align: center; color: #7f8c8d; padding: 12px;">Aucun dossier serveur à déplacer.</td></tr>';
    } else {
        sim.moves.forEach((move, idx) => {
            const tr = document.createElement('tr');
            
            // Fichiers source
            const fileInfo = move.source_exists 
                ? `<strong>${move.source_file_count}</strong> fichier(s)` 
                : '<span style="color:#7f8c8d;">Dossier vide / non créé</span>';

            // Menu déroulant pour la session cible
            let selectOptions = '';
            sim.arrived_sessions.forEach(arrSess => {
                const selected = (move.to_session === arrSess) ? 'selected' : '';
                selectOptions += `<option value="${arrSess}" ${selected}>${arrSess}</option>`;
            });
            selectOptions += `<option value="" title="L'ancien dossier est conservé intact sur le serveur et un dossier vide sera créé dans la nouvelle session">-- Ne pas déplacer (créer nouveau dossier vide) --</option>`;

            // Statut cible
            let statusBadge = '';
            if (move.blocked) {
                statusBadge = `<span class="badge-blocked">Dossier existant (Bloqué)</span>`;
            } else if (move.to_session) {
                statusBadge = `<span class="badge-ready">Disponible</span>`;
            } else {
                statusBadge = `<span style="color: #e67e22;">Aucune session cible</span>`;
            }

            tr.innerHTML = `
                <td><strong>${move.from_session}</strong></td>
                <td>${fileInfo}</td>
                <td>
                    <select id="move_target_${idx}" onchange="onMoveTargetChange(${idx})">
                        ${selectOptions}
                    </select>
                </td>
                <td id="move_status_${idx}">${statusBadge}</td>
            `;
            tbody.appendChild(tr);
        });
    }

    // 3. Sessions arrivées sans dossier source
    const unmatchedSection = document.getElementById('unmatchedArrivedSection');
    const unmatchedList = document.getElementById('unmatchedArrivedList');
    const targetSessionsInMoves = (sim.moves || []).map(m => m.to_session).filter(Boolean);
    const orphanArrived = (sim.arrived_sessions || []).filter(s => !targetSessionsInMoves.includes(s));
    if (orphanArrived.length > 0) {
        unmatchedList.innerHTML = orphanArrived.map(s => `<strong>${s}</strong>`).join(', ');
        unmatchedSection.style.display = 'block';
    } else {
        unmatchedSection.style.display = 'none';
    }

    // 4. Alertes et validation du bouton
    if (!sim.can_apply) {
        alertBox.className = 'alert-box alert-danger';
        alertBox.textContent = `🛑 Opération bloquée : ${sim.block_reason || 'Un dossier existe déjà à destination.'}`;
        alertBox.style.display = 'block';
        applyBtn.disabled = true;
    } else if (sim.is_connected) {
        alertBox.className = 'alert-box alert-warning';
        alertBox.textContent = `⚠️ L'étudiant est actuellement connecté au serveur. Il sera automatiquement déconnecté pour garantir l'intégrité du transfert.`;
        alertBox.style.display = 'block';
        applyBtn.disabled = false;
    } else {
        alertBox.style.display = 'none';
        applyBtn.disabled = false;
    }
}

// Appelé si l'administrateur change manuellement la session de destination d'un dossier
function onMoveTargetChange(moveIndex) {
    if (!currentSimulation) return;
    const selectElem = document.getElementById(`move_target_${moveIndex}`);
    const selectedTarget = selectElem.value;
    
    // Mettre à jour dans la simulation
    currentSimulation.moves[moveIndex].to_session = selectedTarget || null;

    const statusElem = document.getElementById(`move_status_${moveIndex}`);
    if (statusElem) {
        if (!selectedTarget) {
            statusElem.innerHTML = `<span style="color: #7f8c8d;">Non déplacé</span>`;
        } else {
            statusElem.innerHTML = `<span class="badge-ready">Disponible</span>`;
        }
    }

    // Recalculer les sessions orphelines
    const targetSessionsInMoves = currentSimulation.moves.map(m => m.to_session).filter(Boolean);
    const orphanArrived = (currentSimulation.arrived_sessions || []).filter(s => !targetSessionsInMoves.includes(s));
    const unmatchedSection = document.getElementById('unmatchedArrivedSection');
    const unmatchedList = document.getElementById('unmatchedArrivedList');
    if (orphanArrived.length > 0) {
        unmatchedList.innerHTML = orphanArrived.map(s => `<strong>${s}</strong>`).join(', ');
        unmatchedSection.style.display = 'block';
    } else {
        unmatchedSection.style.display = 'none';
    }
}

// Applique le changement de groupe
async function applyGroupChange() {
    if (!currentModalStudent || !currentSimulation) return;

    const oldGroup = document.getElementById('oldGroupSelect').value;
    const newGroup = document.getElementById('newGroupSelect').value;
    const applyBtn = document.getElementById('applyGroupChangeBtn');

    // Récupérer la liste des déplacements choisis
    const moves = [];
    if (currentSimulation.moves && currentSimulation.moves.length > 0) {
        for (let i = 0; i < currentSimulation.moves.length; i++) {
            const selectElem = document.getElementById(`move_target_${i}`);
            const fromSess = currentSimulation.moves[i].from_session;
            const toSess = selectElem ? selectElem.value : null;
            if (toSess) {
                moves.push({ from: fromSess, to: toSess });
            }
        }
    }

    const movesSummary = moves.length > 0 
        ? moves.map(m => `• ${m.from} ➔ ${m.to}`).join('\n')
        : '• Aucun dossier à déplacer';

    const confirmMsg = `Confirmez-vous le changement de groupe pour ${currentModalStudent.fullname || currentModalStudent.login} (${currentModalStudent.login}) ?\n\n` +
        `Remplacement : ${oldGroup} ➔ ${newGroup}\n\n` +
        `Déplacement des dossiers :\n${movesSummary}\n\n` +
        `Le fichier sessions.xml sera mis à jour avec une sauvegarde datée (.bak).`;

    if (!confirm(confirmMsg)) {
        return;
    }

    applyBtn.disabled = true;
    applyBtn.textContent = 'Transfert en cours...';

    try {
        const response = await fetch('/api/students/apply_group_change', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                login: currentModalStudent.login,
                old_group: oldGroup,
                new_group: newGroup,
                moves: moves
            })
        });

        const result = await response.json();

        if (response.ok && result.status === 'success') {
            debugLog(`Changement de groupe réussi pour ${currentModalStudent.login}: ${oldGroup} -> ${newGroup}`, 'success');
            alert(`✅ ${result.message}\n\n${moves.length} dossier(s) déplacé(s) sur le serveur.`);
            closeChangeGroupModal();
            // Recharger les données pour rafraîchir l'interface
            await loadUsersData();
            await loadSessionsData();
            await updateDashboard();
        } else {
            const errorMsg = result.message || 'Erreur inconnue';
            debugLog(`Erreur lors du changement de groupe: ${errorMsg}`, 'error');
            alert(`❌ Opération échouée :\n\n${errorMsg}`);
            applyBtn.disabled = false;
            applyBtn.textContent = 'Valider le transfert';
        }
    } catch (e) {
        debugLog(`Exception lors de l'application: ${e.message}`, 'error');
        alert(`❌ Erreur réseau : ${e.message}`);
        applyBtn.disabled = false;
        applyBtn.textContent = 'Valider le transfert';
    }
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
    initDebugConsole();
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