# Local Android toolchain (cleanup note)

This machine now has a working local build+test loop, installed **user-locally** (no
root/sudo) inside `.toolchain/` so it can be deleted for cleanup. `local.properties`
points `sdk.dir` at it (machine-specific, gitignored).

| Piece | Path (relative) | Notes |
|---|---|---|
| JDK 17 (Temurin) | `.toolchain/jdk` | ~180MB |
| Android SDK | `.toolchain/android` | platforms;android-35, build-tools;35.0.0, platform-tools/adb |
| Gradle 8.11.1 | `.toolchain/gradle-8.11.1` | ~140MB |
| Gradle cache | `~/.gradle` | shared, ~1GB |

Optional / not yet installed: **NDK + Rust + cargo-ndk** (for the native speech/phostt
layer), several GB more.

## Build & test locally

```bash
cd /home/alex/Documents/Phở-netics
export JAVA_HOME=$PWD/.toolchain/jdk
export PATH=$JAVA_HOME/bin:$PATH
.toolchain/gradle-8.11.1/bin/gradle test --console=plain --no-daemon       # unit tests
.toolchain/gradle-8.11.1/bin/gradle assembleDebug --console=plain --no-daemon # APK
```

## From-scratch toolchain install (no root/sudo; all inside the project)

The local `.toolchain/` is NOT in git (gitignored) and was wiped on this machine, so if it
is missing you can recreate it exactly with:

```bash
cd /home/alex/Documents/Phở-netics
mkdir -p .toolchain && cd .toolchain

# 1) JDK 17 (Temurin)
curl -sSL -o jdk.tgz 'https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse'
mkdir -p jdk && tar -xzf jdk.tgz -C jdk --strip-components=1

# 2) Android command-line tools
curl -sSL -o cmdtools.zip 'https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip'
mkdir -p android && unzip -q cmdtools.zip -d android/
mv android/cmdline-tools android/cmt && mkdir -p android/cmdline-tools && mv android/cmt android/cmdline-tools/latest

# 3) Gradle 8.11.1
curl -sSL -o gradle.zip 'https://services.gradle.org/distributions/gradle-8.11.1-bin.zip'
unzip -q gradle.zip

# 4) Install SDK platform + build-tools (accept licenses)
export JAVA_HOME=$PWD/jdk && export PATH=$JAVA_HOME/bin:$PATH
export ANDROID_HOME=$PWD/android
yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --sdk_root=$ANDROID_HOME --licenses
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --sdk_root=$ANDROID_HOME \
  "platform-tools" "platforms;android-35" "build-tools;35.0.0"

# 5) Point Gradle at the SDK (gitignored)
cd /home/alex/Documents/Phở-netics
echo "sdk.dir=$PWD/.toolchain/android" > local.properties
```

Then run the "Build & test locally" block above. Cleanup: `rm -rf .toolchain local.properties`
(and optionally `~/.gradle`).

## Clean up at the end

- `rm -rf .toolchain`   (SDK + JDK + Gradle)
- `rm local.properties`
- optionally clear `~/.gradle` if no longer wanted
