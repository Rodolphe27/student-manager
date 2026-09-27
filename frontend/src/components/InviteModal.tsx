import { useState } from 'react';
import type { RegistrationInvite } from '../types';

interface InviteModalProps {
  invite: RegistrationInvite;
  targetName: string;
  onClose: () => void;
}

/**
 * Shows a freshly issued registration invite: the code, its expiry, and a
 * ready-to-share registration link (built from the current origin — works for
 * both local dev and whatever host the frontend is deployed to).
 */
export default function InviteModal({ invite, targetName, onClose }: InviteModalProps) {
  const [copied, setCopied] = useState<boolean>(false);

  const link = `${window.location.origin}/register?code=${encodeURIComponent(invite.code)}`;

  const handleCopy = async (): Promise<void> => {
    try {
      await navigator.clipboard.writeText(link);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // Clipboard access can fail (e.g. insecure context) — the link is still
      // shown and selectable, so this isn't fatal.
    }
  };

  const expires = new Date(invite.expiresAt);

  return (
    <div className="fixed inset-0 bg-black/40 z-50 flex items-center justify-center p-4">
      <div className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 w-full max-w-md">
        <h2 className="font-semibold text-gray-800 mb-1">Invite sent for {targetName}</h2>
        <p className="text-sm text-gray-400 mb-4">
          Share this link with them — it lets them register and links their account to this
          {invite.targetType === 'STUDENT' ? ' student' : ' teacher'} profile.
        </p>

        <label className="block text-xs font-medium text-gray-500 mb-1">Registration link</label>
        <div className="flex gap-2 mb-4">
          <input
            readOnly
            value={link}
            onFocus={(e) => e.target.select()}
            className="flex-1 border border-gray-200 rounded-lg px-3 py-2 text-xs font-mono text-gray-600 focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          <button
            onClick={handleCopy}
            className="bg-blue-600 text-white px-3 py-2 rounded-lg text-xs font-medium hover:bg-blue-700 whitespace-nowrap"
          >
            {copied ? 'Copied!' : 'Copy'}
          </button>
        </div>

        <p className="text-xs text-gray-400 mb-6">
          Code <span className="font-mono text-gray-600">{invite.code}</span> — expires{' '}
          {expires.toLocaleString()}
        </p>

        <button
          onClick={onClose}
          className="w-full bg-gray-100 text-gray-600 py-2 rounded-lg text-sm font-medium hover:bg-gray-200"
        >
          Close
        </button>
      </div>
    </div>
  );
}
