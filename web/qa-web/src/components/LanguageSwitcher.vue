<template>
  <a-dropdown :trigger="['click']" placement="bottomRight">
    <a-button class="lang-switch-btn" :aria-label="t('language.label')">
      <GlobalOutlined />
      <span class="lang-current">{{ currentLabel }}</span>
      <DownOutlined class="lang-arrow" />
    </a-button>
    <template #overlay>
      <a-menu :selected-keys="[locale]" @click="handleSelect">
        <a-menu-item key="zh-CN">
          <CheckOutlined v-if="locale === 'zh-CN'" class="lang-check" />
          <span v-else class="lang-check lang-check-placeholder"></span>
          {{ t('language.zhCN') }}
        </a-menu-item>
        <a-menu-item key="en-US">
          <CheckOutlined v-if="locale === 'en-US'" class="lang-check" />
          <span v-else class="lang-check lang-check-placeholder"></span>
          {{ t('language.enUS') }}
        </a-menu-item>
      </a-menu>
    </template>
  </a-dropdown>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { useI18n } from 'vue-i18n';
import { GlobalOutlined, DownOutlined, CheckOutlined } from '@ant-design/icons-vue';
import { setLocale, type LocaleKey } from '../locales';

const { t, locale } = useI18n();

const currentLabel = computed(() => t(`language.${locale.value === 'en-US' ? 'enUS' : 'zhCN'}`));

const handleSelect = ({ key }: { key: string }) => {
  setLocale(key as LocaleKey);
};
</script>

<style scoped>
.lang-switch-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  line-height: 32px;
}

.lang-current {
  font-size: 14px;
}

.lang-arrow {
  font-size: 10px;
}

.lang-check {
  font-size: 12px;
  color: #1890ff;
}

.lang-check-placeholder {
  display: inline-block;
  width: 12px;
}
</style>
