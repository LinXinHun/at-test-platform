import axios from 'axios';

// Create axios instance with base URL
const api = axios.create({
  baseURL: 'http://localhost:8080/api',
});

// API Services
export const testScriptApi = {
  getAll: (page: number = 0, size: number = 10) => api.get(`/scripts?page=${page}&size=${size}`),
  getById: (id: number) => api.get(`/scripts/${id}`),
  create: (data: any) => api.post('/scripts', data, { headers: { 'Content-Type': undefined } }),
  update: (id: number, data: any) => api.put(`/scripts/${id}`, data, { headers: { 'Content-Type': undefined } }),
  delete: (id: number) => api.delete(`/scripts/${id}`),
  execute: (id: number) => api.post(`/scripts/${id}/execute`),
  download: (filePath: string) => api.get(`/scripts/download?filePath=${filePath}`, { responseType: 'text' }),
};

export const testPlanApi = {
  getAll: (page: number = 0, size: number = 10) => api.get(`/plans?page=${page}&size=${size}`),
  getById: (id: number) => api.get(`/plans/${id}`),
  create: (data: any) => api.post('/plans', data),
  update: (id: number, data: any) => api.put(`/plans/${id}`, data),
  delete: (id: number) => api.delete(`/plans/${id}`),
};

export const testPlanExecutionApi = {
  execute: (planId: number, nodeId: number) => api.post(`/plan-executions?planId=${planId}&nodeIdList=${nodeId}`),
  getById: (id: number) => api.get(`/plan-executions/${id}`),
  getByPlanId: (planId: number) => api.get(`/plan-executions/plan/${planId}`),
  getLogsByExecutionId: (executionId: number) => api.get(`/plan-executions/${executionId}/logs`),
  getLogsByExecutionIdWithPagination: (executionId: number, page: number, size: number) => 
    api.get(`/plan-executions/${executionId}/logs/page?page=${page}&size=${size}`),
  downloadLog: (planId: number, executionId: number, scriptId: number) => 
    api.get(`/plan-executions/logs/download?planId=${planId}&executionId=${executionId}&scriptId=${scriptId}`, { responseType: 'blob' }),
};

export const executionNodeApi = {
  getAll: () => api.get('/execution-nodes'),
  getById: (id: number) => api.get(`/execution-nodes/${id}`),
};

export const testExecutionTaskApi = {
  getAll: () => api.get('/tasks'),
  getById: (id: number) => api.get(`/tasks/${id}`),
  create: (data: any) => api.post('/tasks', data),
  execute: (id: number) => api.post(`/tasks/${id}/execute`),
  delete: (id: number) => api.delete(`/tasks/${id}`),
};

export const testResultApi = {
  getAll: () => api.get('/results'),
  getById: (id: number) => api.get(`/results/${id}`),
  getByTaskId: (taskId: number) => api.get(`/results/task/${taskId}`),
};

export const testReportApi = {
  getAll: (page: number, size: number) => api.get(`/reports?page=${page}&size=${size}`),
  getById: (id: number) => api.get(`/reports/${id}`),
  getByExecutionId: (executionId: number) => api.get(`/reports/execution/${executionId}`),
  generate: (executionId: number) => api.post(`/reports/generate/${executionId}`),
  delete: (id: number) => api.delete(`/reports/${id}`),
  search: (planName: string, page: number = 0, size: number = 10) => api.get(`/reports/search?planName=${planName}&page=${page}&size=${size}`),
};

export const testExecutionResultApi = {
  getByScriptId: (scriptId: number) => api.get(`/execution-results/script/${scriptId}`),
  getLastByScriptId: (scriptId: number) => api.get(`/execution-results/script/${scriptId}/last`),
  getById: (id: number) => api.get(`/execution-results/${id}`),
  delete: (id: number) => api.delete(`/execution-results/${id}`),
  downloadLog: (id: number) => api.get(`/execution-results/${id}/log`, { responseType: 'blob' }),
};

export default api;