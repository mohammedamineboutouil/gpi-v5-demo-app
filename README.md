# SWIFT GPI-Tracking and Pre-Validation APIs demo application in Java (SpringBoot and Picocli)

Make calls to SWIFT APIs is easy using SWIFT SDK. All you need to do is add the SWIFT SDKs as dependency when building
your Java application through Maven or Gradle.

We built this demo Java application with Maven to show you how we are using it to make calls to SWIFT GPI-Tracking and
Pre-Validation APIs in
the [API Sandbox](https://developer.swift.com/reference#gsg) using SWIFT Java SDK.

## Getting Started ##

### Prerequisites ###

* Java 17 and above
* Gradle 8.* and above
* Swift SDKs 2.17.* and above

### Install SWIFT SDK ###

Download [SWIFT SDK](https://developer.swift.com/swift-sdk) from SWIFT Developer Portal, login is required for download.

Unpackage the zip file and copy the dependency in your local [./libs](libs) repository.

```bash
export SWIFT_SDK_VERSION=2.17.5-1
unzip swift-sdk-$SWIFT_SDK_VERSION.zip
cp swift-sdk-$SWIFT_SDK_VERSION/lib/*.jar ./libs
```

Update the [gradle.properties](gradle.properties) SWIFT SDK Version `swiftSdkVersion=2.17.5-1`.

### Install SWIFT Security SDK ###

Download [SWIFT Security SDK](https://developer.swift.com/swift-sdk) from SWIFT Developer Portal, login is required for
download.

Unpackage the zip file and copy the dependency in your local [./libs](libs) repository.

```bash
export SWIFT_SDK_VERSION=2.17.4-6
unzip swift-security-sdk-$SWIFT_SDK_VERSION.zip
cp swift-security-sdk-$SWIFT_SDK_VERSION/lib/*.jar ./libs
```

Update the [gradle.properties](gradle.properties) SWIFT Security SDK Version `swiftSecuritySdkVersion=2.17.4-6`.

## Initial Setup ##

Before starting, please make sure you have updated the configuration attributes
in ```config/config-swift-connect.yaml``` with your application credentials, consumer-key & consumer-secret. Obtain
from SWIFT Developer Portal by [creating an app](https://developer.swift.com/reference#sandbox-getting-started).

### Encrypting the Configuration File

Here are the steps to encrypt your configuration YAML file:

1. Open your command line terminal.
2. Navigate to the `bin` directory where the scripts reside by running `cd bin`.
3. Run the encryption script:
    - On Linux: `./encrypt.sh`
    - On Windows: `encrypt.cmd`

The script will prompt you to enter a password for the secret file.

**Password rules for the secret file:**

- The password needs to be exactly 16 characters long.
- You can use alphanumeric characters, numbers, and special characters.
- Spaces are not allowed.

Upon execution, the script creates an encryption file and a secret file. The secret file is used for decryption, and the
original unencrypted configuration file is deleted from the disk.

### Decrypting the Configuration File

If you need to modify the encrypted configuration, you'll need to decrypt it first. Here's how:

1. Open your command line terminal.
2. Navigate to the `bin` directory by running `cd bin`.
3. Run the decryption script:
    - On Linux: `./decrypt.sh`
    - On Windows: `decrypt.cmd`

This will decrypt your encrypted configuration file, allowing you to modify it as needed.

**Remember**: After modifying your decrypted configuration file, make sure to encrypt it again before starting the
Microgateway application.

**IMPORTANT**: Keep your secret file and password secure. Do not share them, and avoid uploading them to version control
systems.

### Build ###

```bash
./gradlew clean build
```

### Run ###

```bash
java -Dswift.connect.config=file:config/config-swift-connect.enc \
     -Dswift.connect.secret=file:config/config-swift-connect-secret.ks \
      -jar build/libs/gpi-v5-demo-app-0.0.1.jar
```
