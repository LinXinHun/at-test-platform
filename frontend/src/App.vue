<script setup lang="ts">
import { RouterView } from 'vue-router';
</script>

<template>
  <div class="app-container">
    <header class="app-header">
      <h1>自动化测试执行平台</h1>
    </header>
    
    <div class="app-body">
      <nav class="app-sidebar">
        <ul class="sidebar-menu">
          <li>
            <router-link to="/">首页</router-link>
          </li>
          <li>
            <router-link to="/scripts">测试脚本管理</router-link>
          </li>
          <li>
            <router-link to="/plans">测试计划管理</router-link>
          </li>
          <li>
            <router-link to="/tasks">执行任务管理</router-link>
          </li>
          <li>
            <router-link to="/execution-nodes">执行节点管理</router-link>
          </li>
          <li>
            <router-link to="/reports">测试报告管理</router-link>
          </li>
        </ul>
      </nav>
      
      <main class="app-content">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<style>
/* Global Styles */
* {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

body {
  font-family: Arial, sans-serif;
  line-height: 1.6;
  color: #333;
}

/* App Container */
.app-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  overflow: hidden;
}

/* App Header */
.app-header {
  background-color: #35495e;
  color: white;
  padding: 1rem;
  text-align: center;
  flex-shrink: 0;
}

/* App Body */
.app-body {
  display: flex;
  flex: 1;
  overflow: hidden;
}

/* Sidebar */
.app-sidebar {
  width: 250px;
  background-color: #2c3e50;
  color: white;
  overflow-y: auto;
}

.sidebar-menu {
  list-style: none;
}

.sidebar-menu li {
  border-bottom: 1px solid #34495e;
}

.sidebar-menu a {
  display: block;
  padding: 1rem;
  color: white;
  text-decoration: none;
  transition: background-color 0.3s;
}

.sidebar-menu a:hover {
  background-color: #42b983;
}

/* Content Area */
.app-content {
  flex: 1;
  padding: 2rem;
  background-color: #f5f5f5;
  overflow-y: auto;
}
</style>