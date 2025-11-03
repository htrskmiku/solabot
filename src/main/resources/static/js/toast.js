const toastConfig = {
    success: {
        icon: '✓',
        title: '操作成功',
        message: '您的操作已成功完成！'
    },
    error: {
        icon: '✕',
        title: '操作失败',
        message: '抱歉，操作失败，请稍后重试。'
    },
    warning: {
        icon: '⚠',
        title: '警告提示',
        message: '请注意，此操作可能产生风险。'
    },
    info: {
        icon: 'ℹ',
        title: '信息提示',
        message: '这是一条普通的信息通知。'
    }
};

// 显示消息框
function showToast(type) {
    const container = document.getElementById('toastContainer');
    const config = toastConfig[type];

    // 创建消息框元素
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;

    toast.innerHTML = `
                <div class="toast-icon">${config.icon}</div>
                <div class="toast-content">
                    <div class="toast-title">${config.title}</div>
                    <div class="toast-message">${config.message}</div>
                </div>
                <button class="toast-close" onclick="closeToast(this)">×</button>
                <div class="toast-progress"></div>
            `;

    // 添加到容器
    container.appendChild(toast);

    // 3秒后自动关闭
    setTimeout(() => {
        if (toast.parentElement) {
            closeToast(toast.querySelector('.toast-close'));
        }
    }, 3000);
}

// 关闭消息框
function closeToast(button) {
    const toast = button.closest('.toast');
    toast.classList.add('removing');

    // 等待动画结束后移除元素
    setTimeout(() => {
        toast.remove();
    }, 400);
}

// 自定义消息框（可选）
function showCustomToast(type, title, message, duration = 3000) {
    const container = document.getElementById('toastContainer');
    const icons = {
        success: '✓',
        error: '✕',
        warning: '⚠',
        info: 'ℹ'
    };

    const toast = document.createElement('div');
    toast.className = `toast ${type}`;

    toast.innerHTML = `
                <div class="toast-icon">${icons[type]}</div>
                <div class="toast-content">
                    <div class="toast-title">${title}</div>
                    <div class="toast-message">${message}</div>
                </div>
                <button class="toast-close" onclick="closeToast(this)">×</button>
                <div class="toast-progress"></div>
            `;

    container.appendChild(toast);

    if (duration > 0) {
        setTimeout(() => {
            if (toast.parentElement) {
                closeToast(toast.querySelector('.toast-close'));
            }
        }, duration);
    }
}

// 示例：在页面加载时显示欢迎消息
