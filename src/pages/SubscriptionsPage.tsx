import React from 'react';
import Subscriptions from '../components/Subscriptions';
import './SubscriptionsPage.css';

const SubscriptionsPage: React.FC = () => {
  return (
    <div className="subscriptions-page">
      <header className="page-header">
        <h1>My Subscriptions</h1>
      </header>
      <main className="page-content">
        <Subscriptions />
      </main>
    </div>
  );
};

export default SubscriptionsPage; 