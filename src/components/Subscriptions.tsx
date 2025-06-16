import React, { useState, useEffect } from 'react';
import './Subscriptions.css';

// Mock data types
interface Channel {
  name: string;
  active: boolean;
}

interface Subscription {
  id: string;
  name: string;
  active: boolean;
  channels: Channel[];
}

// Mock data
const mockSubscriptions: Subscription[] = [
  {
    id: '1',
    name: 'Account Balance Alerts',
    active: true,
    channels: [
      { name: 'Email', active: true },
      { name: 'SMS', active: true },
      { name: 'Push Notification', active: false }
    ]
  },
  {
    id: '2',
    name: 'Transaction Alerts',
    active: true,
    channels: [
      { name: 'Email', active: true },
      { name: 'SMS', active: false }
    ]
  },
  {
    id: '3',
    name: 'Bill Payment Reminders',
    active: false,
    channels: [
      { name: 'Email', active: false },
      { name: 'SMS', active: false },
      { name: 'Push Notification', active: false }
    ]
  },
  {
    id: '4',
    name: 'Security Alerts',
    active: true,
    channels: [
      { name: 'Email', active: true },
      { name: 'SMS', active: true },
      { name: 'Push Notification', active: true }
    ]
  },
  {
    id: '5',
    name: 'Market Updates',
    active: true,
    channels: [
      { name: 'Email', active: true },
      { name: 'Push Notification', active: false }
    ]
  }
];

const Subscriptions: React.FC = () => {
  const [subscriptions, setSubscriptions] = useState<Subscription[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    // Simulate API call with mock data
    const fetchSubscriptions = async () => {
      try {
        // Simulate network delay
        await new Promise(resolve => setTimeout(resolve, 1000));
        setSubscriptions(mockSubscriptions);
        setError(null);
      } catch (err) {
        setError('Failed to load subscriptions. Please try again later.');
      } finally {
        setLoading(false);
      }
    };

    fetchSubscriptions();
  }, []);

  if (loading) {
    return (
      <div className="subscriptions-container">
        <div className="loading">Loading subscriptions...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="subscriptions-container">
        <div className="error">{error}</div>
      </div>
    );
  }

  return (
    <div className="subscriptions-container">
      <div className="subscriptions-grid">
        {subscriptions.map((subscription) => (
          <div 
            key={subscription.id} 
            className={`subscription-card ${subscription.active ? 'active' : 'inactive'}`}
          >
            <div className="subscription-content">
              <div className="subscription-main">
                <div className="subscription-header">
                  <h3>{subscription.name}</h3>
                  <span className={`status-badge ${subscription.active ? 'active' : 'inactive'}`}>
                    {subscription.active ? 'Active' : 'Inactive'}
                  </span>
                </div>
              </div>
              <div className="channels-section">
                <h4>Channels</h4>
                <div className="channels-container">
                  {subscription.channels.map((channel, index) => (
                    <div 
                      key={index} 
                      className={`channel-tag ${channel.active ? 'active' : 'inactive'}`}
                    >
                      {channel.name}
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
};

export default Subscriptions; 