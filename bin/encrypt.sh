#!/bin/bash -p
###############################################################################
# Name  : encrypt.sh
#
###############################################################################
###############################################################################
#
# Copyright (C) S.W.I.F.T. sc. 2020. All rights reserved.
#
#
#  This software and its associated documentation contain
#  proprietary, confidential and trade secret information of
#  S.W.I.F.T. sc. and except as provided by written agreement
#  with S.W.I.F.T. sc.
#  a) no part may be disclosed, distributed, reproduced,
#     transmitted, transcribed, stored in a retrieval system,
#     adapted or translated in any form or by any means
#     electronic, mechanical, magnetic, optical, chemical,
#     manual or otherwise, and
#  b) the recipient is not entitled to discover through reverse
#     engineering or reverse compiling or other such techniques
#     or processes the trade secrets contained in the software
#     code or to use any such trade secrets contained therein or
#     in the documentation.
#
###############################################################################
#
#####################
# Main
#####################
#
echo "Encrypting Microgateway properties..."

MGW_PATH=`pwd`

MGW_PATH="$MGW_PATH/.."

echo $MGW_PATH

commandline1="java -cp"

commandline2="$MGW_PATH/libs/internal/file-encryptor-decryptor-2.17.0.jar"

commandline3="com.swift.encoder.FileEncryptor"

commandline4="$MGW_PATH/config/config-swift-connect.yaml"

commandline5="$MGW_PATH/config/config-swift-connect-secret.ks"

commandline6="$MGW_PATH/config/config-swift-connect-schema.json"

commandline7="config-swift-connect.enc"


if [ ! -f "$commandline2" ]; then
  echo 'EncryptorDecryptor jar file is missing'
  exit 1
fi


if [ ! -f "$commandline4" ] ; then
  echo 'config-swift-connect.yaml file is missing'
  exit 1
fi


commandLine="$commandline1 $commandline2 $commandline3 $commandline4 $commandline5 $commandline6 $commandline7"

$commandLine

echo "commandline = $commandLine"
