#!/bin/bash
# bellatrix: the phone's location, calendar, AirPods and battery as JSON,
# notifications and Android's share sheet, and the phone folders the user let
# Linux reach, for agents and scripts (see PhoneBridge.kt). Only what the launcher was already allowed; nothing here
# asks. Drops a request into the launcher's inbox and waits.
# /usr/local/bin/phone, its name until 0.20, runs this too.
set -u
# Elsewhere only for the guest CLI test.
inbox=${BELLATRIX_INBOX:-/run/user/0/phone}

# Asked for, it goes to stdout and exits 0; for wrong arguments, to stderr and exits 2.
usage() {
  local out=2 code=2
  [ "${1-}" = help ] && out=1 code=0
  cat >&$out <<USAGE
usage: bellatrix location       {lat, lon, accuracy (metres), time}, approximate
       bellatrix calendar [days]
                                  the events still to come from today on, 14 days
                                  unless given: [{id, eventId, title, start, end,
                                  allDay, calendar, color, location}]
       bellatrix airpods        the connected AirPods: batteries, mode, settings
       bellatrix airpods <control> [value]
                                  off, anc, transparency, adaptive, next;
                                  conversation, oneBud, earDetection [on|off];
                                  adaptiveLevel <0-100>
       bellatrix battery        {level, charging, status, plugged, saver,
                                  temp, voltage, health, technology, cycles}
       bellatrix notify <title> [text]
                                  a notification on the phone (tapped, it opens
                                  the home screen); prints nothing
       bellatrix notify --reply <title> [text]
                                  the same with a Reply button, waiting up to
                                  110 s for what the user types: {reply}; exits
                                  1 when it is dismissed or not answered
       bellatrix share <file>...
                                  Android's share sheet for the files, for the
                                  user to pick an app; prints nothing
       bellatrix folders        the phone folders you can reach: [{name}]; the
                                  user adds them in the launcher's settings
                                  (permissions)
       bellatrix apps           what's on home: {desktop, taskbar, layout}; desktop
                                  icons and pinned taskbar apps in the order
                                  shown, each {position, kind, name, target,
                                  label}; layout is the file of home's tiles
                                  and widgets
       bellatrix ls [folder[/path]]
                                  what's in one: [{name, dir, size, modified}]
       bellatrix get <folder/path>... <dest>
                                  copies phone files or folders into Linux, as
                                  cp -r does; prints the paths written
       bellatrix put <file>... <folder[/path]>
                                  copies Linux files or folders into a phone
                                  folder, as cp -r does, leaving out links inside
                                  a folder; prints the paths written
       bellatrix pick [dir]     Android's file picker, for the user to choose
                                  files from anywhere on the phone (Download
                                  too); copies them into dir (~/Downloads) and
                                  prints their paths
       bellatrix version        {version, versionCode, commands}

Words that start with - are kept for options; put -- before one meant as
a title, text or file (bellatrix notify -- "-5 degrees out").
/sdcard holds only files Linux put there itself (any in Download and
Documents); the rest of the phone's files are reached through the folders.
Times are Unix seconds. Prints JSON and exits 0, or says why not on stderr:
  1  nothing to say (no fix yet, no AirPods connected, no such file...)
  2  wrong arguments
  3  the launcher isn't allowed it: a widget showing it asks, or Android's
     settings for the app (notifications: the launcher's settings, permissions;
     a phone folder: the launcher's settings, permissions)
  4  the launcher isn't running or didn't answer
USAGE
  exit $code
}

# The launcher doesn't know where this runs: Linux paths go to it as absolute ones.
absolute() {
  case $1 in
    /*) printf '%s' "$1" ;;
    *) printf '%s' "$PWD/$1" ;;
  esac
}

[ $# -ge 1 ] || usage
case $1 in
  -h | --help | help) usage help ;;
  --version) set -- version ;;
esac
# Only notify has an option (--reply); any other given is refused, so adding some later breaks no script.
cmd=$1
shift
words=()
reply=
while [ $# -gt 0 ]; do
  case $1 in
    --)
      shift
      words+=("$@")
      break
      ;;
    -h | --help) usage help ;;
    --reply)
      if [ "$cmd" != notify ]; then
        echo "bellatrix: $cmd has no option $1 (put -- before a word that starts with -)" >&2
        exit 2
      fi
      reply=1
      ;;
    -?*)
      echo "bellatrix: $cmd has no option $1 (put -- before a word that starts with -)" >&2
      exit 2
      ;;
    *) words+=("$1") ;;
  esac
  shift
done
set -- "$cmd" ${words[@]+"${words[@]}"}
case $1 in
  location | battery | version | folders | apps) [ $# -eq 1 ] || usage ;;
  ls) [ $# -le 2 ] || usage ;;
  calendar) [ $# -le 2 ] || usage ;;
  airpods) [ $# -le 3 ] || usage ;;
  notify)
    [ $# -ge 2 ] && [ $# -le 3 ] && [ -n "$2" ] || usage
    # A fourth word tells the launcher to wait for a typed reply.
    [ -z "$reply" ] || set -- notify "$2" "${3-}" reply
    ;;
  share)
    [ $# -ge 2 ] || usage
    args=(share)
    for f in "${@:2}"; do
      if [ ! -f "$f" ]; then
        echo "bellatrix: share: $f isn't a file" >&2
        exit 2
      fi
      args+=("$(absolute "$f")")
    done
    set -- "${args[@]}"
    ;;
  get)
    [ $# -ge 3 ] || usage
    set -- "${@:1:$#-1}" "$(absolute "${!#}")"
    ;;
  put)
    [ $# -ge 3 ] || usage
    args=(put)
    for f in "${@:2:$#-2}"; do
      if [ ! -e "$f" ]; then
        echo "bellatrix: put: no $f" >&2
        exit 2
      fi
      args+=("$(absolute "$f")")
    done
    set -- "${args[@]}" "${!#}"
    ;;
  pick)
    [ $# -le 2 ] || usage
    [ $# -eq 1 ] || set -- pick "$(absolute "$2")"
    ;;
  *) usage ;;
esac
if [ ! -d "$inbox" ]; then
  echo "bellatrix: the launcher isn't listening; is it running?" >&2
  exit 4
fi

# bash's own sleep and rm, where it has them, rather than starting them as
# programs: each start is slow under proot.
fast=
enable -f /usr/lib/bash/sleep sleep 2>/dev/null && fast=1
enable -f /usr/lib/bash/rm rm 2>/dev/null

id="${EPOCHREALTIME/./}-$$"
# Each ended by a NUL: a notification's text can span lines. Written in place, not
# moved there: the launcher reads it once it's closed.
printf '%s\0' "$@" >"$inbox/$id.req"

# The launcher says it has the request (.ack) at once; then it is only slow (AirPods
# can take a while to link up), and waited for longer. Most answers come within
# milliseconds: looked for every 10 ms for the first half second, then every 100.
acked=
ms=0
while :; do
  if [ -f "$inbox/$id.done" ]; then
    reply=
    IFS= read -r -d '' reply <"$inbox/$id.done"
    reply=${reply%$'\n'}
    rm -f "$inbox/$id.done" "$inbox/$id.ack"
    case ${reply%%$'\n'*} in
      ok)
        [ "$reply" = ok ] || printf '%s\n' "${reply#*$'\n'}"
        exit 0
        ;;
      usage:*) code=2 ;;
      denied:*) code=3 ;;
      *) code=1 ;;
    esac
    echo "bellatrix: ${reply#*: }" >&2
    exit $code
  fi
  [ -n "$acked" ] || [ ! -f "$inbox/$id.ack" ] || acked=$ms
  if [ -z "$acked" ] && ((ms >= 5000)); then
    rm -f "$inbox/$id.req"
    echo "bellatrix: the launcher didn't take the request; is it running?" >&2
    exit 4
  fi
  if [ -n "$acked" ] && ((ms - acked >= 120000)); then
    rm -f "$inbox/$id.ack"
    echo "bellatrix: the launcher took the request but didn't answer in 2 minutes" >&2
    exit 4
  fi
  if [ -n "$fast" ] && ((ms < 500)); then
    sleep 0.01
    ((ms += 10))
  else
    sleep 0.1
    ((ms += 100))
  fi
done
