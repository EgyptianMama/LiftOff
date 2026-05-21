import React from 'react';
import './Badge.css';

type StatusType = 'QUEUED' | 'RUNNING' | 'SUCCESS' | 'FAILED';

interface BadgeProps {
  status: StatusType;
}

export const Badge: React.FC<BadgeProps> = ({ status }) => {
  const statusConfig = {
    QUEUED: { label: 'Queued', colorClass: 'badge-queued' },
    RUNNING: { label: 'Building', colorClass: 'badge-running' },
    SUCCESS: { label: 'Active', colorClass: 'badge-success' },
    FAILED: { label: 'Failed', colorClass: 'badge-failed' },
  };

  const config = statusConfig[status];

  return (
    <div className={`badge ${config.colorClass}`}>
      <span className="badge-dot"></span>
      {config.label}
    </div>
  );
};
