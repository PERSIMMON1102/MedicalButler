layui.use(['element', 'layer', 'util', 'jquery'], function () {
    var element = layui.element;
    var layer = layui.layer;
    var util = layui.util;
    var $ = layui.$;

    // 原有的JavaScript代码保持不变...
    var $sidebar = $('#sidebar');
    var $toggle = $('#toggleSidebar');
    var $ring = $('#ringIcon');

    // 初始折叠状态（默认展开）
    var sidebarCollapsed = localStorage.getItem('sidebarCollapsed') === 'true';

    function applyRingState() {
        if (sidebarCollapsed) {
            $ring.addClass('collapsed');
            $toggle.attr('aria-pressed', 'true');
        } else {
            $ring.removeClass('collapsed');
            $toggle.attr('aria-pressed', 'false');
        }
    }

    function collapseSidebar() {
        $sidebar.addClass('layui-side-collapsed');
        $('#mainContent').removeClass('layui-body-expanded').addClass('layui-body-collapsed');
        sidebarCollapsed = true;
        localStorage.setItem('sidebarCollapsed', 'true');
        applyRingState();
    }

    function expandSidebar() {
        $sidebar.removeClass('layui-side-collapsed');
        $('#mainContent').removeClass('layui-body-collapsed').addClass('layui-body-expanded');
        sidebarCollapsed = false;
        localStorage.setItem('sidebarCollapsed', 'false');
        applyRingState();
    }

    function toggleSidebar() {
        $ring.addClass('loading');
        setTimeout(function () {
            $ring.removeClass('loading');
            if (sidebarCollapsed) {
                expandSidebar();
            } else {
                collapseSidebar();
            }
        }, 400);
    }

    // 初始化状态
    if (sidebarCollapsed) {
        collapseSidebar();
    } else {
        expandSidebar();
    }

    // 绑定事件
    $toggle.on('click', toggleSidebar);

    // 键盘快捷键: Ctrl+B
    $(document).on('keydown', function (e) {
        if (e.ctrlKey && (e.key === 'b' || e.key === 'B')) {
            e.preventDefault();
            toggleSidebar();
        }
    });

    // 注销功能
    function logout() {
        layer.confirm('确定要退出登录吗？', function(index) {
            localStorage.removeItem('token');
            window.location.href = 'login.html';
            layer.close(index);
        });
    }
    $('#logoutLink, #logoutMenuLink').on('click', logout);

    // 快捷操作功能
    function quickAction(type) {
        switch(type) {
            case 'reserve':
                $('#demoUser').attr('src', './reserve-check.html');
                break;
            case 'renew':
                $('#demoUser').attr('src', './buy-package.html');
                break;
            case 'service':
                $('#demoUser').attr('src', './customer-service.html');
                break;
        }
    }

    // 菜单点击切换 iframe
    $('.layui-side-scroll .layui-nav-tree a').on('click', function (e) {
        e.preventDefault();
        var url = $(this).data('url');
        if (url) {
            $('#demoUser').attr('src', url);
            var idx = layer.load(1, { shade: [0.1, '#fff'] });
            $('#demoUser').on('load', function () {
                layer.close(idx);
            });
        }
    });

    // iframe 自适应高度
    function adjustIframeHeight() {
        var header = document.querySelector('.layui-header');
        var footer = document.querySelector('.layui-footer');
        var headerH = header ? header.getBoundingClientRect().height : 56;
        var footerH = footer ? footer.getBoundingClientRect().height : 40;
        var h = Math.max(200, window.innerHeight - headerH - footerH - 8);
        $('#demoUser').css('height', h + 'px');
    }
    adjustIframeHeight();
    $(window).on('resize', function () {
        clearTimeout(window.__iframeResizeTimer);
        window.__iframeResizeTimer = setTimeout(adjustIframeHeight, 120);
    });

    // 无障碍提示文本
    $('.sr-only').css({
        position: 'absolute',
        width: '1px',
        height: '1px',
        padding: 0,
        margin: '-1px',
        overflow: 'hidden',
        clip: 'rect(0,0,0,0)',
        border: 0
    });
});