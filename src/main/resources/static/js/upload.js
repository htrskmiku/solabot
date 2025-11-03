
    document.addEventListener('DOMContentLoaded', function() {
    const fileInput = document.getElementById('fileInput');
    const fileUploadArea = document.getElementById('fileUploadArea');
    const selectedFile = document.getElementById('selectedFile');
    const fileName = document.getElementById('fileName');
    const fileSize = document.getElementById('fileSize');
    const removeFile = document.getElementById('removeFile');
    const uploadButton = document.getElementById('uploadButton');

    // 文件选择处理
    fileInput.addEventListener('change', function(e) {
    if (this.files && this.files[0]) {
    const file = this.files[0];
    displayFileInfo(file);
}
});

    // 拖拽功能
    fileUploadArea.addEventListener('dragover', function(e) {
    e.preventDefault();
    this.classList.add('dragover');
});

    fileUploadArea.addEventListener('dragleave', function() {
    this.classList.remove('dragover');
});

    fileUploadArea.addEventListener('drop', function(e) {
    e.preventDefault();
    this.classList.remove('dragover');

    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
    fileInput.files = e.dataTransfer.files;
    displayFileInfo(e.dataTransfer.files[0]);
}
});

    // 移除文件
    removeFile.addEventListener('click', function() {
    fileInput.value = '';
    selectedFile.style.display = 'none';
    uploadButton.disabled = true;
});

    // 显示文件信息
    function displayFileInfo(file) {
    fileName.textContent = file.name;
    fileSize.textContent = formatFileSize(file.size);
    selectedFile.style.display = 'flex';
    uploadButton.disabled = false;
}

    // 格式化文件大小
    function formatFileSize(bytes) {
    if (bytes === 0) return '0 Bytes';

    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));

    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}
});


