#!/bin/bash

# CHANGELOG     VERSION     BY                      COMMENT
# 2022-08-10    v1.0        ASOHAIL                 Added compile using JAVAC and improved the script a little bit.
# 2022-06-17    v0.1        ASOHAIL                 Implemented this entrypoint

# source logger from co.meta's github
source <(curl -s https://raw.githubusercontent.com/cometa-rocks/cometa/development/helpers/logger.sh)

info "====================="
info "CoCoMa Compile"
info "====================="

# build folder
BUILD_FOLDER="build"
# libraries folder
LIBRARIES_FOLDER="lib"
# create a temprary file to hold error messages
TMPFILE=`mktemp`

# cleanup function after exit
function cleanup() {
    # save last command exit code
    EXITCODE=$?
    info "Script has finished, cleaning up a littel bit."
    log_wfr "Removing temp file"
    rm ${TMPFILE} && log_res "[done]" || { log_res "[failed]"; error "Unable to delete tmp file: ${TMPFILE}"; }
    exit ${EXITCODE}
}

# shows version related information
function show_version_information() {
    # Get script version from changelog
    DATE=`cat $0 | grep -A1 CHANGELOG | head -n2 | tail -n1 | sed 's/# //g' | awk -F'  +' '{print $1}'`
    VERSION=`cat $0 | grep -A1 CHANGELOG | head -n2 | tail -n1 | sed 's/# //g' | awk -F'  +' '{print $2}'`
    AUTHOR=`cat $0 | grep -A1 CHANGELOG | head -n2 | tail -n1 | sed 's/# //g' | awk -F'  +' '{print $3}'`
    COMMIT=`cat $0 | grep -A1 CHANGELOG | head -n2 | tail -n1 | sed 's/# //g' | awk -F'  +' '{print $4}'`

    log_wfr "SCRIPT VERSION";               log_res "${VERSION}";
    log_wfr "LAST MODIFICATION DATE";       log_res "${DATE}";
    log_wfr "LAST MODIFIED BY";             log_res "${AUTHOR}";
    log_wfr "LAST MODIFICATION MESSAGE";    log_res "${COMMIT}";

    exit 0;
}

# trap exit
trap "cleanup" EXIT

# javac compile
function javac_compile() {
    # check if lib folder exists
    log_wfr "Checking if libraries folder exists or not"
    [[ -d ${LIBRARIES_FOLDER} ]] && log_res "[exists]" || { log_res "[${LIBRARIES_FOLDER} folder missing]"; exit 1; }

    log_wfr "Gettings all the JAR files from the libraries"
    # get all the JAR files from the libraries folder
    LIBRARIES=`find ${LIBRARIES_FOLDER}/ -name "*.jar" 2>${TMPFILE}` && log_res "[done]" || { log_res "[failed]"; error "Unable to get JAR files from the libraries folder .... please check the error below."; cat ${TMPFILE}; exit 2; }
    # debug all the JAR files in the libraries folder
    for LIBRARY in ${LIBRARIES}; do debug "JAR Found: ${LIBRARY}"; done;

    # check if build directory exists
    log_wfr "Checking if build directory exists"
    [[ -d ${BUILD_FOLDER} ]] && log_res "[exists]" || { log_res "[${BUILD_FOLDER} folder missing]"; log_info "Creating ${BUILD_FOLDER} folder"; mkdir ${BUILD_FOLDER} && log_res "[done]"; }

    # compile the code
    log_wfr "Compiling CoCoMa into build directory"
    javac -cp `echo ${LIBRARIES} | xargs | sed "s/ /:/g"` -d ${BUILD_FOLDER} `find src -name "*.java"` &>${TMPFILE} && log_res "[done]" || { log_res "[failed]"; error "Unable to compile CoCoMa code... please check the error below."; cat ${TMPFILE}; exit 3; }

    # create a manifest file
    log_wfr "Creating a manifest file"
    cat <<EOF > ${BUILD_FOLDER}/MANIFEST.MF && log_res "[done]"
Manifest-Version: 1.0
Created-By: AMVARA CONSULTING S.L.
Main-Class: com.dai.mif.cocoma.CoCoMa
EOF

    log_wfr "Adding Class-Path to the manifest file"
    echo "Class-Path: ${LIBRARIES}" | xargs | sed "s#${LIBRARIES_FOLDER}/#\n  ${LIBRARIES_FOLDER}/#2g" >> ${BUILD_FOLDER}/MANIFEST.MF && log_res "[done]"

    # cd into build dir
    log_wfr "Changing directory to build folder"
    cd ${BUILD_FOLDER} && log_res "[done]"

    # create a CoCoMa JAR file
    log_wfr "Creating CoCoMa JAR file"
    jar -cfm ../CoCoMa.jar MANIFEST.MF com &>${TMPFILE} && log_res "[done]" || { log_res "[failed]"; error "Unable to create CoCoMa JAR file... please check the error below."; cat ${TMPFILE}; exit 4; }

    cd - &>/dev/null
}


# ant compile
function ant_compile() {
    # go to tmp folder
    log_wfr "Changing directory to /tmp/"
    cd /tmp/ && log_res "[done]"
    # download ant binary files
    log_wfr "Downloading Apache Ant Binaries"
    wget https://dlcdn.apache.org//ant/binaries/apache-ant-1.9.16-bin.zip --no-check-certificate &>${TMPFILE} && log_res "[done]" || { log_res "[failed]"; error "Unable to download apache ant binaries ... please check the error below."; cat ${TMPFILE}; exit 5; }
    # unzip the downloaded file
    log_wfr "Unzipping Apache Ant Binaries"
    unzip apache-ant-1.9.16-bin.zip &>${TMPFILE} && log_res "[done]" || { log_res "[failed]"; error "Unable to unzip apache ant binaries ... please check the error below."; cat ${TMPFILE}; exit 6; }
    # cd back to original folder
    cd - &>/dev/null
    # compile
    log_wfr "Compiling using ant binary"
    /tmp/apache-ant-1.9.16/bin/ant -f ./ant/build.xml build-8.4.1 &>${TMPFILE} && log_res "[done]" || { log_res "[failed]"; error "Unable to compile using apache ant binary ... please check the error below."; cat ${TMPFILE}; exit 7; }
}

# help function
function help() {
    echo -ne "${1}

Usage:
${0} [-jc|-ant|-h|-v]

OPTIONS:
        -jc, --javac    -               Compile CoCoMa code using javac command.
                                        For the JAR file to work it needs to be placed with the lib folder.
                                        This compilation method is ~98% smaller in size than the ant compilation.
        -ant            -               Compile CoComa code using ant binary.
                                        This is a one big JAR that can be moved anywhere without copying libraries.
        -h              -               Shows this help message.
        -v              -               Shows version information about the script.

EXAMPLES:
        ${0} --javac
        ${0} -ant
";
        exit 10; # showing usage error code
}


## Loop over all the arguments
while [[ $# -gt 0 ]]; do
    case "$1" in
    -jc|--javac)
        javac_compile
        shift
        ;;
    -ant)
        ant_compile
        shift
        ;;
    -v|--version|version)
        show_version_information
        ;;
    -h|--help|-?|help)
        help "Here is how to use this script."
        ;;
    *)
        help "Unknown option (${ARGUMENT}) found."
        ;;
    esac
done