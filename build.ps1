$ErrorActionPreference = 'Continue'
$root = 'D:\android-build'
$jdk  = "$root\jdk\jdk-17.0.2"
$bt   = "$root\buildtools\android-14"
$ajar = "$root\platform\android-35\android.jar"
$app  = "$root\app"
$out  = "$root\out"

$env:JAVA_HOME = $jdk
$env:PATH = "$jdk\bin;$bt;$env:PATH"

foreach ($d in @($out, "$out\classes", "$out\dex", "$out\gen")) {
    if (Test-Path $d) { Remove-Item $d -Recurse -Force }
    New-Item -ItemType Directory -Force -Path $d | Out-Null
}

Write-Host '=== 1/7 aapt2 compile (res) ==='
& "$bt\aapt2.exe" compile --dir "$app\res" -o "$out\res.zip" 2>&1 | ForEach-Object { "  $_" }
if ($LASTEXITCODE -ne 0) { throw "aapt2 compile failed" }

Write-Host '=== 2/7 aapt2 link (+R.java) ==='
& "$bt\aapt2.exe" link -o "$out\base.apk" -I $ajar --manifest "$app\AndroidManifest.xml" -R "$out\res.zip" --java "$out\gen" --min-sdk-version 26 --target-sdk-version 34 --version-code 9 --version-name 2.1 --auto-add-overlay 2>&1 | ForEach-Object { "  $_" }
if ($LASTEXITCODE -ne 0) { throw "aapt2 link failed" }

Write-Host '=== 3/7 javac ==='
$srcs = Get-ChildItem "$app\src" -Recurse -Filter *.java | ForEach-Object { $_.FullName }
$rjava = Get-ChildItem "$out\gen" -Recurse -Filter R.java | ForEach-Object { $_.FullName }
& "$jdk\bin\javac.exe" -nowarn -source 8 -target 8 -bootclasspath $ajar -classpath $ajar -encoding UTF-8 -d "$out\classes" ($srcs + $rjava) 2>&1 | ForEach-Object { "  $_" }
if ($LASTEXITCODE -ne 0) { throw "javac failed" }

Write-Host '=== 4/7 d8 (dex) ==='
$classes = Get-ChildItem "$out\classes" -Recurse -Filter *.class | ForEach-Object { $_.FullName }
& "$bt\d8.bat" --release --min-api 26 --lib $ajar --output "$out\dex" $classes 2>&1 | ForEach-Object { "  $_" }
if ($LASTEXITCODE -ne 0) { throw "d8 failed" }

Write-Host '=== 5/7 pack classes.dex ==='
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::Open("$out\base.apk", 'Update')
$dex = "$out\dex\classes.dex"
if (Test-Path $dex) {
    $e = $zip.GetEntry('classes.dex')
    if ($e) { $e.Delete() }
    [void][System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, $dex, 'classes.dex', [System.IO.Compression.CompressionLevel]::Optimal)
}
$zip.Dispose()

Write-Host '=== 6/7 zipalign ==='
& "$bt\zipalign.exe" -f 4 "$out\base.apk" "$out\aligned.apk" 2>&1 | ForEach-Object { "  $_" }
if ($LASTEXITCODE -ne 0) { throw "zipalign failed" }

Write-Host '=== 7/7 sign ==='
$ks = "$root\pill.keystore"
if (-not (Test-Path $ks)) {
    & "$jdk\bin\keytool.exe" -genkeypair -v -keystore $ks -alias pill -keyalg RSA -keysize 2048 -validity 10000 -storepass android -keypass android -dname "CN=Pill Reminder, O=Ikun, C=CN" 2>&1 | Out-Null
}
& "$bt\apksigner.bat" sign --ks $ks --ks-key-alias pill --ks-pass pass:android --key-pass pass:android --out "$root\pill-reminder.apk" "$out\aligned.apk" 2>&1 | ForEach-Object { "  $_" }
if ($LASTEXITCODE -ne 0) { throw "apksigner failed" }

& "$bt\apksigner.bat" verify --verbose "$root\pill-reminder.apk" 2>&1 | Select-Object -First 5 | ForEach-Object { "  $_" }
$f = Get-Item "$root\pill-reminder.apk"
Write-Host ("APK: {0}  {1:N1} KB" -f $f.FullName, ($f.Length / 1KB))
Write-Host ("SHA256: {0}" -f (Get-FileHash $f.FullName -Algorithm SHA256).Hash)
