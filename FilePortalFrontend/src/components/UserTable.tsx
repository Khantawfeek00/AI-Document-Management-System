import React, { useMemo, useState } from 'react';

interface UserTableProps {
    users: any[] | any;
    onEdit: (u: any) => void;
    onDeactivate?: (id: string) => void;
    onManage?: (u: any) => void;
    highlight?: string;
    loading?: boolean;
}

export const UserTable: React.FC<UserTableProps> = ({ users, onEdit, onManage, highlight, loading = false }) => {
    const [hovered, setHovered] = useState<string | null>(null);

    const normalizedUsers = useMemo(() => {
        if (!users) return [] as any[];
        if (Array.isArray(users)) return users as any[];
        if (users.content && Array.isArray(users.content)) return users.content as any[];
        if (users.data && Array.isArray(users.data)) return users.data as any[];
        return [] as any[];
    }, [users]);

    console.log("Users:", users);

    const filtered = useMemo(() => {
        if (!highlight) return normalizedUsers;
        const q = highlight.toLowerCase();
        return (normalizedUsers || []).filter((u) => {
            const name = String(getDisplayName(u)).toLowerCase();
            const email = String(u.email || '').toLowerCase();
            const roles = (Array.isArray(u.roles) ? u.roles.join(',') : String(u.role || u.roles || '')).toLowerCase();
            return name.includes(q) || email.includes(q) || roles.includes(q);
        });
    }, [normalizedUsers, highlight]);

    function getDisplayName(u: any): string {
        return u.fullName || u.name || u.username || u.displayName || u.firstName || u.givenName || '';
    }

    const highlightText = (text: string) => {
        if (!highlight) return text;
        const idx = text.toLowerCase().indexOf(highlight.toLowerCase());
        if (idx === -1) return text;
        return (
            <span>
                {text.substring(0, idx)}
                <mark style={{ backgroundColor: 'var(--highlight, #fff176)', padding: '0 2px' }}>{text.substring(idx, idx + highlight.length)}</mark>
                {text.substring(idx + highlight.length)}
            </span>
        );
    };

    const renderSkeleton = () => {
        const rows = new Array(6).fill(0);
        return rows.map((_, rIdx) => (
            <tr key={`skeleton-${rIdx}`} style={styles.tr}>
                <td style={styles.td}><div style={styles.skeletonBox} /></td>
                <td style={styles.td}><div style={{ ...styles.skeletonBox, width: '70%' }} /></td>
                <td style={styles.td}><div style={{ ...styles.skeletonBox, width: '60%' }} /></td>
                <td style={styles.td}><div style={{ ...styles.skeletonBox, width: '40%' }} /></td>
                <td style={{ ...styles.td, textAlign: 'center' }}><div style={{ ...styles.skeletonBox, width: '60px', height: 32, margin: '0 auto' }} /></td>
            </tr>
        ));
    };

    return (
        <div style={{ overflowX: 'auto' }}>
            <table style={styles.table}>
                <thead>
                <tr>
                    <th style={styles.th}>Name</th>
                    <th style={styles.th}>Email</th>
                    <th style={styles.th}>Roles</th>
                    <th style={styles.th}>Department</th>
                    <th style={{ ...styles.th, textAlign: 'center' }}>Actions</th>
                </tr>
                </thead>
                <tbody>
                {loading ? (
                    renderSkeleton()
                ) : (
                    <>
                        {filtered.map((u, idx) => {
                            const id = u.id || u._id || u.userId || String(idx);
                            const rolesText = Array.isArray(u.roles)
                                ? u.roles.join(', ')
                                : (typeof u.roles === 'string' && u.roles.trim() !== '')
                                    ? u.roles
                                    : (u.role || u.title || '');

                            const nameText = getDisplayName(u);

                            // add department field
                            const departmentText = u.department || u.dept || '';

                            return (
                                <tr
                                    key={id}
                                    style={{
                                        ...styles.tr,
                                        backgroundColor: hovered === id ? '#f5f9ff' : idx % 2 === 0 ? '#ffffff' : '#fbfbfb',
                                    }}
                                    onMouseEnter={() => setHovered(id)}
                                    onMouseLeave={() => setHovered(null)}
                                >
                                    <td style={styles.td}>{highlightText(String(nameText || '-'))}</td>
                                    <td style={styles.td}>{highlightText(String(u.email || '-'))}</td>
                                    <td style={styles.td}>{highlightText(String(rolesText || '-'))}</td>
                                    <td style={styles.td}>{highlightText(String(departmentText || '-'))}</td>
                                    <td style={{ ...styles.td, textAlign: 'center' }}>
                                        <button onClick={() => onEdit(u)} style={{ ...styles.actionButton, marginRight: 8 }}>Details</button>
                                        {onManage && <button onClick={() => onManage(u)} style={{ ...styles.actionButton }}>Manage</button>}
                                    </td>
                                </tr>
                            );
                        })}

                        {!loading && filtered.length === 0 && (
                            <tr>
                                <td colSpan={5} style={{ padding: '1rem', textAlign: 'center', color: '#666' }}>
                                    {highlight ? 'No users match your search' : 'No users available'}
                                </td>
                            </tr>
                        )}
                    </>
                )}
                </tbody>
            </table>
        </div>
    );
};

const styles: { [key: string]: React.CSSProperties } = {
    table: {
        width: '100%',
        borderCollapse: 'separate',
        borderSpacing: 0,
        background: '#fff',
        boxShadow: '0 1px 4px rgba(0,0,0,0.04)',
        borderRadius: 8,
        overflow: 'hidden',
    },
    th: {
        textAlign: 'left',
        padding: '0.75rem',
        borderBottom: '2px solid #f0f0f0',
        background: '#fafafa',
        position: 'sticky',
        top: 0,
        fontSize: '0.95rem',
        color: '#333',
    },
    tr: {
        borderBottom: '1px solid #f5f5f5',
        transition: 'background-color 0.12s ease',
    },
    td: {
        padding: '0.75rem',
        verticalAlign: 'middle',
        fontSize: '0.95rem',
        color: '#333',
    },
    actionButton: {
        padding: '0.45rem 0.75rem',
        background: '#1976d2',
        color: 'white',
        border: 'none',
        borderRadius: 4,
        cursor: 'pointer',
        fontSize: '0.9rem',
    },
    skeletonBox: {
        height: 12,
        backgroundColor: '#eee',
        borderRadius: 4,
        width: '100%',
    },
};

export default UserTable;
