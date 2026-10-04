import {useState, useEffect } from "react";

interface CreateUserDialogProps {
    users: any[];
    open: boolean;
    onClose: () => void;
    onCreate: (dto: any) => void;
    roleOptions: string[];
}

const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

const CreateUserDialog: React.FC<CreateUserDialogProps> = ({ users, open, onClose, onCreate, roleOptions }) => {
    const [email, setEmail] = useState('');
    const [fullName, setFullName] = useState('');
    const [password, setPassword] = useState('');
    const [organization, setOrganization] = useState('');
    const [department, setDepartment] = useState('');
    const [role, setRole] = useState<string>(roleOptions?.[0] || '');
    const [submitting, setSubmitting] = useState(false);
    const [errors, setErrors] = useState<{ email?: string; password?: string; general?: string }>({});

    useEffect(() => {
        if (open) {
            setEmail('');
            setFullName('');
            setPassword('');
            setOrganization('');
            setDepartment('');
            setRole(roleOptions?.[0] || '');
            setErrors({});
        }
    }, [open, roleOptions]);

    const validate = () => {
        const e: typeof errors = {};
        if (!email || !emailRegex.test(email)) e.email = 'Email is not valid';

        // check if the email is already used
        const emailLower = email.toLowerCase();
        const emailExists = users.some((u) => (u.email || u.emailAddress || '').toLowerCase() === emailLower);
        if (emailExists) e.email = 'This email is already registered';

        setErrors(e);
        return Object.keys(e).length === 0;
    };

    const handleSubmit = async (e?: React.FormEvent) => {
        e?.preventDefault();
        if (!validate()) return;
        /*if (!email) {
            alert('Email required');
            return;
        }*/
        setSubmitting(true);
        setErrors({});

        const dto = {
            email,
            fullName,
            password: password || undefined,
            organization: organization || undefined,
            department: department || undefined,
            role: role || undefined,
        };
        try {
            await onCreate(dto);
            onClose();
        } catch (err: any) {
            setErrors({ general: err.message || 'Failed to create user' });
        } finally {
            setSubmitting(false);
        }
    };

    if (!open) return null;

    return (
        <div style={{
            position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.35)', display: 'flex',
            alignItems: 'center', justifyContent: 'center', zIndex: 1000
        }}>
            <div style={{ width: 640, background: '#fff', borderRadius: 8, padding: '1rem' }}>
                <h3>New User</h3>
                <form onSubmit={handleSubmit}>
                    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8 }}>
                        <label style={{ display: 'flex', flexDirection: 'column' }}>
                            Email*
                            <input value={email} onChange={(e) => setEmail(e.target.value)} style={{ padding: 8, marginTop: 4 }} disabled={submitting} />
                            {errors.email && <div style={{ color: 'red', marginTop: 4 }}>{errors.email}</div>}

                        </label>

                        <label style={{ display: 'flex', flexDirection: 'column' }}>
                            Full name
                            <input value={fullName} onChange={(e) => setFullName(e.target.value)} style={{ padding: 8, marginTop: 4 }} />
                        </label>

                        <label style={{ display: 'flex', flexDirection: 'column' }}>
                            Password
                            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} style={{ padding: 8, marginTop: 4 }} />
                        </label>

                        <label style={{ display: 'flex', flexDirection: 'column' }}>
                            Organization
                            <input value={organization} onChange={(e) => setOrganization(e.target.value)} style={{ padding: 8, marginTop: 4 }} />
                        </label>

                        <label style={{ display: 'flex', flexDirection: 'column' }}>
                            Department
                            <input value={department} onChange={(e) => setDepartment(e.target.value)} style={{ padding: 8, marginTop: 4 }} />
                        </label>

                        <label style={{ display: 'flex', flexDirection: 'column' }}>
                            Role
                            <select value={role} onChange={(e) => setRole(e.target.value)} style={{ padding: 8, marginTop: 4 }}>
                                <option value="">-- select role --</option>
                                {(roleOptions || []).map((r) => <option key={r} value={r}>{r}</option>)}
                            </select>
                        </label>
                    </div>

                    {errors.general && <div style={{ color: 'red', marginTop: 12 }}>{errors.general}</div>}

                    <div style={{ display: 'flex', justifyContent: 'flex-end', gap: 8, marginTop: 12 }}>
                        <button type="button" onClick={onClose} disabled={submitting}>Cancel</button>
                        <button type="submit" disabled={submitting} style={{ background: '#1976d2', color: '#fff', border: 'none', padding: '0.5rem 1rem' }}>
                            {submitting ? 'Creating...' : 'Create'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
};

export default CreateUserDialog;