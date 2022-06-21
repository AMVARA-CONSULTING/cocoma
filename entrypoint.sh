#!/bin/bash

## NEEDS IMPROVEMENTS....


# CHANGELOG
# 2022-06-17    ASOHAIL     Implemented this entrypoint

# go to tmp folder
cd /tmp/
# download ant binary files
wget https://dlcdn.apache.org//ant/binaries/apache-ant-1.9.16-bin.zip
# unzip the downloaded file
unzip apache-ant-1.9.16-bin.zip
# cd back to original folder
cd -
# compile
/tmp/apache-ant-1.9.16/bin/ant -f ./ant/build.xml build-8.4.1