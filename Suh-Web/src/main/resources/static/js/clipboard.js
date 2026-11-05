/**
 * 웹 클립보드: 붙여넣기·끌어놓기·입력 저장 > 목록 > 복사·내려받기·삭제
 */

const CLIPBOARD_FILE_URL = '/api/clipboard/file/';
let clipboardItemById = {};

$(document).ready(function() {
  loadClipboardItems();

  $('#saveTextButton').on('click', saveTypedText);
  $('#refreshListButton').on('click', loadClipboardItems);
  $('#clipboardFileInput').on('change', function() {
    uploadFiles(this.files);
    this.value = '';
  });

  bindDropZone();
  document.addEventListener('paste', onPaste);

  // 목록 버튼 (copy-text, copy-image, delete)
  $('#clipboardList').on('click', '[data-action]', function() {
    const clipboardItemId = $(this).closest('[data-item-id]').data('item-id');
    const action = $(this).data('action');
    if (action === 'copy-text') {
      copyText(clipboardItemId);
    } else if (action === 'copy-image') {
      copyImage(clipboardItemId);
    } else if (action === 'delete') {
      deleteItem(clipboardItemId);
    }
  });
});

/**
 * Ctrl+V: 파일·이미지 > 파일 업로드, 텍스트 > 텍스트 저장 (입력칸 안 텍스트 붙여넣기는 기본 동작)
 */
function onPaste(event) {
  const clipboardData = event.clipboardData;
  if (!clipboardData) {
    return;
  }
  const pastedFiles = Array.from(clipboardData.files || []);
  if (pastedFiles.length > 0) {
    event.preventDefault();
    uploadFiles(pastedFiles);
    return;
  }

  const isTypingTarget = $(event.target).is('input, textarea, [contenteditable="true"]');
  if (isTypingTarget) {
    return;
  }
  const pastedText = clipboardData.getData('text/plain');
  if (pastedText && pastedText.trim()) {
    event.preventDefault();
    saveText(pastedText);
  }
}

function bindDropZone() {
  const dropZone = $('#clipboardDropZone');
  dropZone.on('dragover', function(event) {
    event.preventDefault();
    dropZone.addClass('border-primary');
  });
  dropZone.on('dragleave', function() {
    dropZone.removeClass('border-primary');
  });
  dropZone.on('drop', function(event) {
    event.preventDefault();
    dropZone.removeClass('border-primary');
    uploadFiles(event.originalEvent.dataTransfer.files);
  });
}

function saveTypedText() {
  const typedText = $('#clipboardText').val();
  if (!typedText || !typedText.trim()) {
    showToast(window.MSG['clipboard.toast.emptyText'], 'warning');
    return;
  }
  saveText(typedText, function() {
    $('#clipboardText').val('');
  });
}

function saveText(text, onSaved) {
  sendFormRequest('/api/clipboard/create', { textContent: text }, function() {
    showToast(window.MSG['clipboard.toast.saved'], 'positive');
    if (onSaved) {
      onSaved();
    }
    loadClipboardItems();
  });
}

// 파일마다 1건씩 업로드
function uploadFiles(fileList) {
  Array.from(fileList || []).forEach(function(file) {
    sendFormRequest('/api/clipboard/create', { file: file }, function() {
      showToast(window.MSG['clipboard.toast.uploaded'] + ' ' + file.name, 'positive');
      loadClipboardItems();
    });
  });
}

function loadClipboardItems() {
  sendFormRequest('/api/clipboard/list', {}, function(response) {
    renderClipboardItems(response.items || []);
  }, function() {
    $('#clipboardList').html('<p class="text-center py-8 text-base-content/60">'
      + escapeHtml(window.MSG['clipboard.toast.loadFailed']) + '</p>');
  });
}

function renderClipboardItems(items) {
  const listContainer = $('#clipboardList');
  const emptyMessage = $('#clipboardEmptyMessage');
  clipboardItemById = {};
  listContainer.empty();

  if (items.length === 0) {
    emptyMessage.removeClass('hidden');
    return;
  }
  emptyMessage.addClass('hidden');

  items.forEach(function(item) {
    clipboardItemById[item.clipboardItemId] = item;
    listContainer.append(renderClipboardItem(item));
  });
}

function renderClipboardItem(item) {
  const fileUrl = CLIPBOARD_FILE_URL + encodeURIComponent(item.clipboardItemId);
  let bodyHtml = '';
  let actionHtml = '';

  if (item.itemType === 'TEXT') {
    bodyHtml = '<pre class="whitespace-pre-wrap break-all text-sm bg-base-200 rounded-lg p-3 max-h-40 overflow-auto">'
      + escapeHtml(item.textContent) + '</pre>';
    actionHtml = actionButton('copy-text', 'fa-copy', window.MSG['clipboard.button.copy']);
  } else if (item.itemType === 'IMAGE') {
    bodyHtml = '<img src="' + fileUrl + '" alt="' + escapeHtml(item.originalFileName) + '" loading="lazy"'
      + ' class="max-h-48 max-w-full rounded-lg border border-base-300">';
    actionHtml = actionButton('copy-image', 'fa-copy', window.MSG['clipboard.button.copyImage'])
      + downloadLink(fileUrl, item.originalFileName);
  } else {
    bodyHtml = '<div class="flex items-center gap-2 text-base-content">'
      + '<i class="fa-solid fa-file text-base-content/60"></i>'
      + '<span class="break-all">' + escapeHtml(item.originalFileName) + '</span></div>';
    actionHtml = downloadLink(fileUrl, item.originalFileName);
  }
  actionHtml += actionButton('delete', 'fa-trash', window.MSG['clipboard.button.delete'], 'btn-error btn-outline');

  return '<div class="border border-base-300 rounded-xl p-4 flex flex-col gap-3" data-item-id="'
    + escapeHtml(item.clipboardItemId) + '">'
    + '<div class="flex flex-wrap items-center gap-2 text-xs text-base-content/60">'
    + '<span class="badge badge-soft badge-primary badge-sm">' + escapeHtml(window.MSG['clipboard.type.' + item.itemType]) + '</span>'
    + '<span>' + escapeHtml(formatCreatedDate(item.createdDate)) + '</span>'
    + '<span>' + escapeHtml(formatItemSize(item)) + '</span>'
    + '</div>'
    + bodyHtml
    + '<div class="flex flex-wrap justify-end gap-2">' + actionHtml + '</div>'
    + '</div>';
}

function actionButton(action, iconClass, label, buttonClass) {
  return '<button type="button" class="btn btn-sm ' + (buttonClass || 'btn-outline') + '" data-action="' + action + '">'
    + '<i class="fa-solid ' + iconClass + '"></i> ' + escapeHtml(label) + '</button>';
}

function downloadLink(fileUrl, fileName) {
  return '<a class="btn btn-sm btn-outline" href="' + fileUrl + '" download="' + escapeHtml(fileName) + '">'
    + '<i class="fa-solid fa-download"></i> ' + escapeHtml(window.MSG['clipboard.button.download']) + '</a>';
}

// yyyy-MM-ddTHH:mm:ss > yyyy-MM-dd HH:mm
function formatCreatedDate(createdDate) {
  return createdDate ? createdDate.replace('T', ' ').substring(0, 16) : '';
}

// 텍스트 > 글자 수, 파일 > B·KB·MB
function formatItemSize(item) {
  if (item.itemType === 'TEXT') {
    return (item.textContent || '').length + window.MSG['clipboard.label.chars'];
  }
  const sizeBytes = item.fileSizeBytes || 0;
  if (sizeBytes < 1024) {
    return sizeBytes + ' B';
  }
  if (sizeBytes < 1024 * 1024) {
    return (sizeBytes / 1024).toFixed(1) + ' KB';
  }
  return (sizeBytes / (1024 * 1024)).toFixed(1) + ' MB';
}

function copyText(clipboardItemId) {
  const item = clipboardItemById[clipboardItemId];
  navigator.clipboard.writeText(item.textContent)
    .then(function() {
      showToast(window.MSG['clipboard.toast.copied'], 'positive');
    })
    .catch(function() {
      showToast(window.MSG['clipboard.toast.copyFailed'], 'negative');
    });
}

// 이미지 클립보드 복사 (브라우저가 PNG만 허용하는 경우가 많아 PNG로 변환)
function copyImage(clipboardItemId) {
  if (!navigator.clipboard || !window.ClipboardItem) {
    showToast(window.MSG['clipboard.toast.copyFailed'], 'negative');
    return;
  }
  const pngBlobPromise = fetch(CLIPBOARD_FILE_URL + encodeURIComponent(clipboardItemId))
    .then(function(response) { return response.blob(); })
    .then(toPngBlob);
  navigator.clipboard.write([new ClipboardItem({ 'image/png': pngBlobPromise })])
    .then(function() {
      showToast(window.MSG['clipboard.toast.copied'], 'positive');
    })
    .catch(function() {
      showToast(window.MSG['clipboard.toast.copyFailed'], 'negative');
    });
}

function toPngBlob(imageBlob) {
  if (imageBlob.type === 'image/png') {
    return imageBlob;
  }
  return createImageBitmap(imageBlob).then(function(bitmap) {
    const canvas = document.createElement('canvas');
    canvas.width = bitmap.width;
    canvas.height = bitmap.height;
    canvas.getContext('2d').drawImage(bitmap, 0, 0);
    return new Promise(function(resolve) {
      canvas.toBlob(resolve, 'image/png');
    });
  });
}

function deleteItem(clipboardItemId) {
  if (!confirm(window.MSG['clipboard.confirm.delete'])) {
    return;
  }
  sendFormRequest('/api/clipboard/delete', { clipboardItemId: clipboardItemId }, function() {
    showToast(window.MSG['clipboard.toast.deleted'], 'positive');
    loadClipboardItems();
  });
}
