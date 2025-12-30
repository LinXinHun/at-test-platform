<template>
  <div class="app-container">
    <header class="app-header">
      <h1>自动化测试执行平台</h1>
    </header>
    <nav class="main-nav">
      <ul>
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
          <router-link to="/reports">测试报告管理</router-link>
        </li>
      </ul>
    </nav>
    <main class="app-content">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
// Home view component
</script>

<style scoped>
.app-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.app-header {
  background-color: #42b983;
  color: white;
  padding: 1rem;
  text-align: center;
}

.main-nav {
  background-color: #35495e;
  padding: 0.5rem;
}

.main-nav ul {
  list-style: none;
  display: flex;
  gap: 1rem;
  margin: 0;
  padding: 0;
}

.main-nav a {
  color: white;
  text-decoration: none;
  padding: 0.5rem 1rem;
  border-radius: 4px;
  transition: background-color 0.3s;
}

.main-nav a:hover {
  background-color: #42b983;
}

.app-content {
  flex: 1;
  padding: 1rem;
}
</style>