from pathlib import Path

root = Path(__file__).resolve().parents[1]
manifest = root / "app/src/main/AndroidManifest.xml"
activity = root / "app/src/main/java/com/focusday/app/MainActivity.kt"
assert manifest.exists() and activity.exists()
assert 'android:icon="@mipmap/ic_launcher"' in manifest.read_text()
assert 'android:roundIcon="@mipmap/ic_launcher_round"' in manifest.read_text()
assert 'R.drawable.welcome_cat' in activity.read_text()
assert (root / "app/src/main/res/drawable-nodpi/welcome_cat.jpg").is_file()
for density in ("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi"):
    folder = root / f"app/src/main/res/mipmap-{density}"
    for name in ("ic_launcher.png", "ic_launcher_round.png"):
        assert (folder / name).is_file(), f"Missing {folder / name}"
print("FocusDay Android resources look correctly placed.")
