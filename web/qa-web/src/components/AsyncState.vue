<template>
  <div class="load-state">
    <!-- 加载中 -->
    <div v-if="loading" class="load-state__loading">
      <a-spin :size="'large'" />
      <p class="load-state__text">{{ loadingText }}</p>
    </div>

    <!-- 加载失败 -->
    <div v-else-if="error" class="load-state__error">
      <a-result status="warning" :title="errorTitle" :sub-title="error">
        <template #extra>
          <a-button type="primary" :loading="retrying" @click="onRetry">
            重试
          </a-button>
        </template>
      </a-result>
    </div>

    <!-- 正常内容 -->
    <slot v-else />
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';

withDefaults(
  defineProps<{
    /** 是否处于加载状态 */
    loading?: boolean;
    /** 错误信息，为空表示无错误 */
    error?: string | null;
    /** 加载中的提示文案 */
    loadingText?: string;
    /** 错误标题 */
    errorTitle?: string;
  }>(),
  {
    loading: false,
    error: null,
    loadingText: '加载中...',
    errorTitle: '数据加载失败',
  }
);

const emit = defineEmits<{ (e: 'retry'): void }>();

const retrying = ref(false);

const onRetry = async () => {
  retrying.value = true;
  try {
    emit('retry');
  } finally {
    // 父组件处理加载状态，这里短暂保持按钮 loading 反馈
    window.setTimeout(() => {
      retrying.value = false;
    }, 300);
  }
};
</script>

<style scoped>
.load-state {
  min-height: 200px;
}

.load-state__loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 64px 24px;
}

.load-state__text {
  color: #666;
  font-size: 14px;
  margin: 0;
}

.load-state__error {
  padding: 48px 24px;
}
</style>
