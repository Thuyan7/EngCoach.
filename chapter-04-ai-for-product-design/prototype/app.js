// app.js
// Simple, readable prototype logic. Uses mock-data.js for content.
// No frameworks, no classes - just plain functions, if/else, and for loops.

var screens = ["login", "dashboard", "test", "result", "progress"];

function showScreen(name) {
  for (var i = 0; i < screens.length; i++) {
    var el = document.getElementById("screen-" + screens[i]);
    if (screens[i] === name) {
      el.classList.remove("hidden");
    } else {
      el.classList.add("hidden");
    }
  }

  if (name === "login") {
    document.getElementById("app-shell").classList.add("hidden");
  } else {
    document.getElementById("app-shell").classList.remove("hidden");
  }

  var navButtons = document.querySelectorAll(".sidebar .navlist button");
  for (var j = 0; j < navButtons.length; j++) {
    if (navButtons[j].dataset.goto === name) {
      navButtons[j].setAttribute("aria-current", "page");
    } else {
      navButtons[j].removeAttribute("aria-current");
    }
  }

  var dashControl = document.getElementById("dash-state-control");
  var progControl = document.getElementById("prog-state-control");
  var timerControl = document.getElementById("timer-control");
  if (name === "dashboard") { dashControl.classList.remove("hidden"); } else { dashControl.classList.add("hidden"); }
  if (name === "progress") { progControl.classList.remove("hidden"); } else { progControl.classList.add("hidden"); }
  if (name === "test") { timerControl.classList.remove("hidden"); } else { timerControl.classList.add("hidden"); }

  // re-run entrance animations so they play each time the screen is opened,
  // not only on first page load
  if (name === "dashboard") {
    renderDashboard();
    renderPartGrid();
    renderRecentAttempts();
  } else if (name === "progress") {
    renderProgress();
    renderTrendLine();
    staggerHistoryRows();
  } else if (name === "test") {
    renderQuestionNavigator();
  } else if (name === "result") {
    renderPartBreakdown();
  }

  replayHeading(name);
  moveNavIndicator(name);
}

function applyDataState(prefix, value) {
  var states = ["loading", "empty", "populated"];
  for (var i = 0; i < states.length; i++) {
    var el = document.getElementById(prefix + "-" + states[i]);
    if (states[i] === value) {
      el.classList.remove("hidden");
    } else {
      el.classList.add("hidden");
    }
  }
}

function updateTimer() {
  var slider = document.getElementById("time-sim");
  var timerEl = document.getElementById("timer");
  var ring = document.getElementById("timer-ring-value");
  var mins = parseInt(slider.value, 10);
  var maxMins = parseInt(slider.max, 10);

  timerEl.textContent = (mins < 10 ? "0" + mins : mins) + ":00";
  document.getElementById("time-sim-val").textContent = mins + " min";

  // drive the circular ring: full circumference when time is full, shrinking as it runs down
  var radius = 46;
  var circumference = 2 * Math.PI * radius;
  var fraction = mins / maxMins;
  ring.style.strokeDasharray = circumference;
  ring.style.strokeDashoffset = circumference * (1 - fraction);

  timerEl.classList.remove("state-warning");
  timerEl.classList.remove("state-danger");
  ring.classList.remove("state-warning");
  ring.classList.remove("state-danger");

  if (mins < MOCK_TIMER.dangerThresholdMinutes) {
    timerEl.classList.add("state-danger");
    ring.classList.add("state-danger");
  } else if (mins < MOCK_TIMER.warningThresholdMinutes) {
    timerEl.classList.add("state-warning");
    ring.classList.add("state-warning");
  }
}

function addRipple(event) {
  var button = event.currentTarget;
  var circle = document.createElement("span");
  var size = Math.max(button.offsetWidth, button.offsetHeight);
  var rect = button.getBoundingClientRect();

  circle.className = "ripple";
  circle.style.width = size + "px";
  circle.style.height = size + "px";
  circle.style.left = (event.clientX - rect.left - size / 2) + "px";
  circle.style.top = (event.clientY - rect.top - size / 2) + "px";

  button.appendChild(circle);
  setTimeout(function () { circle.remove(); }, 600);
}

function openDeleteModal() {
  document.getElementById("delete-modal").classList.remove("hidden");
}

function closeDeleteModal() {
  document.getElementById("delete-modal").classList.add("hidden");
}

function countUp(elementId, targetValue, durationMs) {
  var el = document.getElementById(elementId);
  var steps = 40;
  var current = 0;
  var increment = targetValue / steps;
  var stepTime = durationMs / steps;

  var timer = setInterval(function () {
    current = current + increment;
    if (current >= targetValue) {
      current = targetValue;
      clearInterval(timer);
    }
    el.textContent = Math.round(current);
  }, stepTime);
}

function renderDashboard() {
  document.getElementById("dash-attempts").textContent = MOCK_DASHBOARD_STATS.attemptCount;
  document.getElementById("dash-attempts-inline").textContent = MOCK_DASHBOARD_STATS.attemptCount;
  document.getElementById("dash-last").textContent = MOCK_DASHBOARD_STATS.lastAttempt;

  countUp("dash-score", MOCK_DASHBOARD_STATS.averageScore, 900);

  // start the marker at 0 and glide it to its real position, so the CSS
  // transition on .band-marker has an actual change to animate
  var marker = document.getElementById("band-marker");
  marker.style.left = "0%";
  setTimeout(function () {
    marker.style.left = MOCK_DASHBOARD_STATS.bandPositionPercent + "%";
  }, 80);
}

function renderQuestion() {
  document.getElementById("q-progress").textContent = "Question " + MOCK_QUESTION.number + " of " + MOCK_QUESTION.total;
  document.getElementById("q-text").textContent = MOCK_QUESTION.prompt;

  var optionsContainer = document.getElementById("q-options");
  optionsContainer.innerHTML = "";
  for (var i = 0; i < MOCK_QUESTION.options.length; i++) {
    var opt = MOCK_QUESTION.options[i];
    var btn = document.createElement("button");
    btn.className = "option";
    btn.textContent = opt.id + ". " + opt.text;
    btn.addEventListener("click", function (e) {
      burstFrom(e.currentTarget);
      setTimeout(function () { showScreen("result"); }, 260);
    });
    optionsContainer.appendChild(btn);
  }
}

function renderResult() {
  document.getElementById("result-score").textContent = MOCK_RESULT.score + " / " + MOCK_RESULT.total;
  document.getElementById("result-label").textContent = MOCK_RESULT.testLabel;

  var list = document.getElementById("result-explanations");
  list.innerHTML = "";
  for (var i = 0; i < MOCK_RESULT.explanations.length; i++) {
    var item = MOCK_RESULT.explanations[i];
    var card = document.createElement("div");

    if (item.status === "incorrect") {
      card.className = "explain-card incorrect";
      card.innerHTML =
        '<p class="q">Question ' + item.questionNumber + ' - Incorrect (you chose ' + item.chosenOption + ')</p>' +
        '<p class="body">' + item.text + '</p>';
    } else {
      card.className = "explain-card unavailable";
      card.innerHTML =
        '<p class="q" style="color:var(--text-secondary);">Question ' + item.questionNumber + ' - Incorrect (you chose ' + item.chosenOption + ')</p>' +
        '<p class="body">' + item.text + '</p>' +
        '<button class="retry-link">Retry</button>';
    }
    list.appendChild(card);
  }
}

function renderProgress() {
  var labelsContainer = document.getElementById("prog-bar-labels");
  var listContainer = document.getElementById("prog-history-list");

  labelsContainer.innerHTML = "";
  listContainer.innerHTML = "";

  // history is newest-first; label oldest-first for a left-to-right timeline
  for (var i = MOCK_PROGRESS_HISTORY.length - 1; i >= 0; i--) {
    var label = document.createElement("span");
    label.textContent = MOCK_PROGRESS_HISTORY[i].date;
    labelsContainer.appendChild(label);
  }

  for (var j = 0; j < MOCK_PROGRESS_HISTORY.length; j++) {
    var row = MOCK_PROGRESS_HISTORY[j];
    var rowEl = document.createElement("div");
    rowEl.className = "history-row";
    rowEl.innerHTML =
      '<span>' + row.date + ' - ' + row.type + '</span>' +
      '<span class="score">' + row.score + '</span>' +
      '<button class="icon-btn" data-open-delete aria-label="Delete attempt: ' + row.date + ', ' + row.type + ', score ' + row.score + '">\u2715</button>';
    listContainer.appendChild(rowEl);
  }

  var deleteButtons = document.querySelectorAll("[data-open-delete]");
  for (var k = 0; k < deleteButtons.length; k++) {
    deleteButtons[k].addEventListener("click", openDeleteModal);
  }
}

document.addEventListener("DOMContentLoaded", function () {
  renderDashboard();
  renderQuestion();
  renderResult();
  renderProgress();

  var gotoButtons = document.querySelectorAll("[data-goto]");
  for (var i = 0; i < gotoButtons.length; i++) {
    gotoButtons[i].addEventListener("click", function () {
      showScreen(this.dataset.goto);
    });
  }

  document.getElementById("login-fail-toggle").addEventListener("click", function () {
    document.getElementById("login-error").classList.remove("hidden");
  });
  document.getElementById("login-submit").addEventListener("click", function () {
    showScreen("dashboard");
  });

  document.getElementById("dash-state").addEventListener("change", function (e) {
    applyDataState("dash", e.target.value);
  });
  document.getElementById("prog-state").addEventListener("change", function (e) {
    applyDataState("prog", e.target.value);
  });
  applyDataState("dash", "populated");
  applyDataState("prog", "populated");

  document.getElementById("time-sim").addEventListener("input", updateTimer);
  updateTimer();

  // ripple feedback on every .btn
  var rippleButtons = document.querySelectorAll(".btn");
  for (var r = 0; r < rippleButtons.length; r++) {
    rippleButtons[r].addEventListener("click", addRipple);
  }

  document.getElementById("delete-cancel").addEventListener("click", closeDeleteModalAnimated);
  document.getElementById("delete-confirm").addEventListener("click", closeDeleteModalAnimated);
  document.addEventListener("keydown", function (e) {
    if (e.key === "Escape") { closeDeleteModalAnimated(); }
  });

  renderPartGrid();
  renderRecentAttempts();
  renderQuestionNavigator();
  renderPartBreakdown();
  enableTilt();
  renderTrendLine();

  showScreen("login");
});

/* ============================================================
   Advanced motion layer
   ============================================================ */

// Split a heading into per-character spans so each letter can animate in
function splitHeading(el) {
  if (el.dataset.split === "done") { return; }
  var text = el.textContent;
  el.textContent = "";
  for (var i = 0; i < text.length; i++) {
    var span = document.createElement("span");
    span.className = "char";
    span.textContent = text[i] === " " ? "\u00A0" : text[i];
    span.style.animationDelay = (i * 22) + "ms";
    el.appendChild(span);
  }
  el.dataset.split = "done";
}

function replayHeading(screenName) {
  var section = document.getElementById("screen-" + screenName);
  if (!section) { return; }
  var heading = section.querySelector(".page-head h1");
  if (!heading) { return; }

  splitHeading(heading);
  var chars = heading.querySelectorAll(".char");
  for (var i = 0; i < chars.length; i++) {
    chars[i].style.animation = "none";
    void chars[i].offsetWidth; // force reflow so the animation restarts
    chars[i].style.animation = "";
  }
}

// Cards tilt slightly toward the pointer
function enableTilt() {
  var cards = document.querySelectorAll(".stat, .report");
  for (var i = 0; i < cards.length; i++) {
    cards[i].classList.add("tilt");
    cards[i].addEventListener("mousemove", handleTilt);
    cards[i].addEventListener("mouseleave", resetTilt);
  }
}

function handleTilt(e) {
  var card = e.currentTarget;
  var rect = card.getBoundingClientRect();
  var relX = (e.clientX - rect.left) / rect.width - 0.5;
  var relY = (e.clientY - rect.top) / rect.height - 0.5;
  var rotateY = relX * 7;
  var rotateX = relY * -7;
  card.style.transform = "perspective(700px) rotateX(" + rotateX + "deg) rotateY(" + rotateY + "deg) translateY(-2px)";
}

function resetTilt(e) {
  e.currentTarget.style.transform = "";
}

// Sliding indicator behind the active sidebar item
function moveNavIndicator(screenName) {
  var indicator = document.getElementById("nav-indicator");
  if (!indicator) { return; }
  var active = document.querySelector('.navlist button[data-goto="' + screenName + '"]');
  if (!active) { indicator.style.opacity = "0"; return; }
  indicator.style.opacity = "1";
  indicator.style.transform = "translateY(" + active.offsetTop + "px)";
}

// Draw the score trend as an SVG line chart that animates its own stroke
function renderTrendLine() {
  var container = document.getElementById("trend-line-container");
  if (!container) { return; }

  var points = [];
  for (var i = MOCK_PROGRESS_HISTORY.length - 1; i >= 0; i--) {
    points.push(MOCK_PROGRESS_HISTORY[i].barHeightPercent);
  }

  var width = 520;
  var height = 120;
  var stepX = width / (points.length - 1);
  var path = "";
  var dots = "";

  for (var p = 0; p < points.length; p++) {
    var x = p * stepX;
    var y = height - (points[p] / 100) * (height - 16) - 8;
    path += (p === 0 ? "M" : " L") + x.toFixed(1) + " " + y.toFixed(1);
    dots += '<circle cx="' + x.toFixed(1) + '" cy="' + y.toFixed(1) + '" r="4.5" fill="var(--tint-success-text)" style="animation-delay:' + (1100 + p * 140) + 'ms"></circle>';
  }

  var areaPath = path + " L" + width + " " + height + " L0 " + height + " Z";

  container.innerHTML =
    '<svg class="trend-line" viewBox="0 0 ' + width + ' ' + height + '" preserveAspectRatio="none" style="width:100%;height:120px;">' +
      '<defs><linearGradient id="trendFill" x1="0" y1="0" x2="0" y2="1">' +
        '<stop offset="0%" stop-color="var(--tint-success-text)" stop-opacity="0.22"/>' +
        '<stop offset="100%" stop-color="var(--tint-success-text)" stop-opacity="0"/>' +
      '</linearGradient></defs>' +
      '<path class="area" d="' + areaPath + '"></path>' +
      '<path d="' + path + '"></path>' +
      dots +
    '</svg>';
}

// Small particle burst from a clicked element
function burstFrom(element) {
  var rect = element.getBoundingClientRect();
  var originX = rect.left + rect.width / 2;
  var originY = rect.top + rect.height / 2;
  var colors = ["#1B5FA0", "#3B6D11", "#85B7EB"];

  for (var i = 0; i < 12; i++) {
    var dot = document.createElement("span");
    dot.className = "burst";
    dot.style.left = originX + "px";
    dot.style.top = originY + "px";
    dot.style.background = colors[i % colors.length];

    var angle = (i / 12) * Math.PI * 2;
    var distance = 60 + Math.random() * 40;
    dot.style.setProperty("--bx", Math.cos(angle) * distance + "px");
    dot.style.setProperty("--by", Math.sin(angle) * distance + "px");
    dot.style.animation = "burstOut 0.65s cubic-bezier(0.22,1,0.36,1) forwards";

    document.body.appendChild(dot);
    removeAfter(dot, 700);
  }
}

function removeAfter(element, delayMs) {
  setTimeout(function () { element.remove(); }, delayMs);
}

// Stagger the history rows as they appear
function staggerHistoryRows() {
  var rows = document.querySelectorAll(".history-row");
  for (var i = 0; i < rows.length; i++) {
    rows[i].style.animationDelay = (i * 80) + "ms";
  }
}

// Modal close with an exit animation instead of an instant hide
function closeDeleteModalAnimated() {
  var modal = document.getElementById("delete-modal");
  modal.classList.add("closing");
  setTimeout(function () {
    modal.classList.add("hidden");
    modal.classList.remove("closing");
  }, 200);
}

/* ============================================================
   Multi-region panels
   ============================================================ */

function renderPartGrid() {
  var grid = document.getElementById("part-grid");
  if (!grid) { return; }
  grid.innerHTML = "";

  for (var i = 0; i < MOCK_PARTS.length; i++) {
    var part = MOCK_PARTS[i];
    var card = document.createElement("button");
    card.className = "part-card";
    card.innerHTML =
      '<p class="pc-name">' + part.name + '</p>' +
      '<p class="pc-meta">' + part.meta + '</p>' +
      '<div class="pc-track"><div class="pc-fill" style="width:0%"></div></div>';
    card.addEventListener("click", function () { showScreen("test"); });
    grid.appendChild(card);
  }

  // animate the accuracy bars in after the cards are placed
  var fills = grid.querySelectorAll(".pc-fill");
  for (var f = 0; f < fills.length; f++) {
    fillLater(fills[f], MOCK_PARTS[f].accuracy, f * 70);
  }
}

function fillLater(element, percent, delayMs) {
  setTimeout(function () {
    element.style.width = percent + "%";
  }, delayMs + 150);
}

function renderRecentAttempts() {
  var container = document.getElementById("dash-recent");
  if (!container) { return; }
  container.innerHTML = "";

  for (var i = 0; i < MOCK_PROGRESS_HISTORY.length; i++) {
    var row = MOCK_PROGRESS_HISTORY[i];
    var el = document.createElement("div");
    el.className = "recent-row";
    el.innerHTML =
      '<span>' + row.date + ' - ' + row.type + '</span>' +
      '<span class="rr-score">' + row.score + '</span>';
    container.appendChild(el);
  }
}

function renderQuestionNavigator() {
  var grid = document.getElementById("q-grid");
  if (!grid) { return; }
  grid.innerHTML = "";

  for (var i = 0; i < MOCK_QUESTION_STATES.length; i++) {
    var cell = document.createElement("button");
    cell.className = "q-cell";
    if (MOCK_QUESTION_STATES[i] === "answered") {
      cell.classList.add("answered");
    } else if (MOCK_QUESTION_STATES[i] === "current") {
      cell.classList.add("current");
    }
    cell.textContent = (i + 1);
    cell.setAttribute("aria-label", "Question " + (i + 1));
    grid.appendChild(cell);
  }
}

function renderPartBreakdown() {
  var container = document.getElementById("part-breakdown");
  if (!container) { return; }
  container.innerHTML = "";

  for (var i = 0; i < MOCK_PART_BREAKDOWN.length; i++) {
    var item = MOCK_PART_BREAKDOWN[i];
    var row = document.createElement("div");
    row.className = "pb-row";
    row.innerHTML =
      '<div class="pb-top"><span class="pb-name">' + item.name + '</span><span class="pb-pct">' + item.accuracy + '%</span></div>' +
      '<div class="pb-track"><div class="pb-fill" style="width:0%"></div></div>';
    container.appendChild(row);
  }

  var fills = container.querySelectorAll(".pb-fill");
  for (var f = 0; f < fills.length; f++) {
    fillLater(fills[f], MOCK_PART_BREAKDOWN[f].accuracy, f * 90);
  }
}
