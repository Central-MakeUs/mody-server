"use strict";

const $ = (selector) => document.querySelector(selector);
const weeklyForm = $("#weekly-filters"),
  recordFilters = $("#record-filters");
const phaseLabels = { ACTIVE: "진행 중", UPCOMING: "예정", ENDED: "기간 종료" };
let admin,
  groups = [],
  groupId = "",
  currentView = "weekly",
  phase = "ALL",
  writeBusy = false;
let weeklyItems = [],
  weeklyCursor = null,
  weeklyParams,
  weeklyVersion = 0,
  weeklyBusy = false,
  weeklyAbort;
let recordItems = [],
  recordCursor = null,
  recordParams,
  recordVersion = 0,
  recordBusy = false,
  recordAbort;
let recordTotal = null,
  groupVersion = 0,
  groupAbort,
  editingChallenge,
  editingRecord,
  confirmOperation;

function node(tag, className, text) {
  const element = document.createElement(tag);
  if (className) element.className = className;
  if (text !== undefined) element.textContent = text;
  return element;
}

function message(selector, text = "", error = false) {
  const element = $(selector);
  element.textContent = text;
  element.classList.toggle("error", error);
}

function canWrite() {
  return ["ADMIN", "OPERATOR"].includes(admin?.role);
}

function writeState() {
  document.querySelectorAll("[data-write]").forEach((button) => {
    button.disabled =
      !canWrite() ||
      writeBusy ||
      (button.id === "new-group-weekly" && !weeklyForm.elements.groupId.value);
    button.title = !canWrite() ? "조회 전용 계정은 변경할 수 없어요." : "";
  });
  $("#viewer-note").hidden = canWrite();
}

function showLogin() {
  $("#workspace").hidden = true;
  $("#login").hidden = false;
  document.querySelectorAll("dialog[open]").forEach((dialog) => dialog.close());
}

async function request(path, options = {}) {
  const method = options.method ?? "GET";
  if (method !== "GET" && path !== "/api/v1/admin/auth/logout" && !canWrite()) {
    throw new Error("조회 전용 계정은 데이터를 변경할 수 없어요.");
  }
  const response = await fetch(path, {
    credentials: "same-origin",
    ...options,
    headers: { "X-CSRF-Token": admin?.csrfToken ?? "", ...options.headers },
  });
  const body = await response.json().catch(() => null);
  if (!response.ok || !body?.isSuccess) {
    if (
      response.status === 401 ||
      (response.status === 403 && method === "GET")
    )
      showLogin();
    const error = new Error(
      body?.message ?? "요청을 처리하지 못했어요. 잠시 후 다시 시도해주세요.",
    );
    error.status = response.status;
    throw error;
  }
  return body.result;
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

function today() {
  return new Intl.DateTimeFormat("sv-SE", { timeZone: "Asia/Seoul" }).format(
    new Date(),
  );
}
function shift(date, days) {
  return new Intl.DateTimeFormat("sv-SE", { timeZone: "Asia/Seoul" }).format(
    new Date(new Date(`${date}T12:00:00+09:00`).getTime() + days * 86400000),
  );
}
function dateTime(value) {
  return value ? value.replace("T", " ").slice(0, 16) : "—";
}
function count(value) {
  return Number(value).toLocaleString();
}

function setWeek(value) {
  const at = today();
  if (value === "all") {
    weeklyForm.elements.from.value = shift(at, -180);
    weeklyForm.elements.to.value = shift(at, 30);
  } else {
    const monday = shift(
      at,
      -((new Date(`${at}T00:00:00Z`).getUTCDay() + 6) % 7) + Number(value) * 7,
    );
    weeklyForm.elements.from.value = monday;
    weeklyForm.elements.to.value = shift(monday, 6);
  }
}

function validDates(form, selector) {
  if (!form.reportValidity()) return false;
  if (form.elements.from.value > form.elements.to.value) {
    message(selector, "시작일은 종료일보다 늦을 수 없어요.", true);
    return false;
  }
  return true;
}

function params(form) {
  const result = new URLSearchParams();
  for (const [key, value] of new FormData(form))
    if (value.trim()) result.set(key, value.trim());
  return result;
}

function feedLink(filters) {
  const query = new URLSearchParams(
    Object.entries(filters).filter(
      ([, value]) => value != null && value !== "",
    ),
  );
  return `/admin?${query}`;
}

function action(label, handler, write = false, danger = false) {
  const button = node("button", `row-action${danger ? " danger" : ""}`, label);
  button.type = "button";
  if (write) button.dataset.write = "";
  button.addEventListener("click", () =>
    Promise.resolve()
      .then(handler)
      .catch((error) => message("#workspace-message", error.message, true)),
  );
  return button;
}

function link(label, href, className = "row-action") {
  const anchor = node("a", className, label);
  anchor.href = href;
  return anchor;
}

function setView(view) {
  currentView = view === "groups" ? "groups" : "weekly";
  const weekly = currentView === "weekly";
  $("#weekly-view").hidden = !weekly;
  $("#groups-view").hidden = weekly;
  $("#page-title").textContent = weekly ? "주간 챌린지" : "그룹 관리";
  $("#breadcrumb-view").textContent = weekly ? "주간 챌린지" : "그룹 관리";
  $("#view-kicker").textContent = weekly
    ? "WEEKLY CHALLENGES"
    : "GROUP MANAGEMENT";
  $("#page-description").textContent = weekly
    ? "이번 주 인증 현황부터 다음 주 준비까지 한곳에서 관리해요."
    : "그룹 구성원과 인증 기록을 확인하고 필요한 운영 작업을 이어가요.";
  document.querySelectorAll("[data-view]").forEach((button) => {
    const active = button.dataset.view === currentView;
    button.classList.toggle("selected", active);
    if (active) button.setAttribute("aria-current", "page");
    else button.removeAttribute("aria-current");
  });
  const url = new URL(location.href);
  url.searchParams.set("view", currentView);
  if (groupId) url.searchParams.set("groupId", groupId);
  else url.searchParams.delete("groupId");
  history.replaceState(null, "", url);
}

function weeklyRow(item) {
  const row = node("article", "weekly-row"),
    body = node("div"),
    badges = node("div", "row-badges");
  badges.append(
    node("span", `pill ${item.phase.toLowerCase()}`, phaseLabels[item.phase]),
  );
  badges.append(
    node(
      "span",
      `pill${item.globalWeeklyChallengeId ? " global" : ""}`,
      item.kind === "GLOBAL"
        ? "전역 챌린지"
        : item.globalWeeklyChallengeId
          ? "전역 연동"
          : "그룹 개별",
    ),
  );
  if (item.groupName) badges.append(node("span", "pill", item.groupName));
  body.append(
    badges,
    node("h3", "", item.title),
    node("p", "", item.description),
    node("div", "row-period", `${item.startsOn} ~ ${item.endsOn}`),
  );
  const stats = node("div", "row-stats");
  for (const [label, value] of [
    ["활성 연결 그룹", item.linkedGroupCount],
    ["인증 건수", item.proofCount],
    ["참여 회원", item.participantCount],
  ]) {
    const metric = node("div");
    metric.append(node("span", "", label), node("strong", "", count(value)));
    stats.append(metric);
  }
  const footer = node("div", "row-footer"),
    actions = node("div", "row-actions");
  const filter = { from: item.startsOn, to: item.endsOn, type: "CHALLENGE" };
  if (item.kind === "GLOBAL") filter.globalWeeklyChallengeId = item.id;
  else filter.groupChallengeId = item.id;
  actions.append(link("인증 보기", feedLink(filter)));
  actions.append(
    action(
      item.kind === "GROUP" && item.globalWeeklyChallengeId
        ? "전역에서 관리"
        : "수정",
      () => openChallenge(item),
      true,
    ),
  );
  if (item.kind === "GLOBAL")
    actions.append(action("그룹 동기화", () => confirmSync(item), true));
  if (item.kind === "GLOBAL" || !item.globalWeeklyChallengeId)
    actions.append(
      action("삭제", () => confirmChallengeDelete(item), true, true),
    );
  footer.append(
    node(
      "small",
      "",
      `${item.participatingGroupCount}개 그룹에서 인증 · ID ${item.id}`,
    ),
    actions,
  );
  row.append(body, stats, footer);
  return row;
}

async function loadWeekly(reset = true) {
  if (reset) {
    if (!validDates(weeklyForm, "#weekly-filter-state")) return;
    weeklyAbort?.abort();
    weeklyVersion += 1;
    weeklyItems = [];
    weeklyCursor = null;
    weeklyBusy = false;
    weeklyParams = params(weeklyForm);
    weeklyParams.set("phase", phase);
    $("#weekly-list").replaceChildren();
    $("#weekly-loaded").textContent = "집계 중";
    for (const name of ["total", "active", "upcoming", "ended"])
      $("#weekly-" + name).textContent = "—";
    message(
      "#weekly-filter-state",
      `${weeklyParams.get("from")} ~ ${weeklyParams.get("to")} · 한국시간 기준`,
    );
  }
  if (weeklyBusy) return;
  weeklyBusy = true;
  weeklyAbort = new AbortController();
  const version = weeklyVersion,
    query = new URLSearchParams(weeklyParams);
  if (weeklyCursor) query.set("cursor", weeklyCursor);
  $("#weekly-list").setAttribute("aria-busy", "true");
  $("#weekly-more").hidden = true;
  message("#weekly-message", "챌린지와 인증 현황을 불러오고 있어요.");
  try {
    const page = await request(`/api/v1/admin/challenge-overview?${query}`, {
      signal: weeklyAbort.signal,
    });
    if (version !== weeklyVersion) return;
    weeklyItems.push(...page.items);
    weeklyCursor = page.nextCursor;
    page.items.forEach((item) => $("#weekly-list").append(weeklyRow(item)));
    for (const name of ["total", "active", "upcoming", "ended"])
      $("#weekly-" + name).textContent = count(page.summary[name + "Count"]);
    $("#weekly-loaded").textContent =
      `전체 ${count(page.totalCount)}개 · ${count(weeklyItems.length)}개 표시`;
    $("#weekly-more").hidden = !page.hasMore;
    message(
      "#weekly-message",
      weeklyItems.length ? "" : "이 조건에 해당하는 챌린지가 없어요.",
    );
    writeState();
  } catch (error) {
    if (error.name === "AbortError" || version !== weeklyVersion) return;
    message("#weekly-message", error.message, true);
    $("#weekly-loaded").textContent =
      `${count(weeklyItems.length)}개 표시 · 조회 확인 필요`;
    if (weeklyCursor) $("#weekly-more").hidden = false;
  } finally {
    if (version === weeklyVersion) {
      weeklyBusy = false;
      $("#weekly-list").setAttribute("aria-busy", "false");
    }
  }
}

function renderGroups() {
  const search = $("#group-search").value.trim().toLowerCase();
  const filtered = groups.filter((group) =>
    `${group.name} ${group.groupId}`.toLowerCase().includes(search),
  );
  $("#group-list").replaceChildren();
  $("#group-count").textContent = `${filtered.length} / ${groups.length}`;
  filtered.forEach((group) => {
    const button = node(
      "button",
      `group-row${String(group.groupId) === groupId ? " selected" : ""}`,
    );
    button.type = "button";
    button.dataset.groupId = group.groupId;
    const title = node("div");
    title.append(
      node("strong", "", group.name),
      node("small", "", `ID ${group.groupId}`),
    );
    button.append(title, node("span", "", `${group.memberCount}명`));
    button.addEventListener("click", () => selectGroup(String(group.groupId)));
    $("#group-list").append(button);
  });
  if (!filtered.length)
    $("#group-list").append(node("p", "empty-state", "검색 결과가 없어요."));
}

async function loadGroups() {
  const result = await request("/api/v1/admin/groups");
  groups = result.groups;
  const selected = weeklyForm.elements.groupId.value;
  $("#weekly-group").replaceChildren(new Option("전체 그룹 · 전역 원본", ""));
  groups.forEach((group) =>
    $("#weekly-group").add(
      new Option(`${group.name} · ${group.memberCount}명`, group.groupId),
    ),
  );
  $("#weekly-group").value = groups.some(
    (group) => String(group.groupId) === selected,
  )
    ? selected
    : "";
  renderGroups();
  writeState();
}

function renderGroup(detail) {
  const root = $("#group-detail"),
    heading = node("div", "section-heading"),
    title = node("div");
  title.append(
    node("h2", "", detail.name),
    node("p", "", "이 그룹의 구성원과 인증 기록을 확인하세요."),
  );
  const weeklyButton = node("button", "secondary-button", "그룹 챌린지 보기");
  weeklyButton.type = "button";
  weeklyButton.addEventListener("click", () => {
    weeklyForm.elements.groupId.value = detail.groupId;
    setView("weekly");
    loadWeekly();
    writeState();
  });
  heading.append(title, weeklyButton);
  const identifiers = node("div", "group-identifiers");
  for (const [label, value] of [
    ["그룹 ID", detail.groupId],
    ["초대 코드", detail.code],
  ]) {
    const part = node("span", "", label);
    part.append(node("strong", "", String(value)));
    identifiers.append(part);
  }
  const rename = node("form", "group-name-form");
  rename.id = "group-name-form";
  const label = node("label", "", "그룹명"),
    input = node("input");
  input.name = "name";
  input.value = detail.name;
  input.maxLength = 30;
  input.required = true;
  label.append(input);
  const save = node("button", "primary-button", "그룹명 저장");
  save.type = "submit";
  save.dataset.write = "";
  rename.append(label, save);
  rename.addEventListener("submit", (event) => {
    event.preventDefault();
    mutate(async () => {
      const saved = await request(`/api/v1/admin/groups/${detail.groupId}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ name: input.value.trim() }),
      });
      const group = groups.find(
        (item) => String(item.groupId) === String(detail.groupId),
      );
      if (group) group.name = saved.name;
      await loadGroups();
      if (groupId === String(detail.groupId)) await selectGroup(groupId);
      message("#workspace-message", "그룹명을 저장했어요.");
    });
  });
  const memberHeading = node("div", "member-heading");
  memberHeading.append(node("h3", "", `참여 멤버 ${detail.members.length}명`));
  const members = node("div", "member-list");
  detail.members.forEach((member) => {
    const row = node("div", "member-row"),
      info = node("div");
    const nickname = member.nickname || "이름 없는 회원";
    info.append(
      node("strong", "", nickname),
      node("small", "", `ID ${member.memberId}`),
      node("small", "", `${dateTime(member.joinedAt)} 참여`),
    );
    row.append(
      node("span", "member-avatar", nickname.slice(0, 1)),
      info,
      link(
        "인증 보기",
        feedLink({
          memberId: member.memberId,
          groupId: detail.groupId,
          from: shift(today(), -29),
          to: today(),
        }),
        "member-feed",
      ),
    );
    members.append(row);
  });
  if (!detail.members.length)
    members.append(node("p", "empty-state", "현재 참여 중인 멤버가 없어요."));
  root.replaceChildren(heading, identifiers, rename, memberHeading, members);
  $("#group-feed-link").href = feedLink({
    groupId: detail.groupId,
    from: recordFilters.elements.from.value,
    to: recordFilters.elements.to.value,
  });
  $("#records-panel").hidden = false;
  writeState();
}

async function selectGroup(id) {
  groupAbort?.abort();
  recordAbort?.abort();
  recordVersion += 1;
  const version = ++groupVersion;
  groupId = id;
  groupAbort = new AbortController();
  renderGroups();
  setView("groups");
  $("#records-panel").hidden = true;
  $("#group-detail").replaceChildren(
    node("p", "empty-state", "그룹 정보를 불러오고 있어요."),
  );
  try {
    const detail = await request(`/api/v1/admin/groups/${id}`, {
      signal: groupAbort.signal,
    });
    if (version !== groupVersion) return;
    renderGroup(detail);
    loadRecords();
  } catch (error) {
    if (error.name === "AbortError" || version !== groupVersion) return;
    $("#group-detail").replaceChildren(node("p", "empty-state", error.message));
  }
}

function recordRow(item) {
  const row = node("article", "record-row"),
    photo = node("button", "record-thumbnail");
  photo.type = "button";
  photo.setAttribute("aria-label", "인증 원본 보기");
  const url = safeImage(item.imageUrl);
  if (url) {
    const image = node("img");
    image.src = url;
    image.alt = `${item.nickname} 인증`;
    image.loading = "lazy";
    photo.append(image);
  }
  photo.addEventListener("click", () => {
    $("#record-image").src = url;
    $("#image-original-link").href = url;
    $("#image-dialog").showModal();
  });
  const body = node("div", "record-body"),
    author = node("div");
  author.append(
    node("span", "pill", item.type === "MEAL" ? "식사" : "운동"),
    node("strong", "", item.nickname),
  );
  body.append(
    author,
    node(
      "p",
      "",
      `${item.title || "내용 없음"}${item.type === "EXERCISE" ? ` · ${item.exerciseDurationMinutes ?? 0}분` : ""}`,
    ),
    node("small", "", `${dateTime(item.uploadedAt)} · ID ${item.id}`),
  );
  const actions = node("div", "row-actions");
  if (item.groupRecordEditable) {
    actions.append(
      action("수정", () => openRecord(item), true),
      action("삭제", () => confirmRecordDelete(item), true, true),
    );
  } else {
    actions.append(node("small", "", "이전 기록 · 조회만 가능"));
  }
  row.append(photo, body, actions);
  return row;
}

async function loadRecords(reset = true) {
  if (!groupId) return;
  if (reset) {
    if (!validDates(recordFilters, "#records-message")) return;
    recordAbort?.abort();
    recordVersion += 1;
    recordItems = [];
    recordCursor = null;
    recordBusy = false;
    recordTotal = null;
    recordParams = params(recordFilters);
    recordParams.set("groupId", groupId);
    $("#record-list").replaceChildren();
    $("#records-count").textContent = "집계 중";
    $("#group-feed-link").href = feedLink({
      groupId,
      from: recordParams.get("from"),
      to: recordParams.get("to"),
    });
  }
  if (recordBusy) return;
  recordBusy = true;
  recordAbort = new AbortController();
  const version = recordVersion,
    query = new URLSearchParams(recordParams),
    signal = recordAbort.signal;
  if (recordCursor) query.set("cursor", recordCursor);
  $("#record-list").setAttribute("aria-busy", "true");
  $("#records-more").hidden = true;
  message("#records-message", "활동 기록을 불러오고 있어요.");
  try {
    const [page, summary] = await Promise.all([
      request(`/api/v1/admin/feed?${query}`, { signal }),
      reset
        ? request(`/api/v1/admin/feed/summary?${recordParams}`, { signal })
        : Promise.resolve(null),
    ]);
    if (version !== recordVersion) return;
    if (summary) recordTotal = summary.totalCount;
    recordItems.push(...page.items);
    recordCursor = page.nextCursor;
    page.items.forEach((item) => $("#record-list").append(recordRow(item)));
    $("#records-count").textContent =
      `전체 ${count(recordTotal)}건 · ${count(recordItems.length)}건 표시`;
    $("#records-more").hidden = !page.hasMore;
    message(
      "#records-message",
      recordItems.length ? "" : "이 기간에 해당하는 활동 기록이 없어요.",
    );
    writeState();
  } catch (error) {
    if (error.name === "AbortError" || version !== recordVersion) return;
    message("#records-message", error.message, true);
    $("#records-count").textContent = "조회 확인 필요";
    if (recordCursor) $("#records-more").hidden = false;
  } finally {
    if (version === recordVersion) {
      recordBusy = false;
      $("#record-list").setAttribute("aria-busy", "false");
    }
  }
}

async function openChallenge(item = null, kind = "GLOBAL") {
  if (!canWrite() || writeBusy) return;
  if (item?.kind === "GROUP" && item.globalWeeklyChallengeId) {
    const result = await request("/api/v1/admin/weekly-challenges");
    const original = result.challenges.find(
      (row) =>
        String(row.globalWeeklyChallengeId) === item.globalWeeklyChallengeId,
    );
    if (!original)
      throw new Error("전역 챌린지 원본을 찾지 못했어요. 새로고침해주세요.");
    item = {
      ...original,
      kind: "GLOBAL",
      id: String(original.globalWeeklyChallengeId),
    };
  }
  if (!item && kind === "GROUP" && !weeklyForm.elements.groupId.value)
    throw new Error("먼저 등록할 그룹을 선택해주세요.");
  editingChallenge = {
    kind: item?.kind ?? kind,
    id: item?.id ?? null,
    groupId: item?.groupId ?? weeklyForm.elements.groupId.value,
    idempotencyKey: crypto.randomUUID(),
  };
  const form = $("#challenge-form");
  form.reset();
  form.elements.title.value = item?.title ?? "";
  form.elements.description.value = item?.description ?? "";
  form.elements.startsOn.value =
    item?.startsOn ?? weeklyForm.elements.from.value;
  form.elements.endsOn.value = item?.endsOn ?? weeklyForm.elements.to.value;
  $("#challenge-dialog-title").textContent =
    `${editingChallenge.kind === "GLOBAL" ? "전역" : "그룹"} 챌린지 ${item ? "수정" : "등록"}`;
  $("#challenge-impact").textContent =
    editingChallenge.kind === "GLOBAL"
      ? item
        ? "전역 원본을 수정하면 연결된 모든 그룹의 챌린지 제목·안내·기간도 함께 변경돼요."
        : "등록하면 모든 활성 그룹에 챌린지가 생성돼요. 선택한 그룹에만 등록하려면 그룹 챌린지를 사용하세요."
      : "선택한 그룹의 개별 챌린지만 변경돼요.";
  message("#challenge-form-message");
  writeState();
  $("#challenge-dialog").showModal();
}

function openRecord(item) {
  if (!canWrite() || writeBusy) return;
  editingRecord = { ...item, groupId };
  const form = $("#record-form");
  form.reset();
  form.elements.recordType.value = item.type;
  form.elements.mealTime.value = item.mealTime?.slice(0, 5) ?? "";
  form.elements.menu.value = item.type === "MEAL" ? (item.title ?? "") : "";
  form.elements.exerciseName.value =
    item.type === "EXERCISE" ? (item.title ?? "") : "";
  form.elements.exerciseDurationMinutes.value =
    item.exerciseDurationMinutes ?? "";
  syncRecordFields();
  message("#record-form-message");
  writeState();
  $("#record-dialog").showModal();
}

function syncRecordFields() {
  const form = $("#record-form"),
    meal = form.elements.recordType.value === "MEAL";
  $("#meal-fields").hidden = !meal;
  $("#exercise-fields").hidden = meal;
  for (const name of ["mealTime", "menu"]) {
    form.elements[name].disabled = !meal;
    form.elements[name].required = meal;
  }
  for (const name of ["exerciseName", "exerciseDurationMinutes"]) {
    form.elements[name].disabled = meal;
    form.elements[name].required = !meal;
  }
}

function confirm(title, description, label, operation) {
  if (!canWrite() || writeBusy) return;
  confirmOperation = operation;
  $("#confirm-title").textContent = title;
  $("#confirm-description").textContent = description;
  $("#confirm-action").textContent = label;
  $("#confirm-action").className =
    label === "삭제" ? "danger-button" : "primary-button";
  message("#confirm-message");
  writeState();
  $("#confirm-dialog").showModal();
}

function confirmSync(item) {
  confirm(
    "그룹 연결 동기화",
    `「${item.title}」에 아직 연결되지 않은 활성 그룹을 추가할까요? 이미 연결된 그룹은 중복 생성하지 않아요.`,
    "동기화",
    async () => {
      const result = await request(
        `/api/v1/admin/weekly-challenges/${item.id}/sync-groups`,
        { method: "POST" },
      );
      await loadWeekly();
      message(
        "#workspace-message",
        `${result.linkedGroupCount}개 그룹을 새로 연결했어요.`,
      );
    },
  );
}

function confirmChallengeDelete(item) {
  const path =
    item.kind === "GLOBAL"
      ? `/api/v1/admin/weekly-challenges/${item.id}`
      : `/api/v1/admin/groups/${item.groupId}/weekly-challenges/${item.id}`;
  const impact =
    item.kind === "GLOBAL"
      ? "연결된 모든 그룹에서 챌린지가 사라지고, 관련 인증도 관리자 피드에서 보이지 않게 돼요."
      : "이 그룹에서 챌린지가 사라지고, 관련 인증도 관리자 피드에서 보이지 않게 돼요.";
  confirm(
    "챌린지 삭제",
    `「${item.title}」을 삭제할까요?\n${impact}\n관리 화면에는 복구 기능이 없어요.`,
    "삭제",
    async () => {
      await request(path, { method: "DELETE" });
      await loadWeekly();
      message("#workspace-message", "챌린지를 삭제했어요.");
    },
  );
}

function confirmRecordDelete(item) {
  const targetGroup = groupId;
  confirm(
    "활동 기록 삭제",
    `「${item.title || "활동 기록"}」을 삭제할까요?\n공유된 다른 그룹에서도 원본 기록과 댓글이 함께 삭제돼요. 관리 화면에는 복구 기능이 없어요.`,
    "삭제",
    async () => {
      await request(`/api/v1/admin/groups/${targetGroup}/records/${item.id}`, {
        method: "DELETE",
      });
      if (groupId === targetGroup) await loadRecords();
      message("#workspace-message", "활동 기록을 삭제했어요.");
    },
  );
}

async function mutate(operation, selector = "#workspace-message") {
  if (!canWrite() || writeBusy) return;
  writeBusy = true;
  writeState();
  message(selector);
  try {
    await operation();
  } catch (error) {
    message(selector, error.message, true);
  } finally {
    writeBusy = false;
    writeState();
  }
}

document
  .querySelectorAll("[data-view]")
  .forEach((button) =>
    button.addEventListener("click", () => setView(button.dataset.view)),
  );
document
  .querySelectorAll("[data-close]")
  .forEach((button) =>
    button.addEventListener("click", () =>
      $("#" + button.dataset.close).close(),
    ),
  );
document.querySelectorAll("[data-week]").forEach((button) =>
  button.addEventListener("click", () => {
    setWeek(button.dataset.week);
    loadWeekly();
  }),
);
document.querySelectorAll("[data-phase]").forEach((button) =>
  button.addEventListener("click", () => {
    phase = button.dataset.phase;
    document.querySelectorAll("[data-phase]").forEach((tab) => {
      tab.classList.toggle("active", tab === button);
      tab.setAttribute("aria-pressed", String(tab === button));
    });
    loadWeekly();
  }),
);
weeklyForm.addEventListener("submit", (event) => {
  event.preventDefault();
  loadWeekly();
});
weeklyForm.elements.keyword.addEventListener("input", () =>
  message(
    "#weekly-filter-state",
    "검색어가 변경됐어요. 조회를 눌러 적용해주세요.",
  ),
);
for (const name of ["from", "to", "groupId"])
  weeklyForm.elements[name].addEventListener("change", () => {
    loadWeekly();
    writeState();
  });
$("#weekly-more").addEventListener("click", () => loadWeekly(false));
$("#group-search").addEventListener("input", renderGroups);
$("#records-more").addEventListener("click", () => loadRecords(false));
recordFilters.addEventListener("submit", (event) => {
  event.preventDefault();
  loadRecords();
});
recordFilters.elements.keyword.addEventListener("input", () =>
  message("#records-message", "검색어가 변경됐어요. 조회를 눌러 적용해주세요."),
);
for (const name of ["from", "to", "type"])
  recordFilters.elements[name].addEventListener("change", () => loadRecords());
$("#new-global-weekly").addEventListener("click", () =>
  openChallenge().catch((error) =>
    message("#workspace-message", error.message, true),
  ),
);
$("#new-group-weekly").addEventListener("click", () =>
  openChallenge(null, "GROUP").catch((error) =>
    message("#workspace-message", error.message, true),
  ),
);
$("#record-form").elements.recordType.addEventListener(
  "change",
  syncRecordFields,
);
$("#challenge-form").addEventListener("submit", (event) => {
  event.preventDefault();
  const form = event.currentTarget;
  if (form.elements.startsOn.value > form.elements.endsOn.value)
    return message(
      "#challenge-form-message",
      "마감일은 시작일 이후여야 해요.",
      true,
    );
  const target = { ...editingChallenge },
    body = Object.fromEntries(new FormData(form));
  const path =
    target.kind === "GLOBAL"
      ? `/api/v1/admin/weekly-challenges${target.id ? `/${target.id}` : ""}`
      : `/api/v1/admin/groups/${target.groupId}/weekly-challenges${target.id ? `/${target.id}` : ""}`;
  mutate(async () => {
    const headers = { "Content-Type": "application/json" };
    if (!target.id && target.kind === "GLOBAL")
      headers["Idempotency-Key"] = target.idempotencyKey;
    await request(path, {
      method: target.id ? "PUT" : "POST",
      headers,
      body: JSON.stringify(body),
    });
    $("#challenge-dialog").close();
    await loadWeekly();
    message(
      "#workspace-message",
      "챌린지를 저장했어요. 선택한 조회 기간 밖이면 기간을 변경해 확인해주세요.",
    );
  }, "#challenge-form-message");
});
$("#record-form").addEventListener("submit", (event) => {
  event.preventDefault();
  const form = event.currentTarget,
    target = { ...editingRecord },
    meal = form.elements.recordType.value === "MEAL";
  const body = {
    recordType: form.elements.recordType.value,
    mealTime: meal ? form.elements.mealTime.value : null,
    menu: meal ? form.elements.menu.value : null,
    exerciseName: meal ? null : form.elements.exerciseName.value,
    exerciseDurationMinutes: meal
      ? null
      : Number(form.elements.exerciseDurationMinutes.value),
  };
  mutate(async () => {
    await request(
      `/api/v1/admin/groups/${target.groupId}/records/${target.id}`,
      {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
      },
    );
    $("#record-dialog").close();
    if (groupId === target.groupId) await loadRecords();
    message("#workspace-message", "활동 기록을 저장했어요.");
  }, "#record-form-message");
});
$("#confirm-action").addEventListener("click", () => {
  const operation = confirmOperation;
  if (!operation) return;
  mutate(async () => {
    await operation();
    $("#confirm-dialog").close();
    confirmOperation = null;
  }, "#confirm-message");
});
$("#refresh").addEventListener("click", () =>
  loadGroups()
    .then(() =>
      currentView === "weekly"
        ? loadWeekly()
        : groupId
          ? selectGroup(groupId)
          : null,
    )
    .catch((error) => message("#workspace-message", error.message, true)),
);
$("#logout").addEventListener("click", () =>
  request("/api/v1/admin/auth/logout", { method: "POST" })
    .then(() => location.assign("/admin"))
    .catch((error) => message("#workspace-message", error.message, true)),
);

async function init() {
  setWeek("0");
  recordFilters.elements.from.value = shift(today(), -29);
  recordFilters.elements.to.value = today();
  const config = await request("/api/v1/admin/auth/config");
  admin = await request("/api/v1/admin/auth/me");
  const environment =
    config.environment === "prod"
      ? "운영"
      : config.environment === "dev"
        ? "개발"
        : "로컬";
  $("#environment").textContent = environment;
  $("#environment").classList.toggle("prod", config.environment === "prod");
  $("#sidebar-environment").textContent = `${environment} 환경`;
  $("#account-email").textContent = admin.email;
  $("#workspace").hidden = false;
  $("#login").hidden = true;
  await loadGroups();
  const initial = new URLSearchParams(location.search),
    id = initial.get("groupId");
  if (id && groups.some((group) => String(group.groupId) === id)) {
    groupId = id;
    weeklyForm.elements.groupId.value = id;
  }
  setView(initial.get("view"));
  await loadWeekly();
  if (currentView === "groups" && groupId) await selectGroup(groupId);
  writeState();
}

init().catch((error) => {
  if (!admin) {
    showLogin();
    message("#login-message", error.message, true);
  } else message("#workspace-message", error.message, true);
});
