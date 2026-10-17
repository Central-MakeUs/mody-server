#!/usr/bin/env python3
"""MODY 일일 지표 리포트를 Slack(mody-bot)으로 보낸다.

운영 MySQL을 SSH 너머에서 읽기 전용 계정으로 조회한다. DB 비밀번호는 명령줄에
남지 않도록 stdin 첫 줄로 넘긴다.

환경변수
  MODY_SSH_TARGET        ssh 대상 (기본: mody-prod, CI에서는 user@host)
  MODY_SSH_KEY           ssh 키 파일 경로 (선택)
  REPORT_DB_USER         기본: grafana
  REPORT_DB_PASSWORD     필수
  SLACK_BOT_TOKEN        필수 (chat:write)
  SLACK_REPORT_CHANNEL   필수 (채널 ID)
  REPORT_DRY_RUN=1       Slack에 보내지 않고 출력만
  REPORT_FORCE=1         같은 날짜 리포트를 이미 보냈어도 다시 보낸다

같은 날짜 리포트가 채널에 이미 있으면 보내지 않는다. 모디 봇(09:00 정시 실행)과
GitHub 예약(예비, 늦게 도는 경우가 많음)이 둘 다 실행해도 한 번만 발송된다.
"""
import json
import os
import subprocess
import sys
import urllib.request
from datetime import date, timedelta

ACTIVE = "status = 'ACTIVE' AND deleted_at IS NULL"

# 하루(d) 단위 지표. {d}에 날짜 리터럴이 들어간다.
DAILY = {
    "signup":      f"SELECT COUNT(*) FROM member WHERE DATE(created_at) = {{d}}",
    "withdraw":    f"SELECT COUNT(*) FROM member WHERE DATE(deleted_at) = {{d}}",
    "dau":         f"""SELECT COUNT(DISTINCT member_id) FROM (
                         SELECT member_id, created_at FROM activity_record WHERE {ACTIVE}
                         UNION ALL SELECT member_id, created_at FROM weight_record WHERE {ACTIVE}
                         UNION ALL SELECT member_id, created_at FROM step_record WHERE {ACTIVE}
                       ) t WHERE DATE(created_at) = {{d}}""",
    "activity":    f"SELECT COUNT(*) FROM activity_record WHERE {ACTIVE} AND DATE(created_at) = {{d}}",
    "weight":      f"SELECT COUNT(*) FROM weight_record WHERE {ACTIVE} AND DATE(created_at) = {{d}}",
    "step":        f"SELECT COUNT(*) FROM step_record WHERE {ACTIVE} AND DATE(created_at) = {{d}}",
    "comment":     f"SELECT COUNT(*) FROM record_comment WHERE {ACTIVE} AND DATE(created_at) = {{d}}",
    "ch_created":  f"SELECT COUNT(*) FROM group_challenge WHERE {ACTIVE} AND DATE(created_at) = {{d}}",
    "ch_done":     "SELECT COUNT(*) FROM group_challenge WHERE DATE(completed_at) = {d}",
    "noti_sent":   "SELECT COUNT(*) FROM notification WHERE DATE(created_at) = {d}",
    "noti_failed": "SELECT COUNT(*) FROM notification WHERE DATE(created_at) = {d} AND delivery_status = 'FAILED'",
}

# 그날 마감(다음날 0시) 시점의 누적 지표. 전일 마감과 비교한다.
AS_OF = {
    "members":    """SELECT COUNT(*) FROM member
                     WHERE created_at < {d} + INTERVAL 1 DAY
                       AND (deleted_at IS NULL OR deleted_at >= {d} + INTERVAL 1 DAY)""",
    "groups":     """SELECT COUNT(*) FROM mody_group
                     WHERE created_at < {d} + INTERVAL 1 DAY
                       AND (deleted_at IS NULL OR deleted_at >= {d} + INTERVAL 1 DAY)""",
    "ch_running": """SELECT COUNT(*) FROM group_challenge
                     WHERE starts_on <= {d} AND ends_on >= {d}
                       AND (deleted_at IS NULL OR deleted_at >= {d} + INTERVAL 1 DAY)""",
    "wau":        f"""SELECT COUNT(DISTINCT member_id) FROM (
                         SELECT member_id, created_at FROM activity_record WHERE {ACTIVE}
                         UNION ALL SELECT member_id, created_at FROM weight_record WHERE {ACTIVE}
                         UNION ALL SELECT member_id, created_at FROM step_record WHERE {ACTIVE}
                       ) t WHERE created_at >= {{d}} - INTERVAL 6 DAY AND created_at < {{d}} + INTERVAL 1 DAY""",
}


def build_sql(target: date, prev: date) -> str:
    """모든 지표를 'key<TAB>value' 행으로 돌려주는 단일 SQL."""
    parts = []
    for key, q in {**DAILY, **AS_OF}.items():
        for suffix, d in (("", target), ("_prev", prev)):
            sql = " ".join(q.format(d=f"DATE '{d.isoformat()}'").split())
            parts.append(f"SELECT '{key}{suffix}', ({sql})")
    return "START TRANSACTION READ ONLY;\n" + "\nUNION ALL\n".join(parts) + ";\nROLLBACK;\n"


def fetch(sql: str) -> dict:
    target = os.environ.get("MODY_SSH_TARGET", "mody-prod")
    user = os.environ.get("REPORT_DB_USER", "grafana")
    password = os.environ["REPORT_DB_PASSWORD"]
    remote = ("docker exec -i mody-mysql sh -c "
              f"'read -r P; MYSQL_PWD=\"$P\" mysql -u{user} -N -B mody'")
    cmd = ["ssh", "-o", "BatchMode=yes", "-o", "StrictHostKeyChecking=accept-new"]
    if os.environ.get("MODY_SSH_KEY"):
        cmd += ["-i", os.environ["MODY_SSH_KEY"], "-o", "IdentitiesOnly=yes"]
    cmd += [target, remote]
    out = subprocess.run(cmd, input=password + "\n" + sql, capture_output=True,
                         text=True, timeout=180, check=True).stdout
    values = {}
    for line in out.strip().splitlines():
        key, _, val = line.partition("\t")
        values[key] = int(val or 0)
    return values


def t(text: str, bold: bool = False) -> dict:
    el = {"type": "text", "text": text}
    if bold:
        el["style"] = {"bold": True}
    return el


# 워크스페이스 커스텀 이모지 (빨간 ▲, 파란 ▼, 회색 –). 이미지는 Slack에 직접 업로드한다.
UP, DOWN, FLAT = "mody-up", "mody-down", "mody-flat"


def change(cur: int, prev: int) -> list:
    """증감 표시: 커스텀 이모지 + 부호 있는 숫자."""
    diff = cur - prev
    if diff > 0:
        return [{"type": "emoji", "name": UP}, t(f" +{diff:,}")]
    if diff < 0:
        return [{"type": "emoji", "name": DOWN}, t(f" −{-diff:,}")]
    return [{"type": "emoji", "name": FLAT}]


# ---- Slack rich text 조립 ----

def metric(label: str, prev: int, cur: int, unit: str) -> list:
    """`라벨   전일 → *오늘단위*   📈 +n`"""
    return [t(f"{label}　"), t(f"{prev:,} → "), t(f"{cur:,}{unit}", bold=True), t("　"), *change(cur, prev)]


def bullets(items: list, indent: int = 0) -> dict:
    return {"type": "rich_text_list", "style": "bullet", "indent": indent,
            "elements": [{"type": "rich_text_section", "elements": i} for i in items]}


def title(text: str) -> dict:
    return {"type": "rich_text_section", "elements": [t(text, bold=True)]}


def build_blocks(v: dict, target: date) -> list:
    prev = target - timedelta(days=1)
    wd = "월화수목금토일"
    m = lambda label, key, unit: metric(label, v[key + "_prev"], v[key], unit)
    sub = lambda label, key: [t(f"{label}　{v[key + '_prev']:,} → {v[key]:,}")]
    records = v["activity"] + v["weight"] + v["step"]
    records_prev = v["activity_prev"] + v["weight_prev"] + v["step_prev"]
    fail_rate = (100 * v["noti_failed"] / v["noti_sent"]) if v["noti_sent"] else 0.0
    gap = {"type": "rich_text_section", "elements": [t("\n")]}

    body = [
        title("핵심 지표"),
        bullets([
            m("👥 전체 회원", "members", "명"),
            m("🏠 활성 그룹", "groups", "개"),
            m("🌱 신규 가입", "signup", "명"),
            m("🔥 활동 사용자", "dau", "명"),
        ]),
        gap,
        title("활동"),
        bullets([metric("📝 기록", records_prev, records, "건")]),
        bullets([sub("활동", "activity"), sub("체중", "weight"),
                 sub("걸음수", "step"), sub("댓글", "comment")], indent=1),
        bullets([
            m("📅 주간 활동 사용자", "wau", "명"),
            m("👋 탈퇴", "withdraw", "명"),
        ]),
        gap,
        title("챌린지"),
        bullets([
            m("🏃 새 그룹 챌린지", "ch_created", "개"),
            m("✅ 완료", "ch_done", "개"),
            m("⏳ 진행 중", "ch_running", "개"),
        ]),
        gap,
        title("알림"),
        bullets([m("🔔 발송", "noti_sent", "건")]),
        bullets([[t(f"실패　{v['noti_failed']:,}건 ({fail_rate:.1f}%)")]], indent=1),
    ]

    return [
        {"type": "header", "text": {"type": "plain_text", "text": "📊 MODY 데일리 리포트"}},
        {"type": "context", "elements": [{"type": "mrkdwn", "text":
            f"*{target.year}.{target.month:02d}.{target.day:02d} ({wd[target.weekday()]})*　·　"
            f"{prev.month}/{prev.day}({wd[prev.weekday()]}) → {target.month}/{target.day}({wd[target.weekday()]})"}]},
        {"type": "divider"},
        {"type": "rich_text", "elements": body},
        {"type": "divider"},
        {"type": "context", "elements": [{"type": "mrkdwn", "text":
            "💬  보고 싶은 지표가 있으면 스레드에 남겨주세요. 데이터로 뽑을 수 있는 건 추가할게요."}]},
    ]


def builder_url(blocks: list) -> str:
    """Slack Block Kit Builder 미리보기 링크."""
    import urllib.parse
    return "https://app.slack.com/block-kit-builder/#" + urllib.parse.quote(
        json.dumps({"blocks": blocks}, ensure_ascii=False, separators=(",", ":")))


def report_text(target: date) -> str:
    """알림·검색에 쓰이는 메시지 본문. 중복 발송 판단의 기준이기도 하다."""
    return f"MODY 일일 리포트 {target.isoformat()}"


def slack_api(method: str, params: dict) -> dict:
    query = urllib.parse.urlencode(params)
    req = urllib.request.Request(f"https://slack.com/api/{method}?{query}", headers={
        "Authorization": f"Bearer {os.environ['SLACK_BOT_TOKEN']}",
    })
    return json.load(urllib.request.urlopen(req, timeout=30))


def already_posted(target: date) -> bool:
    """최근 메시지 중 같은 날짜 리포트가 있는지 본다."""
    res = slack_api("conversations.history", {"channel": os.environ["SLACK_REPORT_CHANNEL"], "limit": 50})
    if not res.get("ok"):
        sys.exit(f"Slack 이력 조회 실패: {res.get('error')}")
    return any(m.get("bot_id") and m.get("text", "") == report_text(target) for m in res.get("messages", []))


def post(blocks: list, target: date) -> None:
    body = json.dumps({
        "channel": os.environ["SLACK_REPORT_CHANNEL"],
        "text": report_text(target),
        "blocks": blocks,
        "unfurl_links": False,
    }).encode()
    req = urllib.request.Request("https://slack.com/api/chat.postMessage", data=body, headers={
        "Content-Type": "application/json; charset=utf-8",
        "Authorization": f"Bearer {os.environ['SLACK_BOT_TOKEN']}",
    })
    res = json.load(urllib.request.urlopen(req, timeout=30))
    if not res.get("ok"):
        sys.exit(f"Slack 발송 실패: {res.get('error')}")


def main() -> None:
    # 운영 DB와 Slack 모두 KST 기준. 러너는 UTC이므로 TZ=Asia/Seoul로 실행한다.
    target = date.fromisoformat(os.environ["REPORT_DATE"]) if os.environ.get("REPORT_DATE") \
        else date.today() - timedelta(days=1)
    sending = os.environ.get("REPORT_DRY_RUN") != "1" and os.environ.get("REPORT_PREVIEW") != "1"
    if sending and os.environ.get("REPORT_FORCE") != "1" and already_posted(target):
        print(f"report for {target.isoformat()} already posted, skip")
        return
    values = fetch(build_sql(target, target - timedelta(days=1)))
    blocks = build_blocks(values, target)
    if os.environ.get("REPORT_DRY_RUN") == "1":
        print(json.dumps(values, ensure_ascii=False, indent=2))
        return
    if os.environ.get("REPORT_PREVIEW") == "1":
        print(builder_url(blocks))
        return
    post(blocks, target)
    print(f"sent report for {target.isoformat()}")


if __name__ == "__main__":
    main()
