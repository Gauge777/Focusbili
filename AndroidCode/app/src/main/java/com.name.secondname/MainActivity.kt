package com.gut.focusbili

import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

// 如果下面这一行爆红，先别管它，执行第二步的“重生仪式”后会自动变绿
import com.gut.focusbili.BuildConfig

class MainActivity : AppCompatActivity() {

    // 1. 全局变量
    private lateinit var webView: WebView

    // 2. 动态获取 UID (如果 BuildConfig 爆红，说明还没编译，不影响逻辑)
    private val targetUid = BuildConfig.TARGET_UID
    private val targetUrl = "https://space.bilibili.com/$targetUid"

    override fun onCreate(savedInstanceState: Bundle?) {
        // 0. 启动前的准备
        Thread.sleep(2000)

        // 修改：使用 Themes.xml 里定义的标准主题名 (Theme_FocusBili)
        // 注意：这里是下划线，XML 里是点
        setTheme(R.style.Theme_FocusBili)

        super.onCreate(savedInstanceState)

        // 1. 绑定视图
        webView = WebView(this)
        setContentView(webView)

        // 2. 配置
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.databaseEnabled = true
        settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"

        // 3. Cookie
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)
        cookieManager.flush()

        // 4. 拦截器与注入器
        webView.webViewClient = object : WebViewClient() {

            // 拦截逻辑
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val url = request?.url.toString()

                if (url.contains("space.bilibili.com/$targetUid") ||
                    url.contains("bilibili.com/video/") ||
                    url.contains("passport.bilibili.com")) {
                    return false
                }

                view?.loadUrl(targetUrl)
                Toast.makeText(applicationContext, "🚫 专注模式：禁止乱逛", Toast.LENGTH_SHORT).show()
                return true
            }

            // 加载完成后注入
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (url != null && url.contains("bilibili.com/video/")) {
                    hideDistractions(view)
                }
            }
        }

        // 5. 启动
        webView.loadUrl(targetUrl)
    }

    // 核弹级屏蔽注入
// 终极版：参考 Bilibili-Evolved 的 MutationObserver 方案
    private fun hideDistractions(view: WebView?) {
        val js = """
            javascript:(function() {
                console.log("FocusBili: 净化程序已注入");

                // 1. 定义 CSS 规则 (参考 Bilibili-Evolved 的屏蔽列表)
                var css = `
                    /* ============================
                       核心布局净化
                       ============================ */
                    /* 右侧栏 (推荐列表、广告、弹幕列表外框) */
                    .right-container, .right-container-inner, .recommend-list-v1, .rec-list { display: none !important; }
                    #reco_list, #slide_ad, .pop-live-small-mode { display: none !important; }
                    
                    /* 强制拉伸左侧视频区 (否则右边会有一大块空白) */
                    .left-container { width: 100% !important; max-width: 100% !important; }
                    .video-info-container { width: 100% !important; }
                    
                    /* ============================
                       播放器内部净化 (.bpx- 开头是新版播放器特征)
                       ============================ */
                    /* 播放结束后的“相关推荐”九宫格 */
                    .bpx-player-ending-related { display: none !important; width: 0 !important; height: 0 !important; opacity: 0 !important; pointer-events: none !important; }
                    .bpx-player-ending-panel { display: none !important; }
                    
                    /* 暂停时的广告/推荐 */
                    .bpx-player-toast-wrap { display: none !important; }
                    
                    /* 右下角的“弹幕礼仪”等浮窗 */
                    .bpx-player-cmd-dm-wrap { display: none !important; }
                    
                    /* 视频下方的广告 Banner */
                    #v_tag, #activity_vote, .ad-report { display: none !important; }
                    
                    /* ============================
                       移动端净化 (m.bilibili.com)
                       ============================ */
                    .m-recommend-list, .m-recommend-tier, .card-box { display: none !important; }
                    .m-float-openapp, .m-nav-openapp { display: none !important; }
                    
                    /* ============================
                       评论区 (如需保留评论，删除下面这行)
                       ============================ */
                    #comment, .comment-m, .bili-comment, .comment-container { display: none !important; }
                `;

                // 2. 注入 CSS
                var style = document.createElement('style');
                style.type = 'text/css';
                style.appendChild(document.createTextNode(css));
                document.head.appendChild(style);

                // 3. 启用 MutationObserver (DOM 变动观察者)
                // 这是比 setInterval 更高级的方案，B站动态加载元素时会立即触发
                var observer = new MutationObserver(function(mutations) {
                    mutations.forEach(function(mutation) {
                        // 检查是否有新的节点被添加
                        if (mutation.addedNodes.length > 0) {
                            // 再次强制隐藏那些顽固分子 (双重保险)
                            var targets = document.querySelectorAll('.right-container, .recommend-list-v1, .bpx-player-ending-related');
                            targets.forEach(function(el) {
                                if (el.style.display !== 'none') {
                                    el.style.display = 'none';
                                    // 甚至直接从 DOM 树里删掉它
                                    // el.remove(); 
                                }
                            });
                            
                            // 修正宽度
                            var leftContainer = document.querySelector('.left-container');
                            if (leftContainer && leftContainer.style.width !== '100%') {
                                leftContainer.style.width = '100%';
                            }
                        }
                    });
                });

                // 4. 开始监听整个 Body 的变动
                observer.observe(document.body, { childList: true, subtree: true });
                
                // 5. 保底策略：依然保留一个低频定时器，处理 CSS 没覆盖到的漏网之鱼
                setInterval(function() {
                    document.querySelectorAll('.bpx-player-ending-related, .right-container').forEach(e => e.style.display='none');
                }, 2000);
                
            })()
        """.trimIndent()

        view?.evaluateJavascript(js, null)
    }
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }
}