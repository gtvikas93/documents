import React, { useState, useEffect } from 'react';
import './Subscriptions.css';
import { getSubscriptions, Subscription } from '../services/subscriptionService';

const Subscriptions: React.FC = () => {
  const [subscriptions, setSubscriptions] = useState<Subscription[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchSubscriptions = async () => {
      try {
        const data = await getSubscriptions();
        setSubscriptions(data);
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
      <h2>My Subscriptions</h2>
      <div className="subscriptions-grid">
        {subscriptions.map((subscription) => (
          <div 
            key={subscription.id} 
            className={`subscription-card ${subscription.active ? 'active' : 'inactive'}`}
          >
            <h3>{subscription.name}</h3>
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
        ))}
      </div>
    </div>
  );
};

export default Subscriptions; 