<template>
  <div class="result-view">
    <h2>测试结果查看</h2>
    <div class="task-info" v-if="task">
      <h3>任务信息</h3>
      <p><strong>任务名称:</strong> {{ task.name }}</p>
      <p><strong>测试计划:</strong> {{ task.testPlan.name }}</p>
      <p><strong>状态:</strong> <span :class="`status-${task.status.toLowerCase()}`">{{ task.status }}</span></p>
      <p><strong>开始时间:</strong> {{ task.startTime ? formatDate(task.startTime) : '-' }}</p>
      <p><strong>结束时间:</strong> {{ task.endTime ? formatDate(task.endTime) : '-' }}</p>
    </div>
    
    <div class="result-summary" v-if="results.length > 0">
      <h3>结果统计</h3>
      <div class="summary-stats">
        <div class="stat">
          <span class="stat-label">总测试数</span>
          <span class="stat-value">{{ results.length }}</span>
        </div>
        <div class="stat">
          <span class="stat-label">通过</span>
          <span class="stat-value passed">{{ passedCount }}</span>
        </div>
        <div class="stat">
          <span class="stat-label">失败</span>
          <span class="stat-value failed">{{ failedCount }}</span>
        </div>
      </div>
    </div>
    
    <div class="result-list">
      <h3>测试脚本结果</h3>
      <table>
        <thead>
          <tr>
            <th>脚本名称</th>
            <th>语言</th>
            <th>状态</th>
            <th>执行时间 (ms)</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="result in results" :key="result.id">
            <td>{{ result.script.name }}</td>
            <td>{{ result.script.language }}</td>
            <td>
              <span :class="`status-${result.status.toLowerCase()}`">{{ result.status }}</span>
            </td>
            <td>{{ result.executionTime }}</td>
            <td>
              <button @click="showResultDetails = !showResultDetails; selectedResult = result">
                {{ showResultDetails && selectedResult?.id === result.id ? '隐藏详情' : '查看详情' }}
              </button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Result Details -->
    <div v-if="showResultDetails && selectedResult" class="result-details">
      <h3>测试结果详情</h3>
      <div class="details-content">
        <div class="details-section">
          <h4>脚本信息</h4>
          <p><strong>名称:</strong> {{ selectedResult.script.name }}</p>
          <p><strong>描述:</strong> {{ selectedResult.script.description }}</p>
        </div>
        <div class="details-section">
          <h4>执行输出</h4>
          <pre>{{ selectedResult.output }}</pre>
        </div>
        <div v-if="selectedResult.error" class="details-section error">
          <h4>错误信息</h4>
          <pre>{{ selectedResult.error }}</pre>
        </div>
      </div>
    </div>

    <!-- Back Button -->
    <div class="back-button">
      <button @click="$router.push('/tasks')">返回任务列表</button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue';
import { useRoute } from 'vue-router';
import { testResultApi, testExecutionTaskApi } from '../services/api';
import websocketService from '../services/websocket';

interface TestScript {
  id: number;
  name: string;
  description: string;
  language: string;
}

interface TestPlan {
  id: number;
  name: string;
}

interface TestExecutionTask {
  id: number;
  name: string;
  testPlan: TestPlan;
  status: string;
  startTime: string | null;
  endTime: string | null;
  createdAt: string;
}

interface TestResult {
  id: number;
  taskId: number;
  script: TestScript;
  status: string;
  executionTime: number;
  output: string;
  error: string | null;
}

const route = useRoute();
const taskId = computed(() => parseInt(route.params.taskId as string));

const task = ref<TestExecutionTask | null>(null);
const results = ref<TestResult[]>([]);
const showResultDetails = ref(false);
const selectedResult = ref<TestResult | null>(null);

const passedCount = computed(() => results.value.filter(r => r.status === 'PASSED').length);
const failedCount = computed(() => results.value.filter(r => r.status === 'FAILED').length);

onMounted(() => {
  fetchTaskInfo();
  fetchResults();
  
  // Connect to WebSocket for real-time updates
  websocketService.connect();
  
  // Listen for test result updates
  websocketService.on('test-result', handleTestResultUpdate);
  
  // Listen for task status updates
  websocketService.on('task-status', handleTaskStatusUpdate);
  
  // Subscribe to updates for this specific task
  websocketService.subscribeToTaskResults(taskId.value);
});

onBeforeUnmount(() => {
  // Disconnect WebSocket
  websocketService.unsubscribeFromTaskResults(taskId.value);
  websocketService.off('test-result', handleTestResultUpdate);
  websocketService.off('task-status', handleTaskStatusUpdate);
  websocketService.disconnect();
});

const fetchTaskInfo = async () => {
  try {
    const response = await testExecutionTaskApi.getById(taskId.value);
    task.value = response.data;
  } catch (error) {
    console.error('Failed to fetch task info:', error);
  }
};

const fetchResults = async () => {
  try {
    const response = await testResultApi.getByTaskId(taskId.value);
    results.value = response.data;
  } catch (error) {
    console.error('Failed to fetch results:', error);
  }
};

const handleTestResultUpdate = (data: TestResult) => {
  if (data.taskId === taskId.value) {
    // Check if result already exists
    const existingIndex = results.value.findIndex(r => r.id === data.id);
    if (existingIndex >= 0) {
      // Update existing result
      results.value[existingIndex] = data;
    } else {
      // Add new result
      results.value.push(data);
    }
  }
};

const handleTaskStatusUpdate = (data: any) => {
  if (data.id === taskId.value) {
    fetchTaskInfo();
  }
};

const formatDate = (dateString: string | null) => {
  if (!dateString) return '-';
  const date = new Date(dateString);
  return date.toLocaleString();
};
</script>

<style scoped>
.result-view {
  max-width: 1200px;
  margin: 0 auto;
}

.task-info {
  background-color: #f5f5f5;
  padding: 1rem;
  border-radius: 8px;
  margin-bottom: 1rem;
}

.result-summary {
  margin-bottom: 1rem;
}

.summary-stats {
  display: flex;
  gap: 1rem;
}

.stat {
  background-color: #f5f5f5;
  padding: 1rem;
  border-radius: 8px;
  text-align: center;
  min-width: 100px;
}

.stat-label {
  display: block;
  font-size: 0.9rem;
  margin-bottom: 0.5rem;
}

.stat-value {
  display: block;
  font-size: 1.5rem;
  font-weight: bold;
}

.stat-value.passed {
  color: #67c23a;
}

.stat-value.failed {
  color: #f56c6c;
}

.result-list {
  margin-bottom: 1rem;
}

.result-list table {
  width: 100%;
  border-collapse: collapse;
}

.result-list th, .result-list td {
  border: 1px solid #ddd;
  padding: 0.5rem;
  text-align: left;
}

.result-list th {
  background-color: #f0f0f0;
}

/* Status Styles */
.status-running {
  color: #e6a23c;
  font-weight: bold;
}

.status-completed {
  color: #67c23a;
  font-weight: bold;
}

.status-failed {
  color: #f56c6c;
  font-weight: bold;
}

.status-passed {
  color: #67c23a;
  font-weight: bold;
}

.status-created {
  color: #909399;
  font-weight: bold;
}

.result-details {
  background-color: #f5f5f5;
  padding: 1rem;
  border-radius: 8px;
  margin-bottom: 1rem;
}

.details-content {
  margin-top: 1rem;
}

.details-section {
  margin-bottom: 1rem;
}

.details-section h4 {
  margin-top: 0;
  margin-bottom: 0.5rem;
}

.details-section pre {
  background-color: #ffffff;
  padding: 0.5rem;
  border-radius: 4px;
  overflow-x: auto;
  max-height: 300px;
}

.details-section.error {
  border-left: 4px solid #f56c6c;
  padding-left: 1rem;
}

.back-button {
  margin-top: 2rem;
}

button {
  background-color: #42b983;
  color: white;
  border: none;
  padding: 0.5rem 1rem;
  border-radius: 4px;
  cursor: pointer;
  margin-right: 0.5rem;
}

button:hover {
  background-color: #35495e;
}
</style>