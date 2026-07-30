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
  background: #2E3238;
  color: #aaa;
  margin-top: 56px;
  box-shadow: 0 -4px 20px rgba(0, 0, 0, 0.08);

  .footer-inner {
    max-width: 1400px;
    margin: 0 auto;
    padding: 56px 48px 24px;
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 48px;
  }

  .footer-section {
    h4 {
      color: #fff;
      margin-bottom: 20px;
      font-size: 17px;
      font-weight: 600;
      letter-spacing: 0.5px;
      padding-bottom: 8px;
      border-bottom: 2px solid rgba(216, 169, 169, 0.3);
      display: inline-block;
    }
    a {
      display: block;
      color: #b8b8b8;
      text-decoration: none;
      padding: 6px 0;
      font-size: 14px;
      cursor: pointer;
      transition: all 0.3s;
      position: relative;

      &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 50%;
        transform: translateY(-50%);
        width: 0;
        height: 2px;
        background: #D8A9A9;
        transition: width 0.3s;
      }

      &:hover {
        color: #D8A9A9;
        padding-left: 10px;

        &::before {
          width: 6px;
        }
      }
    }
  }

  .payment-methods {
    display: flex;
    flex-wrap: wrap;
    gap: 10px;
    span {
      background: rgba(216, 169, 169, 0.15);
      color: #D8A9A9;
      padding: 8px 14px;
      border-radius: 8px;
      font-size: 13px;
      border: 1px solid rgba(216, 169, 169, 0.2);
      transition: all 0.3s;

      &:hover {
        background: rgba(216, 169, 169, 0.25);
        transform: translateY(-2px);
      }
    }
  }

  .footer-bottom {
    border-top: 1px solid rgba(255, 255, 255, 0.08);
    padding: 24px;
    text-align: center;
    font-size: 13px;
    color: #888;
    margin-top: 24px;
  }
}

@media (max-width: 768px) {
  .app-footer {
    .footer-inner {
      grid-template-columns: repeat(2, 1fr);
      gap: 32px;
      padding: 40px 24px 16px;
    }
  }
}
</style>
