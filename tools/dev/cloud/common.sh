# Shared by the cloud scripts: repository root, work directory and a retrying downloader (Maven Central answers 429 under load).
C17_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../../.." && pwd)"
C17_WORK="${C17_CLOUD_WORK:-${TMPDIR:-/tmp}/c17-cloud}"
C17_CENTRAL="${C17_CENTRAL:-https://repo1.maven.org/maven2}"
mkdir -p "$C17_WORK"
c17_download() { # relative-maven-path destination
  local url="$C17_CENTRAL/$1" dest="$2" code i
  [ -s "$dest" ] && return 0
  mkdir -p "$(dirname "$dest")"
  for i in 1 2 3 4 5 6 7 8; do
    code=$(curl -sS -m 900 -o "$dest.part" -w '%{http_code}' "$url" 2>/dev/null)
    if [ "$code" = 200 ]; then mv "$dest.part" "$dest"; return 0; fi
    sleep 10
  done
  rm -f "$dest.part"; echo "download failed: $url (HTTP $code)" >&2; return 1
}
c17_quiet() { grep -v 'JAVA_TOOL_OPTIONS'; }
