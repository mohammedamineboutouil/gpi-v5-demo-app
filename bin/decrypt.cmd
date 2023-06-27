:: !/bin/bash
:::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
:: Name  : decrypt.cmd
::
::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
::
:: Copyright (C) S.W.I.F.T. sc. 2020. All rights reserved.
::
::
::  This software and its associated documentation contain
::  proprietary, confidential and trade secret information of
::  S.W.I.F.T. sc. and except as provided by written agreement
::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::
::  with S.W.I.F.T. sc.
::  a) no part may be disclosed, distributed, reproduced,
::     transmitted, transcribed, stored in a retrieval system,
::     adapted or translated in any form or by any means
::     electronic, mechanical, magnetic, optical, chemical,
::     manual or otherwise, and
::  b) the recipient is not entitled to discover through reverse
::     engineering or reverse compiling or other such techniques
::     or processes the trade secrets contained in the software
::     code or to use any such trade secrets contained therein or
::     in the documentation.
::
::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

:::::::::::::::::::::
:: Main
:::::::::::::::::::::
@echo off

echo Decrypting Microgateway Properties...

SET mgwPath=%cd%\..

SET configPath=%mgwPath%\config\config-swift-connect.enc

SET classpath=%mgwPath%\lib\internal\file-encryptor-decryptor-2.17.0.jar

SET secretPath=%mgwPath%\config\config-swift-connect-secret.ks

SET decryptedFile=config-swift-connect.yaml

if not exist %classpath% (
   ECHO EncryptorDecryptor jar file is missing
   TIMEOUT /T 5
   exit 2
)


if not exist %configPath% (
   ECHO config-swift-connect.enc file is missing
   TIMEOUT /T 5
   exit 2
)

if not exist %secretPath% (
   ECHO config-swift-connect-secret.ks is missing
   TIMEOUT /T 5
   exit 2
)

java -cp  %classpath% com.swift.encoder.FileDecryptor %configPath% %secretPath% %decryptedFile%

TIMEOUT /T 10

pause