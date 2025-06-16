import axios from 'axios';

const API_URL = process.env.REACT_APP_API_URL || 'http://localhost:3001/api';

export interface Channel {
  name: string;
  active: boolean;
}

export interface Subscription {
  id: string;
  name: string;
  channels: Channel[];
  active: boolean;
}

export const getSubscriptions = async (): Promise<Subscription[]> => {
  try {
    const response = await axios.get(`${API_URL}/subscriptions`);
    return response.data;
  } catch (error) {
    console.error('Error fetching subscriptions:', error);
    throw error;
  }
}; 