<!--
  * 底部：版权、公司等信息
-->
<template>
  <div class="version">
    <span class="smart-copyright">©{{ currentYear }} SmartAdmin</span>
  </div>
</template>
<script setup lang="ts">
  import dayjs from 'dayjs';

  const currentYear = dayjs().year();
</script>

<style lang="less" scoped>
  .version {
    font-size: 14px;
    color: rgba(0, 0, 0, 0.45);
    .smart-copyright {
      color: rgba(0, 0, 0, 0.45);
    }
  }
</style>
