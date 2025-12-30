<template>
  <div class="report-detail">
    <h2>测试报告详情</h2>
    
    <!-- 报告基本信息 -->
    <div class="report-info">
      <div class="info-item">
        <span class="label">报告ID:</span>
        <span class="value">{{ report.id }}</span>
      </div>
      <div class="info-item">
        <span class="label">执行计划名称:</span>
        <span class="value">{{ report.planName }}</span>
      </div>
      <div class="info-item">
        <span class="label">执行记录ID:</span>
        <span class="value">{{ report.executionId }}</span>
      </div>
      <div class="info-item">
        <span class="label">生成时间:</span>
        <span class="value">{{ formatDate(report.generationTime) }}</span>
      </div>
    </div>
    
    <!-- 测试结果统计图表 -->
    <div class="charts-container">
      <div class="chart-item">
        <h3>测试结果分布</h3>
        <div class="pie-chart" ref="pieChartRef"></div>
        <div class="stats">
          <div class="stat">
            <span class="stat-label">总数</span>
            <span class="stat-value">{{ report.totalScripts }}</span>
          </div>
          <div class="stat">
            <span class="stat-label">成功</span>
            <span class="stat-value success">{{ report.successScripts }}</span>
          </div>
          <div class="stat">
            <span class="stat-label">失败</span>
            <span class="stat-value failed">{{ report.failedScripts }}</span>
          </div>
        </div>
      </div>
      
      <!-- 脚本执行耗时占比 -->
      <div class="chart-item">
        <h3>脚本执行耗时占比</h3>
        <div class="bar-chart" ref="barChartRef"></div>
      </div>
      
      <!-- 脚本耗时分布饼图 -->
      <div class="chart-item">
        <h3>脚本耗时分布</h3>
        <div class="time-pie-chart" ref="timePieChartRef"></div>
      </div>
      
      <!-- 脚本耗时差异对比 -->
      <div class="chart-item" v-show="hasPreviousExecution">
        <h3>脚本耗时差异对比 (与上一次执行)</h3>
        <div class="diff-chart" ref="diffChartRef"></div>
      </div>
    </div>
    
    <!-- 测试脚本结果列表 -->
    <div class="script-results">
      <h3>测试脚本结果</h3>
      <table>
        <thead>
          <tr>
            <th>脚本名称</th>
            <th>状态</th>
            <th>执行耗时 (ms)</th>
            <th>错误信息</th>
          </tr>
        </thead>
        <tbody>
          <!-- 根据reportData解析出脚本结果 -->
          <tr v-for="(result, index) in scriptResults" :key="index">
            <td>{{ result.scriptName }}</td>
            <td>
              <span :class="`status-${result.status.toLowerCase()}`">{{ result.status }}</span>
            </td>
            <td>{{ result.executionTime }}</td>
            <td>{{ result.errorMessage || '-' }}</td>
          </tr>
        </tbody>
      </table>
    </div>
    
    <button @click="goBack" class="back-button">返回报告列表</button>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { testReportApi } from '../services/api';
import { use, init, ECharts } from 'echarts/core';
import { PieChart, BarChart } from 'echarts/charts';
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent
} from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';

// 注册组件
use([
  PieChart,
  BarChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
  CanvasRenderer
]);

const route = useRoute();
const router = useRouter();
const reportId = Number(route.params.id);

// 报告数据
const report = ref({
  id: 0,
  executionId: 0,
  planName: '',
  totalScripts: 0,
  successScripts: 0,
  failedScripts: 0,
  passRate: 0,
  generationTime: '',
  reportData: '{}',
  status: ''
});

// 解析后的脚本结果
const scriptResults = ref<any[]>([]);

// 是否有上一次执行记录
const hasPreviousExecution = ref(false);

// 图表引用
const pieChartRef = ref<HTMLElement | null>(null);
const barChartRef = ref<HTMLElement | null>(null);
const diffChartRef = ref<HTMLElement | null>(null);
const timePieChartRef = ref<HTMLElement | null>(null);

// 图表实例
let pieChart: ECharts | null = null;
let barChart: ECharts | null = null;
let diffChart: ECharts | null = null;
let timePieChart: ECharts | null = null;

onMounted(() => {
  fetchReportDetail();
});

onBeforeUnmount(() => {
  // 销毁图表实例
  if (pieChart) {
    pieChart.dispose();
    pieChart = null;
  }
  if (barChart) {
    barChart.dispose();
    barChart = null;
  }
  if (diffChart) {
    diffChart.dispose();
    diffChart = null;
  }
  if (timePieChart) {
    timePieChart.dispose();
    timePieChart = null;
  }
});

// 获取报告详情
const fetchReportDetail = async () => {
  try {
    console.log('开始获取报告详情，报告ID:', reportId);
    const response = await testReportApi.getById(reportId);
    console.log('API响应:', response);
    console.log('API响应数据:', response.data);
    report.value = response.data;
    
    // 解析reportData中的脚本结果
    console.log('原始reportData:', report.value.reportData);
    console.log('reportData类型:', typeof report.value.reportData);
    
    const reportData = JSON.parse(report.value.reportData || '{}');
    console.log('解析后的reportData:', reportData);
    console.log('reportData.scriptDetails:', reportData.scriptDetails);
    console.log('reportData.hasPreviousExecution:', reportData.hasPreviousExecution);
    
    scriptResults.value = reportData.scriptDetails || [];
    hasPreviousExecution.value = reportData.hasPreviousExecution || false;
    
    console.log('最终设置的scriptResults:', scriptResults.value);
    console.log('最终设置的hasPreviousExecution:', hasPreviousExecution.value);
    
    // 添加调试日志
    console.log('报告数据:', report.value);
    console.log('脚本结果:', scriptResults.value);
    console.log('是否有上一次执行:', hasPreviousExecution.value);
    console.log('脚本结果数量:', scriptResults.value.length);
    if (scriptResults.value.length > 0) {
      console.log('第一个脚本的耗时对比数据:', {
        scriptName: scriptResults.value[0].scriptName,
        executionTime: scriptResults.value[0].executionTime,
        previousExecutionTime: scriptResults.value[0].previousExecutionTime,
        timeDiff: scriptResults.value[0].timeDiff,
        timeDiffPercent: scriptResults.value[0].timeDiffPercent
      });
    }
    
    // 准备初始化基础图表（饼图和柱状图）
    console.log('准备初始化基础图表...');
    
    // 使用nextTick确保DOM已更新
    await nextTick();
    console.log('DOM已更新，开始初始化基础图表...');
    
    initCharts();
    
    // 如果有上一次执行数据，初始化差异对比图表
    if (hasPreviousExecution && scriptResults.value.length > 0) {
      console.log('有上一次执行数据，准备初始化差异对比图表...');
      await nextTick();
      initDiffChart();
    }
  } catch (error) {
    console.error('获取报告详情失败:', error);
  }
};

// 初始化图表
const initCharts = () => {
  // 初始化饼图
  if (pieChartRef.value) {
    pieChart = init(pieChartRef.value);
    
    const pieOption = {
      title: {
        text: '测试结果分布',
        left: 'center'
      },
      tooltip: {
        trigger: 'item',
        formatter: '{b}: {c} ({d}%)'
      },
      legend: {
        orient: 'vertical',
        left: 'left'
      },
      series: [
        {
          name: '测试结果',
          type: 'pie',
          radius: '50%',
          data: [
            { value: report.value.successScripts, name: '成功' },
            { value: report.value.failedScripts, name: '失败' }
          ],
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.5)'
            }
          }
        }
      ]
    };
    
    pieChart.setOption(pieOption);
  }
  
  // 初始化柱状图
  if (barChartRef.value && scriptResults.value.length > 0) {
    barChart = init(barChartRef.value);
    
    // 准备柱状图数据
    const barOption = {
      title: {
        text: '脚本执行耗时',
        left: 'center'
      },
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'shadow'
        }
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: scriptResults.value.map(r => r.scriptName),
        axisLabel: {
          interval: 0,
          rotate: 45
        }
      },
      yAxis: {
        type: 'value'
      },
      series: [
        {
          name: '执行耗时 (ms)',
          type: 'bar',
          data: scriptResults.value.map(r => r.executionTime),
          itemStyle: {
            color: function(params: any) {
              return params.dataIndex % 2 === 0 ? '#5470c6' : '#91cc75';
            }
          }
        }
      ]
    };
    
    barChart.setOption(barOption);
  }
  
  // 初始化耗时分布饼图
  if (timePieChartRef.value && scriptResults.value.length > 0) {
    timePieChart = init(timePieChartRef.value);
    
    // 计算总耗时
    const totalTime = scriptResults.value.reduce((sum, r) => sum + r.executionTime, 0);
    
    // 准备饼图数据
    const pieData = scriptResults.value.map(r => ({
      value: r.executionTime,
      name: r.scriptName
    }));
    
    const timePieOption = {
      title: {
        text: '脚本耗时分布',
        left: 'center'
      },
      tooltip: {
        trigger: 'item',
        formatter: function(params: any) {
          const percent = ((params.value / totalTime) * 100).toFixed(2);
          return `${params.name}<br/>耗时: ${params.value} ms<br/>占比: ${percent}%`;
        }
      },
      legend: {
        orient: 'vertical',
        left: 'left',
        type: 'scroll'
      },
      series: [
        {
          name: '脚本耗时',
          type: 'pie',
          radius: '50%',
          data: pieData,
          emphasis: {
            itemStyle: {
              shadowBlur: 10,
              shadowOffsetX: 0,
              shadowColor: 'rgba(0, 0, 0, 0.5)'
            }
          },
          label: {
            formatter: function(params: any) {
              const percent = ((params.value / totalTime) * 100).toFixed(1);
              return `${params.name}\n${percent}%`;
            }
          }
        }
      ]
    };
    
    timePieChart.setOption(timePieOption);
  }
  
  // 窗口大小变化时重绘图表
  window.addEventListener('resize', handleResize);
};

// 初始化耗时差异对比图表
const initDiffChart = () => {
  console.log('开始初始化耗时差异对比图表');
  console.log('diffChartRef.value:', diffChartRef.value);
  console.log('hasPreviousExecution.value:', hasPreviousExecution.value);
  console.log('scriptResults.value.length:', scriptResults.value.length);
  
  if (diffChartRef.value && hasPreviousExecution.value && scriptResults.value.length > 0) {
    console.log('满足初始化条件，开始创建图表');
    
    // 如果图表已存在，先销毁
    if (diffChart) {
      diffChart.dispose();
    }
    
    diffChart = init(diffChartRef.value);
    
    // 准备耗时差异数据
    const scriptNames = scriptResults.value.map(r => r.scriptName);
    const currentTimes = scriptResults.value.map(r => r.executionTime);
    const previousTimes = scriptResults.value.map(r => r.previousExecutionTime || 0);
    const timeDiffs = scriptResults.value.map(r => r.timeDiff || 0);
    
    console.log('脚本名称:', scriptNames);
    console.log('本次执行时间:', currentTimes);
    console.log('上次执行时间:', previousTimes);
    console.log('时间差:', timeDiffs);
    
    const diffOption = {
      title: {
        text: '脚本耗时差异对比',
        left: 'center'
      },
      tooltip: {
        trigger: 'axis',
        axisPointer: {
          type: 'shadow'
        },
        formatter: function(params: any) {
          let result = params[0].name + '<br/>';
          params.forEach((param: any) => {
            result += param.marker + param.seriesName + ': ' + param.value + ' ms<br/>';
          });
          const dataIndex = params[0].dataIndex;
          const diff = timeDiffs[dataIndex];
          const diffPercent = scriptResults.value[dataIndex].timeDiffPercent || 0;
          result += '差异: ' + (diff >= 0 ? '+' : '') + diff + ' ms (' + (diffPercent >= 0 ? '+' : '') + diffPercent.toFixed(2) + '%)';
          return result;
        }
      },
      legend: {
        data: ['本次执行', '上次执行'],
        top: 30
      },
      grid: {
        left: '3%',
        right: '4%',
        bottom: '3%',
        top: 80,
        containLabel: true
      },
      xAxis: {
        type: 'category',
        data: scriptNames,
        axisLabel: {
          interval: 0,
          rotate: 45
        }
      },
      yAxis: {
        type: 'value',
        name: '耗时 (ms)'
      },
      series: [
        {
          name: '本次执行',
          type: 'bar',
          data: currentTimes,
          itemStyle: {
            color: '#5470c6'
          }
        },
        {
          name: '上次执行',
          type: 'bar',
          data: previousTimes,
          itemStyle: {
            color: '#91cc75'
          }
        }
      ]
    };
    
    console.log('图表配置:', diffOption);
    diffChart.setOption(diffOption);
    console.log('耗时差异对比图表初始化完成');
  } else {
    console.log('不满足初始化条件:');
    console.log('- diffChartRef.value:', diffChartRef.value);
    console.log('- hasPreviousExecution.value:', hasPreviousExecution.value);
    console.log('- scriptResults.value.length > 0:', scriptResults.value.length > 0);
  }
};

// 处理窗口大小变化
const handleResize = () => {
  pieChart?.resize();
  barChart?.resize();
  diffChart?.resize();
  timePieChart?.resize();
};

// 格式化日期
const formatDate = (dateString: string) => {
  const date = new Date(dateString);
  return date.toLocaleString();
};

// 返回报告列表
const goBack = () => {
  router.push('/reports');
};
</script>

<style scoped>
.report-detail {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.report-info {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  margin-bottom: 20px;
  padding: 20px;
  background-color: #f5f5f5;
  border-radius: 8px;
}

.info-item {
  display: flex;
  align-items: center;
}

.label {
  font-weight: bold;
  margin-right: 10px;
}

.charts-container {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  margin-bottom: 20px;
}

.chart-item {
  flex: 1;
  min-width: 400px;
  padding: 20px;
  background-color: #f5f5f5;
  border-radius: 8px;
}

.pie-chart, .bar-chart, .diff-chart, .time-pie-chart {
  width: 100%;
  height: 300px;
  margin-bottom: 20px;
}

.stats {
  display: flex;
  justify-content: space-around;
  margin-top: 20px;
}

.stat {
  text-align: center;
}

.stat-value {
  display: block;
  font-size: 24px;
  font-weight: bold;
}

.stat-value.success {
  color: #52c41a;
}

.stat-value.failed {
  color: #f5222d;
}

.script-results {
  margin-bottom: 20px;
}

table {
  width: 100%;
  border-collapse: collapse;
  background-color: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

th, td {
  padding: 12px;
  text-align: left;
  border-bottom: 1px solid #e8e8e8;
}

th {
  background-color: #fafafa;
  font-weight: bold;
}

.status-success {
  color: #52c41a;
  font-weight: bold;
}

.status-failure {
  color: #f5222d;
  font-weight: bold;
}

.back-button {
  padding: 10px 20px;
  background-color: #1890ff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.back-button:hover {
  background-color: #40a9ff;
}
</style>