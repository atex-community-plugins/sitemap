#!/bin/bash

function usage {
  echo "Usage: $scriptName url"
  exit 1  
}

function dumpUrl {
  d=$1
  if [[ "$OSTYPE" == "darwin"* ]]; then
    # Mac OSX
    dateParam=$(date -jf"%s" ${d} +"%Y-%m-%d")
  else
    dateParam=$(date -jf"%s" ${d} +"%Y-%m-%d")
  fi
  echo "${url}?date=${dateParam}"
}

scriptName=$(basename $0)
url=$1

if [ "$url" == "" ]; then
  echo "Missing 'url'"
  usage
fi

now=$(date +"%Y-%m")
if [[ "$OSTYPE" == "darwin"* ]]; then
  # Mac OSX
  from=$(date -jf"%Y-%m-%d" ${now}-01 +%s)
else
  from=$(date -jf"%Y-%m-%d" ${now}-01 +%s)
fi

dumpUrl ${from}
echo $url
