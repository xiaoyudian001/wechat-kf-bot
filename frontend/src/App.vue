<script setup>
import { ref } from 'vue'

const apiBaseUrl = ref('http://localhost:8080')
const userId = ref('dev-user')
const text = ref('附近有什么吃的？')
const loading = ref(false)
const error = ref('')
const messages = ref([])

async function sendMessage() {
  const content = text.value.trim()
  if (!content || loading.value) {
    return
  }

  error.value = ''
  loading.value = true
  messages.value.push({ role: 'user', content })
  text.value = ''

  try {
    const response = await fetch(`${apiBaseUrl.value}/dev/chat/reply`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        userId: userId.value || 'dev-user',
        text: content
      })
    })

    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`)
    }

    const data = await response.json()
    messages.value.push({
      role: 'assistant',
      content: data.reply,
      route: data.route
    })
  } catch (currentError) {
    error.value = `请求失败：${currentError.message}`
    messages.value.push({
      role: 'assistant',
      content: '测试接口暂时没有返回，请确认后端正在以 dev profile 运行。',
      route: 'ERROR'
    })
  } finally {
    loading.value = false
  }
}

function usePrompt(prompt) {
  text.value = prompt
}
</script>

<template>
  <main class="shell">
    <section class="workspace">
      <aside class="panel">
        <p class="eyebrow">Wechat KF Bot</p>
        <h1>本地回复测试台</h1>
        <p class="copy">直接调用后端 dev 接口，验证 FAQ、规则和 MiniMax 兜底回复。</p>

        <label>
          后端地址
          <input v-model="apiBaseUrl" />
        </label>

        <label>
          测试用户
          <input v-model="userId" />
        </label>

        <div class="quick">
          <button type="button" @click="usePrompt('几点入住？')">FAQ</button>
          <button type="button" @click="usePrompt('我要退款')">转人工</button>
          <button type="button" @click="usePrompt('帮我查订单')">订单</button>
          <button type="button" @click="usePrompt('附近有什么吃的？')">MiniMax</button>
        </div>
      </aside>

      <section class="chat">
        <div class="messages">
          <article v-if="messages.length === 0" class="empty">
            <strong>先试这句：</strong>
            <span>附近有什么吃的？</span>
          </article>

          <article
            v-for="(message, index) in messages"
            :key="index"
            class="message"
            :class="message.role"
          >
            <div class="bubble">
              <small v-if="message.route">{{ message.route }}</small>
              <p>{{ message.content }}</p>
            </div>
          </article>
        </div>

        <form class="composer" @submit.prevent="sendMessage">
          <textarea
            v-model="text"
            rows="3"
            placeholder="输入要测试的问题"
            @keydown.ctrl.enter.prevent="sendMessage"
          />
          <button type="submit" :disabled="loading || !text.trim()">
            {{ loading ? '发送中...' : '发送测试' }}
          </button>
        </form>

        <p v-if="error" class="error">{{ error }}</p>
      </section>
    </section>
  </main>
</template>
