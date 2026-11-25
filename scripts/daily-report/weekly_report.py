#!/usr/bin/env python3
"""MODY 주간 지표 리포트를 Slack(mody-bot)으로 보낸다.

매주 월요일에 지난주(월 00:00 ~ 일 24:00, KST) 지표를 전주와 비교해 올린다.
PM·디자이너가 기획 방향을 잡을 때 보는 리포트라서 가입·기록·활동 패턴·신규 회원 첫 주·
그룹·반응·연령대를 본다. DB 접속, Slack 발송, 중복 발송 방지는 daily_report.py와 같다.

환경변수 (daily_report.py와 같고, 아래만 다르다)
  REPORT_WEEK            집계할 주의 월요일 (YYYY-MM-DD, 비우면 지난주)
  REPORT_DRY_RUN=1       Slack에 보내지 않고 지표 JSON만 출력
  REPORT_PREVIEW=1       Slack에 보내지 않고 Block Kit Builder 링크 출력

지표 정의
  - 기록은 activity_record(식사·운동)만 센다. 삭제·비활성 기록과 댓글은 뺀다.
  - 가입은 그 주에 가입한 모든 회원(이후 탈퇴 포함), 회원 구성은 그 주 마감 시점에 탈퇴하지 않은 회원이다.
  - 기록에는 그룹이 없다(모든 그룹에 보인다). 그래서 "활동한 그룹"은 그 주 마감 시점 구성원 중
    누군가 그 주에 기록한 그룹이다.
  - 개인이 드러나지 않도록 사람 수가 5명 미만인 칸은 숫자 대신 "5명 미만"으로 쓰고, 증감도 쓰지 않는다.
  - 퍼센트는 비교 대상(분모·전주 값)이 10 이상일 때만 붙인다.
"""
import json
import os
import sys
from datetime import date, timedelta

from daily_report import (DOWN, FLAT, UP, already_posted_text, builder_url, bullets, fetch, post_text,
                          t, title)

MIN_CELL = 5       # 이보다 적은 사람 수는 숫자로 쓰지 않는다
MIN_PERCENT = 10   # 비교 대상이 이보다 적으면 퍼센트를 붙이지 않는다

# {s}: 주 시작(월), {e}: 다음 주 시작, {p}: 전주 시작. 모두 DATE 리터럴로 바뀐다.
LIVE_A = "a.status = 'ACTIVE' AND a.deleted_at IS NULL"
LIVE_C = "c.status = 'ACTIVE' AND c.deleted_at IS NULL"
IN_WEEK = "a.created_at >= {s} AND a.created_at < {e}"
RECORDS = f"activity_record a WHERE {LIVE_A} AND {IN_WEEK}"
# 그 주 마감 시점에 그룹에 참여 중인 구성원
MEMBERS_AT_END = "gm.joined_at < {e} AND (gm.left_at IS NULL OR gm.left_at >= {e})"
# 그 주 마감 시점에 탈퇴하지 않은 회원
ALIVE_AT_END = "m.created_at < {e} AND (m.deleted_at IS NULL OR m.deleted_at >= {e})"
AGE = """CASE WHEN m.birth_date IS NULL THEN 'none'
              WHEN TIMESTAMPDIFF(YEAR, m.birth_date, {e} - INTERVAL 1 DAY) < 20 THEN '10s'
              WHEN TIMESTAMPDIFF(YEAR, m.birth_date, {e} - INTERVAL 1 DAY) < 30 THEN '20s'
              WHEN TIMESTAMPDIFF(YEAR, m.birth_date, {e} - INTERVAL 1 DAY) < 40 THEN '30s'
              ELSE '40p' END"""
# 회원별 그룹 첫 참여. 그룹의 첫 번째 회원을 그룹을 만든 회원으로 본다.
FIRST_JOINS = """(SELECT group_id, member_id, joined_at,
                         ROW_NUMBER() OVER (PARTITION BY group_id ORDER BY joined_at, id) rn
                  FROM (SELECT group_id, member_id, MIN(joined_at) joined_at, MIN(id) id
                        FROM group_member GROUP BY group_id, member_id) per_member) fj"""

WEEKLY = {
    # 가입
    "signup":   "SELECT COUNT(*) FROM member WHERE created_at >= {s} AND created_at < {e}",
    "withdraw": "SELECT COUNT(*) FROM member WHERE deleted_at >= {s} AND deleted_at < {e}",

    # 기록
    "records":          f"SELECT COUNT(*) FROM {RECORDS}",
    "recorders":        f"SELECT COUNT(DISTINCT a.member_id) FROM {RECORDS}",
    "meal":             f"SELECT COUNT(*) FROM {RECORDS} AND a.record_type = 'MEAL'",
    "meal_members":     f"SELECT COUNT(DISTINCT a.member_id) FROM {RECORDS} AND a.record_type = 'MEAL'",
    "exercise":         f"SELECT COUNT(*) FROM {RECORDS} AND a.record_type = 'EXERCISE'",
    "exercise_members": f"SELECT COUNT(DISTINCT a.member_id) FROM {RECORDS} AND a.record_type = 'EXERCISE'",
    "exercise_minutes": f"SELECT COALESCE(SUM(a.exercise_duration_minutes), 0) FROM {RECORDS} "
                        "AND a.record_type = 'EXERCISE'",
    # 식사 시간대: 앱에서 고른 식사 시각 기준
    "meal_morning":     f"SELECT COUNT(*) FROM {RECORDS} AND a.record_type = 'MEAL' "
                        "AND TIME(a.meal_time) >= '04:00' AND TIME(a.meal_time) < '10:30'",
    "meal_lunch":       f"SELECT COUNT(*) FROM {RECORDS} AND a.record_type = 'MEAL' "
                        "AND TIME(a.meal_time) >= '10:30' AND TIME(a.meal_time) < '15:00'",
    "meal_dinner":      f"SELECT COUNT(*) FROM {RECORDS} AND a.record_type = 'MEAL' "
                        "AND TIME(a.meal_time) >= '15:00' AND TIME(a.meal_time) < '21:00'",
    "meal_night":       f"SELECT COUNT(*) FROM {RECORDS} AND a.record_type = 'MEAL' "
                        "AND (TIME(a.meal_time) >= '21:00' OR TIME(a.meal_time) < '04:00')",
    # 체중·걸음 수 (참고)
    "weight":           "SELECT COUNT(*) FROM weight_record WHERE status = 'ACTIVE' AND deleted_at IS NULL "
                        "AND created_at >= {s} AND created_at < {e}",
    "weight_members":   "SELECT COUNT(DISTINCT member_id) FROM weight_record WHERE status = 'ACTIVE' "
                        "AND deleted_at IS NULL AND created_at >= {s} AND created_at < {e}",
    "step_members":     "SELECT COUNT(DISTINCT member_id) FROM step_record WHERE status = 'ACTIVE' "
                        "AND deleted_at IS NULL AND created_at >= {s} AND created_at < {e}",

    # 활동 패턴: 이번 주 기록한 회원을 전주 기록 여부와 가입 시점으로 나눈다
    "recorders_before": "SELECT COUNT(DISTINCT a.member_id) FROM activity_record a "
                        f"WHERE {LIVE_A} AND a.created_at >= {{p}} AND a.created_at < {{s}}",
    "retained":         f"SELECT COUNT(DISTINCT a.member_id) FROM {RECORDS} AND EXISTS ("
                        f"SELECT 1 FROM activity_record b WHERE b.member_id = a.member_id "
                        "AND b.status = 'ACTIVE' AND b.deleted_at IS NULL "
                        "AND b.created_at >= {p} AND b.created_at < {s})",
    "new_recorders":    f"SELECT COUNT(DISTINCT a.member_id) FROM {RECORDS} AND EXISTS ("
                        "SELECT 1 FROM member m WHERE m.id = a.member_id AND m.created_at >= {s})",
}

# 기록한 날 수: 회원별로 그 주에 기록을 남긴 날(KST)의 수
for key, cond in {"days_1": "= 1", "days_2_3": "BETWEEN 2 AND 3", "days_4_6": "BETWEEN 4 AND 6", "days_7": "= 7"}.items():
    WEEKLY[key] = (f"SELECT COUNT(*) FROM (SELECT a.member_id, COUNT(DISTINCT DATE(a.created_at)) d "
                   f"FROM {RECORDS} GROUP BY a.member_id) per_member WHERE d {cond}")

# 요일별 기록 수 (0=월 ~ 6=일)
for i in range(7):
    WEEKLY[f"dow_{i}"] = f"SELECT COUNT(*) FROM {RECORDS} AND WEEKDAY(a.created_at) = {i}"

WEEKLY.update({
    # 신규 회원 첫 주: 그 주 가입자 중 주가 끝나기 전에 기록을 남기거나 그룹에 들어간 회원
    "new_recorded":   "SELECT COUNT(*) FROM member m WHERE m.created_at >= {s} AND m.created_at < {e} "
                      f"AND EXISTS (SELECT 1 FROM activity_record a WHERE {LIVE_A} "
                      "AND a.member_id = m.id AND a.created_at < {e})",
    "new_same_day":   "SELECT COUNT(*) FROM member m WHERE m.created_at >= {s} AND m.created_at < {e} "
                      f"AND EXISTS (SELECT 1 FROM activity_record a WHERE {LIVE_A} "
                      "AND a.member_id = m.id AND DATE(a.created_at) = DATE(m.created_at))",
    "new_grouped":    "SELECT COUNT(*) FROM member m WHERE m.created_at >= {s} AND m.created_at < {e} "
                      "AND EXISTS (SELECT 1 FROM group_member g WHERE g.member_id = m.id AND g.joined_at < {e})",
    "new_own_group":  "SELECT COUNT(*) FROM member m WHERE m.created_at >= {s} AND m.created_at < {e} "
                      f"AND EXISTS (SELECT 1 FROM {FIRST_JOINS} WHERE fj.member_id = m.id AND fj.rn = 1 "
                      "AND fj.joined_at < {e})",
    "new_joined":     "SELECT COUNT(*) FROM member m WHERE m.created_at >= {s} AND m.created_at < {e} "
                      f"AND EXISTS (SELECT 1 FROM {FIRST_JOINS} WHERE fj.member_id = m.id AND fj.rn > 1 "
                      "AND fj.joined_at < {e})",

    # 그룹
    "new_groups":     "SELECT COUNT(*) FROM mody_group WHERE deleted_at IS NULL "
                      "AND created_at >= {s} AND created_at < {e}",
    "second_joined":  f"SELECT COUNT(*) FROM {FIRST_JOINS} WHERE fj.rn = 2 "
                      "AND fj.joined_at >= {s} AND fj.joined_at < {e}",
    "groups_active":  "SELECT COUNT(DISTINCT gm.group_id) FROM group_member gm "
                      "JOIN mody_group g ON g.id = gm.group_id AND g.deleted_at IS NULL "
                      f"WHERE {MEMBERS_AT_END} AND EXISTS (SELECT 1 FROM {RECORDS} AND a.member_id = gm.member_id)",
})

# 그 주 마감 시점의 그룹 크기(참여 중인 구성원 수) 분포. 구성원이 없는 그룹은 뺀다.
for key, cond in {"size_1": "= 1", "size_2_3": "BETWEEN 2 AND 3", "size_4p": ">= 4"}.items():
    WEEKLY[key] = ("SELECT COUNT(*) FROM (SELECT gm.group_id, COUNT(*) n FROM group_member gm "
                   "JOIN mody_group g ON g.id = gm.group_id AND g.deleted_at IS NULL "
                   f"WHERE {MEMBERS_AT_END} GROUP BY gm.group_id) sizes WHERE n {cond}")

WEEKLY.update({
    # 반응
    "comments":      f"SELECT COUNT(*) FROM record_comment c WHERE {LIVE_C} "
                     "AND c.created_at >= {s} AND c.created_at < {e}",
    "commenters":    f"SELECT COUNT(DISTINCT c.member_id) FROM record_comment c WHERE {LIVE_C} "
                     "AND c.created_at >= {s} AND c.created_at < {e}",
    # 다른 회원에게 댓글을 받은 회원 (자기 기록에 단 댓글은 뺀다)
    "comment_receivers": f"SELECT COUNT(DISTINCT a.member_id) FROM record_comment c "
                     f"JOIN activity_record a ON a.id = c.record_id WHERE {LIVE_C} "
                     "AND c.created_at >= {s} AND c.created_at < {e} AND c.member_id <> a.member_id",
    # 그 주 기록 중 주가 끝나기 전에 댓글이 1개 이상 달린 기록
    "commented":     f"SELECT COUNT(*) FROM {RECORDS} AND EXISTS (SELECT 1 FROM record_comment c "
                     f"WHERE {LIVE_C} AND c.record_id = a.id AND c.created_at < {{e}})",
})

# 연령대: 주 마지막 날 기준 만 나이
AGE_BANDS = [("10s", "10대 이하"), ("20s", "20대"), ("30s", "30대"), ("40p", "40대 이상"), ("none", "미입력")]
for band, _ in AGE_BANDS:
    WEEKLY[f"age_all_{band}"] = f"SELECT COUNT(*) FROM member m WHERE {ALIVE_AT_END} AND {AGE} = '{band}'"
    WEEKLY[f"age_recent_{band}"] = (f"SELECT COUNT(*) FROM member m WHERE {ALIVE_AT_END} "
                                    f"AND m.created_at >= {{e}} - INTERVAL 28 DAY AND {AGE} = '{band}'")
    # 연령대별 활동: 그 주에 기록한 회원 수와 그들의 기록 수
    WEEKLY[f"age_rec_{band}"] = (f"SELECT COUNT(DISTINCT a.member_id) FROM {RECORDS} AND EXISTS ("
                                 f"SELECT 1 FROM member m WHERE m.id = a.member_id AND {AGE} = '{band}')")
    WEEKLY[f"age_recn_{band}"] = (f"SELECT COUNT(*) FROM {RECORDS} AND EXISTS ("
                                  f"SELECT 1 FROM member m WHERE m.id = a.member_id AND {AGE} = '{band}')")


def last_week(today: date) -> date:
    """오늘 기준으로 끝난 가장 최근 주의 월요일."""
    return today - timedelta(days=today.weekday() + 7)


def build_sql(start: date) -> str:
    """이번 주와 전주의 모든 지표를 'key<TAB>value' 행으로 돌려주는 단일 SQL."""
    lit = lambda d: f"DATE '{d.isoformat()}'"
    parts = []
    for key, q in WEEKLY.items():
        for suffix, s in (("", start), ("_prev", start - timedelta(days=7))):
            sql = " ".join(q.format(s=lit(s), e=lit(s + timedelta(days=7)), p=lit(s - timedelta(days=7))).split())
            parts.append(f"SELECT '{key}{suffix}', ({sql})")
    return "START TRANSACTION READ ONLY;\n" + "\nUNION ALL\n".join(parts) + ";\nROLLBACK;\n"


# ---- 숫자 표기 ----

def people(n: int) -> str:
    return "5명 미만" if n < MIN_CELL else f"{n:,}명"


def pct(part: float, whole: int) -> str:
    """비교 대상이 10 이상일 때만 퍼센트를 붙인다."""
    return f" ({100 * part / whole:.0f}%)" if whole >= MIN_PERCENT else ""


def change(cur: float, prev: float, people_count: bool = False) -> list:
    """증감 이모지 + 부호 있는 차이 (+ 전주가 10 이상이면 증감률)."""
    if people_count and (cur < MIN_CELL or prev < MIN_CELL):
        return []  # 5명 미만 칸이 끼면 차이로 실제 수가 드러나므로 증감을 쓰지 않는다
    diff = cur - prev
    rate = f" ({diff / prev * 100:+.0f}%)".replace("-", "−") if prev >= MIN_PERCENT else ""
    fmt = (lambda x: f"{x:,.1f}") if isinstance(diff, float) else (lambda x: f"{x:,}")
    if diff > 0:
        return [{"type": "emoji", "name": UP}, t(f" +{fmt(diff)}{rate}")]
    if diff < 0:
        return [{"type": "emoji", "name": DOWN}, t(f" −{fmt(-diff)}{rate}")]
    return [{"type": "emoji", "name": FLAT}]


def metric(label: str, prev, cur, unit: str = "", people_count: bool = False) -> list:
    """`라벨　전주 → *이번주*　▲ +n (+x%)`"""
    show = people if people_count else (lambda n: f"{n:,}{unit}")
    return [t(f"{label}　"), t(f"{show(prev)} → "), t(show(cur), bold=True), t("　"),
            *change(cur, prev, people_count)]


def per(records: int, members: int) -> float:
    return round(records / members, 1) if members else 0.0


def share(part: int, whole: int, people_count: bool = True) -> str:
    """`7/16명 (44%)` 또는 `17/441건 (4%)`. 사람 수 5명 미만 칸은 숫자를 감춘다."""
    if people_count:
        if whole < MIN_CELL:
            return "5명 미만"
        if part < MIN_CELL:
            return f"5명 미만/{whole:,}명"
        return f"{part:,}/{whole:,}명{pct(part, whole)}"
    return f"{part:,}/{whole:,}건{pct(part, whole)}"


def compare(label: str, prev: str, cur: str) -> list:
    return [t(f"{label}　{prev} → "), t(cur, bold=True)]


def count_share(n: int, whole: int) -> str:
    """사람 수와 전체 중 비율. `12명 (28%)`"""
    return people(n) + (pct(n, whole) if n >= MIN_CELL else "")


def merge_bands(cur: dict, prev: dict) -> list:
    """5명 미만인 가장 어린/나이 많은 연령대를 안쪽 칸과 합친다. 미입력은 합치지 않는다."""
    groups = [["10s"], ["20s"], ["30s"], ["40p"]]
    small = lambda keys: min(sum(cur[k] for k in keys), sum(prev[k] for k in keys)) < MIN_CELL
    while len(groups) > 1 and small(groups[0]):
        groups[1] = groups[0] + groups[1]
        groups.pop(0)
    while len(groups) > 1 and small(groups[-1]):
        groups[-2] = groups[-2] + groups[-1]
        groups.pop()
    names = {"10s": "10대", "20s": "20대", "30s": "30대", "40p": "40대"}
    labeled = []
    for keys in groups:
        if len(keys) == 1:
            label = dict(AGE_BANDS)[keys[0]]
        elif keys[-1] == "40p":
            label = f"{names[keys[0]]} 이상"
        elif keys[0] == "10s":
            label = f"{names[keys[-1]]} 이하"
        else:
            label = f"{names[keys[0]]}~{names[keys[-1]]}"
        labeled.append((label, keys))
    return labeled + [("미입력", ["none"])]


def band_values(v: dict, prefix: str, suffix: str = "") -> dict:
    return {b: v[f"{prefix}_{b}{suffix}"] for b, _ in AGE_BANDS}


def age_rows(v: dict, population: str) -> list:
    """연령대 구성: `20대　150명 (51%) → 155명 (50%)`"""
    cur, prev = band_values(v, f"age_{population}"), band_values(v, f"age_{population}", "_prev")
    total, total_prev = sum(cur.values()), sum(prev.values())
    rows = []
    for label, keys in merge_bands(cur, prev):
        c, p = sum(cur[k] for k in keys), sum(prev[k] for k in keys)
        rows.append(compare(label, count_share(p, total_prev), count_share(c, total)))
    return rows


def age_activity_rows(v: dict) -> list:
    """연령대별 활동: `20대　기록한 회원 20명 → 22명 · 1명당 9.0건 → 8.5건`"""
    cur, prev = band_values(v, "age_rec"), band_values(v, "age_rec", "_prev")
    n_cur, n_prev = band_values(v, "age_recn"), band_values(v, "age_recn", "_prev")
    rows = []
    for label, keys in merge_bands(cur, prev):
        c, p = sum(cur[k] for k in keys), sum(prev[k] for k in keys)
        rc, rp = sum(n_cur[k] for k in keys), sum(n_prev[k] for k in keys)
        if c < MIN_CELL or p < MIN_CELL:
            rows.append([t(f"{label}　기록한 회원 {people(p)} → {people(c)}")])
            continue
        rows.append([t(f"{label}　기록한 회원 {people(p)} → "), t(people(c), bold=True),
                     t(f" · 1명당 {per(rp, p)}건 → {per(rc, c)}건")])
    return rows


def build_blocks(v: dict, start: date) -> list:
    end = start + timedelta(days=6)
    prev_start, prev_end = start - timedelta(days=7), start - timedelta(days=1)
    md = lambda d: f"{d.month}/{d.day}"
    p = lambda key: v[key + "_prev"]
    gap = {"type": "rich_text_section", "elements": [t("\n")]}
    sub = lambda text: [t(text)]
    wd = "월화수목금토일"

    returning, returning_prev = (v[f"recorders{s}"] - v[f"retained{s}"] - v[f"new_recorders{s}"]
                                 for s in ("", "_prev"))
    recent, recent_prev = (sum(band_values(v, "age_recent", s).values()) for s in ("", "_prev"))
    everyone, everyone_prev = (sum(band_values(v, "age_all", s).values()) for s in ("", "_prev"))
    groups_live, groups_live_prev = (v[f"size_1{s}"] + v[f"size_2_3{s}"] + v[f"size_4p{s}"] for s in ("", "_prev"))
    meal_slots = [("아침 (4~10:30)", "meal_morning"), ("점심 (10:30~15시)", "meal_lunch"),
                  ("저녁 (15~21시)", "meal_dinner"), ("밤 (21~4시)", "meal_night")]
    days = [("1일", "days_1"), ("2~3일", "days_2_3"), ("4~6일", "days_4_6"), ("7일 모두", "days_7")]
    busiest = max(range(7), key=lambda i: v[f"dow_{i}"])

    body = [
        title("한눈에"),
        bullets([
            metric("🔥 기록한 회원", p("recorders"), v["recorders"], people_count=True),
            metric("📝 전체 기록", p("records"), v["records"], "건"),
            [t("🔁 전주 기록한 회원이 이번 주도 기록　"),
             t(f"{share(p('retained'), p('recorders_before'))} → "),
             t(share(v["retained"], v["recorders_before"]), bold=True)],
        ]),
        gap,
        title("가입"),
        bullets([
            metric("🌱 신규 가입", p("signup"), v["signup"], people_count=True),
            metric("👋 탈퇴", p("withdraw"), v["withdraw"], people_count=True),
        ]),
        gap,
        title("기록"),
        bullets([metric("🍚 식사 기록", p("meal"), v["meal"], "건")]),
        bullets([
            metric("기록한 회원", p("meal_members"), v["meal_members"], people_count=True),
            metric("1명당", per(p("meal"), p("meal_members")), per(v["meal"], v["meal_members"]), "건"),
            *[compare(label, share(p(key), p("meal"), False), share(v[key], v["meal"], False))
              for label, key in meal_slots],
        ], indent=1),
        bullets([metric("🏃 운동 기록", p("exercise"), v["exercise"], "건")]),
        bullets([
            metric("기록한 회원", p("exercise_members"), v["exercise_members"], people_count=True),
            metric("1명당", per(p("exercise"), p("exercise_members")),
                   per(v["exercise"], v["exercise_members"]), "건"),
            metric("운동 시간 합계", p("exercise_minutes"), v["exercise_minutes"], "분"),
            metric("기록 1건당 운동 시간", per(p("exercise_minutes"), p("exercise")),
                   per(v["exercise_minutes"], v["exercise"]), "분"),
        ], indent=1),
        bullets([sub("📅 요일별 기록 (식사+운동)")]),
        bullets([sub("　".join(f"{wd[i]} {p(f'dow_{i}'):,}→{v[f'dow_{i}']:,}" for i in range(7))),
                 sub(f"가장 많은 요일: {wd[busiest]}요일 {v[f'dow_{busiest}']:,}건")], indent=1),
        bullets([sub("⚖️ 참고 (리포트 기준 기록에는 안 들어감)")]),
        bullets([
            compare("체중 기록", f"{p('weight'):,}건 · {people(p('weight_members'))}",
                    f"{v['weight']:,}건 · {people(v['weight_members'])}"),
            compare("걸음 수 연동 회원", people(p("step_members")), people(v["step_members"])),
        ], indent=1),
        gap,
        title("활동 패턴 (이번 주 기록한 회원 기준)"),
        bullets([sub("이번 주 기록한 회원은 어디서 왔나")]),
        bullets([
            compare("전주에도 기록", people(p("retained")), people(v["retained"])),
            compare("전주엔 없다가 다시 기록", people(returning_prev), people(returning)),
            compare("이번 주 가입", people(p("new_recorders")), people(v["new_recorders"])),
        ], indent=1),
        bullets([sub("일주일 중 기록한 날 수")]),
        bullets([compare(label, count_share(p(key), p("recorders")), count_share(v[key], v["recorders"]))
                 for label, key in days], indent=1),
        gap,
        title("신규 회원 첫 주 (그 주 가입자 기준)"),
        bullets([
            compare("📝 기록 1번 이상", share(p("new_recorded"), p("signup")), share(v["new_recorded"], v["signup"])),
            compare("⚡ 가입한 날 바로 기록", share(p("new_same_day"), p("signup")),
                    share(v["new_same_day"], v["signup"])),
            compare("🏠 그룹 참여", share(p("new_grouped"), p("signup")), share(v["new_grouped"], v["signup"])),
        ]),
        bullets([
            compare("직접 그룹을 만든 회원", people(p("new_own_group")), people(v["new_own_group"])),
            compare("다른 사람 그룹에 들어간 회원", people(p("new_joined")), people(v["new_joined"])),
        ], indent=1),
        gap,
        title("그룹"),
        bullets([
            metric("🆕 새 그룹", p("new_groups"), v["new_groups"], "개"),
            metric("👥 1인 그룹에 두 번째 회원 참여", p("second_joined"), v["second_joined"], "개"),
            compare("🔥 이번 주 기록이 있는 그룹", share(p("groups_active"), groups_live_prev, False).replace("건", "개"),
                    share(v["groups_active"], groups_live, False).replace("건", "개")),
        ]),
        bullets([sub(f"그룹 크기 (구성원이 있는 그룹 {groups_live_prev:,} → {groups_live:,}개)")]),
        bullets([compare(label, share(p(key), groups_live_prev, False).replace("건", "개"),
                         share(v[key], groups_live, False).replace("건", "개"))
                 for label, key in (("1인", "size_1"), ("2~3인", "size_2_3"), ("4인 이상", "size_4p"))],
                indent=1),
        gap,
        title("반응"),
        bullets([metric("💬 댓글", p("comments"), v["comments"], "개")]),
        bullets([
            compare("댓글 단 회원", people(p("commenters")), people(v["commenters"])),
            compare("다른 회원에게 댓글 받은 회원", people(p("comment_receivers")), people(v["comment_receivers"])),
            compare("댓글 달린 기록", share(p("commented"), p("records"), False),
                    share(v["commented"], v["records"], False)),
        ], indent=1),
        gap,
        title("연령대 (주 마지막 날 기준 만 나이)"),
        bullets([sub(f"최근 4주 신규 가입 구성 ({people(recent_prev)} → {people(recent)})")]),
        bullets(age_rows(v, "recent"), indent=1),
        bullets([sub(f"전체 회원 구성 ({people(everyone_prev)} → {people(everyone)})")]),
        bullets(age_rows(v, "all"), indent=1),
        bullets([sub("연령대별 활동")]),
        bullets(age_activity_rows(v), indent=1),
    ]

    return [
        {"type": "header", "text": {"type": "plain_text", "text": "📈 MODY 주간 리포트"}},
        {"type": "context", "elements": [{"type": "mrkdwn", "text":
            f"*{start.year}.{start.month:02d}.{start.day:02d}({wd[0]}) ~ {md(end)}({wd[6]})*　·　"
            f"전주 {md(prev_start)}~{md(prev_end)}와 비교 (전주 → 이번 주)"}]},
        {"type": "divider"},
        {"type": "rich_text", "elements": body},
        {"type": "divider"},
        {"type": "context", "elements": [{"type": "mrkdwn", "text":
            "ℹ️  기록은 식사·운동 인증만 셉니다. 삭제된 기록·댓글은 빼고 KST 월~일로 셉니다. "
            "5명 미만인 칸은 숫자를 가립니다.\n"
            "💬  보고 싶은 지표가 있으면 스레드에 남겨주세요."}]},
    ]


def report_text(start: date) -> str:
    """알림·검색에 쓰이는 메시지 본문. 중복 발송 판단과 인사이트 대상 찾기의 기준이다."""
    return f"MODY 주간 리포트 {start.isoformat()}"


def main() -> None:
    # 운영 DB와 Slack 모두 KST 기준. 러너는 UTC이므로 TZ=Asia/Seoul로 실행한다.
    today = date.today()
    start = date.fromisoformat(os.environ["REPORT_WEEK"]) if os.environ.get("REPORT_WEEK") else last_week(today)
    if start.weekday() != 0:
        sys.exit(f"REPORT_WEEK는 월요일이어야 합니다: {start.isoformat()}")
    if start + timedelta(days=7) > today:
        sys.exit(f"{start.isoformat()} 주는 아직 끝나지 않았습니다 (일요일 24시 이후에 집계)")

    sending = os.environ.get("REPORT_DRY_RUN") != "1" and os.environ.get("REPORT_PREVIEW") != "1"
    if sending and os.environ.get("REPORT_FORCE") != "1" and already_posted_text(report_text(start)):
        print(f"weekly report for {start.isoformat()} already posted, skip")
        return
    values = fetch(build_sql(start))
    blocks = build_blocks(values, start)
    if os.environ.get("REPORT_DRY_RUN") == "1":
        print(json.dumps(values, ensure_ascii=False, indent=2))
        return
    if os.environ.get("REPORT_PREVIEW") == "1":
        print(builder_url(blocks))
        return
    post_text(blocks, report_text(start))
    print(f"sent weekly report for {start.isoformat()}")


if __name__ == "__main__":
    main()
