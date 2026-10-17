"use strict";

// 회원 · 운영 화면. 경로(/admin/members 등)로 탭을 고르고, 로그인은 /admin 콘솔에 맡긴다.
const $ = (selector, root = document) => root.querySelector(selector);
const VIEWS = {
  members: { path: "/admin/members", title: "회원 관리" },
  reports: { path: "/admin/reports", title: "신고 처리" },
  audit: { path: "/admin/audit", title: "작업 이력" },
};
const LOGIN_LABELS = {
  KAKAO: "카카오",
  APPLE: "애플",
  GOOGLE: "구글",
  ANDROIDTEST: "심사용(AOS)",
  IOSTEST: "심사용(iOS)",
};
let config, admin;

async function api(path, options = {}) {
  const response = await fetch(path, {
    credentials: "same-origin",
    ...options,
    headers: {
      "X-CSRF-Token": admin?.csrfToken ?? config?.csrfToken ?? "",
      ...options.headers,
    },
  });
  const payload = await response.json().catch(() => null);
  if (!response.ok || !payload?.isSuccess) {
    const error = new Error(payload?.message ?? "요청을 처리하지 못했어요. 잠시 후 다시 시도해주세요.");
    error.status = response.status;
    throw error;
  }
  return payload.result;
}

function el(tag, className, text) {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text !== undefined && text !== null) node.textContent = text;
  return node;
}

function safeImage(value) {
  if (typeof value !== "string" || !value.trim()) return "";
  try {
    const url = new URL(value, location.origin);
    return ["https:", "http:"].includes(url.protocol) ? url.href : "";
  } catch {
    return "";
  }
}

function fmt(value, withTime = true) {
  if (!value) return "—";
  const [date, time = ""] = value.split("T");
  return withTime ? `${date} ${time.slice(0, 5)}` : date;
}

function num(value) {
  return Number(value ?? 0).toLocaleString("ko-KR");
}

function message(id, text = "", error = false) {
  const node = $(id);
  node.textContent = text;
  node.classList.toggle("error", error);
}

function chip(text, tone = "") {
  return el("span", `chip ${tone}`.trim(), text);
}

function currentView() {
  return Object.keys(VIEWS).find((key) => location.pathname === VIEWS[key].path) ?? "members";
}

/* ---------- 회원 관리 ---------- */
const members = { cursor: null, loading: false, generation: 0 };

async function loadMemberSummary() {
  try {
    const summary = await api("/api/v1/admin/members/summary");
    $("#m-total").textContent = num(summary.totalCount);
    $("#m-active").textContent = num(summary.activeCount);
    $("#m-joined").textContent = num(summary.joinedLast7Days);
    $("#m-withdrawn").textContent = num(summary.withdrawnLast7Days);
  } catch (error) {
    message("#member-message", `집계를 불러오지 못했어요. ${error.message}`, true);
  }
}

function memberQuery(cursor) {
  const form = $("#member-filters");
  const params = new URLSearchParams({ size: "30" });
  for (const name of ["keyword", "state", "loginType"]) {
    const value = form.elements[name].value.trim();
    if (value) params.set(name, value);
  }
  if (cursor) params.set("cursor", cursor);
  return params;
}

function memberRow(item) {
  const row = el("tr", "clickable");
  row.tabIndex = 0;
  const who = el("td");
  const cell = el("div", "member-cell");
  const avatar = el("img", "avatar");
  avatar.alt = "";
  avatar.loading = "lazy";
  const src = safeImage(item.profileImageUrl);
  if (src) avatar.src = src;
  const name = el("div");
  name.append(el("strong", null, item.nickname), el("div", "muted", `ID ${item.id}`));
  cell.append(avatar, name);
  who.append(cell);
  const logins = el("td");
  (item.loginTypes.length ? item.loginTypes : ["—"]).forEach((type) =>
    logins.append(chip(LOGIN_LABELS[type] ?? type)),
  );
  row.append(
    who,
    logins,
    el("td", null, fmt(item.createdAt, false)),
    el("td", null, num(item.groupCount)),
    el("td", null, num(item.recordCount)),
    el("td", null, fmt(item.lastRecordAt)),
  );
  const state = el("td");
  state.append(item.active ? chip("활성", "ok") : chip(`탈퇴 ${fmt(item.deletedAt, false)}`, "off"));
  row.append(state);
  const open = () => openMember(item.id);
  row.addEventListener("click", open);
  row.addEventListener("keydown", (event) => {
    if (event.key === "Enter") open();
  });
  return row;
}

async function loadMembers(reset) {
  if (members.loading && !reset) return;
  const generation = reset ? ++members.generation : members.generation;
  members.loading = true;
  if (reset) {
    members.cursor = null;
    $("#member-rows").replaceChildren();
  }
  message("#member-message", "회원을 불러오고 있어요.");
  try {
    const page = await api(`/api/v1/admin/members?${memberQuery(members.cursor)}`);
    if (generation !== members.generation) return;
    page.items.forEach((item) => $("#member-rows").append(memberRow(item)));
    members.cursor = page.nextCursor;
    $("#member-more").hidden = !page.hasMore;
    message("#member-message", $("#member-rows").children.length ? "" : "조건에 맞는 회원이 없어요.");
  } catch (error) {
    if (generation === members.generation) message("#member-message", error.message, true);
  } finally {
    if (generation === members.generation) members.loading = false;
  }
}

function kv(pairs) {
  const grid = el("div", "kv");
  pairs.forEach(([label, value]) => {
    const box = el("div");
    box.append(el("span", null, label), el("strong", null, value));
    grid.append(box);
  });
  return grid;
}

function section(title, ...children) {
  const node = el("section");
  node.append(el("h3", null, title), ...children);
  return node;
}

async function openMember(id) {
  const dialog = $("#member-detail");
  const body = $("#member-detail-body");
  body.replaceChildren(el("p", "message", "회원 정보를 불러오고 있어요."));
  if (!dialog.open) dialog.showModal();
  try {
    const d = await api(`/api/v1/admin/members/${encodeURIComponent(id)}`);
    $("#member-detail-title").textContent = `${d.nickname} · ID ${d.id}`;
    const status = d.active ? "활성" : `탈퇴 (${fmt(d.deletedAt)})`;
    const logins = el("div");
    d.logins.forEach((login) =>
      logins.append(chip(`${LOGIN_LABELS[login.loginType] ?? login.loginType} · ${fmt(login.linkedAt, false)}`,
        login.active ? "" : "off")),
    );
    const groups = el("div");
    if (!d.groups.length) groups.append(el("p", "muted", "참여한 그룹이 없어요."));
    d.groups.forEach((group) => {
      const line = el("div");
      const link = el("a", null, group.name);
      link.href = `/admin/challenges?view=groups&groupId=${encodeURIComponent(group.id)}`;
      line.append(
        link,
        " ",
        chip(group.memberStatus === "JOINED" ? "참여 중" : "나감", group.memberStatus === "JOINED" ? "ok" : "off"),
        el("span", "muted", ` 참여 ${fmt(group.joinedAt, false)}${group.leftAt ? ` · 나감 ${fmt(group.leftAt, false)}` : ""}`),
      );
      if (!group.groupActive) line.append(" ", chip("삭제된 그룹", "off"));
      groups.append(line);
    });
    const devices = el("div");
    if (!d.devices.length) devices.append(el("p", "muted", "등록된 푸시 기기가 없어요."));
    d.devices.forEach((device) =>
      devices.append(chip(`${device.platform} · ${device.enabled ? "수신" : "꺼짐"} · ${fmt(device.lastSeenAt)}`,
        device.enabled ? "" : "off")),
    );
    const thumbs = el("div", "thumbs");
    d.recentRecords.forEach((record) => {
      const figure = el("figure");
      const img = el("img");
      img.alt = record.title ?? "";
      img.loading = "lazy";
      const src = safeImage(record.imageUrl);
      if (src) img.src = src;
      figure.append(img, el("figcaption", null, `${record.type === "MEAL" ? "식사" : "운동"} · ${fmt(record.uploadedAt, false)}`));
      thumbs.append(figure);
    });
    const feedLink = el("a", null, "인증 피드에서 이 회원 기록 보기");
    feedLink.href = `/admin?memberId=${encodeURIComponent(d.id)}&from=2026-01-01`;
    const c = d.counts;
    body.replaceChildren(
      section("기본 정보", kv([
        ["상태", status],
        ["가입일", fmt(d.createdAt)],
        ["생년월일", d.birthDate ?? "—"],
        ["목표 체중", d.targetWeightKg ? `${d.targetWeightKg}kg` : "—"],
        ["건강 앱 연동", d.healthConnection === "CONNECTED" ? "연결됨" : "연결 안 됨"],
        ["그룹 온보딩", d.groupOnboardingCompleted ? "완료" : "미완료"],
        ["로그인 세션", `${d.activeSessions}개`],
      ])),
      section("가입 경로", logins),
      section("활동", kv([
        ["식사 기록", num(c.mealRecords)],
        ["운동 기록", num(c.exerciseRecords)],
        ["챌린지 인증", num(c.challengeProofs)],
        ["댓글", num(c.comments)],
        ["걸음 기록 일수", num(c.stepDays)],
        ["누적 걸음", num(c.totalSteps)],
        ["최근 기록", fmt(c.lastRecordAt)],
        ["한 신고", num(c.reportsMade)],
        ["받은 신고", num(c.reportsReceived)],
      ])),
      section("그룹", groups),
      section("푸시 기기", devices),
      section("최근 기록", thumbs.children.length ? thumbs : el("p", "muted", "기록이 없어요."), el("p", null, ""), feedLink),
    );
  } catch (error) {
    body.replaceChildren(el("p", "message error", error.message));
  }
}

/* ---------- 신고 처리 ---------- */
const reports = { cursor: null, loading: false, generation: 0, state: "PENDING", busy: false };
const RESOLUTION_LABELS = { HIDDEN: "숨김", DISMISSED: "기각" };

function canWrite() {
  return admin?.role !== "VIEWER";
}

async function loadReportSummary() {
  try {
    const summary = await api("/api/v1/admin/reports/summary");
    $("#r-pending-records").textContent = num(summary.pendingRecords);
    $("#r-pending-reports").textContent = num(summary.pendingReports);
    $("#r-hidden").textContent = num(summary.hiddenLast7Days);
    $("#r-dismissed").textContent = num(summary.dismissedLast7Days);
  } catch (error) {
    message("#report-message", `집계를 불러오지 못했어요. ${error.message}`, true);
  }
}

async function resolveReport(item, action) {
  if (reports.busy) return;
  const label = action === "hide" ? "숨김" : "기각";
  const warning = action === "hide"
    ? "이 기록을 숨기면 기록, 댓글, 모든 그룹 공유가 삭제돼요. 되돌릴 수 없어요."
    : "신고를 기각하면 기록은 그대로 두고 신고만 처리 완료돼요.";
  if (!window.confirm(`${warning}\n\n기록 ID ${item.recordId}을(를) ${label} 처리할까요?`)) return;
  reports.busy = true;
  try {
    const result = await api(`/api/v1/admin/reports/records/${encodeURIComponent(item.recordId)}/${action}`, {
      method: "POST",
    });
    message("#report-message", `기록 ${result.recordId}: 신고 ${result.resolvedReports}건을 ${label} 처리했어요.`);
    loadReportSummary();
    loadReports(true);
  } catch (error) {
    message("#report-message", error.message, true);
  } finally {
    reports.busy = false;
  }
}

function reportCard(item) {
  const card = el("article", "report-card");
  const img = el("img");
  img.alt = item.title ?? "신고된 기록";
  img.loading = "lazy";
  const src = safeImage(item.imageUrl);
  if (src) img.src = src;
  const body = el("div");
  const type = item.recordType === "MEAL" ? "식사" : item.recordType === "EXERCISE" ? "운동" : "기록";
  body.append(el("h3", null, `${type} · ${item.title ?? "제목 없음"}`));
  const author = el("p");
  author.append("작성자 ");
  if (item.authorId) {
    const link = el("a", null, `${item.authorNickname} (ID ${item.authorId})`);
    link.href = "#";
    link.addEventListener("click", (event) => {
      event.preventDefault();
      openMember(item.authorId);
    });
    author.append(link);
  } else {
    author.append("알 수 없음");
  }
  author.append(el("span", "muted", ` · 업로드 ${fmt(item.uploadedAt)}`));
  const state = el("p");
  state.append(
    chip(`신고 ${num(item.reportCount)}건`, "warn"),
    item.pendingCount > 0 ? chip(`대기 ${num(item.pendingCount)}`, "") : chip(RESOLUTION_LABELS[item.resolution] ?? "처리 완료", "ok"),
    item.recordActive ? chip("게시 중", "ok") : chip("삭제됨", "off"),
  );
  const when = el("p", "muted",
    `첫 신고 ${fmt(item.firstReportedAt)} · 최근 신고 ${fmt(item.lastReportedAt)}${item.resolvedAt ? ` · 처리 ${fmt(item.resolvedAt)}` : ""}`);
  const who = el("p", "muted", `신고자: ${item.reporters.map((r) => r.nickname).join(", ")}${item.reportCount > item.reporters.length ? " 외" : ""}`);
  body.append(author, state, when, who);
  const actions = el("div", "report-actions");
  if (item.pendingCount > 0) {
    const hide = el("button", "danger-button", "숨김");
    hide.type = "button";
    const dismiss = el("button", "secondary-button", "기각");
    dismiss.type = "button";
    hide.disabled = dismiss.disabled = !canWrite();
    if (!canWrite()) hide.title = dismiss.title = "조회 전용 권한이에요.";
    hide.addEventListener("click", () => resolveReport(item, "hide"));
    dismiss.addEventListener("click", () => resolveReport(item, "dismiss"));
    actions.append(hide, dismiss);
  }
  card.append(img, body, actions);
  return card;
}

async function loadReports(reset) {
  if (reports.loading && !reset) return;
  const generation = reset ? ++reports.generation : reports.generation;
  reports.loading = true;
  if (reset) {
    reports.cursor = null;
    $("#report-list").replaceChildren();
  }
  const params = new URLSearchParams({ size: "20", state: reports.state });
  if (reports.cursor) params.set("cursor", reports.cursor);
  try {
    const page = await api(`/api/v1/admin/reports?${params}`);
    if (generation !== reports.generation) return;
    page.items.forEach((item) => $("#report-list").append(reportCard(item)));
    reports.cursor = page.nextCursor;
    $("#report-more").hidden = !page.hasMore;
    if (!$("#report-list").children.length) {
      message("#report-message", reports.state === "PENDING" ? "처리할 신고가 없어요." : "해당하는 신고가 없어요.");
    }
  } catch (error) {
    if (generation === reports.generation) message("#report-message", error.message, true);
  } finally {
    if (generation === reports.generation) reports.loading = false;
  }
}

/* ---------- 작업 이력 ---------- */
const audit = { cursor: null, loading: false, generation: 0 };

async function loadAudit(reset) {
  if (audit.loading && !reset) return;
  const generation = reset ? ++audit.generation : audit.generation;
  audit.loading = true;
  if (reset) {
    audit.cursor = null;
    $("#audit-rows").replaceChildren();
  }
  const params = new URLSearchParams({ size: "50" });
  const actor = $("#audit-filters").elements.actor.value.trim();
  if (actor) params.set("actor", actor);
  if (audit.cursor) params.set("cursor", audit.cursor);
  message("#audit-message", "작업 이력을 불러오고 있어요.");
  try {
    const page = await api(`/api/v1/admin/audit-logs?${params}`);
    if (generation !== audit.generation) return;
    page.items.forEach((item) => {
      const row = el("tr");
      const ok = item.status < 400;
      const result = el("td");
      result.append(chip(String(item.status), ok ? "ok" : "warn"));
      const path = el("td");
      path.append(el("code", null, item.path));
      row.append(el("td", null, fmt(item.createdAt)), el("td", null, item.actor), el("td", null, item.method), path, result);
      $("#audit-rows").append(row);
    });
    audit.cursor = page.nextCursor;
    $("#audit-more").hidden = !page.hasMore;
    message("#audit-message", $("#audit-rows").children.length ? "" : "기록된 변경 작업이 없어요.");
  } catch (error) {
    if (generation === audit.generation) message("#audit-message", error.message, true);
  } finally {
    if (generation === audit.generation) audit.loading = false;
  }
}

/* ---------- 공통 ---------- */
const LOADERS = {
  members: () => {
    loadMemberSummary();
    loadMembers(true);
  },
  reports: () => {
    loadReportSummary();
    loadReports(true);
  },
  audit: () => loadAudit(true),
};

function show(view) {
  document.querySelectorAll("[data-panel]").forEach((panel) => {
    panel.hidden = panel.dataset.panel !== view;
  });
  document.querySelectorAll("#ops-nav [data-view]").forEach((link) => {
    const active = link.dataset.view === view;
    link.classList.toggle("selected", active);
    if (active) link.setAttribute("aria-current", "page");
    else link.removeAttribute("aria-current");
  });
  $("#crumb").textContent = VIEWS[view].title;
  document.title = `${VIEWS[view].title} · 모디 운영 콘솔`;
  LOADERS[view]();
}

$("#member-filters").addEventListener("submit", (event) => {
  event.preventDefault();
  loadMembers(true);
});
$("#member-filters").addEventListener("change", (event) => {
  if (event.target.tagName === "SELECT") loadMembers(true);
});
$("#member-more").addEventListener("click", () => loadMembers(false));
document.querySelectorAll("#report-tabs [data-state]").forEach((tab) =>
  tab.addEventListener("click", () => {
    reports.state = tab.dataset.state;
    document.querySelectorAll("#report-tabs [data-state]").forEach((other) => {
      const active = other === tab;
      other.classList.toggle("active", active);
      other.setAttribute("aria-pressed", String(active));
    });
    message("#report-message");
    loadReports(true);
  }),
);
$("#report-more").addEventListener("click", () => loadReports(false));
$("#audit-filters").addEventListener("submit", (event) => {
  event.preventDefault();
  loadAudit(true);
});
$("#audit-more").addEventListener("click", () => loadAudit(false));
document.querySelectorAll("dialog [data-close]").forEach((button) =>
  button.addEventListener("click", () => button.closest("dialog").close()),
);
$("#logout").addEventListener("click", async () => {
  try {
    await api("/api/v1/admin/auth/logout", { method: "POST" });
  } finally {
    location.assign("/admin");
  }
});

async function init() {
  config = await api("/api/v1/admin/auth/config");
  const env = config.environment === "prod" ? "운영" : config.environment === "dev" ? "개발" : "로컬";
  document.querySelectorAll("[data-env]").forEach((badge) => {
    badge.textContent = env;
    badge.classList.toggle("prod", config.environment === "prod");
  });
  $("#sidebar-environment").textContent = `${env} 환경`;
  try {
    admin = await api("/api/v1/admin/auth/me");
  } catch (error) {
    if ([401, 403].includes(error.status)) {
      location.assign(`/admin?next=${encodeURIComponent(location.pathname)}`);
      return;
    }
    throw error;
  }
  $("#account-email").textContent = admin.email;
  $("#workspace").hidden = false;
  show(currentView());
}

init().catch((error) => {
  document.body.prepend(el("p", "message error", error.message));
});
