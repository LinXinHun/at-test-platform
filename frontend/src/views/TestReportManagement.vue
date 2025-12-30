<template>
  <div class="report-management">
    <h2>测试报告管理</h2>
    <div class="filter-section">
      <input type="text" placeholder="搜索执行计划名称..." v-model="searchQuery" />
      <button @click="searchReports">搜索</button>
    </div>
    
    <div class="report-list">
      <h3>报告列表</h3>
      <table>
        <thead>
          <tr>
            <th>报告ID</th>
            <th>执行计划名称</th>
            <th>执行记录ID</th>
            <th>生成时间</th>
            <th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="report in reports" :key="report.id">
            <td>{{ report.id }}</td>
            <td>{{ report.planName }}</td>
            <td>{{ report.executionId }}</td>
            <td>{{ formatDate(report.createdAt) }}</td>
            <td>
              <button @click="viewReportDetails(report.id)">查看详情</button>
              <button @click="deleteReport(report.id)">删除</button>
            </td>
          </tr>
        </tbody>
      </table>
      
      <!-- 分页控件 -->
      <div class="pagination">
        <button @click="changePage(1)" :disabled="currentPage === 1">首页</button>
        <button @click="changePage(currentPage - 1)" :disabled="currentPage === 1">上一页</button>
        <span>第 {{ currentPage }} 页，共 {{ totalPages }} 页</span>
        <button @click="changePage(currentPage + 1)" :disabled="currentPage === totalPages">下一页</button>
        <button @click="changePage(totalPages)" :disabled="currentPage === totalPages">末页</button>
        <select v-model="pageSize" @change="changePageSize">
          <option value="5">5条/页</option>
          <option value="10">10条/页</option>
          <option value="20">20条/页</option>
        </select>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { testReportApi } from '../services/api';

const router = useRouter();

interface TestReport {
  id: number;
  planName: string;
  executionId: number;
  createdAt: string;
}

const reports = ref<TestReport[]>([]);
const searchQuery = ref('');
const currentPage = ref(1);
const pageSize = ref(10);
const totalPages = ref(1);
const totalItems = ref(0);

onMounted(() => {
  fetchReports();
});

const fetchReports = async () => {
  try {
    const response = await testReportApi.getAll(currentPage.value - 1, pageSize.value);
    reports.value = response.data.content;
    totalItems.value = response.data.totalElements;
    totalPages.value = response.data.totalPages;
  } catch (error) {
    console.error('获取报告列表失败:', error);
  }
};

const searchReports = async () => {
  try {
    currentPage.value = 1;
    const response = await testReportApi.search(searchQuery.value, currentPage.value - 1, pageSize.value);
    reports.value = response.data.content;
    totalItems.value = response.data.totalElements;
    totalPages.value = response.data.totalPages;
  } catch (error) {
    console.error('搜索报告失败:', error);
  }
};

const viewReportDetails = (reportId: number) => {
  router.push(`/reports/${reportId}`);
};

const deleteReport = async (reportId: number) => {
  try {
    await testReportApi.delete(reportId);
    // 刷新当前页数据
    fetchReports();
  } catch (error) {
    console.error('删除报告失败:', error);
  }
};

const changePage = (page: number) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page;
    fetchReports();
  }
};

const changePageSize = () => {
  currentPage.value = 1;
  fetchReports();
};

const formatDate = (dateString: string) => {
  const date = new Date(dateString);
  return date.toLocaleString();
};
</script>

<style scoped>
.report-management {
  max-width: 1200px;
  margin: 0 auto;
}

.filter-section {
  display: flex;
  gap: 1rem;
  margin-bottom: 1rem;
  flex-wrap: wrap;
}

.filter-section input {
  padding: 0.5rem;
  border-radius: 4px;
  border: 1px solid #ddd;
  flex: 1;
  min-width: 200px;
}

.report-list {
  margin-bottom: 1rem;
}

.report-list table {
  width: 100%;
  border-collapse: collapse;
}

.report-list th, .report-list td {
  border: 1px solid #ddd;
  padding: 0.5rem;
  text-align: left;
}

.report-list th {
  background-color: #f0f0f0;
}

/* 分页样式 */
.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 1rem;
  gap: 0.5rem;
}

.pagination button {
  background-color: #42b983;
  color: white;
  border: none;
  padding: 0.3rem 0.8rem;
  border-radius: 4px;
  cursor: pointer;
}

.pagination button:hover:not(:disabled) {
  background-color: #35495e;
}

.pagination button:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

.pagination select {
  padding: 0.3rem;
  border-radius: 4px;
  border: 1px solid #ddd;
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