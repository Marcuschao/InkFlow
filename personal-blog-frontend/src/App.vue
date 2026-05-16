<script setup>
import { onMounted, onUnmounted } from 'vue';
import { RouterView } from 'vue-router';
import Navbar from './components/Navbar.vue';
import Footer from './components/Footer.vue';
import AiChatbot from './components/AiChatbot.vue';
import ScrollToTop from './components/ScrollToTop.vue';
import ToastHost from './components/ToastHost.vue';
import MobileDock from './components/MobileDock.vue';
import { useSiteStore } from './stores/site';
import { mountClickRipple } from './composables/useClickRipple';

const siteStore = useSiteStore();

let stopRipple = () => {};

onMounted(() => {
  siteStore.loadPublicConfig();
  stopRipple = mountClickRipple();
});

onUnmounted(() => {
  stopRipple();
});
</script>

<template>
  <div id="app-wrapper">
    <Navbar />
    <main class="main-content">
      <RouterView v-slot="{ Component }">
        <Transition name="page-fade" mode="out-in">
          <component :is="Component" />
        </Transition>
      </RouterView>
    </main>
    <Footer />
    <MobileDock />
    <ScrollToTop />
    <ToastHost />
    <AiChatbot />
  </div>
</template>

<style scoped>
#app-wrapper {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.main-content {
  flex: 1;
  width: 100%;
}

@media (max-width: 767px) {
  .main-content {
    padding-bottom: calc(3.5rem + env(safe-area-inset-bottom, 0px));
  }
}
</style>

<style>
.page-fade-enter-active,
.page-fade-leave-active {
  transition: opacity 0.22s var(--ease-out-soft), transform 0.22s var(--ease-out-soft);
}

.page-fade-enter-from,
.page-fade-leave-to {
  opacity: 0;
  transform: translateY(8px);
}

@media (prefers-reduced-motion: reduce) {
  .page-fade-enter-active,
  .page-fade-leave-active {
    transition-duration: 0.01ms;
  }

  .page-fade-enter-from,
  .page-fade-leave-to {
    transform: none;
  }
}
</style>
