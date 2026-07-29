<script setup lang="ts">
import { useRouter } from 'vue-router'
const router = useRouter()
</script>

<template>
  <footer class="app-footer">
    <div class="footer-inner">
      <div class="footer-section">
        <h4>购物指南</h4>
        <a @click="router.push('/')">首页</a>
        <a @click="router.push('/categories')">商品分类</a>
        <a @click="router.push('/login')">登录/注册</a>
      </div>
      <div class="footer-section">
        <h4>客户服务</h4>
        <a>联系客服</a>
        <a>退换货政策</a>
        <a>价格保护</a>
      </div>
      <div class="footer-section">
        <h4>关于我们</h4>
        <a>公司介绍</a>
        <a>加入我们</a>
        <a>联系我们</a>
      </div>
      <div class="footer-section">
        <h4>支付方式</h4>
        <div class="payment-methods">
          <span>微信支付</span>
          <span>支付宝</span>
          <span>银行卡</span>
        </div>
      </div>
    </div>
    <div class="footer-bottom">
      <p>© 2024 Mall 商城系统 All Rights Reserved.</p>
    </div>
  </footer>
</template>

<style scoped lang="scss">
.app-footer {
  background: #2c2c2c;
  color: #aaa;
  margin-top: 40px;

  .footer-inner {
    max-width: 1200px;
    margin: 0 auto;
    padding: 40px 20px 20px;
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 32px;
  }

  .footer-section {
    h4 {
      color: #fff;
      margin-bottom: 16px;
      font-size: 15px;
    }
    a {
      display: block;
      color: #aaa;
      text-decoration: none;
      padding: 4px 0;
      font-size: 13px;
      cursor: pointer;
      &:hover { color: #ff6b35; }
    }
  }

  .payment-methods {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    span {
      background: #3a3a3a;
      padding: 6px 12px;
      border-radius: 4px;
      font-size: 12px;
    }
  }

  .footer-bottom {
    border-top: 1px solid #3a3a3a;
    padding: 20px;
    text-align: center;
    font-size: 12px;
  }
}
</style>
