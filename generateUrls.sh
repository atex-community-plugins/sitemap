#!/bin/bash

function usage {
  echo "Usage: $scriptName url yyyy-mm"
  exit 1
}

function dumpUrl {
  d=$1
  if [[ "$OSTYPE" == "darwin"* ]]; then
    # Mac OSX
    dateParam=$(date -jf"%s" ${d} +"%Y-%m-%d")
  else
    dateParam=$(printf "%(%Y-%m-%d)T" "${d}")
  fi
  echo "${url}?date=${dateParam}"
}

scriptName=$(basename $0)
url=$1
from=$2

if [ "$url" == "" ]; then
  echo "Missing 'url'"
  usage
fi

if [ "$from" == "" ]; then
  echo "Missing 'from' date"
  usage
fi

now=$(date +%s)
if [[ "$OSTYPE" == "darwin"* ]]; then
  # Mac OSX
  from=$(date -jf"%Y-%m-%d" ${from}-01 +%s)
else
  from=$(date -d ${from}-01 +%s)
fi
if [ $? -ne 0 ]; then
  echo "Wrong date format"
  usage
fi

curDate=${from}
while true; do
  dumpUrl ${curDate}

  if [[ "$OSTYPE" == "darwin"* ]]; then
    # Mac OSX
    curDate=$(date -v+1m -jf"%s" ${curDate} +"%s")
  else
    curDate=$(printf "%(%F)T" "${curDate}")
    curDate=$(date -d "${curDate} 1 month" +"%s")
  fi

  if [[ $curDate -gt $now ]]; then
    break
  fi
done
