<template>
  <div class="task-management">
    <h2>执行任务管理</h2>
    <div class="task-actions">
      <button @click="showAddTaskDialog = true">创建执行任务</button>
    </div>
    
    <div class="task-list">
      <table>
        <thead>
          <tr>
            <th>ID</th>
            <th>名称</th>
            <th>测试计划</th>
            <th>状态</th>
            <th>开始时间</th>
            <th>结束时间</th>
            <th>创建时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="task in tasks" :key="task.id">
            <td>{{ task.id }}</td>
            <td>{{ task.name }}</td>
            <td>{{ task.testPlan.name }}</td>
            <td>
              <span :class="`status-${task.status.toLowerCase()}`">{{ task.status }}</span>
            </td>
            <td>{{ task.startTime ? formatDate(task.startTime) : '-' }}</td>
            <td>{{ task.endTime ? formatDate(task.endTime) : '-' }}</td>
            <td>{{ formatDate(task.createdAt) }}</td>
            <td>
              <button 
                @click="executeTask(task.id)" 
                :disabled="task.status === 'RUNNING'"
                class="execute-btn"
              >
                执行
              </button>
              <button 
                @click="viewResults(task.id)" 
                :disabled="!['COMPLETED', 'FAILED'].includes(task.status)"
                class="view-btn"
              >
                查看结果
              </button>
              <button @click="deleteTask(task.id)" class="delete-btn">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- Add Task Dialog -->
    <div v-if="showAddTaskDialog" class="dialog-overlay">
      <div class="dialog">
        <h3>创建执行任务</h3>
        <form @submit.prevent="saveTask">
          <div class="form-group">
            <label for="name">任务名称</label>
            <input type="text" id="name" v-model="currentTask.name" required>
          </div>
          <div class="form-group">
            <label for="plan">选择测试计划</label>
            <select id="plan" v-model="currentTask.selectedPlan" required>
              <option value="">请选择测试计划</option>
              <option v-for="plan in plans" :key="plan.id" :value="plan">
                {{ plan.name }} ({{ plan.scripts.length }}个脚本)
              </option>
            </select>
          </div>
          <div class="dialog-actions">
            <button type="button" @click="closeDialog">取消</button>
            <button type="submit">创建</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { testExecutionTaskApi, testPlanApi } from '../services/api';

interface TestPlan {
  id: number;
  name: string;
  scripts: any[];
}

interface TestExecutionTask {
  id: number;
  name: string;
  testPlan: TestPlan;
  status: string;
  startTime: string | null;
  endTime: string | null;
  createdAt: string;
  updatedAt: string;
}

const router = useRouter();
const tasks = ref<TestExecutionTask[]>([]);
const plans = ref<TestPlan[]>([]);
const showAddTaskDialog = ref(false);
const currentTask = ref<Partial<TestExecutionTask> & { selectedPlan?: TestPlan }>({
  name: '',
  selectedPlan: undefined
});

onMounted(() => {
  fetchTasks();
  fetchPlans();
});

const fetchTasks = async () => {
  try {
    const response = await testExecutionTaskApi.getAll();
    tasks.value = response.data;
  } catch (error) {
    console.error('Failed to fetch tasks:', error);
  }
};

const fetchPlans = async () => {
  try {
    const response = await testPlanApi.getAll();
    plans.value = response.data;
  } catch (error) {
    console.error('Failed to fetch plans:', error);
  }
};

const saveTask = async () => {
  try {
    if (currentTask.value.selectedPlan) {
      const taskData = {
        name: currentTask.value.name,
        testPlan: currentTask.value.selectedPlan
      };
      
      await testExecutionTaskApi.create(taskData);
      await fetchTasks();
      closeDialog();
    }
  } catch (error) {
    console.error('Failed to save task:', error);
  }
};

const executeTask = async (id: number) => {
  try {
    await testExecutionTaskApi.execute(id);
    await fetchTasks();
  } catch (error) {
    console.error('Failed to execute task:', error);
  }
};

const viewResults = (taskId: number) => {
  router.push(`/results/${taskId}`);
};

const deleteTask = async (id: number) => {
  if (confirm('确定要删除这个执行任务吗？')) {
    try {
      await testExecutionTaskApi.delete(id);
      await fetchTasks();
    } catch (error) {
      console.error('Failed to delete task:', error);
    }
  }
};

const closeDialog = () => {
  showAddTaskDialog.value = false;
  currentTask.value = {
    name: '',
    selectedPlan: undefined
  };
};

const formatDate = (dateString: string | null) => {
  if (!dateString) return '-';
  const date = new Date(dateString);
  return date.toLocaleString();
};
</script>

<style scoped>
.task-management {
  max-width: 1200px;
  margin: 0 auto;
}

.task-actions {
  margin-bottom: 1rem;
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

button:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
}

.execute-btn {
  background-color: #409eff;
}

.view-btn {
  background-color: #67c23a;
}

.delete-btn {
  background-color: #ff6b6b;
}

.task-list table {
  width: 100%;
  border-collapse: collapse;
}

.task-list th, .task-list td {
  border: 1px solid #ddd;
  padding: 0.5rem;
  text-align: left;
}

.task-list th {
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

.status-created {
  color: #909399;
  font-weight: bold;
}

/* Dialog Styles */
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

.dialog {
  background-color: white;
  padding: 1.5rem;
  border-radius: 8px;
  width: 90%;
  max-width: 600px;
  max-height: 80vh;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
}

.dialog-actions {
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

.form-group {
  margin-bottom: 1rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
}

.form-group input,
.form-group select {
  width: 100%;
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.dialog-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  margin-top: 1rem;
}
</style>