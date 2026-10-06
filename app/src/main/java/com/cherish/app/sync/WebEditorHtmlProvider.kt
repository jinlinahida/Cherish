package com.cherish.app.sync

/**
 * Provides the self-contained single-page web editor application (HTML/CSS/JS)
 * served by the watch's embedded HTTP server.
 *
 * Runs locally on any phone browser without internet connection or external CDN assets.
 */
object WebEditorHtmlProvider {

    fun getHtml(sessionToken: String, deviceName: String = "Cherish Watch"): String {
        return """
<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
<title>Cherish - 手机编辑</title>
<style>
:root {
  --bg-primary: #0e1117;
  --bg-card: #171b22;
  --bg-card-hover: #1f242e;
  --accent-gold: #e6b800;
  --accent-copper: #e07a5f;
  --text-main: #f0f3f6;
  --text-muted: #8b949e;
  --border-color: rgba(255, 255, 255, 0.08);
  --border-highlight: rgba(230, 184, 0, 0.4);
}
* { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif; -webkit-tap-highlight-color: transparent; }
body { background: var(--bg-primary); color: var(--text-main); min-height: 100vh; padding: 16px; padding-bottom: 80px; }
header { display: flex; justify-content: space-between; align-items: center; padding-bottom: 16px; border-bottom: 1px solid var(--border-color); margin-bottom: 16px; }
.logo-title { font-size: 20px; font-weight: 800; color: var(--accent-gold); display: flex; align-items: center; gap: 8px; }
.device-badge { font-size: 11px; background: rgba(230, 184, 0, 0.15); color: var(--accent-gold); padding: 4px 8px; border-radius: 12px; border: 1px solid rgba(230, 184, 0, 0.3); }
.status-bar { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #4ade80; margin-bottom: 16px; }
.status-dot { width: 8px; height: 8px; background: #4ade80; border-radius: 50%; box-shadow: 0 0 6px #4ade80; }

/* Event Cards */
.event-list { display: flex; flex-direction: column; gap: 12px; }
.event-card { background: var(--bg-card); border-radius: 14px; padding: 14px; border: 1px solid var(--border-color); position: relative; overflow: hidden; transition: transform 0.1s, border-color 0.2s; }
.event-card.pinned { border-color: var(--border-highlight); }
.event-color-stripe { position: absolute; left: 0; top: 0; bottom: 0; width: 4px; }
.event-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 8px; }
.event-title-wrap { display: flex; align-items: center; gap: 8px; font-size: 16px; font-weight: bold; }
.event-days { font-size: 22px; font-weight: 900; color: var(--accent-gold); }
.event-meta { font-size: 12px; color: var(--text-muted); display: flex; flex-wrap: wrap; gap: 8px; align-items: center; }
.badge { background: rgba(255, 255, 255, 0.06); padding: 2px 6px; border-radius: 6px; font-size: 11px; }
.event-actions { display: flex; gap: 8px; margin-top: 10px; justify-content: flex-end; }
.btn-icon { background: rgba(255,255,255,0.06); border: 1px solid var(--border-color); color: var(--text-main); padding: 6px 12px; border-radius: 8px; font-size: 12px; cursor: pointer; }
.btn-icon:hover { background: rgba(255,255,255,0.12); }
.btn-danger { color: var(--accent-copper); }

/* Floating Action Button */
.btn-add { position: fixed; bottom: 20px; left: 16px; right: 16px; background: var(--accent-gold); color: #000; padding: 14px; border-radius: 14px; font-size: 16px; font-weight: bold; border: none; cursor: pointer; box-shadow: 0 4px 16px rgba(230, 184, 0, 0.35); text-align: center; }

/* Modal Editor */
.modal-overlay { position: fixed; inset: 0; background: rgba(0, 0, 0, 0.75); backdrop-filter: blur(4px); display: none; justify-content: center; align-items: flex-end; z-index: 100; }
.modal-overlay.active { display: flex; }
.modal-content { background: var(--bg-card); width: 100%; max-height: 90vh; border-radius: 20px 20px 0 0; padding: 20px; overflow-y: auto; border-top: 1px solid var(--border-color); }
.modal-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.form-group { margin-bottom: 14px; }
.form-label { display: block; font-size: 12px; color: var(--text-muted); margin-bottom: 6px; font-weight: 600; }
.form-input, .form-select { width: 100%; background: #0c0e12; border: 1px solid var(--border-color); border-radius: 8px; padding: 10px 12px; color: var(--text-main); font-size: 14px; outline: none; }
.form-input:focus { border-color: var(--accent-gold); }
.segmented-control { display: flex; background: #0c0e12; border-radius: 8px; padding: 2px; border: 1px solid var(--border-color); }
.segment-btn { flex: 1; padding: 8px; text-align: center; font-size: 13px; border-radius: 6px; cursor: pointer; color: var(--text-muted); }
.segment-btn.active { background: var(--accent-gold); color: #000; font-weight: bold; }

/* Color Swatches */
.swatches { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 6px; }
.swatch { width: 32px; height: 32px; border-radius: 50%; border: 2px solid transparent; cursor: pointer; position: relative; }
.swatch.selected { border-color: #fff; transform: scale(1.1); }

/* Interactive Photo Studio Viewport */
.photo-studio { background: #000; border-radius: 12px; padding: 12px; margin-top: 10px; border: 1px solid var(--border-color); }
.canvas-wrapper { position: relative; width: 260px; height: 260px; margin: 0 auto; overflow: hidden; border-radius: 50%; box-shadow: 0 0 16px rgba(0,0,0,0.8); background: #111; cursor: grab; }
.canvas-wrapper.square { border-radius: 18px; }
.canvas-wrapper:active { cursor: grabbing; }
#cropCanvas { width: 100%; height: 100%; display: block; }
.crop-bezel-vignette { position: absolute; inset: 0; pointer-events: none; border-radius: inherit; box-shadow: inset 0 0 30px rgba(0,0,0,0.85); border: 2px solid rgba(255,255,255,0.15); }
.crop-live-preview { position: absolute; inset: 0; pointer-events: none; display: flex; flex-direction: column; align-items: center; justify-content: center; text-align: center; }
.crop-hero-number { font-size: 40px; font-weight: 900; color: #fff; text-shadow: 0 2px 10px rgba(0,0,0,0.9); line-height: 1; }
.crop-hero-unit { font-size: 13px; font-weight: bold; color: rgba(255,255,255,0.85); text-shadow: 0 2px 6px rgba(0,0,0,0.9); }
.crop-hero-title { font-size: 13px; font-weight: bold; color: #fff; text-shadow: 0 2px 6px rgba(0,0,0,0.9); margin-bottom: 4px; }
.crop-hero-date { font-size: 10px; color: rgba(255,255,255,0.8); text-shadow: 0 2px 4px rgba(0,0,0,0.9); margin-top: 4px; }

.studio-controls { margin-top: 12px; display: flex; flex-direction: column; gap: 8px; }
.slider-group { display: flex; align-items: center; gap: 8px; font-size: 12px; color: var(--text-muted); }
.slider-group input { flex: 1; accent-color: var(--accent-gold); }
.studio-buttons { display: flex; gap: 8px; }
.btn-studio { flex: 1; background: rgba(255,255,255,0.08); border: 1px solid var(--border-color); color: var(--text-main); padding: 8px; border-radius: 8px; font-size: 12px; cursor: pointer; }
.btn-studio.active { background: rgba(230, 184, 0, 0.2); border-color: var(--accent-gold); color: var(--accent-gold); font-weight: bold; }

/* Toast */
.toast { position: fixed; top: 16px; left: 50%; transform: translateX(-50%); background: var(--accent-gold); color: #000; padding: 10px 18px; border-radius: 20px; font-size: 13px; font-weight: bold; display: none; z-index: 200; box-shadow: 0 4px 12px rgba(0,0,0,0.5); }
.toast.active { display: block; animation: fadeInOut 2.5s forwards; }
@keyframes fadeInOut { 0% { opacity: 0; transform: translate(-50%, -10px); } 15% { opacity: 1; transform: translate(-50%, 0); } 85% { opacity: 1; } 100% { opacity: 0; transform: translate(-50%, -10px); } }
</style>
</head>
<body>

<header>
  <div class="logo-title">
    <span>✨ Cherish</span>
    <span class="device-badge">$deviceName</span>
  </div>
  <button class="btn-icon" onclick="loadEvents()">🔄 刷新</button>
</header>

<div class="status-bar">
  <div class="status-dot"></div>
  <span>已与手表建立双向安全会话</span>
</div>

<div id="eventList" class="event-list">
  <div style="text-align: center; color: var(--text-muted); padding: 40px 0;">正在加载手表事件...</div>
</div>

<button class="btn-add" onclick="openCreateModal()">+ 新建倒数日</button>

<!-- Edit/Create Modal -->
<div id="modalOverlay" class="modal-overlay">
  <div class="modal-content">
    <div class="modal-header">
      <h3 id="modalTitle">编辑倒数日</h3>
      <button class="btn-icon" onclick="closeModal()">✕</button>
    </div>

    <form id="eventForm" onsubmit="handleFormSubmit(event)">
      <input type="hidden" id="editEventId">

      <div class="form-group">
        <label class="form-label">事件名称</label>
        <div style="display: flex; gap: 8px;">
          <input type="text" id="eventEmoji" class="form-input" style="width: 54px; text-align: center; font-size: 18px;" placeholder="🎂">
          <input type="text" id="eventTitle" class="form-input" placeholder="例如：结婚纪念日" maxlength="50" required>
        </div>
      </div>

      <div class="form-group">
        <label class="form-label">日历类型</label>
        <div class="segmented-control">
          <div id="segSolar" class="segment-btn active" onclick="switchCalendarType('SOLAR')">公历 (阳历)</div>
          <div id="segLunar" class="segment-btn" onclick="switchCalendarType('LUNAR')">农历 (阴历)</div>
        </div>
      </div>

      <!-- Solar Date Inputs -->
      <div id="solarSection" class="form-group">
        <label class="form-label">目标公历日期</label>
        <input type="date" id="solarDateInput" class="form-input" required>
      </div>

      <!-- Lunar Date Inputs -->
      <div id="lunarSection" class="form-group" style="display: none;">
        <label class="form-label">目标农历日期</label>
        <div style="display: flex; gap: 6px;">
          <input type="number" id="lunarYearInput" class="form-input" placeholder="年 (如 2026)" style="flex: 2;" min="1900" max="2100">
          <input type="number" id="lunarMonthInput" class="form-input" placeholder="月" style="flex: 1;" min="1" max="12">
          <input type="number" id="lunarDayInput" class="form-input" placeholder="日" style="flex: 1;" min="1" max="30">
        </div>
        <div style="margin-top: 6px; font-size: 12px; color: var(--text-muted); display: flex; align-items: center; gap: 6px;">
          <input type="checkbox" id="lunarLeapCheckbox">
          <label for="lunarLeapCheckbox">闰月</label>
        </div>
      </div>

      <div class="form-group">
        <label class="form-label">重复规则</label>
        <select id="repeatSelect" class="form-select" onchange="onRepeatRuleChanged()">
          <option value="NONE">不重复</option>
          <option value="DAILY">每天重复</option>
          <option value="MONTHLY">每月重复</option>
          <option value="YEARLY">每年重复</option>
          <option value="CUSTOM">自定义间隔...</option>
        </select>
        <div id="customRepeatGroup" style="display: none; margin-top: 6px; display: flex; gap: 6px;">
          <input type="number" id="customRepeatInterval" class="form-input" value="1" min="1" style="width: 80px;">
          <select id="customRepeatUnit" class="form-select">
            <option value="DAY">天</option>
            <option value="WEEK">周</option>
            <option value="MONTH">月</option>
            <option value="YEAR">年</option>
          </select>
        </div>
      </div>

      <div class="form-group">
        <label class="form-label">分类</label>
        <select id="categorySelect" class="form-select">
          <option value="GENERAL">常规</option>
          <option value="ANNIVERSARY">纪念日</option>
          <option value="BIRTHDAY">生日</option>
          <option value="HOLIDAY">节日</option>
          <option value="WORK">工作</option>
          <option value="LIFE">生活</option>
          <option value="OTHER">其他</option>
        </select>
      </div>

      <div class="form-group" style="display: flex; justify-content: space-between; align-items: center;">
        <div>
          <label class="form-label" style="margin: 0;">首页置顶</label>
          <span style="font-size: 11px; color: var(--text-muted);">置顶显示在首页第一优先级</span>
        </div>
        <input type="checkbox" id="pinnedCheckbox" style="width: 20px; height: 20px; accent-color: var(--accent-gold);">
      </div>

      <!-- Event Color Picker -->
      <div class="form-group">
        <label class="form-label">首页卡片颜色 (Event Color)</label>
        <div class="swatches" id="colorSwatches"></div>
      </div>

      <!-- Event Background Picker & Studio -->
      <div class="form-group">
        <label class="form-label">详情页背景 (Event Background)</label>
        <div class="segmented-control" style="margin-bottom: 8px;">
          <div id="bgTabPreset" class="segment-btn active" onclick="switchBgTab('PRESET')">预设微光</div>
          <div id="bgTabPhoto" class="segment-btn" onclick="switchBgTab('PHOTO')">自定义照片</div>
        </div>

        <div id="presetBgSection">
          <select id="presetBgSelect" class="form-select"></select>
        </div>

        <div id="photoBgSection" style="display: none;">
          <input type="file" id="photoFileInput" accept="image/*" style="display: none;" onchange="onPhotoSelected(event)">
          <button type="button" class="btn-icon" style="width: 100%; padding: 10px;" onclick="document.getElementById('photoFileInput').click()">🖼️ 从相册选择照片并裁剪</button>

          <!-- Interactive Mobile Photo Studio -->
          <div id="photoStudioBox" class="photo-studio" style="display: none;">
            <div id="canvasWrapper" class="canvas-wrapper">
              <canvas id="cropCanvas" width="360" height="360"></canvas>
              <div class="crop-bezel-vignette"></div>
              <div class="crop-live-preview">
                <div id="previewTitle" class="crop-hero-title">结婚纪念日</div>
                <div class="crop-hero-number">10</div>
                <div class="crop-hero-unit">天</div>
                <div id="previewDate" class="crop-hero-date">2026年10月15日</div>
              </div>
            </div>

            <div class="studio-controls">
              <div class="studio-buttons">
                <button type="button" id="btnMaskRound" class="btn-studio active" onclick="setWatchMaskShape('round')">圆屏预览 (454px)</button>
                <button type="button" id="btnMaskSquare" class="btn-studio" onclick="setWatchMaskShape('square')">方屏预览 (384px)</button>
              </div>

              <div class="slider-group">
                <span>缩放</span>
                <input type="range" id="zoomSlider" min="0.5" max="3.0" step="0.05" value="1" oninput="onZoomSliderChanged(this.value)">
              </div>

              <div class="slider-group">
                <span>遮罩暗度</span>
                <input type="range" id="dimSlider" min="0.2" max="0.8" step="0.05" value="0.45" oninput="onDimSliderChanged(this.value)">
                <span id="dimValText">45%</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="form-group">
        <label class="form-label">备注</label>
        <input type="text" id="eventNotes" class="form-input" placeholder="备忘事项...">
      </div>

      <div style="display: flex; gap: 10px; margin-top: 20px;">
        <button type="button" class="btn-icon" style="flex: 1; padding: 12px;" onclick="closeModal()">取消</button>
        <button type="submit" class="btn-add" style="position: static; flex: 2; margin: 0; padding: 12px;">保存并同步到手表</button>
      </div>
    </form>
  </div>
</div>

<div id="toast" class="toast">✅ 已同步到手表</div>

<script>
var TOKEN = "$sessionToken";
var currentEvents = [];
var selectedColorType = "Default";
var selectedColorHex = null;
var currentBgType = "DEFAULT";
var customImageUploadedPath = null;
var customImageDimAlpha = 0.45;

var studioImg = null;
var studioScale = 1.0;
var studioOffsetX = 0;
var studioOffsetY = 0;
var isDragging = false;
var dragStartX = 0;
var dragStartY = 0;
var pinchStartDist = 0;

var COLOR_PRESETS = [
  { name: "墨色微光", type: "Default", color: "#1e222b" },
  { name: "琥珀金", type: "Amber", color: "#ffa000" },
  { name: "青空翡翠", type: "Emerald", color: "#009688" },
  { name: "星河深蓝", type: "Ocean", color: "#1e88e5" },
  { name: "玫瑰粉", type: "Rose", color: "#e91e63" },
  { name: "紫罗兰", type: "Violet", color: "#9c27b0" },
  { name: "珊瑚橙", type: "Coral", color: "#ff5722" }
];

var BG_PRESETS = [
  { name: "默认 (墨色深邃微光)", value: "DEFAULT" },
  { name: "粉红微光", value: "COLOR_PINK" },
  { name: "琥珀金光", value: "COLOR_GOLD" },
  { name: "青空翡翠", value: "COLOR_TEAL" },
  { name: "星河深蓝", value: "COLOR_BLUE" },
  { name: "紫罗兰光", value: "COLOR_PURPLE" },
  { name: "日落渐变", value: "GRAD_SUNSET" },
  { name: "极光渐变", value: "GRAD_AURORA" },
  { name: "星云渐变", value: "GRAD_NEBULA" }
];

function showToast(msg) {
  var t = document.getElementById("toast");
  t.innerText = msg || "✅ 已同步到手表";
  t.classList.add("active");
  setTimeout(function() { t.classList.remove("active"); }, 2500);
}

function api(path, method, body) {
  method = method || "GET";
  var url = path + (path.indexOf("?") >= 0 ? "&" : "?") + "token=" + TOKEN;
  var opts = { method: method, headers: {} };
  if (body) {
    opts.headers["Content-Type"] = "application/json";
    opts.body = JSON.stringify(body);
  }
  return fetch(url, opts).then(function(res) {
    if (!res.ok) throw new Error("HTTP " + res.status);
    return res.json();
  });
}

function loadEvents() {
  api("/api/events").then(function(data) {
    currentEvents = data.events || [];
    renderEventList(currentEvents);
  }).catch(function(e) {
    document.getElementById("eventList").innerHTML =
      '<div style="text-align:center; color:var(--accent-copper); padding: 40px 0;">无法连接手表，请检查手机网络</div>';
  });
}

function renderEventList(events) {
  var container = document.getElementById("eventList");
  if (!events || events.length === 0) {
    container.innerHTML = '<div style="text-align: center; color: var(--text-muted); padding: 40px 0;">暂无倒数日事件，点击下方按钮新建</div>';
    return;
  }

  var html = "";
  for (var i = 0; i < events.length; i++) {
    var evt = events[i];
    var isPinned = !!evt.isPinned;
    var colorHex = evt.colorHex || "#1e222b";
    var days = evt.daysCount !== undefined ? evt.daysCount : "--";
    var statusText = evt.status === "PAST" ? "已过去" : (evt.status === "TODAY" ? "今天" : "还有");

    html += '<div class="event-card ' + (isPinned ? 'pinned' : '') + '">' +
      '<div class="event-color-stripe" style="background: ' + colorHex + ';"></div>' +
      '<div class="event-header">' +
        '<div class="event-title-wrap">' +
          '<span>' + (evt.emoji || '📅') + '</span>' +
          '<span>' + evt.title + '</span>' +
          (isPinned ? '<span class="badge" style="color:var(--accent-gold);">📌 置顶</span>' : '') +
        '</div>' +
        '<div class="event-days">' +
          '<span style="font-size: 11px; color: var(--text-muted); font-weight: normal;">' + statusText + '</span> ' +
          days + ' <span style="font-size: 13px;">天</span>' +
        '</div>' +
      '</div>' +
      '<div class="event-meta">' +
        '<span>📅 ' + (evt.targetDateStr || '') + '</span>' +
        '<span>' + (evt.isLunar ? '🌙 农历' : '☀️ 公历') + '</span>' +
        '<span>🔄 ' + (evt.repeatDesc || '不重复') + '</span>' +
        '<span>🏷️ ' + (evt.categoryDesc || '纪念日') + '</span>' +
      '</div>' +
      '<div class="event-actions">' +
        '<button class="btn-icon" onclick="moveEvent(' + i + ', -1)" ' + (i === 0 ? 'disabled' : '') + '>▲ 上移</button>' +
        '<button class="btn-icon" onclick="moveEvent(' + i + ', 1)" ' + (i === events.length - 1 ? 'disabled' : '') + '>▼ 下移</button>' +
        '<button class="btn-icon" onclick="openEditModal(\'' + evt.id + '\')">✏️ 编辑</button>' +
        '<button class="btn-icon btn-danger" onclick="deleteEvent(\'' + evt.id + '\', \'' + evt.title + '\')">🗑️ 删除</button>' +
      '</div>' +
    '</div>';
  }
  container.innerHTML = html;
}

function moveEvent(index, delta) {
  var target = index + delta;
  if (target < 0 || target >= currentEvents.length) return;
  var temp = currentEvents[index];
  currentEvents[index] = currentEvents[target];
  currentEvents[target] = temp;
  renderEventList(currentEvents);

  var ids = [];
  for (var i = 0; i < currentEvents.length; i++) {
    ids.push(currentEvents[i].id);
  }
  api("/api/reorder", "POST", { eventIds: ids }).then(function() {
    showToast("已调整排序");
  }).catch(function(e) {
    showToast("排序同步失败");
    loadEvents();
  });
}

function deleteEvent(id, title) {
  if (!confirm("确定要删除「" + title + "」吗？")) return;
  api("/api/events/" + id, "DELETE").then(function() {
    showToast("已删除事件");
    loadEvents();
  }).catch(function(e) {
    alert("删除失败：" + e.message);
  });
}

function initColorSwatches() {
  var container = document.getElementById("colorSwatches");
  var html = "";
  for (var i = 0; i < COLOR_PRESETS.length; i++) {
    var c = COLOR_PRESETS[i];
    var isSel = selectedColorType === c.type;
    html += '<div class="swatch ' + (isSel ? 'selected' : '') + '" style="background: ' + c.color + ';" title="' + c.name + '" onclick="selectColor(\'' + c.type + '\', \'' + c.color + '\')"></div>';
  }
  container.innerHTML = html;
}

function selectColor(type, hex) {
  selectedColorType = type;
  selectedColorHex = hex;
  initColorSwatches();
}

function initBgPresets() {
  var select = document.getElementById("presetBgSelect");
  var html = "";
  for (var i = 0; i < BG_PRESETS.length; i++) {
    var b = BG_PRESETS[i];
    html += '<option value="' + b.value + '">' + b.name + '</option>';
  }
  select.innerHTML = html;
}

function openCreateModal() {
  document.getElementById("modalTitle").innerText = "新建倒数日";
  document.getElementById("editEventId").value = "";
  document.getElementById("eventEmoji").value = "🎂";
  document.getElementById("eventTitle").value = "";
  document.getElementById("eventNotes").value = "";
  document.getElementById("pinnedCheckbox").checked = false;
  document.getElementById("repeatSelect").value = "NONE";
  document.getElementById("categorySelect").value = "ANNIVERSARY";
  onRepeatRuleChanged();

  var todayStr = new Date().toISOString().split("T")[0];
  document.getElementById("solarDateInput").value = todayStr;
  switchCalendarType("SOLAR");

  selectColor("Default", "#1e222b");
  switchBgTab("PRESET");
  document.getElementById("presetBgSelect").value = "DEFAULT";
  customImageUploadedPath = null;

  document.getElementById("modalOverlay").classList.add("active");
}

function openEditModal(id) {
  var evt = null;
  for (var i = 0; i < currentEvents.length; i++) {
    if (currentEvents[i].id === id) { evt = currentEvents[i]; break; }
  }
  if (!evt) return;

  document.getElementById("modalTitle").innerText = "编辑倒数日";
  document.getElementById("editEventId").value = evt.id;
  document.getElementById("eventEmoji").value = evt.emoji || "";
  document.getElementById("eventTitle").value = evt.title || "";
  document.getElementById("eventNotes").value = evt.notes || "";
  document.getElementById("pinnedCheckbox").checked = !!evt.isPinned;
  document.getElementById("categorySelect").value = evt.category || "ANNIVERSARY";

  if (evt.isLunar && evt.lunarDate) {
    switchCalendarType("LUNAR");
    document.getElementById("lunarYearInput").value = evt.lunarDate.year;
    document.getElementById("lunarMonthInput").value = evt.lunarDate.month;
    document.getElementById("lunarDayInput").value = evt.lunarDate.day;
    document.getElementById("lunarLeapCheckbox").checked = !!evt.lunarDate.isLeap;
  } else {
    switchCalendarType("SOLAR");
    document.getElementById("solarDateInput").value = evt.solarDateStr || new Date().toISOString().split("T")[0];
  }

  if (evt.repeatRule && evt.repeatRule.type === "CUSTOM") {
    document.getElementById("repeatSelect").value = "CUSTOM";
    document.getElementById("customRepeatInterval").value = evt.repeatRule.interval || 1;
    document.getElementById("customRepeatUnit").value = evt.repeatRule.unit || "DAY";
  } else {
    document.getElementById("repeatSelect").value = (evt.repeatRule && evt.repeatRule.type) || "NONE";
  }
  onRepeatRuleChanged();

  selectColor(evt.colorType || "Default", evt.colorHex || "#1e222b");

  if (evt.background && evt.background.type === "IMAGE") {
    switchBgTab("PHOTO");
    customImageUploadedPath = evt.background.path;
    customImageDimAlpha = evt.background.dimAlpha || 0.45;
    document.getElementById("dimSlider").value = customImageDimAlpha;
    document.getElementById("dimValText").innerText = Math.round(customImageDimAlpha * 100) + "%";
  } else {
    switchBgTab("PRESET");
    document.getElementById("presetBgSelect").value = (evt.background && evt.background.presetKey) || "DEFAULT";
    customImageUploadedPath = null;
  }

  document.getElementById("modalOverlay").classList.add("active");
}

function closeModal() {
  document.getElementById("modalOverlay").classList.remove("active");
}

function switchCalendarType(type) {
  var isSolar = type === "SOLAR";
  document.getElementById("segSolar").classList.toggle("active", isSolar);
  document.getElementById("segLunar").classList.toggle("active", !isSolar);
  document.getElementById("solarSection").style.display = isSolar ? "block" : "none";
  document.getElementById("lunarSection").style.display = isSolar ? "none" : "block";
}

function onRepeatRuleChanged() {
  var val = document.getElementById("repeatSelect").value;
  document.getElementById("customRepeatGroup").style.display = val === "CUSTOM" ? "flex" : "none";
}

function switchBgTab(tab) {
  var isPreset = tab === "PRESET";
  document.getElementById("bgTabPreset").classList.toggle("active", isPreset);
  document.getElementById("bgTabPhoto").classList.toggle("active", !isPreset);
  document.getElementById("presetBgSection").style.display = isPreset ? "block" : "none";
  document.getElementById("photoBgSection").style.display = isPreset ? "none" : "block";
}

function onPhotoSelected(e) {
  var file = e.target.files && e.target.files[0];
  if (!file) return;

  var reader = new FileReader();
  reader.onload = function(evt) {
    var img = new Image();
    img.onload = function() {
      studioImg = img;
      studioScale = 1.0;
      studioOffsetX = 0;
      studioOffsetY = 0;
      document.getElementById("photoStudioBox").style.display = "block";
      document.getElementById("zoomSlider").value = "1";
      renderStudioCanvas();
    };
    img.src = evt.target.result;
  };
  reader.readAsDataURL(file);
}

function setWatchMaskShape(shape) {
  var wrapper = document.getElementById("canvasWrapper");
  var isRound = shape === "round";
  wrapper.classList.toggle("square", !isRound);
  document.getElementById("btnMaskRound").classList.toggle("active", isRound);
  document.getElementById("btnMaskSquare").classList.toggle("active", !isRound);
}

function onZoomSliderChanged(val) {
  studioScale = parseFloat(val);
  renderStudioCanvas();
}

function onDimSliderChanged(val) {
  customImageDimAlpha = parseFloat(val);
  document.getElementById("dimValText").innerText = Math.round(customImageDimAlpha * 100) + "%";
  renderStudioCanvas();
}

function renderStudioCanvas() {
  if (!studioImg) return;
  var canvas = document.getElementById("cropCanvas");
  var ctx = canvas.getContext("2d");
  var w = canvas.width;
  var h = canvas.height;

  ctx.clearRect(0, 0, w, h);

  var imgRatio = studioImg.width / studioImg.height;
  var drawW = w * studioScale;
  var drawH = drawW / imgRatio;
  if (drawH < h * studioScale) {
    drawH = h * studioScale;
    drawW = drawH * imgRatio;
  }

  var posX = (w - drawW) / 2 + studioOffsetX;
  var posY = (h - drawH) / 2 + studioOffsetY;
  ctx.drawImage(studioImg, posX, posY, drawW, drawH);

  ctx.fillStyle = "rgba(0, 0, 0, " + customImageDimAlpha + ")";
  ctx.fillRect(0, 0, w, h);

  var grad = ctx.createRadialGradient(w/2, h/2, w*0.35, w/2, h/2, w*0.7);
  grad.addColorStop(0, "rgba(0,0,0,0)");
  grad.addColorStop(0.5, "rgba(0,0,0,0.35)");
  grad.addColorStop(1, "rgba(0,0,0,0.85)");
  ctx.fillStyle = grad;
  ctx.fillRect(0, 0, w, h);

  document.getElementById("previewTitle").innerText = document.getElementById("eventTitle").value || "纪念日";
}

var canvasWrap = document.getElementById("canvasWrapper");
canvasWrap.addEventListener("mousedown", function(e) {
  isDragging = true;
  dragStartX = e.clientX - studioOffsetX;
  dragStartY = e.clientY - studioOffsetY;
});
window.addEventListener("mousemove", function(e) {
  if (!isDragging) return;
  studioOffsetX = e.clientX - dragStartX;
  studioOffsetY = e.clientY - dragStartY;
  renderStudioCanvas();
});
window.addEventListener("mouseup", function() { isDragging = false; });

canvasWrap.addEventListener("touchstart", function(e) {
  if (e.touches.length === 1) {
    isDragging = true;
    dragStartX = e.touches[0].clientX - studioOffsetX;
    dragStartY = e.touches[0].clientY - studioOffsetY;
  } else if (e.touches.length === 2) {
    isDragging = false;
    pinchStartDist = Math.hypot(
      e.touches[0].clientX - e.touches[1].clientX,
      e.touches[0].clientY - e.touches[1].clientY
    );
  }
});
canvasWrap.addEventListener("touchmove", function(e) {
  e.preventDefault();
  if (e.touches.length === 1 && isDragging) {
    studioOffsetX = e.touches[0].clientX - dragStartX;
    studioOffsetY = e.touches[0].clientY - dragStartY;
    renderStudioCanvas();
  } else if (e.touches.length === 2) {
    var dist = Math.hypot(
      e.touches[0].clientX - e.touches[1].clientX,
      e.touches[0].clientY - e.touches[1].clientY
    );
    var delta = dist / (pinchStartDist || dist);
    studioScale = Math.max(0.5, Math.min(3.0, studioScale * delta));
    document.getElementById("zoomSlider").value = studioScale.toFixed(2);
    pinchStartDist = dist;
    renderStudioCanvas();
  }
}, { passive: false });
canvasWrap.addEventListener("touchend", function() { isDragging = false; });

async function handleFormSubmit(e) {
  e.preventDefault();
  var id = document.getElementById("editEventId").value;
  var isCreate = !id;

  var backgroundPayload = { type: "DEFAULT" };
  var isPhotoTab = document.getElementById("bgTabPhoto").classList.contains("active");

  if (isPhotoTab) {
    if (studioImg) {
      var exportCanvas = document.createElement("canvas");
      exportCanvas.width = 600;
      exportCanvas.height = 600;
      var expCtx = exportCanvas.getContext("2d");

      var imgRatio = studioImg.width / studioImg.height;
      var drawW = 600 * studioScale;
      var drawH = drawW / imgRatio;
      if (drawH < 600 * studioScale) {
        drawH = 600 * studioScale;
        drawW = drawH * imgRatio;
      }
      var scaleMultiplier = 600 / 360;
      var posX = (600 - drawW) / 2 + studioOffsetX * scaleMultiplier;
      var posY = (600 - drawH) / 2 + studioOffsetY * scaleMultiplier;
      expCtx.drawImage(studioImg, posX, posY, drawW, drawH);

      var base64Data = exportCanvas.toDataURL("image/jpeg", 0.92);
      var uploadRes = await api("/api/upload-image", "POST", {
        base64: base64Data,
        eventId: id || "phone_draft",
        dimAlpha: customImageDimAlpha
      });
      customImageUploadedPath = uploadRes.path;
    }

    if (customImageUploadedPath) {
      backgroundPayload = {
        type: "IMAGE",
        path: customImageUploadedPath,
        dimAlpha: customImageDimAlpha
      };
    }
  } else {
    backgroundPayload = {
      type: "PRESET",
      presetKey: document.getElementById("presetBgSelect").value
    };
  }

  var isLunar = document.getElementById("segLunar").classList.contains("active");
  var eventPayload = {
    title: document.getElementById("eventTitle").value,
    emoji: document.getElementById("eventEmoji").value || "🎂",
    notes: document.getElementById("eventNotes").value,
    isPinned: document.getElementById("pinnedCheckbox").checked,
    category: document.getElementById("categorySelect").value,
    colorType: selectedColorType,
    colorHex: selectedColorHex,
    background: backgroundPayload,
    isLunar: isLunar
  };

  if (isLunar) {
    eventPayload.lunarDate = {
      year: parseInt(document.getElementById("lunarYearInput").value) || 2026,
      month: parseInt(document.getElementById("lunarMonthInput").value) || 1,
      day: parseInt(document.getElementById("lunarDayInput").value) || 1,
      isLeap: document.getElementById("lunarLeapCheckbox").checked
    };
  } else {
    eventPayload.solarDateStr = document.getElementById("solarDateInput").value;
  }

  var repeatVal = document.getElementById("repeatSelect").value;
  if (repeatVal === "CUSTOM") {
    eventPayload.repeatRule = {
      type: "CUSTOM",
      interval: parseInt(document.getElementById("customRepeatInterval").value) || 1,
      unit: document.getElementById("customRepeatUnit").value
    };
  } else {
    eventPayload.repeatRule = { type: repeatVal };
  }

  try {
    if (isCreate) {
      await api("/api/events", "POST", eventPayload);
      showToast("✅ 已创建并同步到手表");
    } else {
      await api("/api/events/" + id, "PUT", eventPayload);
      showToast("✅ 已保存并同步到手表");
    }
    closeModal();
    loadEvents();
  } catch (err) {
    alert("保存失败：" + err.message);
  }
}

initColorSwatches();
initBgPresets();
loadEvents();
</script>
</body>
</html>
        """.trimIndent()
    }
}
