import { createRouter, createWebHistory } from 'vue-router';
import type { RouteRecordRaw } from 'vue-router';
import HomeView from '../views/HomeView.vue';
import TestScriptManagement from '../views/TestScriptManagement.vue';
import TestPlanManagement from '../views/TestPlanManagement.vue';
import TestExecutionTaskManagement from '../views/TestExecutionTaskManagement.vue';
import TestResultView from '../views/TestResultView.vue';
import TestReportManagement from '../views/TestReportManagement.vue';
import TestReportDetail from '../views/TestReportDetail.vue';
import ExecutionNodeManagement from '../views/ExecutionNodeManagement.vue';

const routes: Array<RouteRecordRaw> = [
  { path: '/', name: 'home', component: HomeView },
  { path: '/scripts', name: 'test-scripts', component: TestScriptManagement },
  { path: '/plans', name: 'test-plans', component: TestPlanManagement },
  { path: '/tasks', name: 'test-tasks', component: TestExecutionTaskManagement },
  { path: '/results/:taskId', name: 'test-results', component: TestResultView },
  { path: '/reports', name: 'test-reports', component: TestReportManagement },
  { path: '/reports/:id', name: 'test-report-detail', component: TestReportDetail },
  { path: '/execution-nodes', name: 'execution-nodes', component: ExecutionNodeManagement },
];

const router = createRouter({
  history: createWebHistory(), // Removed process.env.BASE_URL
  routes,
});

export default router;