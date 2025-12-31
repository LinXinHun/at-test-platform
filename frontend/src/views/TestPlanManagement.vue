<template>
  <div class="plan-management">
    <h2>测试计划管理</h2>
    <div class="action-bar">
      <button @click="handleAddPlan">添加测试计划</button>
      <button @click="handleRefreshPlans" class="refresh-button">刷新</button>
    </div>
    
    <div class="plan-list">
      <table>
        <thead>
          <tr>
            <th>计划名称</th>
            <th>描述</th>
            <th>执行端点类型</th>
            <th>测试脚本数量</th>
            <th>创建时间</th>
            <th>更新时间</th>
            <th>最后执行状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="plan in testPlans" :key="plan.id">
            <td>{{ plan.name }}</td>
            <td>{{ plan.description }}</td>
            <td>{{ plan.executionEndpointType }}</td>
            <td>{{ plan.scripts.length }}</td>
            <td>{{ formatDate(plan.createdAt) }}</td>
            <td>{{ formatDate(plan.updatedAt) }}</td>
            <td>
              <span :class="['status-badge', plan.lastExecutionStatus]">
                {{ plan.lastExecutionStatus || '未执行' }}
              </span>
            </td>
            <td class="action-buttons">
              <button @click="handleEditPlan(plan)">编辑</button>
              <button @click="handleDeletePlan(plan.id)">删除</button>
              <button @click="handleExecutePlan(plan)">执行</button>
              <button @click="handleViewExecutionRecords(plan)">执行记录</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
    
    <!-- 分页组件 -->
    <div class="pagination">
      <button @click="changePage(0)" :disabled="currentPage === 0">首页</button>
      <button @click="changePage(currentPage - 1)" :disabled="currentPage === 0">上一页</button>
      <span>第 {{ currentPage + 1 }} 页，共 {{ totalPages }} 页</span>
      <button @click="changePage(currentPage + 1)" :disabled="currentPage >= totalPages - 1">下一页</button>
      <button @click="changePage(totalPages - 1)" :disabled="currentPage >= totalPages - 1">末页</button>
      <div class="page-size">
        <label>每页显示：</label>
        <select v-model="pageSize" @change="fetchTestPlans">
          <option value="10">10</option>
          <option value="20">20</option>
          <option value="50">50</option>
          <option value="100">100</option>
        </select>
      </div>
    </div>
    
    <!-- Add/Edit Dialog -->
    <div v-if="dialogVisible" class="modal">
      <div class="modal-content">
        <div class="modal-header">
          <h3>{{ editingPlan ? '编辑测试计划' : '添加测试计划' }}</h3>
          <button class="close-button" @click="dialogVisible = false">&times;</button>
        </div>
        <div class="modal-body">
          <form @submit.prevent="handleSavePlan">
            <div class="form-group">
              <label for="name">计划名称</label>
              <input type="text" id="name" v-model="editingPlan.name" required />
            </div>
            
            <div class="form-group">
              <label for="description">描述</label>
              <textarea id="description" v-model="editingPlan.description"></textarea>
            </div>
            
            <div class="form-group">
              <label for="executionEndpointType">执行端点类型</label>
              <select id="executionEndpointType" v-model="editingPlan.executionEndpointType" required>
                <option value="MiniApp">MiniApp</option>
                <option value="Web">Web</option>
                <option value="App">App</option>
                <option value="Api">Api</option>
              </select>
            </div>
            
            <div class="form-group">
              <label>选择测试脚本 <span class="selected-count">(已选择 {{ selectedScripts.length }} 个)</span></label>
              <div class="checkbox-group">
                <label v-for="script in testScripts" :key="script.id" class="checkbox-item">
                  <input type="checkbox" :value="script.id" v-model="selectedScripts">
                  {{ script.name }}
                </label>
              </div>
              <!-- 测试脚本分页组件 -->
              <div class="script-pagination">
                <button @click="changeScriptPage(0)" :disabled="scriptCurrentPage === 0">首页</button>
                <button @click="changeScriptPage(scriptCurrentPage - 1)" :disabled="scriptCurrentPage === 0">上一页</button>
                <span>第 {{ scriptCurrentPage + 1 }} 页，共 {{ scriptTotalPages }} 页</span>
                <button @click="changeScriptPage(scriptCurrentPage + 1)" :disabled="scriptCurrentPage >= scriptTotalPages - 1">下一页</button>
                <button @click="changeScriptPage(scriptTotalPages - 1)" :disabled="scriptCurrentPage >= scriptTotalPages - 1">末页</button>
                <div class="page-size">
                  <label>每页显示：</label>
                  <select v-model="scriptPageSize" @change="fetchTestScripts">
                    <option value="10">10</option>
                    <option value="20">20</option>
                    <option value="50">50</option>
                    <option value="100">100</option>
                  </select>
                </div>
              </div>
            </div>
            
            <div class="modal-footer">
              <button type="submit">保存</button>
              <button type="button" @click="dialogVisible = false">取消</button>
            </div>
          </form>
        </div>
      </div>
    </div>
    
    <!-- Execute Dialog -->
    <div v-if="executeDialogVisible" class="modal">
      <div class="modal-content execute-modal">
        <div class="modal-header">
          <h3>执行测试计划</h3>
          <button class="close-button" @click="executeDialogVisible = false">&times;</button>
        </div>
        <div class="modal-body">
          <h4>{{ currentPlan?.name }}</h4>
          <div class="form-group">
            <label for="executionNode">选择执行节点</label>
            <select id="executionNode" v-model="selectedNodeId" required class="node-select">
                <option value="" disabled>请选择执行节点</option>
                <option v-for="node in executionNodes.filter(n => n.endpointType === currentPlan?.executionEndpointType)" :key="node.id" :value="node.id" 
                        :class="['node-option', node.status]"
                        :disabled="node.status !== 'ONLINE'">
                  {{ node.name }} - {{ node.host }}:{{ node.port }} ({{ node.status === 'ONLINE' ? '在线' : '离线' }} - {{ node.endpointType }})
                </option>
              </select>
          </div>
          
          <div class="modal-footer">
            <button @click="handleConfirmExecution" class="primary-button" :disabled="!selectedNodeId">确认执行</button>
            <button type="button" @click="executeDialogVisible = false" class="secondary-button">取消</button>
          </div>
        </div>
      </div>
    </div>

    <!-- Execution Records Dialog -->
    <div v-if="executionRecordsDialogVisible" class="modal">
      <div class="modal-content">
        <div class="modal-header">
          <h3>测试计划执行记录</h3>
          <button class="close-button" @click="executionRecordsDialogVisible = false">&times;</button>
        </div>
        <div class="modal-body">
          <h4>{{ currentPlan?.name }}</h4>
          <div class="execution-records-list">
            <table>
              <thead>
                <tr>
                  <th>执行时间</th>
                  <th>执行节点</th>
                  <th>状态</th>
                  <th>成功/失败/总数</th>
                  <th>耗时</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="execution in executionRecords" :key="execution.id">
                  <td>{{ formatDate(execution.createdAt) }}</td>
                  <td>{{ execution.executionNode.name }}</td>
                  <td>
                    <span :class="['status-badge', execution.status]">
                      {{ getStatusText(execution.status) }}
                    </span>
                  </td>
                  <td>{{ execution.successScripts }}/{{ execution.failedScripts }}/{{ execution.totalScripts }}</td>
                  <td>{{ formatDuration(execution.createdAt, execution.endTime) }}</td>
                  <td>
                    <button @click="handleViewExecutionLogs(execution)">查看日志</button>
                    <button @click="handleGenerateReport(execution)" :disabled="execution.status !== 'SUCCESS' && execution.status !== 'FAILURE'">
                      生成报告
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>

    <!-- Execution Logs Dialog -->
    <div v-if="executionLogsDialogVisible" class="modal">
      <div class="modal-content logs-modal">
        <div class="modal-header">
          <h3>测试计划执行日志</h3>
          <button class="close-button" @click="executionLogsDialogVisible = false">&times;</button>
        </div>
        <div class="modal-body">
          <h4>执行记录 #{{ currentExecution?.id }}</h4>
          <div class="execution-logs-list">
            <table>
              <thead>
                <tr>
                  <th>脚本名称</th>
                  <th>状态</th>
                  <th>耗时(ms)</th>
                  <th>开始时间</th>
                  <th>结束时间</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="log in executionLogs" :key="log.id">
                  <td>{{ log.testScript.name }}</td>
                  <td>
                    <span :class="['status-badge', log.status]">
                      {{ log.status }}
                    </span>
                  </td>
                  <td>{{ log.executionTime }}</td>
                  <td>{{ formatDate(log.createdAt) }}</td>
                  <td>{{ formatDate(log.endTime) }}</td>
                  <td>
                    <button @click="handleViewLogDetails(log)">查看详情</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          
          <!-- Log Details Dialog -->
          <div v-if="logDetailsDialogVisible" class="modal">
            <div class="modal-content">
              <div class="modal-header">
                <h3>脚本执行日志详情</h3>
                <button class="close-button" @click="logDetailsDialogVisible = false">&times;</button>
              </div>
              <div class="modal-body">
                <h4>{{ currentLog?.testScript.name }}</h4>
                <div class="log-details">
                  <div class="log-item">
                    <strong>状态:</strong>
                    <span :class="['status-badge', currentLog?.status]">
                      {{ currentLog?.status }}
                    </span>
                  </div>
                  <div class="log-item">
                    <strong>耗时:</strong> {{ currentLog?.executionTime }} ms
                  </div>
                  <div class="log-item">
                    <strong>开始时间:</strong> {{ formatDate(currentLog?.createdAt) }}
                  </div>
                  <div class="log-item">
                    <strong>结束时间:</strong> {{ formatDate(currentLog?.endTime) }}
                  </div>
                  <div class="log-item">
                    <strong>结果:</strong>
                    <pre>{{ currentLog?.result }}</pre>
                  </div>
                  <div v-if="currentLog?.errorMessage" class="log-item error">
                    <strong>错误信息:</strong>
                    <pre>{{ currentLog?.errorMessage }}</pre>
                  </div>
                  <div class="log-item">
                    <button @click="downloadLog(currentLog)">下载日志</button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch } from 'vue';
import { useRouter } from 'vue-router';
import { testPlanApi, testScriptApi, testPlanExecutionApi, executionNodeApi, testReportApi } from '../services/api';

interface TestScript {
  id: number;
  name: string;
  language: string;
}

interface ExecutionNode {
  id: number;
  name: string;
  host: string;
  port: number;
  status: string;
  endpointType: string;
}

interface TestPlan {
  id: number;
  name: string;
  description: string;
  scripts: TestScript[];
  executionEndpointType: string;
  createdAt: string;
  updatedAt: string;
  lastExecutionStatus?: string;
  lastExecutionTime?: string;
  lastExecutionNode?: ExecutionNode;
}

interface TestPlanExecution {
  id: number;
  testPlan: TestPlan;
  executionNode: ExecutionNode;
  status: string;
  totalScripts: number;
  successScripts: number;
  failedScripts: number;
  createdAt: string;
  endTime?: string;
}

interface TestPlanExecutionLog {
  id: number;
  execution: TestPlanExecution;
  testScript: TestScript;
  status: string;
  result: string;
  errorMessage?: string;
  executionTime: number;
  createdAt: string;
  endTime: string;
}

const testPlans = ref<TestPlan[]>([]);
const testScripts = ref<TestScript[]>([]);
const executionNodes = ref<ExecutionNode[]>([]);
const executionRecords = ref<TestPlanExecution[]>([]);
const executionLogs = ref<TestPlanExecutionLog[]>([]);

const router = useRouter();

const dialogVisible = ref(false);
const executeDialogVisible = ref(false);
const executionRecordsDialogVisible = ref(false);
const executionLogsDialogVisible = ref(false);
const logDetailsDialogVisible = ref(false);

// 测试计划分页相关状态
const currentPage = ref(0);
const pageSize = ref(10);
const totalPages = ref(1);
const totalElements = ref(0);

// 测试脚本分页相关状态
const scriptCurrentPage = ref(0);
const scriptPageSize = ref(10);
const scriptTotalPages = ref(1);
const scriptTotalElements = ref(0);

// 轮询相关变量
const pollInterval = ref<number | null>(null);
const pollIntervalTime = 3000; // 每3秒轮询一次

const editingPlan = ref<TestPlan>({
  id: 0,
  name: '',
  description: '',
  scripts: [],
  executionEndpointType: 'Web',
  createdAt: '',
  updatedAt: ''
});

const selectedScripts = ref<number[]>([]);
const selectedNodeId = ref<number | null>(null);
const currentPlan = ref<TestPlan | null>(null);
const currentExecution = ref<TestPlanExecution | null>(null);
const currentLog = ref<TestPlanExecutionLog | null>(null);

onMounted(() => {
  fetchTestPlans();
  fetchTestScripts();
  fetchExecutionNodes();
});

const handleRefreshPlans = async () => {
  await fetchTestPlans();
  await fetchExecutionNodes();
};

const fetchTestPlans = async () => {
  try {
    const response = await testPlanApi.getAll(currentPage.value, pageSize.value);
    testPlans.value = response.data.content;
    totalPages.value = response.data.totalPages;
    totalElements.value = response.data.totalElements;
  } catch (error) {
    console.error('Failed to fetch test plans:', error);
  }
};

const fetchTestScripts = async () => {
  try {
    const response = await testScriptApi.getAll(scriptCurrentPage.value, scriptPageSize.value);
    testScripts.value = response.data.content;
    scriptTotalPages.value = response.data.totalPages;
    scriptTotalElements.value = response.data.totalElements;
  } catch (error) {
    console.error('Failed to fetch test scripts:', error);
  }
};

// 测试脚本分页函数
const changeScriptPage = (newPage: number) => {
  if (newPage >= 0 && newPage < scriptTotalPages.value) {
    scriptCurrentPage.value = newPage;
    fetchTestScripts();
  }
};

const fetchExecutionNodes = async () => {
  try {
    const response = await executionNodeApi.getAll();
    executionNodes.value = response.data;
  } catch (error) {
    console.error('Failed to fetch execution nodes:', error);
  }
};

// 分页函数
const changePage = (newPage: number) => {
  currentPage.value = newPage;
  fetchTestPlans();
};

const fetchExecutionRecords = async (planId: number) => {
  try {
    const response = await testPlanExecutionApi.getByPlanId(planId);
    executionRecords.value = response.data;
    console.log('获取到执行记录:', executionRecords.value);
    console.log('弹窗可见状态:', executionRecordsDialogVisible.value);
  } catch (error) {
    console.error('Failed to fetch execution records:', error);
  }
};

const fetchExecutionLogs = async (executionId: number) => {
  try {
    const response = await testPlanExecutionApi.getLogsByExecutionId(executionId);
    executionLogs.value = response.data;
  } catch (error) {
    console.error('Failed to fetch execution logs:', error);
  }
};

const handleAddPlan = () => {
  editingPlan.value = {
    id: 0,
    name: '',
    description: '',
    scripts: [],
    executionEndpointType: 'Web',
    createdAt: '',
    updatedAt: ''
  };
  selectedScripts.value = [];
  dialogVisible.value = true;
}

const handleEditPlan = (plan: TestPlan) => {
  editingPlan.value = { ...plan };
  selectedScripts.value = plan.scripts.map(script => script.id);
  dialogVisible.value = true;
};

const handleSavePlan = async () => {
  try {
    const planData = {
      ...editingPlan.value,
      scriptIds: selectedScripts.value
    };
    
    if (editingPlan.value.id) {
      await testPlanApi.update(editingPlan.value.id, planData);
    } else {
      await testPlanApi.create(planData);
    }
    
    await fetchTestPlans();
    dialogVisible.value = false;
  } catch (error) {
    console.error('Failed to save plan:', error);
  }
};

const handleDeletePlan = async (id: number) => {
  try {
    await testPlanApi.delete(id);
    await fetchTestPlans();
  } catch (error) {
    console.error('Failed to delete plan:', error);
  }
};

const handleExecutePlan = (plan: TestPlan) => {
  currentPlan.value = plan;
  executeDialogVisible.value = true;
  
  // 重置选择的节点
  selectedNodeId.value = null;
  
  // 默认选择第一个匹配类型且在线的执行节点
  const onlineNode = executionNodes.value.find(node => node.status === 'ONLINE' && node.endpointType === plan.executionEndpointType);
  if (onlineNode) {
    selectedNodeId.value = onlineNode.id;
  }
};

const handleConfirmExecution = async () => {
  if (!currentPlan.value || selectedNodeId.value === null) return;
  
  try {
    await testPlanExecutionApi.execute(currentPlan.value.id, selectedNodeId.value);
    await fetchTestPlans();
    executeDialogVisible.value = false;
    alert('测试计划已开始执行');
  } catch (error) {
    console.error('Failed to execute plan:', error);
    alert('执行测试计划失败');
  }
};

const handleViewExecutionRecords = async (plan: TestPlan) => {
  currentPlan.value = plan;
  await fetchExecutionRecords(plan.id);
  executionRecordsDialogVisible.value = true;
  
  // 启动轮询
  startPolling();
};

const handleViewExecutionLogs = async (execution: TestPlanExecution) => {
  currentExecution.value = execution;
  await fetchExecutionLogs(execution.id);
  executionRecordsDialogVisible.value = false;
  executionLogsDialogVisible.value = true;
  
  // 停止轮询，因为已经进入日志查看页面
  stopPolling();
};

// 启动轮询
const startPolling = () => {
  // 先停止可能存在的轮询
  stopPolling();
  
  // 启动新的轮询
  pollInterval.value = window.setInterval(async () => {
    if (currentPlan.value && executionRecordsDialogVisible.value) {
      await fetchExecutionRecords(currentPlan.value.id);
    }
  }, pollIntervalTime);
};

// 停止轮询
const stopPolling = () => {
  if (pollInterval.value !== null) {
    clearInterval(pollInterval.value);
    pollInterval.value = null;
  }
};

// 监听执行记录对话框关闭事件
watch(executionRecordsDialogVisible, (newValue) => {
  if (!newValue) {
    stopPolling();
  }
});

// 组件卸载时停止轮询
onUnmounted(() => {
  stopPolling();
});

const handleViewLogDetails = (log: TestPlanExecutionLog) => {
  currentLog.value = log;
  logDetailsDialogVisible.value = true;
};

const downloadLog = async (log: TestPlanExecutionLog | null) => {
  if (!log || !currentPlan.value || !currentExecution.value) {
    return;
  }
  
  try {
    const response = await testPlanExecutionApi.downloadLog(
      currentPlan.value.id,
      currentExecution.value.id,
      log.testScript.id
    );
    
    // Create a blob from the response data
    const blob = new Blob([response.data], { type: 'text/plain' });
    
    // Create a download link and trigger it
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `${log.testScript.name}_${new Date().toISOString().slice(0, 19).replace(/[:T]/g, '-')}.log`;
    document.body.appendChild(link);
    link.click();
    
    // Clean up
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  } catch (error) {
    console.error('Failed to download log:', error);
  }
};
const formatDate = (dateString?: string) => {
  if (!dateString) return '';
  const date = new Date(dateString);
  return date.toLocaleString();
};

// 状态映射函数
const getStatusText = (status?: string) => {
  if (!status) return '';
  const statusMap: Record<string, string> = {
    'SUCCESS': '成功',
    'FAILURE': '失败',
    'EXECUTING': '执行中'
  };
  return statusMap[status] || status;
};

const formatDuration = (startTime?: string, endTime?: string) => {
  if (!startTime || !endTime) return '进行中';
  
  const start = new Date(startTime).getTime();
  const end = new Date(endTime).getTime();
  const duration = end - start;
  
  if (duration < 1000) {
    return `${duration} ms`;
  } else if (duration < 60000) {
    return `${(duration / 1000).toFixed(2)} s`;
  } else {
    const minutes = Math.floor(duration / 60000);
    const seconds = ((duration % 60000) / 1000).toFixed(2);
    return `${minutes} m ${seconds} s`;
  }
};
const handleGenerateReport = async (execution: TestPlanExecution) => {
  try {
    const response = await testReportApi.generate(execution.id);
    alert('报告生成成功！');
    // 跳转到报告详情页面
    if (response.data && response.data.id) {
      router.push(`/reports/${response.data.id}`);
    }
  } catch (error) {
    console.error('生成报告失败:', error);
    alert('生成报告失败，请稍后重试');
  }
};
</script>

<style scoped>
.plan-management {
  max-width: 1200px;
  margin: 0 auto;
}

.action-bar {
  margin-bottom: 1rem;
  display: flex;
  gap: 0.75rem;
  align-items: center;
}

.refresh-button {
  background-color: #409eff;
  color: white;
  border: 1px solid #409eff;
  border-radius: 4px;
  padding: 8px 12px;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.3s ease;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-weight: 500;
}

.refresh-button:hover {
  background-color: #66b1ff;
  border-color: #66b1ff;
  transform: rotate(15deg);
}

.refresh-button:active {
  transform: rotate(360deg);
  transition: transform 0.2s ease;
}

/* 简单的刷新图标效果 */
.refresh-button::before {
  content: '↻';
  font-size: 16px;
  display: inline-block;
  transition: transform 0.3s ease;
}

.refresh-button:hover::before {
  transform: rotate(360deg);
}

.plan-list {
  margin-bottom: 1rem;
}

.plan-list table {
  width: 100%;
  border-collapse: collapse;
}

.plan-list th, .plan-list td {
  border: 1px solid #ddd;
  padding: 0.5rem;
  text-align: left;
}

.plan-list th {
  background-color: #f0f0f0;
}

.modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.action-buttons {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.action-buttons button {
  margin: 0;
  box-sizing: border-box;
  white-space: nowrap; /* 禁止文字换行 */
  text-align: center; /* 文字居中 */
  padding: 8px 4px; /* 适当的内边距 */
}

#executionEndpointType {
  padding: 0.5rem 0.75rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  background-color: white;
  cursor: pointer;
  height: 35px;
  box-sizing: border-box;
}

#executionEndpointType option {
  padding: 8px;
}

.modal-content {
  background-color: white;
  padding: 1.5rem;
  border-radius: 8px;
  width: 90%;
  max-width: 800px;
  max-height: 80vh;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.modal-content.logs-modal {
  max-width: 1000px;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  margin-top: 1rem;
  padding-top: 1rem;
  border-top: 1px solid #eee;
  background-color: white;
  position: sticky;
  bottom: 0;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  border-bottom: 1px solid #ddd;
  padding-bottom: 0.5rem;
}

.modal-header h3 {
  margin: 0;
}

.close-button {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: #999;
  padding: 0;
}

/* Form Styles */
.form-group {
  margin-bottom: 1.5rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
  color: #333;
}

.form-group input,
.form-group textarea,
.form-group select {
  width: 100%;
  padding: 0.5rem 0.75rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
  transition: border-color 0.3s;
  height: 35px;
  box-sizing: border-box;
}

/* 特殊处理textarea，不限制高度 */
.form-group textarea {
  height: auto;
  min-height: 80px;
}

.form-group input:focus,
.form-group textarea:focus,
.form-group select:focus {
  outline: none;
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.1);
}

.form-group textarea {
  resize: vertical;
  min-height: 80px;
}

/* Script Pagination Styles */
.script-pagination {
  margin-top: 1rem;
  padding: 1rem;
  background-color: #f8f9fa;
  border-radius: 6px;
  border: 1px solid #e9ecef;
  display: flex;
  flex-wrap: wrap;
  gap: 0.75rem;
  align-items: center;
  justify-content: space-between;
}

.script-pagination > div {
  display: flex;
  align-items: center;
  gap: 0.75rem;
}

.script-pagination button {
  background-color: white;
  color: #409eff;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  padding: 6px 12px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.3s ease;
  font-weight: 500;
}

.script-pagination button:hover:not(:disabled) {
  background-color: #ecf5ff;
  border-color: #c6e2ff;
  color: #409eff;
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.script-pagination button:disabled {
  background-color: #f5f7fa;
  color: #c0c4cc;
  border-color: #e4e7ed;
  cursor: not-allowed;
  opacity: 0.6;
}

.script-pagination span {
  color: #606266;
  font-size: 13px;
  font-weight: 500;
}

.script-pagination .page-size {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  white-space: nowrap; /* 确保文字不换行 */
}

.script-pagination .page-size label {
  color: #606266;
  font-size: 13px;
  font-weight: 500;
  white-space: nowrap; /* 确保文字不换行 */
}

.script-pagination select {
  background-color: white !important;
  color: #606266 !important;
  border: 1px solid #dcdfe6 !important;
  border-radius: 4px !important;
  padding: 0.5rem 0.75rem !important;
  font-size: 13px !important;
  cursor: pointer !important;
  transition: all 0.3s ease !important;
  height: 35px !important;
  box-sizing: border-box !important;
  min-height: 35px !important;
  max-height: 35px !important;
}

.script-pagination select:hover {
  border-color: #c6e2ff;
}

.script-pagination select:focus {
  outline: none;
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

/* Checkbox Group Styles */
.checkbox-group {
  max-height: 200px;
  overflow-y: auto;
  border: 1px solid #e4e7ed;
  border-radius: 4px;
  padding: 0.75rem;
  margin-bottom: 0.75rem;
  background-color: #fafafa;
}

.checkbox-item {
  display: block;
  margin-bottom: 0.5rem;
  padding: 0.5rem;
  background-color: white;
  border-radius: 4px;
  border: 1px solid #e4e7ed;
  cursor: pointer;
  transition: all 0.3s ease;
}

.checkbox-item:hover {
  border-color: #409eff;
  background-color: #ecf5ff;
  transform: translateY(-1px);
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.checkbox-item input[type="checkbox"] {
  margin-right: 0.5rem;
  accent-color: #409eff;
  cursor: pointer;
}

.selected-count {
  color: #409eff;
  font-weight: 500;
  margin-left: 0.5rem;
}

/* Modal Header Styles */
.modal-header {
  margin-bottom: 1.5rem;
  padding-bottom: 1rem;
  border-bottom: 1px solid #eee;
}

.modal-header h3 {
  margin: 0;
  font-size: 1.25rem;
  color: #333;
}

/* Modal Footer Styles */
.modal-footer {
  margin-top: 1.5rem;
  padding-top: 1rem;
  border-top: 1px solid #eee;
  display: flex;
  gap: 0.75rem;
  justify-content: flex-end;
}

.modal-footer button {
  padding: 8px 16px;
  border: none;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
  transition: background-color 0.3s;
}

.modal-footer button[type="submit"] {
  background-color: #409eff;
  color: white;
}

.modal-footer button[type="submit"]:hover {
  background-color: #66b1ff;
}

.modal-footer button[type="button"] {
  background-color: #f5f7fa;
  color: #606266;
}

.modal-footer button[type="button"]:hover {
  background-color: #e4e7ed;
}

.form-group input[type="text"],
.form-group textarea {
  width: 100%;
  padding: 0.75rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 1rem;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.form-group input[type="text"]:focus,
.form-group textarea:focus {
  outline: none;
  border-color: #42b983;
  box-shadow: 0 0 0 3px rgba(66, 185, 131, 0.1);
}

.form-group textarea {
  resize: vertical;
  min-height: 100px;
  font-family: inherit;
}

/* Button Styles */
.primary-button {
  background-color: #42b983;
  color: white;
  border: none;
  padding: 0.75rem 1.5rem;
  border-radius: 4px;
  cursor: pointer;
  font-size: 1rem;
  transition: background-color 0.2s, transform 0.1s;
}

.primary-button:hover:not(:disabled) {
  background-color: #369e6c;
  transform: translateY(-1px);
}

.primary-button:active:not(:disabled) {
  transform: translateY(0);
}

.primary-button:disabled {
  background-color: #a0e3c1;
  cursor: not-allowed;
}

.secondary-button {
  background-color: #f8f9fa;
  color: #495057;
  border: 1px solid #dee2e6;
  padding: 0.75rem 1.5rem;
  border-radius: 4px;
  cursor: pointer;
  font-size: 1rem;
  transition: background-color 0.2s, border-color 0.2s, transform 0.1s;
}

.secondary-button:hover {
  background-color: #e9ecef;
  border-color: #adb5bd;
  transform: translateY(-1px);
}

.secondary-button:active {
  transform: translateY(0);
}

/* Checkbox Group Styles */
.checkbox-group {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  max-height: 200px;
  overflow-y: auto;
  padding: 0.75rem;
  border: 1px solid #ddd;
  border-radius: 4px;
  background-color: #fafafa;
  transition: border-color 0.2s;
}

.checkbox-group:focus-within {
  outline: none;
  border-color: #42b983;
  box-shadow: 0 0 0 3px rgba(66, 185, 131, 0.1);
}

.checkbox-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
  padding: 0.25rem;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.checkbox-item:hover {
  background-color: rgba(66, 185, 131, 0.05);
}

.checkbox-item input[type="checkbox"] {
  width: 1rem;
  height: 1rem;
  accent-color: #42b983;
  cursor: pointer;
}

.selected-count {
  font-size: 0.85rem;
  color: #42b983;
  font-weight: bold;
  margin-left: 0.5rem;
}

.checkbox-item label {
  margin: 0;
  cursor: pointer;
  font-weight: normal;
}

.form-group select {
  height: 150px;
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

.status-badge {
  display: inline-block;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
  font-size: 0.8rem;
  font-weight: bold;
}

.status-badge.EXECUTING {
  background-color: #ffc107;
  color: #212529;
}

.status-badge.SUCCESS {
  background-color: #28a745;
  color: white;
}

.status-badge.FAILURE {
  background-color: #dc3545;
  color: white;
}

.execution-records-list, .execution-logs-list {
  margin-top: 1rem;
}

.execution-records-list table, .execution-logs-list table {
  width: 100%;
  border-collapse: collapse;
}

.execution-records-list th, .execution-records-list td,
.execution-logs-list th, .execution-logs-list td {
  border: 1px solid #ddd;
  padding: 0.5rem;
  text-align: left;
  font-size: 0.9rem;
}

.log-details {
  margin-top: 1rem;
}

.log-item {
  margin-bottom: 1rem;
}

.log-item pre {
  background-color: #f5f5f5;
  padding: 0.5rem;
  border-radius: 4px;
  overflow-x: auto;
  margin-top: 0.25rem;
}

.log-item.error pre {
  background-color: #f8d7da;
  border-color: #f5c6cb;
  color: #721c24;
}

/* Execution Node Selection Styles */
.execute-modal .modal-content {
  max-width: 500px;
}

.node-select {
  height: auto;
  padding: 0.5rem;
  font-size: 1rem;
  border-radius: 4px;
  border: 1px solid #ddd;
  background-color: white;
  cursor: pointer;
}

.node-select:focus {
  outline: none;
  border-color: #42b983;
  box-shadow: 0 0 0 2px rgba(66, 185, 131, 0.2);
}

.node-option {
  padding: 0.75rem;
  border-bottom: 1px solid #f0f0f0;
}

.node-option:last-child {
  border-bottom: none;
}

.node-info {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}

.node-name {
  font-weight: bold;
  font-size: 1rem;
}

.node-details {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.85rem;
  color: #666;
}

.node-host {
  font-family: monospace;
}

.node-status {
  display: inline-block;
  padding: 0.2rem 0.5rem;
  border-radius: 12px;
  font-size: 0.75rem;
  font-weight: bold;
  text-transform: uppercase;
}

.node-status.ONLINE {
  background-color: #d4edda;
  color: #155724;
  border: 1px solid #c3e6cb;
}

.node-status.OFFLINE {
  background-color: #f8d7da;
  color: #721c24;
  border: 1px solid #f5c6cb;
}

/* Button Styles */
.primary-button {
  background-color: #42b983;
  color: white;
  border: none;
  padding: 0.75rem 1.5rem;
  border-radius: 4px;
  cursor: pointer;
  font-size: 1rem;
  transition: background-color 0.2s;
}

.primary-button:hover:not(:disabled) {
  background-color: #369e6c;
}

.primary-button:disabled {
  background-color: #a0e3c1;
  cursor: not-allowed;
}

.secondary-button {
  background-color: #f8f9fa;
  color: #495057;
  border: 1px solid #dee2e6;
  padding: 0.75rem 1.5rem;
  border-radius: 4px;
  cursor: pointer;
  font-size: 1rem;
  transition: background-color 0.2s;
}

.secondary-button:hover {
  background-color: #e9ecef;
}

/* Execution Plan Info */
.execute-modal .modal-body h4 {
  margin-top: 0;
  margin-bottom: 1rem;
  color: #495057;
}

.checkbox-group {
  display: flex;
  flex-wrap: wrap;
  gap: 1rem;
  max-height: 200px;
  overflow-y: auto;
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.checkbox-item {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  cursor: pointer;
  padding: 0.25rem;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.checkbox-item:hover {
  background-color: rgba(66, 185, 131, 0.05);
}

.checkbox-item input[type="checkbox"] {
  width: 1rem;
  height: 1rem;
  accent-color: #42b983;
  cursor: pointer;
}

.selected-count {
  font-size: 0.85rem;
  color: #42b983;
  font-weight: bold;
  margin-left: 0.5rem;
}

.checkbox-item label {
  margin: 0;
  cursor: pointer;
  font-weight: normal;
}
/* Pagination Styles */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 20px;
  gap: 10px;
}

.pagination button {
  padding: 8px 12px;
  border: 1px solid #ddd;
  background-color: white;
  color: #333;
  border-radius: 4px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.pagination button:hover:not(:disabled) {
  background-color: #f0f0f0;
}

.pagination button:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.pagination span {
  color: #666;
}

.page-size {
  display: flex;
  align-items: center;
  gap: 5px;
  color: #666;
}

.page-size select {
  padding: 5px;
  border-radius: 4px;
  border: 1px solid #ddd;
}

/* Script Pagination Styles */
.script-pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 15px;
  gap: 10px;
  padding-top: 15px;
  border-top: 1px solid #eee;
}

.script-pagination button {
  padding: 6px 10px;
  border: 1px solid #ddd;
  background-color: white;
  color: #333;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 0.85rem;
}

.script-pagination button:hover:not(:disabled) {
  background-color: #f0f0f0;
  border-color: #ccc;
}

.script-pagination button:disabled {
  background-color: #f5f5f5;
  color: #999;
  cursor: not-allowed;
}

.script-pagination span {
  font-size: 0.85rem;
  color: #666;
}

.script-pagination .page-size {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-left: 10px;
}

.script-pagination .page-size label {
  font-size: 0.8rem;
  color: #666;
}

.script-pagination .page-size select {
  padding: 4px 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
  background-color: white;
  cursor: pointer;
  font-size: 0.85rem;
}
</style>