import React, {useEffect, useRef, useState} from 'react';

interface Props {
    open: boolean;
    user: any | null;
    users: any[];
    onClose: () => void;
    onSave: (id: string, updatedPayload: any) => Promise<any> | any;
    onDeleteRequest: (id: string) => void;
    roleOptions?: string[];
}

const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

const ManageUserDialog: React.FC<Props> = ({ open, user, users, onClose, onSave, onDeleteRequest, roleOptions }) => {
    const defaultRoles = roleOptions && roleOptions.length > 0 ? roleOptions : ['ADMIN', 'EDITOR', 'VIEWER'];
    const [fullName, setFullName] = useState('');
    const [email, setEmail] = useState('');
    const [organization, setOrganization] = useState('');
    const [role, setRole] = useState('');
    const [department, setDepartment] = useState('');
    const [saving, setSaving] = useState(false);
    const [errors, setErrors] = useState<{ email?: string; password?: string; general?: string }>({});
    const originalEmailRef = useRef<string>('');

    console.log("User roles", defaultRoles)
    console.log("role options", roleOptions)
    useEffect(() => {
        if (open && user) {
            setFullName(user.fullName || user.name || '');
            const initialEmail = user.email || user.emailAddress || '';
            setEmail(initialEmail);
            originalEmailRef.current = initialEmail.toLowerCase();

            setOrganization(user.organization || user.org || user.company || '');
            const initialRole =
                (Array.isArray(user.roles) ? user.roles[0] : user.role) ||
                user.title ||
                defaultRoles[0] ||
                '';
            setRole(initialRole);
            setDepartment(user.department || user.dept || '');
            setErrors({});
        }
        if (!open) {
            setSaving(false);
        }
    }, [open, user, roleOptions]);

    const validate = () => {
        const e: typeof errors = {};
        if (!email || !emailRegex.test(email)) {
            e.email = 'Email is not valid';
        } else {
            // check if the email is already used, but skip if unchanged for this user
            const emailLower = email.toLowerCase();
            const emailExists = users.some((u) => (u.email || u.emailAddress || '').toLowerCase() === emailLower);
            if (emailExists && emailLower !== originalEmailRef.current) {
                e.email = 'This email is already registered';
            }
        }

        setErrors(e);
        return Object.keys(e).length === 0;
    };

    const extractUserId = (u: any): string | undefined => {
        if (!u) return undefined;
        if (typeof u === 'string') return u;
        return (u.id ?? u._id ?? u.userId ?? u.kcId ?? u.idString) as string | undefined;
    };

    const handleSave = async () => {
        if (!user) return;
        if (!validate()) return;
        const id = extractUserId(user);
        if (!id) {
            setErrors({ general: 'Missing user id — cannot save' });
            return;
        }

        console.log("Saving user with id:", id);
        setSaving(true);
        setErrors({});
        const payload = {
            fullName: fullName || undefined,
            email: email || undefined,
            organization: organization || undefined,
            role: role || undefined,
            department: department || undefined,
        };
        try {
            await onSave(id, payload);
        } finally {
            setSaving(false);
        }
    };

    const handleDelete = () => {
        if (!user) return;
        onDeleteRequest(user.id || user._id);
    };

    if (!open || !user) return null;

    return (
        <div style={backdrop}>
            <div style={modal}>
                <h3>Manage user</h3>
                <div style={{ display: 'grid', gap: 8 }}>
                    <label>
                        Full Name
                        <input value={fullName} onChange={(e) => setFullName(e.target.value)} style={inputStyle} />
                    </label>
                    <label>
                        Email
                        <input value={email} onChange={(e) => setEmail(e.target.value)} style={inputStyle} />
                        {errors.email && <div style={{ color: 'red', marginTop: 4 }}>{errors.email}</div>}
                    </label>
                    <label>
                        Organization
                        <input value={organization} onChange={(e) => setOrganization(e.target.value)} style={inputStyle} />
                    </label>
                    <label>
                        Role
                        <select value={role} onChange={(e) => setRole(e.target.value)} style={{ ...inputStyle, height: 36 }}>
                            {defaultRoles.map((r) => (
                                <option key={r} value={r}>
                                    {r}
                                </option>
                            ))}
                        </select>
                    </label>
                    <label>
                        Department
                        <input value={department} onChange={(e) => setDepartment(e.target.value)} style={inputStyle} />
                    </label>
                </div>

                {errors.general && <div style={{ color: 'red', marginTop: 12 }}>{errors.general}</div>}

                <div style={{ marginTop: 12, display: 'flex', justifyContent: 'space-between' }}>
                    <div>
                        <button onClick={handleDelete} style={{ ...dangerButton }}>Delete</button>
                    </div>
                    <div>
                        <button onClick={onClose} style={{ marginRight: 8 }}>Cancel</button>
                        <button onClick={handleSave} style={{ ...primaryButton }} disabled={saving}>{saving ? 'Saving...' : 'Save'}</button>
                    </div>
                </div>
            </div>
        </div>
    );
};

const backdrop: React.CSSProperties = {
    position: 'fixed', inset: 0, backgroundColor: 'rgba(0,0,0,0.3)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 1000
};

const modal: React.CSSProperties = {
    width: 520, background: '#fff', padding: 16, borderRadius: 8, boxShadow: '0 6px 20px rgba(0,0,0,0.12)'
};

const inputStyle: React.CSSProperties = { width: '100%', padding: '0.5rem', marginTop: 4, border: '1px solid #ddd', borderRadius: 4 };

const primaryButton: React.CSSProperties = { padding: '0.5rem 0.75rem', background: '#1976d2', color: '#fff', border: 'none', borderRadius: 4, cursor: 'pointer' };
const dangerButton: React.CSSProperties = { padding: '0.45rem 0.75rem', background: '#d32f2f', color: '#fff', border: 'none', borderRadius: 4, cursor: 'pointer' };

export default ManageUserDialog;
