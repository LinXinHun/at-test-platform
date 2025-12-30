<template>
  <div class="script-management">
    <h2>测试脚本管理</h2>
    <div class="script-actions">
      <button @click="showAddScriptDialog = true">添加测试脚本</button>
    </div>
    
    <div class="script-list">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>名称</th>
            <th>描述</th>
            <th>脚本类型</th>
            <th>创建时间</th>
            <th>执行状态</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="script in scripts" :key="script.id">
            <td>{{ script.id }}</td>
            <td>{{ script.name }}</td>
            <td>{{ script.description }}</td>
            <td>{{ script.scriptType }}</td>
            <td>{{ formatDate(script.createdAt) }}</td>
            <td>
              <div v-if="executionStatus.get(script.id)" :class="['execution-status', executionStatus.get(script.id)?.status]">
                {{ executionStatus.get(script.id)?.message }}
              </div>
            </td>
            <td>
              <div class="action-buttons">
                <div class="action-row">
                  <button @click="executeScript(script)" class="action-btn execute-btn" title="执行脚本">
                    <span>执行</span>
                  </button>
                  <button @click="viewExecutionResults(script.id)" class="action-btn view-btn" title="查看执行记录">
                    <span>查看记录</span>
                  </button>
                </div>
                <div class="action-row">
                  <button @click="previewScript(script)" class="action-btn preview-btn" title="预览脚本内容">
                    <span>预览</span>
                  </button>
                  <button @click="editScript(script)" class="action-btn edit-btn" title="编辑脚本">
                    <span>编辑</span>
                  </button>
                  <button @click="deleteScript(script.id)" class="action-btn delete-btn" title="删除脚本">
                    <span>删除</span>
                  </button>
                </div>
              </div>
            </td>
          </tr>
          <tr v-if="scripts.length === 0">
            <td colspan="7" style="text-align: center; padding: 1rem;">
              暂无测试脚本，请点击"添加测试脚本"按钮创建脚本
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
        <select v-model="pageSize" @change="fetchScripts">
          <option value="10">10</option>
          <option value="20">20</option>
          <option value="50">50</option>
          <option value="100">100</option>
        </select>
      </div>
    </div>

    <!-- Execution Results Dialog -->
    <div v-if="showExecutionResults" class="dialog-overlay">
      <div class="dialog execution-results-dialog">
        <div class="dialog-header">
          <h3>脚本执行记录</h3>
          <button @click="closeExecutionResults" class="close-btn">×</button>
        </div>
        <div v-if="loadingResults" class="loading">加载中...</div>
        <div v-else-if="executionResults.length === 0" class="no-results">暂无执行记录</div>
        <div v-else class="execution-results-list">
          <div v-for="result in executionResults" :key="result.id" class="execution-result-item">
            <div class="execution-result-header">
              <div class="execution-result-status" :class="result.status.toLowerCase()">
                {{ result.status }}
              </div>
              <div class="execution-result-time">
                <span>{{ formatDate(result.startTime) }}</span>
                <span class="execution-duration">{{ result.executionTime }}ms</span>
              </div>
              <button @click="deleteExecutionResult(result.id)" class="delete-btn">删除</button>
              <button v-if="result.logFilePath" @click="downloadLog(result.id)" class="download-btn">下载日志</button>
            </div>
            <div class="execution-result-content">
              <div v-if="result.status.toLowerCase() === 'success' && result.output" class="execution-output">
                <h4>输出:</h4>
                <pre>{{ result.output.length > 30 ? result.output.substring(0, 30) + '...' : result.output }}</pre>
              </div>
              <div v-else-if="(result.status.toLowerCase() === 'failure' || result.status.toLowerCase() === 'timeout') && result.error" class="execution-error">
                <h4>错误信息:</h4>
                <pre>{{ result.error }}</pre>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Add/Edit Script Dialog -->
    <div v-if="showAddScriptDialog || showEditScriptDialog" class="dialog-overlay">
      <div class="dialog">
        <div class="dialog-header">
          <h3>{{ showEditScriptDialog ? '编辑测试脚本' : '添加测试脚本' }}</h3>
          <button @click="closeDialog" class="close-btn">×</button>
        </div>
        <form @submit.prevent="saveScript" enctype="multipart/form-data">
          <div class="form-group">
            <label for="name">名称</label>
            <input type="text" id="name" v-model="currentScript.name" required>
          </div>
          <div class="form-group">
            <label for="description">描述</label>
            <textarea id="description" v-model="currentScript.description" required></textarea>
          </div>
          <div class="form-group">
            <label for="scriptFile">脚本文件</label>
            <input type="file" id="scriptFile" @change="handleFileChange" accept=".py,.sh,.js" required>
            <div v-if="uploadedFileName" class="file-name">已选择文件: {{ uploadedFileName }}</div>
          </div>
          <div class="form-group">
            <label for="parameters">参数</label>
            <input type="text" id="parameters" v-model="currentScript.parameters" placeholder="param1,param2">
          </div>
          <div class="form-group">
            <label for="scriptType">脚本类型</label>
            <select id="scriptType" v-model="currentScript.scriptType" required>
              <option value="python">Python</option>
              <option value="bash">Bash</option>
              <option value="shell">Shell</option>
              <option value="node">Node</option>
            </select>
          </div>
          <div class="form-group">
            <label for="timeout">超时时间</label>
            <input type="number" id="timeout" v-model="currentScript.timeout" placeholder="秒">
          </div>
          <div class="form-group">
            <label for="retryCount">重试次数</label>
            <input type="number" id="retryCount" v-model="currentScript.retryCount" placeholder="次数">
          </div>
          <div class="dialog-actions">
            <button type="button" @click="closeDialog">取消</button>
            <button type="submit">保存</button>
          </div>
        </form>
      </div>
    </div>
    <!-- Preview Script Dialog -->
    <div v-if="showPreviewDialog" class="dialog-overlay">
      <div class="dialog preview-dialog">
        <div class="dialog-header">
          <h3>脚本预览</h3>
          <button @click="closePreviewDialog" class="close-btn">×</button>
        </div>
        <div class="preview-content">
          <div v-if="loadingPreview" class="loading">加载中...</div>
          <div v-else>
            <div class="preview-info">
              <div class="info-item">
                <span class="info-label">脚本名称：</span>
                <span class="info-value">{{ previewScriptData.name }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">脚本类型：</span>
                <span class="info-value">{{ previewScriptData.scriptType }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">描述：</span>
                <span class="info-value">{{ previewScriptData.description }}</span>
              </div>
            </div>
            <div class="script-content">
              <h4>脚本内容</h4>
              <pre>{{ previewScriptContent }}</pre>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { testScriptApi, testExecutionResultApi } from '../services/api';

interface TestScript {
  id: number;
  name: string;
  description: string;
  filePath: string;
  parameters: string;
  scriptType: string;
  timeout: number;
  retryCount: number;
  createdAt: string;
  updatedAt: string;
}

interface TestExecutionResult {
  id: number;
  testScriptId: number;
  status: string;
  output: string;
  error: string;
  startTime: string;
  endTime: string;
  executionTime: number;
  logFilePath: string;
}

const scripts = ref<TestScript[]>([]);
const showAddScriptDialog = ref(false);
const showEditScriptDialog = ref(false);
const currentScript = ref<Partial<TestScript>>({
  name: '',
  description: '',
  parameters: '',
  scriptType: 'python',
  timeout: 0,
  retryCount: 0
});
const executionStatus = ref<Map<number, { status: 'running' | 'success' | 'error', message: string }>>(new Map());

// 分页相关状态
const currentPage = ref(0);
const pageSize = ref(10);
const totalPages = ref(1);
const totalElements = ref(0);

// 文件上传相关
const scriptFile = ref<File | null>(null);
const uploadedFileName = ref('');

// 执行记录相关状态
const showExecutionResults = ref(false);
const selectedScriptId = ref(0);
const executionResults = ref<TestExecutionResult[]>([]);
const loadingResults = ref(false);

onMounted(() => {
  fetchScripts();
});

const fetchScripts = async () => {
  try {
    const response = await testScriptApi.getAll(currentPage.value, pageSize.value);
    scripts.value = response.data.content;
    totalPages.value = response.data.totalPages;
    totalElements.value = response.data.totalElements;
    
    // 获取每个脚本的最后一次执行记录
    for (const script of scripts.value) {
      try {
        const resultResponse = await testExecutionResultApi.getLastByScriptId(script.id);
        const lastExecution = resultResponse.data;
        if (lastExecution) {
          // 根据后端返回的状态设置
          const statusMap: Record<string, 'success' | 'error'> = {
            'success': 'success',
            'failed': 'error',
            'failure': 'error',
            'error': 'error'
          };
          
          const finalStatus = statusMap[lastExecution.status] || 'error';
          const statusMessageMap: Record<string, string> = {
            'success': '执行成功',
            'failed': '执行失败',
            'failure': '执行失败',
            'error': '执行失败'
          };
          
          executionStatus.value.set(script.id, {
            status: finalStatus,
            message: statusMessageMap[lastExecution.status] || '执行失败'
          });
        }
      } catch (error) {
        console.error(`Failed to fetch last execution result for script ${script.id}:`, error);
      }
    }
  } catch (error) {
    console.error('Failed to fetch scripts:', error);
  }
};

const changePage = (page: number) => {
  if (page >= 0 && page < totalPages.value) {
    currentPage.value = page;
    fetchScripts();
  }
};

const handleFileChange = (event: Event) => {
  const input = event.target as HTMLInputElement;
  if (input.files && input.files[0]) {
    scriptFile.value = input.files[0];
    uploadedFileName.value = input.files[0].name;
  }
};

const saveScript = async () => {
  try {
    if (!scriptFile.value) {
      alert('请选择脚本文件');
      return;
    }

    const formData = new FormData();
    formData.append('name', currentScript.value.name || '');
    formData.append('description', currentScript.value.description || '');
    formData.append('scriptFile', scriptFile.value);
    formData.append('parameters', currentScript.value.parameters || '');
    formData.append('scriptType', currentScript.value.scriptType || 'python');
    formData.append('timeout', currentScript.value.timeout?.toString() || '0');
    formData.append('retryCount', currentScript.value.retryCount?.toString() || '0');

    if (showEditScriptDialog.value && currentScript.value.id) {
      await testScriptApi.update(currentScript.value.id, formData);
    } else {
      await testScriptApi.create(formData);
    }
    await fetchScripts();
    closeDialog();
  } catch (error) {
    console.error('Failed to save script:', error);
  }
};

const editScript = (script: TestScript) => {
  currentScript.value = { ...script };
  showEditScriptDialog.value = true;
  // 编辑时显示已上传的文件名（从filePath中提取）
  if (script.filePath) {
    uploadedFileName.value = script.filePath;
  } else {
    uploadedFileName.value = '';
  }
  scriptFile.value = null;
};

const deleteScript = async (id: number) => {
  if (confirm('确定要删除这个测试脚本吗？')) {
    try {
      await testScriptApi.delete(id);
      await fetchScripts();
    } catch (error) {
      console.error('Failed to delete script:', error);
    }
  }
};

const closeDialog = () => {
  showAddScriptDialog.value = false;
  showEditScriptDialog.value = false;
  currentScript.value = {
    name: '',
    description: '',
    parameters: '',
    scriptType: 'python',
    timeout: 0,
    retryCount: 0
  };
  // 重置文件上传
  scriptFile.value = null;
  uploadedFileName.value = '';
  // 重置文件输入框
  const input = document.getElementById('scriptFile') as HTMLInputElement;
  if (input) {
    input.value = '';
  }
};

const formatDate = (dateString: string) => {
  const date = new Date(dateString);
  return date.toLocaleString();
};

const executeScript = async (script: TestScript) => {
  try {
    executionStatus.value.set(script.id, { status: 'running', message: '执行中' });
    const response = await testScriptApi.execute(script.id);
    console.log('执行脚本API返回结果:', response.data);
    
    // 定义默认的成功状态
    const successStatus = {
      status: 'success' as const,
      message: '执行成功'
    };
    
    // 定义默认的错误状态
    const errorStatus = {
      status: 'error' as const,
      message: '执行失败'
    };
    
    // 根据脚本执行结果设置初始状态
    const initialStatus = response.data.success ? successStatus : errorStatus;
    
    // 执行成功后获取最后一次执行记录，确保状态与后端一致
    try {
      const resultResponse = await testExecutionResultApi.getLastByScriptId(script.id);
      const lastExecution = resultResponse.data;
      console.log('获取最后一次执行记录:', lastExecution);
      if (lastExecution) {
        // 根据后端返回的状态设置
        const statusMap: Record<string, 'success' | 'error'> = {
          'success': 'success',
          'failed': 'error',
          'error': 'error'
        };
        
        const finalStatus = statusMap[lastExecution.status] || 'error';
        const statusMessageMap: Record<string, string> = {
          'success': '执行成功',
          'failed': '执行失败',
          'error': '执行失败'
        };
        
        executionStatus.value.set(script.id, {
          status: finalStatus,
          message: statusMessageMap[lastExecution.status] || '执行失败'
        });
      } else {
        // 如果获取不到执行记录，使用初始状态
        executionStatus.value.set(script.id, initialStatus);
      }
    } catch (fetchError: any) {
      console.error('获取最后一次执行记录失败:', fetchError);
      // 即使获取最后一次执行记录失败，也要使用初始状态
      executionStatus.value.set(script.id, initialStatus);
    }
  } catch (error: any) {
    console.error('执行脚本失败:', error);
    executionStatus.value.set(script.id, {
      status: 'error',
      message: '执行失败'
    });
  }
};

// 执行记录相关函数
const viewExecutionResults = async (scriptId: number) => {
  try {
    selectedScriptId.value = scriptId;
    loadingResults.value = true;
    const response = await testExecutionResultApi.getByScriptId(scriptId);
    executionResults.value = response.data;
    showExecutionResults.value = true;
    loadingResults.value = false;
  } catch (error) {
    console.error('Failed to fetch execution results:', error);
    loadingResults.value = false;
  }
};

const closeExecutionResults = () => {
  showExecutionResults.value = false;
  selectedScriptId.value = 0;
  executionResults.value = [];
};

const deleteExecutionResult = async (id: number) => {
  if (confirm('确定要删除这个执行记录吗？')) {
    try {
      await testExecutionResultApi.delete(id);
      // 重新获取执行记录
      const response = await testExecutionResultApi.getByScriptId(selectedScriptId.value);
      executionResults.value = response.data;
    } catch (error) {
      console.error('Failed to delete execution result:', error);
    }
  }
};

const downloadLog = async (resultId: number) => {
  try {
    const response = await testExecutionResultApi.downloadLog(resultId);
    
    // 创建下载链接
    const url = window.URL.createObjectURL(new Blob([response.data]));
    const link = document.createElement('a');
    link.href = url;
    
    // 设置文件名
    const contentDisposition = response.headers['content-disposition'];
    let fileName = `execution-${resultId}.log`;
    if (contentDisposition) {
      const match = contentDisposition.match(/filename="([^"]+)"/);
      if (match) {
        fileName = match[1];
      }
    }
    
    link.setAttribute('download', fileName);
    document.body.appendChild(link);
    link.click();
    
    // 清理
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);
  } catch (error) {
    console.error('下载日志失败:', error);
    alert('下载日志失败，请检查网络连接或重试');
  }
};

// 预览脚本相关状态
const showPreviewDialog = ref(false);
const previewScriptData = ref<TestScript | null>(null);
const previewScriptContent = ref('');
const loadingPreview = ref(false);

const previewScript = async (script: TestScript) => {
  try {
    loadingPreview.value = true;
    previewScriptData.value = script;
    
    // 下载脚本文件内容
    const response = await testScriptApi.download(script.filePath);
    previewScriptContent.value = response.data;
    
    showPreviewDialog.value = true;
  } catch (error) {
    console.error('Failed to fetch script content:', error);
    alert('预览脚本内容失败，请检查网络连接或重试');
  } finally {
    loadingPreview.value = false;
  }
};

const closePreviewDialog = () => {
  showPreviewDialog.value = false;
  previewScriptData.value = null;
  previewScriptContent.value = '';
};
</script>

<style scoped>
.script-management {
  padding: 20px;
}

.script-actions {
  margin-bottom: 20px;
}

button.download-btn {
  background-color: #409eff;
  margin-right: 0.5rem;
}

button.download-btn:hover {
  background-color: #66b1ff;
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

.delete-btn {
  background-color: #dc3545;
  color: white;
}

.delete-btn:hover {
  background-color: #c82333;
}

.script-list table {
  width: 100%;
  border-collapse: collapse;
}

.script-list th,
.script-list td {
  border: 1px solid #ddd;
  padding: 8px;
  text-align: left;
}

.script-list th {
  background-color: #f2f2f2;
}

.dialog-overlay {
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

.dialog {
  background-color: white;
  padding: 20px;
  border-radius: 8px;
  width: 80%;
  max-width: 600px;
  max-height: 80vh;
  overflow-y: auto;
}

.form-group {
  margin-bottom: 15px;
}

.form-group label {
  display: block;
  margin-bottom: 5px;
  font-weight: bold;
}

.form-group input,
.form-group textarea,
.form-group select {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.form-group select {
  cursor: pointer;
  background-color: white;
  appearance: none;
  background-image: url("data:image/svg+xml;charset=UTF-8,%3csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 24 24' fill='none' stroke='currentColor' stroke-width='2' stroke-linecap='round' stroke-linejoin='round'%3e%3cpolyline points='6 9 12 15 18 9'%3e%3c/polyline%3e%3c/svg%3e");
  background-repeat: no-repeat;
  background-position: right 0.7em top 50%;
  background-size: 1em;
  padding-right: 2.5em;
}

.form-group select:focus {
  outline: none;
  border-color: #646cff;
  box-shadow: 0 0 0 2px rgba(100, 108, 255, 0.2);
}

.form-group select:hover {
  border-color: #646cff;
}

.file-name {
  margin-top: 5px;
  color: #666;
  font-size: 14px;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 20px;
}

.execution-status {
  display: inline-block;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: bold;
}

.execution-status.running {
  background-color: #ffc107;
  color: #212529;
}

.execution-status.success {
  background-color: #28a745;
  color: white;
}

.execution-status.error {
  background-color: #dc3545;
  color: white;
}

.execution-results-dialog {
  width: 90%;
  max-width: 800px;
}

.execution-result-item {
  margin-bottom: 20px;
  padding: 15px;
  border: 1px solid #ddd;
  border-radius: 8px;
}

.execution-result-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.execution-result-status {
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: bold;
}

.execution-result-status.success {
  background-color: #28a745;
  color: white;
}

.execution-result-status.failed {
  background-color: #dc3545;
  color: white;
}

.execution-result-status.error {
  background-color: #6c757d;
  color: white;
}

/* 确保弹窗头部和关闭按钮样式正确 */
.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  width: 100%;
}

.dialog-header h3 {
  margin: 0;
  padding: 0;
}

.close-btn {
  background: none;
  border: none;
  font-size: 24px;
  cursor: pointer;
  color: #666;
  padding: 0;
  line-height: 1;
  float: right;
}

.close-btn:hover {
  color: #333;
  background: none;
}

.execution-output {
  margin-top: 10px;
}

.execution-output pre {
  background-color: #f8f9fa;
  padding: 10px;
  border-radius: 4px;
  overflow-x: auto;
  white-space: pre-wrap;
}

/* 分页组件样式 */
.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px;
  border-top: 1px solid #ddd;
}

.pagination button {
  background-color: #409eff;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
  margin: 0 5px;
  transition: background-color 0.3s;
}

.pagination button:hover {
  background-color: #66b1ff;
}

.pagination button:disabled {
  background-color: #c0c4cc;
  cursor: not-allowed;
}

.pagination span {
  margin: 0 10px;
  color: #606266;
}

.pagination .page-size {
  display: flex;
  align-items: center;
}

.pagination .page-size label {
  margin-right: 5px;
  color: #606266;
}

.pagination .page-size select {
  padding: 5px 10px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  background-color: white;
  cursor: pointer;
}
</style>

/* Preview Dialog Styles */
.preview-dialog {
  width: 80%;
  max-width: 1000px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
}

.preview-content {
  flex: 1;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
  padding-bottom: 0.5rem;
  border-bottom: 1px solid #e0e0e0;
}

.preview-header h4 {
  margin: 0;
  color: #333;
}

.script-type {
  background-color: #e3f2fd;
  color: #1976d2;
  padding: 0.25rem 0.75rem;
  border-radius: 1rem;
  font-size: 0.875rem;
  font-weight: 500;
}

.script-content {
  flex: 1;
  overflow: auto;
  background-color: #f5f5f5;
  padding: 1rem;
  border-radius: 4px;
  max-height: 60vh;
}

.script-content pre {
  margin: 0;
  white-space: pre-wrap;
  word-wrap: break-word;
  font-family: 'Courier New', Courier, monospace;
  font-size: 0.9rem;
  line-height: 1.5;
  color: #333;
}

/* Ensure dialog overlay covers the entire screen */
.dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

/* Preview Info Styles */
.preview-info {
  margin-bottom: 20px;
  padding: 15px;
  background-color: #f8f9fa;
  border-radius: 4px;
  border: 1px solid #e0e0e0;
}

.info-item {
  display: flex;
  margin-bottom: 10px;
  align-items: center;
}

.info-item:last-child {
  margin-bottom: 0;
}

.info-label {
  font-weight: bold;
  color: #606266;
  min-width: 80px;
}

.info-value {
  color: #303133;
  flex: 1;
}

.loading {
  text-align: center;
  padding: 40px;
  color: #909399;
  font-size: 14px;
}

/* Action Buttons Styles */
.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.action-row {
  display: flex;
  gap: 5px;
}

.action-btn {
  padding: 4px 8px;
  font-size: 12px;
  margin: 0;
  white-space: nowrap;
}

.execute-btn {
  background-color: #67c23a;
}

.execute-btn:hover {
  background-color: #85ce61;
}

.view-btn {
  background-color: #409eff;
}

.view-btn:hover {
  background-color: #66b1ff;
}

.preview-btn {
  background-color: #e6a23c;
}

.preview-btn:hover {
  background-color: #ebb563;
}

.edit-btn {
  background-color: #909399;
}

.edit-btn:hover {
  background-color: #a6a9ad;
}
