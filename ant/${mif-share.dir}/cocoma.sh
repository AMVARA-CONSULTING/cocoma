#!/bin/bash

jarName=./CoCoMa_cognos_1021.jar

if test -z "${1}"; then
	echo --------------------------------------------
	echo Christian Riedel, NOW! Consulting GmbH, 2010
	echo Ralf Roeber, AMVARA CONSULTING S.L., 2014
	echo --------------------------------------------
	echo
	echo "Usage: ${0} <ENVNAME>"
	echo
	echo example:
	echo ${0} D4E0
	echo will execute COCOMA_CONFIG_D4E0.xml
	exit 0
else
	config=COCOMA_CONFIG_${1}.xml
	if test -f ${config}; then
		command="java -jar ${jarName} --config ${config} --console"
		echo ${command}
		exec ${command}
		exit ?
	else
		echo "The config file ${config} does not exist."
		exit 1
	fi
fi
