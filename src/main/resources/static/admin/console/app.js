"use strict";

const $ = (selector) => document.querySelector(selector);
const typeLabels = { MEAL: "식사", EXERCISE: "운동", CHALLENGE: "챌린지" };
const form = $("#filters");
let config,
  admin,
  selectedType = "",
  nextCursor = null,
  loaded = 0,
  loading = false,
  filters;
let generation = 0,
  loginInProgress = false;
let total = null;
let summaryFailed = false;
let pageAbort, summaryAbort;

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
    const error = new Error(
      payload?.message ??
        "요청을 처리하지 못했어요. 잠시 후 다시 시도해주세요.",
    );
    error.status = response.status;
    throw error;
  }
  return payload.result;
}

function message(selector, text = "", error = false) {
  const element = $(selector);
  element.textContent = text;
  element.classList.toggle("error", error);
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

function element(tag, className, text) {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text !== undefined) node.textContent = text;
  return node;
}

function kstDate(offset = 0) {
  const date = new Date(Date.now() + offset * 86400000);
  const parts = new Intl.DateTimeFormat("en", {
    timeZone: "Asia/Seoul",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).formatToParts(date);
  const part = (type) => parts.find((item) => item.type === type).value;
  return `${part("year")}-${part("month")}-${part("day")}`;
}

function showWorkspace() {
  $("#login").hidden = true;
  $("#workspace").hidden = false;
  $("#account-email").textContent = admin.email;
  if (new URLSearchParams(location.search).get("next") === "challenges") {
    location.assign("/admin/challenges");
    return;
  }
  reload();
}

async function showLogin() {
  $("#workspace").hidden = true;
  $("#login").hidden = false;
  if ($("#detail").open) $("#detail").close();
  config = await api("/api/v1/admin/auth/config");
  if (!config.googleClientId) {
    message(
      "#login-message",
      "Google 로그인 설정을 준비 중이에요. 관리자용 웹 클라이언트 ID 설정이 필요합니다.",
    );
    return;
  }
  try {
    await new Promise((resolve, reject) => {
      if (window.google?.accounts?.id) return resolve();
      const script = document.createElement("script");
      script.src = "https://accounts.google.com/gsi/client";
      script.onload = resolve;
      script.onerror = reject;
      document.head.append(script);
    });
    renderGoogleButton();
  } catch {
    message(
      "#login-message",
      "Google 로그인 화면을 불러오지 못했어요. 연결 상태를 확인해주세요.",
      true,
    );
  }
}

function renderGoogleButton() {
  google.accounts.id.initialize({
    client_id: config.googleClientId,
    nonce: config.nonce,
    auto_select: false,
    callback: loginWithGoogle,
  });
  $("#google-button").replaceChildren();
  const width = Math.min(290, $("#google-button").clientWidth);
  google.accounts.id.renderButton($("#google-button"), {
    theme: "outline",
    size: "large",
    width,
    locale: "ko",
  });
}

async function loginWithGoogle({ credential }) {
  if (loginInProgress) return;
  loginInProgress = true;
  message("#login-message", "계정과 관리자 권한을 확인하고 있어요.");
  try {
    admin = await api("/api/v1/admin/auth/google", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ credential }),
    });
    showWorkspace();
  } catch (error) {
    message(
      "#login-message",
      error.status === 403
        ? "이 계정에는 관리자 권한이 없어요."
        : error.message,
      true,
    );
    try {
      config = await api("/api/v1/admin/auth/config");
      renderGoogleButton();
    } catch {
      message(
        "#login-message",
        "로그인을 다시 준비하지 못했어요. 페이지를 새로고침해주세요.",
        true,
      );
    }
  } finally {
    loginInProgress = false;
  }
}

function card(item) {
  const button = element("button", "feed-card");
  button.type = "button";
  button.setAttribute(
    "aria-label",
    `${item.nickname} ${typeLabels[item.type]} 인증 상세 보기`,
  );
  const photo = element("div", "photo");
  const image = element("img");
  image.src = safeImage(item.imageUrl);
  image.loading = "lazy";
  image.alt = `${item.nickname}의 ${typeLabels[item.type]} 인증`;
  image.addEventListener("error", () => {
    image.alt = "이미지를 불러오지 못했어요";
  });
  const badge = element(
    "span",
    `type-badge ${item.type.toLowerCase()}`,
    typeLabels[item.type],
  );
  photo.append(image, badge);
  const body = element("div", "card-body");
  const author = element("div", "author-row");
  const at = element(
    "time",
    "",
    item.uploadedAt.replace("T", " ").slice(5, 16),
  );
  at.dateTime = `${item.uploadedAt}+09:00`;
  author.append(element("strong", "", item.nickname || "이름 없는 회원"), at);
  const suffix =
    item.type === "EXERCISE" ? ` · ${item.exerciseDurationMinutes ?? 0}분` : "";
  body.append(
    author,
    element(
      "p",
      "card-title",
      `${item.title || typeLabels[item.type]}${suffix}`,
    ),
    element("div", "card-id", `ID ${item.id}`),
  );
  button.append(photo, body);
  button.addEventListener("click", () => detail(item));
  return button;
}

function reload() {
  if (!form.reportValidity()) return;
  if (form.elements.from.value > form.elements.to.value) {
    message("#filter-state", "시작일은 종료일보다 늦을 수 없어요.", true);
    return;
  }
  pageAbort?.abort();
  summaryAbort?.abort();
  generation += 1;
  loaded = 0;
  total = null;
  summaryFailed = false;
  nextCursor = null;
  filters = new URLSearchParams();
  for (const [key, value] of new FormData(form))
    if (value.trim()) filters.set(key, value.trim());
  if (selectedType) filters.set("type", selectedType);
  $("#feed").replaceChildren();
  renderCount();
  for (const id of [
    "total-count",
    "meal-count",
    "exercise-count",
    "challenge-count",
  ])
    $("#" + id).textContent = "—";
  message(
    "#filter-state",
    `${filters.get("from")} ~ ${filters.get("to")} · 한국시간 기준`,
  );
  loading = false;
  loadSummary(generation);
  loadPage(generation);
}

function renderCount() {
  $("#loaded-count").textContent =
    total == null
      ? `${loaded}건 표시 · ${summaryFailed ? "전체 집계 확인 필요" : "전체 집계 중"}`
      : `전체 ${total.toLocaleString()}건 · ${loaded.toLocaleString()}건 표시`;
}

async function loadSummary(version) {
  summaryAbort = new AbortController();
  try {
    const summary = await api(`/api/v1/admin/feed/summary?${filters}`, {
      signal: summaryAbort.signal,
    });
    if (version !== generation) return;
    total = summary.totalCount;
    for (const [id, value] of [
      ["total", total],
      ["meal", summary.mealCount],
      ["exercise", summary.exerciseCount],
      ["challenge", summary.challengeCount],
    ]) {
      $("#" + id + "-count").textContent = value.toLocaleString();
    }
    renderCount();
  } catch (error) {
    if (error.name === "AbortError" || version !== generation) return;
    summaryFailed = true;
    renderCount();
    if (error.status === 401 || error.status === 403) {
      admin = null;
      await showLogin();
      return;
    }
    message(
      "#filter-state",
      "목록은 볼 수 있지만 전체 건수를 가져오지 못했어요. 새로고침해주세요.",
      true,
    );
  }
}

async function loadPage(version) {
  if (loading) return;
  loading = true;
  $("#more").hidden = true;
  $("#feed").setAttribute("aria-busy", "true");
  message("#feed-message", "인증을 불러오고 있어요.");
  const params = new URLSearchParams(filters);
  if (nextCursor) params.set("cursor", nextCursor);
  pageAbort = new AbortController();
  try {
    const page = await api(`/api/v1/admin/feed?${params}`, {
      signal: pageAbort.signal,
    });
    if (version !== generation) return;
    page.items.forEach((item) => $("#feed").append(card(item)));
    loaded += page.items.length;
    nextCursor = page.nextCursor;
    renderCount();
    $("#more").hidden = !page.hasMore;
    message(
      "#feed-message",
      loaded ? "" : "이 조건에 해당하는 인증이 아직 없어요.",
    );
  } catch (error) {
    if (error.name === "AbortError") return;
    if (version !== generation) return;
    if (error.status === 401 || error.status === 403) {
      admin = null;
      await showLogin();
    } else {
      message(
        "#feed-message",
        error.status === 400
          ? "기간(최대 366일)과 필터 값을 확인해주세요."
          : error.message,
        true,
      );
      $("#more").hidden = loaded === 0;
    }
  } finally {
    if (version === generation) {
      loading = false;
      $("#feed").setAttribute("aria-busy", "false");
    }
  }
}

async function detail(item) {
  try {
    const result = await api(`/api/v1/admin/feed/${item.type}/${item.id}`);
    const data = result.item;
    $("#detail-title").textContent = `${typeLabels[data.type]} 인증 상세`;
    $("#detail-image").src = safeImage(data.imageUrl);
    $("#original-link").href = safeImage(data.imageUrl);
    const info = $("#detail-info");
    info.replaceChildren(element("h3", "", data.title || "인증 정보"));
    const list = element("dl");
    const pairs = [
      ["작성자", `${data.nickname} · 회원 ID ${data.memberId}`],
      [
        "인증 일시",
        `${data.uploadedAt.replace("T", " ").slice(0, 19)} (한국시간)`,
      ],
      ["기록 ID", data.id],
      [
        "공유 그룹",
        result.groups
          .map((group) => `${group.name} (#${group.id})`)
          .join("\n") || "연결된 그룹 없음",
      ],
      ["식사 시각", data.mealTime],
      [
        "운동 시간",
        data.exerciseDurationMinutes
          ? `${data.exerciseDurationMinutes}분`
          : null,
      ],
      ["인증 안내", data.description],
      ["그룹 챌린지", data.groupChallengeId],
      ["전역 챌린지", data.globalWeeklyChallengeId],
      [
        "이미지 영역",
        data.crop?.width
          ? `x=${data.crop.x}, y=${data.crop.y}\n너비=${data.crop.width}, 높이=${data.crop.height}`
          : "원본 전체",
      ],
    ];
    for (const [key, value] of pairs)
      if (value != null) {
        const row = element("div");
        row.append(element("dt", "", key), element("dd", "", value));
        list.append(row);
      }
    info.append(list);
    if (!$("#detail").open) $("#detail").showModal();
  } catch (error) {
    message("#feed-message", error.message, true);
  }
}

form.addEventListener("submit", (event) => {
  event.preventDefault();
  reload();
});
form.addEventListener("input", () =>
  message("#filter-state", "필터가 변경됐어요. 조회를 눌러 적용해주세요."),
);
for (const name of ["from", "to"])
  form.elements[name].addEventListener("change", () => {
    if (form.elements.from.value && form.elements.to.value) reload();
  });
document.querySelectorAll("[data-days]").forEach((button) =>
  button.addEventListener("click", () => {
    form.elements.from.value = kstDate(1 - Number(button.dataset.days));
    form.elements.to.value = kstDate();
    reload();
  }),
);
document.querySelectorAll("[data-type]").forEach((button) =>
  button.addEventListener("click", () => {
    selectedType = button.dataset.type;
    document.querySelectorAll("[data-type]").forEach((tab) => {
      const active = tab === button;
      tab.classList.toggle("active", active);
      tab.setAttribute("aria-pressed", String(active));
    });
    reload();
  }),
);
$("#refresh").addEventListener("click", reload);
$("#more").addEventListener("click", () => loadPage(generation));
$("#close-detail").addEventListener("click", () => $("#detail").close());
$("#logout").addEventListener("click", async () => {
  try {
    await api("/api/v1/admin/auth/logout", { method: "POST" });
    location.assign("/admin");
  } catch (error) {
    message("#feed-message", error.message, true);
  }
});

async function init() {
  form.elements.from.value = kstDate(-6);
  form.elements.to.value = kstDate();
  const initial = new URLSearchParams(location.search);
  for (const name of [
    "from",
    "to",
    "memberId",
    "groupId",
    "groupChallengeId",
    "globalWeeklyChallengeId",
    "keyword",
  ]) {
    if (initial.has(name)) form.elements[name].value = initial.get(name);
  }
  if (["MEAL", "EXERCISE", "CHALLENGE"].includes(initial.get("type"))) {
    selectedType = initial.get("type");
    document.querySelectorAll("[data-type]").forEach((tab) => {
      const active = tab.dataset.type === selectedType;
      tab.classList.toggle("active", active);
      tab.setAttribute("aria-pressed", String(active));
    });
  }
  config = await api("/api/v1/admin/auth/config");
  const env =
    config.environment === "prod"
      ? "운영"
      : config.environment === "dev"
        ? "개발"
        : "로컬";
  $("#environment").textContent = env;
  $("#environment").classList.toggle("prod", config.environment === "prod");
  $("#sidebar-environment").textContent = `${env} 환경`;
  try {
    admin = await api("/api/v1/admin/auth/me");
    showWorkspace();
  } catch (error) {
    if ([401, 403].includes(error.status)) await showLogin();
    else throw error;
  }
}

init().catch((error) => {
  $("#login").hidden = false;
  message("#login-message", error.message, true);
});
