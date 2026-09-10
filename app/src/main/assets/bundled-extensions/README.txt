Bundled Extensions Directory
==============================

Place your extension APK files here (e.g., yt_music.apk, jio_saavn.apk, spotify.apk).

These extensions will be automatically installed on first app launch.
The installation is SILENT (no system "install unknown app" dialog) because
extensions are installed as File-type (ImportType.File) — they are just
copied to filesDir/extensions/<id>.apk and loaded by FileRepository.

To add a new bundled extension:
1. Place the APK file in this directory (app/src/main/assets/bundled-extensions/)
2. Optionally, add its filename to the `bundledExtensions` list in
   AutoExtensionInstaller.kt for explicit ordering. If the list is empty,
   the installer will automatically scan this directory for .apk files.
3. Build the app — on first launch, a popup will appear showing the
   installation progress with a slide + bouncy animation.

The extension ID is automatically parsed from the APK's package name.
Already-installed extensions are skipped automatically.
