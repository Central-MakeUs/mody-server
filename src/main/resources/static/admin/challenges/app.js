const ADMIN_KEY_STORAGE = "mody.admin.api-key";

const $ = (selector) => document.querySelector(selector);
const accessSection = $("#access-section");
const workspace = $("#workspace");
const accessForm = $("#access-form");
const accessMessage = $("#access-message");
const adminKeyInput = $("#admin-key");
const groupSelect = $("#group-id");
const workspaceMessage = $("#workspace-message");
const challengeDialog = $("#challenge-dialog");
const recordDialog = $("#record-dialog");
const confirmDialog = $("#confirm-dialog");

let groups = [];
let challenges = [];
let records = [];
let activeTab = "challenge";
let pendingDelete = null;

function adminKey() { return sessionStorage.getItem(ADMIN_KEY_STORAGE); }
function selectedGroupId() { return groupSelect.value; }
function setMessage(element, message = "", type = "") { element.textContent = message; element.className = `form-message ${type}`.trim(); }
function setLoading(button, loading, label) {
  button.disabled = loading;
  if (loading) { button.dataset.label = button.textContent; button.textContent = label; }
  else if (button.dataset.label) { button.textContent = button.dataset.label; }
}

async function request(path, options = {}) {
  const response = await fetch(path, { ...options, headers: { "X-Admin-Api-Key": adminKey(), ...(options.headers ?? {}) } });
  const payload = await response.json().catch(() => null);
  if (!response.ok || !payload?.isSuccess) { const error = new Error(payload?.message ?? "요청을 처리하지 못했습니다."); error.status = response.status; throw error; }
  return payload.result;
}

function showAccess(message = "") {
  workspace.hidden = true; accessSection.hidden = false; setMessage(accessMessage, message, message ? "error" : ""); adminKeyInput.focus();
}
function showWorkspace() { accessSection.hidden = true; workspace.hidden = false; }
function dateString(date) { return new Date(date.getTime() - date.getTimezoneOffset() * 60_000).toISOString().slice(0, 10); }
function setDefaultDates() { const today = new Date(); $("#starts-on").value = dateString(today); const end = new Date(today); end.setDate(end.getDate() + 6); $("#ends-on").value = dateString(end); }
function formatDateTime(value) { return value ? value.replace("T", " ").slice(0, 16) : "-"; }
function escapeHtml(value) { const node = document.createElement("span"); node.textContent = value ?? ""; return node.innerHTML; }

function renderGroups() {
  groupSelect.replaceChildren(new Option("대상 그룹을 선택하세요.", ""));
  groups.forEach((group) => groupSelect.add(new Option(`${group.name} · ${group.memberCount}명`, group.groupId)));
  groupSelect.disabled = groups.length === 0;
  $("#group-summary").textContent = groups.length ? `${groups.length}개 운영 그룹` : "운영 그룹 없음";
}

function challengeStatus(status) { return status === "COMPLETED" ? "완료" : status === "RESET" ? "종료" : "진행 중"; }
function renderChallenges() {
  const list = $("#challenge-list"); list.replaceChildren(); $("#challenge-count").textContent = challenges.length;
  challenges.forEach((challenge) => {
    const row = document.createElement("tr");
    row.innerHTML = `<td><strong>${escapeHtml(challenge.title)}</strong><small>${escapeHtml(challenge.description)}</small></td><td>${challenge.startsOn} ~ ${challenge.endsOn}</td><td><span class="status ${challenge.status === "COMPLETED" ? "completed" : ""}">${challengeStatus(challenge.status)}</span></td><td class="action-column"><div class="actions"><button class="table-button" data-action="edit-challenge" data-id="${challenge.groupChallengeId}" type="button">수정</button><button class="table-button delete" data-action="delete-challenge" data-id="${challenge.groupChallengeId}" type="button">삭제</button></div></td>`;
    list.append(row);
  });
  $("#challenge-empty").hidden = challenges.length !== 0;
}

function recordSummary(record) {
  return record.recordType === "MEAL" ? `${record.mealTime ?? ""} · ${record.menu ?? ""}` : `${record.exerciseName ?? ""} · ${record.exerciseDurationMinutes ?? 0}분`;
}
function renderRecords() {
  const list = $("#record-list"); list.replaceChildren(); $("#record-count").textContent = records.length;
  records.forEach((record) => {
    const row = document.createElement("tr");
    row.innerHTML = `<td><strong>${escapeHtml(record.memberNickname || `회원 #${record.memberId}`)}</strong><small>기록 ID ${record.recordId}</small></td><td><span class="status">${record.recordType === "MEAL" ? "식사" : "운동"}</span></td><td><strong>${escapeHtml(recordSummary(record))}</strong><small>${escapeHtml(record.imageKey)}</small></td><td>${formatDateTime(record.uploadedAt)}</td><td class="action-column"><div class="actions"><button class="table-button" data-action="edit-record" data-id="${record.recordId}" type="button">수정</button><button class="table-button delete" data-action="delete-record" data-id="${record.recordId}" type="button">삭제</button></div></td>`;
    list.append(row);
  });
  $("#record-empty").hidden = records.length !== 0;
}

function selectTab(tab) {
  activeTab = tab;
  $("#challenge-tab").classList.toggle("active", tab === "challenge"); $("#record-tab").classList.toggle("active", tab === "record");
  $("#challenge-tab").setAttribute("aria-selected", tab === "challenge"); $("#record-tab").setAttribute("aria-selected", tab === "record");
  $("#challenge-panel").hidden = tab !== "challenge"; $("#record-panel").hidden = tab !== "record";
}

async function loadGroupData() {
  const groupId = selectedGroupId(); if (!groupId) { challenges = []; records = []; renderChallenges(); renderRecords(); return; }
  setMessage(workspaceMessage);
  try {
    const [challengeResult, recordResult] = await Promise.all([request(`/api/v1/admin/groups/${groupId}/weekly-challenges`), request(`/api/v1/admin/groups/${groupId}/records`)]);
    challenges = challengeResult.challenges; records = recordResult.records; renderChallenges(); renderRecords();
  } catch (error) { setMessage(workspaceMessage, error.message, "error"); }
}
async function loadGroups() {
  setLoading($("#refresh-button"), true, "불러오는 중");
  try {
    groups = (await request("/api/v1/admin/groups")).groups;
    renderGroups();
    if (groups.length) { groupSelect.value = groups[0].groupId; await loadGroupData(); }
    return true;
  } catch (error) {
    if (error.status === 403) { sessionStorage.removeItem(ADMIN_KEY_STORAGE); showAccess("관리자 API 키를 다시 확인해주세요."); }
    else setMessage(workspaceMessage, error.message, "error");
    return false;
  } finally { setLoading($("#refresh-button"), false); }
}

function openChallengeDialog(challenge = null) {
  $("#challenge-form").reset(); setMessage($("#challenge-form-message")); $("#editing-challenge-id").value = challenge?.groupChallengeId ?? "";
  $("#challenge-dialog-title").textContent = challenge ? "챌린지 수정" : "챌린지 등록";
  $("#challenge-title").value = challenge?.title ?? ""; $("#challenge-description").value = challenge?.description ?? "";
  $("#starts-on").value = challenge?.startsOn ?? ""; $("#ends-on").value = challenge?.endsOn ?? ""; if (!challenge) setDefaultDates(); challengeDialog.showModal();
}
function syncRecordFields() { const meal = $("#record-type").value === "MEAL"; $("#meal-fields").hidden = !meal; $("#exercise-fields").hidden = meal; }
function openRecordDialog(record) {
  $("#record-form").reset(); setMessage($("#record-form-message")); $("#editing-record-id").value = record.recordId; $("#record-type").value = record.recordType;
  $("#record-meal-time").value = record.mealTime ?? ""; $("#record-menu").value = record.menu ?? ""; $("#record-duration").value = record.exerciseDurationMinutes ?? ""; $("#record-exercise-name").value = record.exerciseName ?? ""; syncRecordFields(); recordDialog.showModal();
}
function requestDelete(kind, id) {
  pendingDelete = { kind, id }; const isChallenge = kind === "challenge"; $("#confirm-title").textContent = isChallenge ? "챌린지를 삭제할까요?" : "활동 기록을 삭제할까요?"; $("#confirm-copy").textContent = isChallenge ? "삭제한 챌린지는 그룹 화면에서 더 이상 노출되지 않습니다." : "이 기록은 모든 그룹 노출과 연결된 댓글에서 함께 삭제됩니다."; confirmDialog.showModal();
}

accessForm.addEventListener("submit", async (event) => { event.preventDefault(); sessionStorage.setItem(ADMIN_KEY_STORAGE, adminKeyInput.value.trim()); if (await loadGroups()) showWorkspace(); });
$("#key-visibility-button").addEventListener("click", () => { const show = adminKeyInput.type === "password"; adminKeyInput.type = show ? "text" : "password"; $("#key-visibility-button").textContent = show ? "숨김" : "보기"; });
$("#sign-out-button").addEventListener("click", () => { sessionStorage.removeItem(ADMIN_KEY_STORAGE); groups = []; challenges = []; records = []; showAccess(); });
$("#refresh-button").addEventListener("click", loadGroups); groupSelect.addEventListener("change", loadGroupData); $("#challenge-tab").addEventListener("click", () => selectTab("challenge")); $("#record-tab").addEventListener("click", () => selectTab("record")); $("#new-challenge-button").addEventListener("click", () => openChallengeDialog()); $("#record-type").addEventListener("change", syncRecordFields);

document.addEventListener("click", (event) => {
  const dialogId = event.target.dataset.close;
  if (dialogId) { $("#" + dialogId).close(); return; }
  const button = event.target.closest("button[data-action]"); if (!button) return; const id = button.dataset.id;
  if (button.dataset.action === "edit-challenge") openChallengeDialog(challenges.find((item) => String(item.groupChallengeId) === id));
  if (button.dataset.action === "delete-challenge") requestDelete("challenge", id);
  if (button.dataset.action === "edit-record") openRecordDialog(records.find((item) => String(item.recordId) === id));
  if (button.dataset.action === "delete-record") requestDelete("record", id);
});

$("#challenge-form").addEventListener("submit", async (event) => { event.preventDefault(); const id = $("#editing-challenge-id").value; const body = { title: $("#challenge-title").value, description: $("#challenge-description").value, startsOn: $("#starts-on").value, endsOn: $("#ends-on").value }; setLoading($("#save-challenge-button"), true, "저장 중"); try { await request(`/api/v1/admin/groups/${selectedGroupId()}/weekly-challenges${id ? `/${id}` : ""}`, { method: id ? "PUT" : "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(body) }); challengeDialog.close(); await loadGroupData(); setMessage(workspaceMessage, "챌린지를 저장했습니다.", "success"); } catch (error) { setMessage($("#challenge-form-message"), error.message, "error"); } finally { setLoading($("#save-challenge-button"), false); } });
$("#record-form").addEventListener("submit", async (event) => { event.preventDefault(); const type = $("#record-type").value; const body = type === "MEAL" ? { recordType:type, mealTime:$("#record-meal-time").value || null, menu:$("#record-menu").value, exerciseDurationMinutes:null, exerciseName:null } : { recordType:type, mealTime:null, menu:null, exerciseDurationMinutes:Number($("#record-duration").value) || null, exerciseName:$("#record-exercise-name").value }; setLoading($("#save-record-button"), true, "저장 중"); try { await request(`/api/v1/admin/groups/${selectedGroupId()}/records/${$("#editing-record-id").value}`, { method:"PUT", headers:{"Content-Type":"application/json"}, body:JSON.stringify(body) }); recordDialog.close(); await loadGroupData(); setMessage(workspaceMessage, "기록을 저장했습니다.", "success"); } catch (error) { setMessage($("#record-form-message"), error.message, "error"); } finally { setLoading($("#save-record-button"), false); } });
$("#confirm-delete-button").addEventListener("click", async (event) => { event.preventDefault(); if (!pendingDelete) return; const endpoint = pendingDelete.kind === "challenge" ? `/api/v1/admin/groups/${selectedGroupId()}/weekly-challenges/${pendingDelete.id}` : `/api/v1/admin/groups/${selectedGroupId()}/records/${pendingDelete.id}`; setLoading(event.target, true, "삭제 중"); try { await request(endpoint, { method:"DELETE" }); confirmDialog.close(); await loadGroupData(); setMessage(workspaceMessage, "삭제했습니다.", "success"); } catch (error) { confirmDialog.close(); setMessage(workspaceMessage, error.message, "error"); } finally { pendingDelete = null; setLoading(event.target, false); } });

if (adminKey()) { loadGroups().then((loaded) => { if (loaded && adminKey()) showWorkspace(); }); }
