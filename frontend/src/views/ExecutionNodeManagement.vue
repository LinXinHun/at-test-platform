<template>
  <div class="execution-node-management">
    <h1>执行节点管理</h1>
    <div class="node-list">
      <div v-for="node in nodes" :key="node.id" class="node-card" :class="node.status.toLowerCase()">
        <div class="node-header">
          <h3>{{ node.name }}</h3>
          <span class="status-indicator">{{ node.status }}</span>
        </div>
        <div class="node-details">
          <p><strong>节点ID:</strong> {{ node.nodeId }}</p>
          <p><strong>主机:</strong> {{ node.host }}:{{ node.port }}</p>
          <p><strong>操作系统:</strong> {{ node.osInfo }}</p>
          <p><strong>CPU信息:</strong> {{ node.cpuInfo }}</p>
          <p><strong>内存信息:</strong> {{ node.memoryInfo }}</p>
          <p><strong>最后心跳:</strong> {{ formatDateTime(node.lastHeartbeat) }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import api from '../services/api';

interface ExecutionNode {
  id: number;
  nodeId: string;
  name: string;
  description: string;
  host: string;
  port: number;
  status: string;
  osInfo: string;
  cpuInfo: string;
  memoryInfo: string;
  lastHeartbeat: string;
  createdAt: string;
  updatedAt: string;
}

const nodes = ref<ExecutionNode[]>([]);

// 加载执行节点数据
const loadNodes = async () => {
  try {
    const response = await api.get('/execution-nodes');
    nodes.value = response.data;
  } catch (error) {
    console.error('Failed to load execution nodes:', error);
  }
};

// 格式化日期时间
const formatDateTime = (dateTimeStr: string) => {
  const date = new Date(dateTimeStr);
  return date.toLocaleString();
};

// 页面加载时获取执行节点数据
onMounted(() => {
  loadNodes();
  // 每30秒刷新一次节点状态
  setInterval(loadNodes, 30000);
});
</script>

<style scoped>
.execution-node-management {
  padding: 20px;
}

h1 {
  margin-bottom: 20px;
  color: #333;
}

.node-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 20px;
}

.node-card {
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  padding: 20px;
  transition: transform 0.2s, box-shadow 0.2s;
}

.node-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 4px 8px rgba(0, 0, 0, 0.1);
}

.node-card.online {
  border-left: 4px solid #4caf50;
}

.node-card.busy {
  border-left: 4px solid #ff9800;
}

.node-card.offline {
  border-left: 4px solid #f44336;
  opacity: 0.7;
}

.node-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.node-header h3 {
  margin: 0;
  color: #333;
}

.status-indicator {
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: bold;
  text-transform: uppercase;
}

.node-card.online .status-indicator {
  background-color: #e8f5e9;
  color: #4caf50;
}

.node-card.busy .status-indicator {
  background-color: #fff3e0;
  color: #ff9800;
}

.node-card.offline .status-indicator {
  background-color: #ffebee;
  color: #f44336;
}

.node-details p {
  margin: 8px 0;
  font-size: 14px;
  color: #666;
}

.node-details strong {
  color: #333;
}
</style>
