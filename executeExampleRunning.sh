#!/bin/bash
# -*- ENCODING: UTF-8 -*-

# If only LEADER_PROCESS_IP and LEADER_PROCESS_PORT arguments are provided then execute 4 instances in background that sort
# a global array of dimension 1000.
#
# If LEADER_PROCESS_IP, LEADER_PROCESS_PORT, GLOBAL_ARRAY_SIZE and PROCESS_QUANTITY arguments are provided then execute PROCESS_QUANTITY
# instances in background that sort a global array of dimension GLOBAL_ARRAY_SIZE.

echo "Begin example running"

NUMBER_OF_ARGUMENTS=$#

# If not arguments
if [ $NUMBER_OF_ARGUMENTS -eq 2 ]; then
	
	LEADER_PROCESS_IP=$1
	LEADER_PROCESS_PORT=$2

	java -jar modelPGAS.jar $LEADER_PROCESS_IP $LEADER_PROCESS_PORT 0 1000 4 &

	# Wait leader process init
	sleep 1

	for (( i = 1 ; i <= 3 ; i++ )); do
		java -jar modelPGAS.jar $LEADER_PROCESS_IP $LEADER_PROCESS_PORT $i 1000 4 &
	done
elif [ $NUMBER_OF_ARGUMENTS -eq 4 ]; then
	LEADER_PROCESS_IP=$1
	LEADER_PROCESS_PORT=$2
	GLOBAL_ARRAY_SIZE=$3
	PROCESS_QUANTITY=$4

	java -jar modelPGAS.jar $LEADER_PROCESS_IP $LEADER_PROCESS_PORT 0 $GLOBAL_ARRAY_SIZE $PROCESS_QUANTITY &

	# Wait leader process init
	sleep 1

	for (( i = 1 ; i < $PROCESS_QUANTITY ; i++ )); do
		java -jar modelPGAS.jar $LEADER_PROCESS_IP $LEADER_PROCESS_PORT $i $GLOBAL_ARRAY_SIZE $PROCESS_QUANTITY &
	done
else
	echo "Wrong number of parameters"
fi



