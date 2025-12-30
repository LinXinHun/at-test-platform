import { io, Socket } from 'socket.io-client';

class WebSocketService {
  private socket: Socket | null = null;
  private listeners: Map<string, Array<(data: any) => void>> = new Map();

  connect() {
    this.socket = io('http://localhost:8080', {
      path: '/ws/socket.io',
      transports: ['websocket']
    });

    this.socket.on('connect', () => {
      console.log('WebSocket connected');
    });

    this.socket.on('disconnect', () => {
      console.log('WebSocket disconnected');
    });

    // Listen for test results updates
    this.socket.on('test-result', (data) => {
      const eventListeners = this.listeners.get('test-result');
      if (eventListeners) {
        eventListeners.forEach(listener => listener(data));
      }
    });

    // Listen for task status updates
    this.socket.on('task-status', (data) => {
      const eventListeners = this.listeners.get('task-status');
      if (eventListeners) {
        eventListeners.forEach(listener => listener(data));
      }
    });
  }

  disconnect() {
    if (this.socket) {
      this.socket.disconnect();
      this.socket = null;
    }
  }

  on(event: string, callback: (data: any) => void) {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, []);
    }
    this.listeners.get(event)?.push(callback);
  }

  off(event: string, callback: (data: any) => void) {
    const eventListeners = this.listeners.get(event);
    if (eventListeners) {
      this.listeners.set(
        event,
        eventListeners.filter(listener => listener !== callback)
      );
    }
  }

  subscribeToTaskResults(taskId: number) {
    if (this.socket) {
      this.socket.emit('subscribe-to-results', { taskId });
    }
  }

  unsubscribeFromTaskResults(taskId: number) {
    if (this.socket) {
      this.socket.emit('unsubscribe-from-results', { taskId });
    }
  }
}

export default new WebSocketService();