import { useState } from 'react';
import { Link } from 'react-router-dom';
import logo from '../assets/logo.jpg';
import { createPg } from '../services/setupService';

const inputClass = 'w-full rounded-xl border border-nest-line bg-white px-3 py-2.5 text-sm outline-none focus:border-nest-navy/40';

export default function Setup() {
  const [form, setForm] = useState({
    username: '',
    password: '',
    pgName: '',
    floors: 2,
  });
  const [roomsOnFloors, setRoomsOnFloors] = useState(['6', '6']);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const setField = (key, value) => setForm((prev) => ({ ...prev, [key]: value }));

  const setFloors = (value) => {
    const floors = Number(value);
    setField('floors', value);
    if (!Number.isInteger(floors) || floors < 1 || floors > 15) return;
    setRoomsOnFloors((prev) => {
      const next = prev.slice(0, floors);
      while (next.length < floors) next.push('6');
      return next;
    });
  };

  const finish = async (event) => {
    event.preventDefault();
    setError('');
    const floors = Number(form.floors);
    const counts = roomsOnFloors.slice(0, floors).map((value) => Number(value));
    if (!form.username.trim() || !form.pgName.trim()) {
      setError('Enter a username and the PG name.');
      return;
    }
    if (form.password.length < 8) {
      setError('Use a password of at least 8 characters.');
      return;
    }
    if (floors < 1 || floors > 15 || counts.length !== floors) {
      setError('Use 1 to 15 floors.');
      return;
    }
    if (counts.some((count) => count < 1 || count > 30)) {
      setError('Each floor needs 1 to 30 rooms.');
      return;
    }
    setBusy(true);
    try {
      await createPg({
        username: form.username.trim(),
        password: form.password,
        pgName: form.pgName.trim(),
        roomsOnFloors: counts,
      });
      window.location.assign('/');
    } catch (err) {
      setError(err.message || 'Could not create the PG.');
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="min-h-[100dvh] bg-nest-soft px-4 py-8">
      <div className="mx-auto w-full max-w-lg">
        <img src={logo} alt="Nestio" className="mx-auto h-auto w-full max-w-sm object-contain" />
        <h1 className="mt-4 text-center font-display text-2xl font-semibold text-nest-navy">
          Set up your PG
        </h1>
        <p className="mt-1 text-center text-sm text-nest-muted">
          Each floor can have its own number of rooms. Sharing is chosen when the first resident books a room.
        </p>

        {error && (
          <p className="mt-4 rounded-xl border border-red-100 bg-red-50 px-3 py-2 text-sm text-red-700">{error}</p>
        )}

        <form onSubmit={finish} className="mt-5 space-y-4 rounded-2xl border border-nest-line bg-white p-5 shadow-soft">
          <Field label="PG name">
            <input value={form.pgName} onChange={(e) => setField('pgName', e.target.value)} className={inputClass} required />
          </Field>
          <Field label="Username">
            <input value={form.username} onChange={(e) => setField('username', e.target.value)} className={inputClass} required />
          </Field>
          <Field label="Password">
            <input type="password" value={form.password} onChange={(e) => setField('password', e.target.value)} className={inputClass} required />
          </Field>
          <Field label="Floors">
            <input type="number" min="1" max="15" value={form.floors} onChange={(e) => setFloors(e.target.value)} className={inputClass} required />
          </Field>
          <div className="space-y-3">
            {roomsOnFloors.slice(0, Number(form.floors) || 0).map((count, index) => (
              <Field key={index} label={`Rooms on floor ${index + 1}`}>
                <input
                  type="number"
                  min="1"
                  max="30"
                  value={count}
                  onChange={(e) => {
                    const next = [...roomsOnFloors];
                    next[index] = e.target.value;
                    setRoomsOnFloors(next);
                  }}
                  className={inputClass}
                  required
                />
              </Field>
            ))}
          </div>
          <button type="submit" disabled={busy} className="min-h-12 w-full rounded-xl bg-nest-navy text-sm font-semibold text-white disabled:opacity-50">
            {busy ? 'Creating...' : 'Create PG'}
          </button>
        </form>
        <p className="mt-4 text-center text-sm text-nest-muted">
          Already have a PG? <Link to="/login" className="font-semibold text-nest-navy">Sign in</Link>
        </p>
      </div>
    </div>
  );
}

function Field({ label, children }) {
  return (
    <label className="block">
      <span className="mb-1.5 block text-sm font-medium text-nest-navy">{label}</span>
      {children}
    </label>
  );
}
