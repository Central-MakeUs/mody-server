const ADMIN_KEY_STORAGE = "mody.admin.api-key";

const accessSection = document.querySelector("#access-section");
const workspace = document.querySelector("#workspace");
const accessForm = document.querySelector("#access-form");
const accessMessage = document.querySelector("#access-message");
const adminKeyInput = document.querySelector("#admin-key");
const keyVisibilityButton = document.querySelector("#key-visibility-button");
const signOutButton = document.querySelector("#sign-out-button");
const refreshGroupsButton = document.querySelector("#refresh-groups-button");
const groupSelect = document.querySelector("#group-id");
const groupCount = document.querySelector("#group-count");
const challengeForm = document.querySelector("#challenge-form");
const challengeMessage = document.querySelector("#challenge-message");
const createButton = document.querySelector("#create-button");
const creationResult = document.querySelector("#creation-result");

let groups = [];

function adminKey() {
  return sessionStorage.getItem(ADMIN_KEY_STORAGE);
}

function setMessage(element, message, type = "") {
  element.textContent = message;
  element.className = `form-message ${type}`.trim();
}

function setLoading(button, loading, label) {
  button.disabled = loading;
  if (loading) {
    button.dataset.label = button.textContent;
    button.textContent = label;
  } else if (button.dataset.label) {
    button.textContent = button.dataset.label;
  }
}

async function request(path, options = {}) {
  const response = await fetch(path, {
    ...options,
    headers: {
      "X-Admin-Api-Key": adminKey(),
      ...(options.headers ?? {})
    }
  });
  const payload = await response.json().catch(() => null);

  if (!response.ok || !payload?.isSuccess) {
    const error = new Error(payload?.message ?? "요청을 처리하지 못했습니다.");
    error.status = response.status;
    throw error;
  }
  return payload.result;
}

function showAccess(message = "") {
  accessSection.hidden = false;
  workspace.hidden = true;
  signOutButton.hidden = true;
  setMessage(accessMessage, message, message ? "error" : "");
  adminKeyInput.focus();
}

function showWorkspace() {
  accessSection.hidden = true;
  workspace.hidden = false;
  signOutButton.hidden = false;
}

function renderGroups() {
  groupSelect.replaceChildren();
  const placeholder = new Option("대상 그룹을 선택하세요.", "");
  placeholder.disabled = true;
  placeholder.selected = true;
  groupSelect.add(placeholder);

  groups.forEach((group) => {
    const option = new Option(`${group.name} · ${group.memberCount}명 · ${group.code}`, group.groupId);
    option.dataset.name = group.name;
    groupSelect.add(option);
  });
  groupSelect.disabled = groups.length === 0;
  groupCount.textContent = groups.length === 0 ? "운영 그룹 없음" : `${groups.length}개 운영 그룹`;
}

async function loadGroups() {
  setLoading(refreshGroupsButton, true, "불러오는 중");
  groupSelect.disabled = true;
  try {
    groups = await request("/api/v1/admin/groups");
    renderGroups();
    setMessage(challengeMessage, "", "");
  } catch (error) {
    if (error.status === 403) {
      sessionStorage.removeItem(ADMIN_KEY_STORAGE);
      showAccess("관리자 API 키를 다시 확인해주세요.");
      return;
    }
    groupCount.textContent = "그룹 목록을 불러오지 못했습니다.";
    setMessage(challengeMessage, error.message, "error");
  } finally {
    setLoading(refreshGroupsButton, false);
  }
}

function dateString(date) {
  const localDate = new Date(date.getTime() - date.getTimezoneOffset() * 60_000);
  return localDate.toISOString().slice(0, 10);
}

function initializeDates() {
  const today = new Date();
  const startsOn = document.querySelector("#starts-on");
  const endsOn = document.querySelector("#ends-on");
  startsOn.value = dateString(today);
  const end = new Date(today);
  end.setDate(end.getDate() + 6);
  endsOn.value = dateString(end);
}

function renderCreationResult(result, group) {
  document.querySelector("#result-title").textContent = result.title;
  document.querySelector("#result-group").textContent = group?.name ?? "선택한 그룹";
  document.querySelector("#result-period").textContent = `${result.startsOn} ~ ${result.endsOn}`;
  document.querySelector("#result-id").textContent = String(result.groupChallengeId);
  creationResult.hidden = false;
}

accessForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  sessionStorage.setItem(ADMIN_KEY_STORAGE, adminKeyInput.value.trim());
  setLoading(accessForm.querySelector(".primary-button"), true, "확인 중");
  try {
    await loadGroups();
    if (adminKey()) {
      showWorkspace();
    }
  } finally {
    setLoading(accessForm.querySelector(".primary-button"), false);
  }
});

challengeForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  const formData = new FormData(challengeForm);
  const groupId = formData.get("groupId");
  const selectedGroup = groups.find((group) => String(group.groupId) === groupId);
  setLoading(createButton, true, "등록 중");
  setMessage(challengeMessage, "", "");
  try {
    const result = await request(`/api/v1/admin/groups/${groupId}/weekly-challenges`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        title: formData.get("title"),
        description: formData.get("description"),
        startsOn: formData.get("startsOn"),
        endsOn: formData.get("endsOn")
      })
    });
    renderCreationResult(result, selectedGroup);
    setMessage(challengeMessage, "챌린지를 등록했습니다.", "success");
    challengeForm.querySelector("#challenge-title").value = "";
    challengeForm.querySelector("#challenge-description").value = "";
  } catch (error) {
    setMessage(challengeMessage, error.message, "error");
  } finally {
    setLoading(createButton, false);
  }
});

keyVisibilityButton.addEventListener("click", () => {
  const isPassword = adminKeyInput.type === "password";
  adminKeyInput.type = isPassword ? "text" : "password";
  keyVisibilityButton.textContent = isPassword ? "숨김" : "보기";
  keyVisibilityButton.setAttribute("aria-label", isPassword ? "API 키 숨김" : "API 키 표시");
  keyVisibilityButton.title = keyVisibilityButton.getAttribute("aria-label");
});

signOutButton.addEventListener("click", () => {
  sessionStorage.removeItem(ADMIN_KEY_STORAGE);
  groups = [];
  creationResult.hidden = true;
  adminKeyInput.value = "";
  showAccess();
});

refreshGroupsButton.addEventListener("click", loadGroups);

initializeDates();
if (adminKey()) {
  loadGroups().then(() => {
    if (adminKey()) {
      showWorkspace();
    }
  });
}
