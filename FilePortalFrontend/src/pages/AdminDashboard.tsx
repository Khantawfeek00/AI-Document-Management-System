import React, { useEffect, useState } from 'react';
import { userApi, fileApi } from '../services/api';
import UserTable from '../components/UserTable';
import ConfirmDialog from '../components/ConfirmDialog';
import AlertDialog from '../components/AlertDialog';
import ManageUserDialog from '../components/ManageUserDialog';
import CreateUserDialog from '../components/CreateUserDialog';

export const AdminDashboard: React.FC = () => {
    const [users, setUsers] = useState<any[]>([]);
    const [fileStats, setFileStats] = useState<{ count: number; totalSize: number }>({ count: 0, totalSize: 0 });
    const [loadingUsers, setLoadingUsers] = useState(false);
    const [loadingStats, setLoadingStats] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [roleOptions, setRoleOptions] = useState<string[]>([]);

    const [search, setSearch] = useState('');
    const [alertOpen, setAlertOpen] = useState(false);
    const [alertMessage, setAlertMessage] = useState('');

    const [manageOpen, setManageOpen] = useState(false);
    const [manageUser, setManageUser] = useState<any | null>(null);
    const [deleteConfirmOpen, setDeleteConfirmOpen] = useState(false);
    const [deleteTargetId, setDeleteTargetId] = useState<string | null>(null);

    const [createOpen, setCreateOpen] = useState(false);

    const fetchUsers = async (q?: string) => {
        setLoadingUsers(true);
        setError(null);
        try {
            const u = await userApi.listUsers(q);
            const normalized = Array.isArray(u)
                ? u
                : Array.isArray(u?.content)
                    ? u.content
                    : Array.isArray(u?.data)
                        ? u.data
                        : [];

            setUsers(normalized);
        } catch (e: any) {
            setError(e?.message || 'Failed to load users');
            setUsers([]);
        } finally {
            setLoadingUsers(false);
        }
    };

    useEffect(() => {
        const t = setTimeout(() => {
            fetchUsers(search || undefined);
        }, 300);
        return () => clearTimeout(t);
    }, [search]);

    const fetchFileStats = async () => {
        setLoadingStats(true);
        setError(null);
        try {
            const filesPaged = await fileApi.listFiles();
            const items = filesPaged?.content || [];
            const count = items.length;
            const totalSize = items.reduce((acc: number, f: any) => acc + (f.size || 0), 0);
            setFileStats({ count, totalSize });
        } catch (e: any) {
            setError(e?.message || 'Failed to load file stats');
        } finally {
            setLoadingStats(false);
        }
    };

    useEffect(() => {
        fetchUsers();
        fetchFileStats();
        fetchRoleOptions();
    }, []);

    const fetchRoleOptions = async () => {
        try {
            const roles = await userApi.getRoleOptions();
            if (Array.isArray(roles) && roles.length > 0) {
                setRoleOptions(roles);
            } else {
                setRoleOptions([]);
            }
        } catch (e) {
            console.warn('fetchRoleOptions failed', e);
            setRoleOptions([]);
        }
    };

    const formatSize = (bytes: number) => {
        if (!bytes) return '0 B';
        const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
        const i = Math.floor(Math.log(bytes) / Math.log(1024));
        return `${Math.round((bytes / Math.pow(1024, i)) * 100) / 100} ${sizes[i]}`;
    };

    // --- Manage / Edit user flow ---
    const openManage = (u: any) => {
        setManageUser(u);
        setManageOpen(true);
    };

    const closeManage = () => {
        setManageOpen(false);
        setManageUser(null);
    };

    const handleManageSave = async (id: string, payload: any) => {
        try {
            const updated = await userApi.updateUser(id, payload);
            setUsers((prev) => prev.map((u) => (u.id === id || u._id === id ? { ...u, ...updated } : u)));
            setAlertMessage('User updated');
            setAlertOpen(true);
            closeManage();
        } catch (e: any) {
            setAlertMessage(e.message = 'Failed to update user');
            setAlertOpen(true);
        }
    };

    const handleDeleteRequest = (id: string) => {
        setDeleteTargetId(id);
        setDeleteConfirmOpen(true);
    };

    const handleDeleteConfirmed = async () => {
        if (!deleteTargetId) return;
        try {
            await userApi.deleteUser(deleteTargetId);
            setUsers((prev) => prev.filter((u) => u.id !== deleteTargetId && u._id !== deleteTargetId));
            setAlertMessage('User deleted');
            setAlertOpen(true);
            setDeleteConfirmOpen(false);
            closeManage();
        } catch (e: any) {
            setAlertMessage(e.message = 'Failed to delete user');
            setAlertOpen(true);
        }
    };

    const handleShowDetails = (u: any) => {
        const fullName =
            u?.fullName ||
            u?.name ||
            (u?.firstName || u?.givenName ? `${u.firstName || u.givenName}${u.lastName ? ' ' + u.lastName : ''}` : null) ||
            u?.displayName ||
            u?.username ||
            'N/A';

        const email = u?.email || u?.emailAddress || u?.mail || 'N/A';
        const organization = u?.organization || u?.org || u?.company || 'N/A';
        const role = u?.role || (Array.isArray(u?.roles) ? u.roles[0] : u?.title) || 'N/A';
        const department = u?.department || u?.dept || 'N/A';

        const createdRaw = u?.createdAt || u?.created_at || u?.created || u?.createdOn || u?.created_on;
        const updatedRaw = u?.updatedAt || u?.updated_at || u?.updated || u?.updatedOn || u?.updated_on;

        const formatDate = (v: any) => {
            if (!v) return 'N/A';
            const d = typeof v === 'string' || typeof v === 'number' ? new Date(v) : v instanceof Date ? v : null;
            return d && !isNaN(d.getTime()) ? d.toLocaleString() : String(v);
        };

        const message = [
            `Full Name: ${fullName}, `,
            `Email: ${email}, `,
            `Organization: ${organization}, `,
            `Role: ${role}, `,
            `Department: ${department}, `,
            `Created at: ${formatDate(createdRaw)}, `,
            `Updated at: ${formatDate(updatedRaw)}`
        ].join('\n');

        setAlertMessage(message);
        setAlertOpen(true);
    };

    const handleCreateUser = async (payload: any) => {
        try {
            const created = await userApi.createUser(payload);
            setUsers((prev) => [created, ...prev]);
            setAlertMessage('User created');
            setAlertOpen(true);
            setCreateOpen(false);
        } catch (e: any) {
            setAlertMessage(e.message = 'Failed to create user');
            setAlertOpen(true);
        }
    };

    return (
        <div style={{ padding: '2rem' }}>
            <h1>Admin Dashboard</h1>

            {error && <div style={{ marginBottom: '1rem', color: 'red' }}>Error: {error}</div>}

            <section style={{ display: 'flex', gap: '1rem', marginBottom: '1.5rem' }}>
                <div style={{ flex: 1, padding: '1rem', background: '#fff', borderRadius: 8 }}>
                    <h3>File Statistics</h3>
                    {loadingStats ? (
                        <p>Loading...</p>
                    ) : (
                        <>
                            <p><strong>Total files:</strong> {fileStats.count}</p>
                            <p><strong>Total storage:</strong> {formatSize(fileStats.totalSize)}</p>
                            <div style={{ marginTop: '0.5rem' }}>
                                <button onClick={fetchFileStats} style={{ marginRight: '0.5rem' }}>Refresh</button>
                            </div>
                        </>
                    )}
                </div>

                <div style={{ flex: 1, padding: '1rem', background: '#fff', borderRadius: 8 }}>
                    <h3>User Management</h3>
                    <p style={{ marginTop: 0 }}>Quick actions for administrators</p>
                    <div style={{ marginTop: '0.5rem' }}>
                        <button onClick={() => fetchUsers()} style={{ marginRight: '0.5rem' }}>Refresh Users</button>
                        <button onClick={() => setCreateOpen(true)} style={{ marginLeft: 8 }}>Add New User</button>
                    </div>
                </div>
            </section>

            <section style={{ background: '#fff', borderRadius: 8, padding: '1rem' }}>
                <h3>All Users</h3>

                <div style={{ display: 'flex', gap: 8, marginBottom: 12 }}>
                    <input
                        placeholder="Search users by name/email/role..."
                        value={search}
                        onChange={(e) => setSearch(e.target.value)}
                        style={{ flex: 1, padding: '0.5rem', borderRadius: 4, border: '1px solid #ddd' }}
                    />
                    <button onClick={() => fetchUsers(search || undefined)}>Search</button>
                </div>

                {loadingUsers ? (
                    <p>Loading users...</p>
                ) : (
                    <UserTable users={users} onEdit={handleShowDetails} onManage={openManage} highlight={search} loading={loadingUsers} />
                )}
            </section>

            <ManageUserDialog
                open={manageOpen}
                user={manageUser}
                users={users}
                onClose={closeManage}
                onSave={handleManageSave}
                onDeleteRequest={handleDeleteRequest}
                roleOptions={roleOptions}
            />

            <ConfirmDialog
                open={deleteConfirmOpen}
                title="Delete user"
                message="Are you sure you want to permanently delete this user?"
                confirmText="Delete"
                cancelText="Cancel"
                onConfirm={handleDeleteConfirmed}
                onCancel={() => setDeleteConfirmOpen(false)}
            />

            <CreateUserDialog
                users={users}
                open={createOpen}
                onClose={() => setCreateOpen(false)}
                onCreate={handleCreateUser}
                roleOptions={roleOptions}
            />

            <AlertDialog open={alertOpen} message={alertMessage} onClose={() => setAlertOpen(false)} />
        </div>
    );
};
