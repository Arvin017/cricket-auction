import { useEffect, useRef, useState, useCallback } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

/**
 * Connects to the /topic/auction STOMP broadcast and keeps a running feed
 * of AuctionStateMessage events. Falls back to SockJS if a raw WebSocket
 * upgrade is blocked by a proxy.
 */
export function useAuctionSocket() {
  const [connected, setConnected] = useState(false);
  const [lastMessage, setLastMessage] = useState(null);
  const [feed, setFeed] = useState([]);
  const clientRef = useRef(null);

  useEffect(() => {
    // In local dev, Vite's proxy forwards '/ws-sockjs' to the backend. In
    // production, set VITE_WS_URL to your deployed backend's full SockJS
    // URL, e.g. https://your-backend.up.railway.app/ws-sockjs
    const wsUrl = import.meta.env.VITE_WS_URL || '/ws-sockjs';

    const client = new Client({
      webSocketFactory: () => new SockJS(wsUrl),
      reconnectDelay: 3000,
      onConnect: () => {
        setConnected(true);
        client.subscribe('/topic/auction', (message) => {
          try {
            const body = JSON.parse(message.body);
            setLastMessage(body);
            setFeed((prev) => [...prev.slice(-49), body]);
          } catch (e) {
            console.error('Failed to parse auction message', e);
          }
        });
      },
      onDisconnect: () => setConnected(false),
      onStompError: (frame) => console.error('STOMP error', frame),
    });

    client.activate();
    clientRef.current = client;

    return () => {
      client.deactivate();
    };
  }, []);

  const clearLastMessage = useCallback(() => setLastMessage(null), []);

  return { connected, lastMessage, feed, clearLastMessage };
}
